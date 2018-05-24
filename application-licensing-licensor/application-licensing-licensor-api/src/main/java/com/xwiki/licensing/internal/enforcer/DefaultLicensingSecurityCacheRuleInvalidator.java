/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.licensing.internal.enforcer;

import java.util.Collection;
import java.util.concurrent.locks.ReadWriteLock;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.SecurityReferenceFactory;
import org.xwiki.security.authorization.cache.internal.SecurityCache;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.xar.XarEntry;

import com.xpn.xwiki.XWikiContext;

/**
 * Default implementation of the {@link LicensingSecurityCacheRuleInvalidator}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultLicensingSecurityCacheRuleInvalidator
    implements LicensingSecurityCacheRuleInvalidator, Initializable
{
    private static final String WIKI_NAMESPACE = "wiki:";

    private static final String SECURITY_CACHE_RULES_INVALIDATOR_HINT =
        "org.xwiki.security.authorization.internal.DefaultSecurityCacheRulesInvalidator";

    /**
     * Fair read-write lock to suspend the delivery of cache updates while there are loads in progress.
     */
    private ReadWriteLock readWriteLock;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private SecurityCache securityCache;

    @Inject
    private SecurityReferenceFactory securityReferenceFactory;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private DocumentReferenceResolver<EntityReference> documentReferenceResolver;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Logger logger;

    @Override
    public void invalidateAll()
    {
        // Check that the security factory has enough context
        // TODO: this is really fragile, should be improved by securing the factory itself
        if (xcontextProvider.get() == null) {
            return;
        }

        boolean locked = acquireLock();
        try {
            securityCache.remove(securityReferenceFactory.newEntityReference(null));
        } finally {
            if (locked && readWriteLock != null) {
                readWriteLock.writeLock().unlock();
            }
        }
    }

    @Override
    public void invalidate(XarInstalledExtension extension)
    {
        boolean locked = acquireLock();
        try {
            Collection<String> namespaces = extension.getNamespaces();
            if (namespaces != null) {
                for (String namespace : namespaces) {
                    if (namespace.startsWith(WIKI_NAMESPACE)) {
                        invalidateForWiki(extension, new WikiReference(namespace.substring(WIKI_NAMESPACE.length())));
                    }
                }
            } else {
                // As the extension is installed at farm level, we have to invalidate every cache entry for this
                // extension in every wiki.
                try {
                    for (String wikiId : wikiDescriptorManager.getAllIds()) {
                        invalidateForWiki(extension, new WikiReference(wikiId));
                    }
                } catch (WikiManagerException e) {
                    logger.error("Failed to invalidate the cache for the extension [{}] on the farm", extension.getId(),
                        e);
                }
            }
        } finally {
            if (locked && readWriteLock != null) {
                readWriteLock.writeLock().unlock();
            }
        }
    }

    /**
     * Invalidate every entity contained in the security cache coming from a specific XAR extension for a given wiki.
     *
     * @param extension the extension to invalidate
     * @param wikiReference the wiki to invalidate
     */
    private void invalidateForWiki(XarInstalledExtension extension, WikiReference wikiReference)
    {
        for (XarEntry entry : extension.getXarPackage().getEntries()) {
            securityCache.remove(
                securityReferenceFactory.newEntityReference(documentReferenceResolver.resolve(entry, wikiReference)));
        }
    }

    private boolean acquireLock()
    {
        // This class is used by the LicenseManager component at initialization time or when a new license is added
        // (more generally when the license cache is modified). However setting the write lock would block if the
        // Authorization Manager is currently loading security rules (DefaultSecurityCacheLoader#load() sets a read
        // lock preventing the write lock from being acquired and resulting in a dead lock).
        // Thus to prevent this we only acquire the lock if it's not already locked.

        if (readWriteLock != null) {
            boolean locked = readWriteLock.writeLock().tryLock();
            if (!locked) {
                this.logger.warn("Failed to acquire read lock in LicensingSecurityCacheRuleInvalidator. "
                    + "Continuing without lock.");
            }
            return locked;
        }
        return false;
    }

    /**
     * In XWiki 10.4 (XWIKI-15230) the DefaultSecurityCacheRulesInvalidatorLock component was removed and there's no
     * need to perform locking anymore. This is the reason for dynamically lookup the ReadWriteLock component.
     */
    @Override
    public void initialize() throws InitializationException
    {
        if (componentManager.hasComponent(ReadWriteLock.class, SECURITY_CACHE_RULES_INVALIDATOR_HINT)) {
            try {
                readWriteLock =
                    componentManager.getInstance(ReadWriteLock.class, SECURITY_CACHE_RULES_INVALIDATOR_HINT);
            } catch (ComponentLookupException e) {
                e.printStackTrace();
            }
        }
    }
}
