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

import java.util.concurrent.locks.ReadWriteLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.SecurityReferenceFactory;
import org.xwiki.security.authorization.cache.internal.SecurityCache;
import org.xwiki.xar.XarEntry;

import com.xpn.xwiki.XWikiContext;

/**
 * Default implementation of the {@link LicensingSecurityCacheRuleInvalidator}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultLicensingSecurityCacheRuleInvalidator implements LicensingSecurityCacheRuleInvalidator
{
    /**
     * Fair read-write lock to suspend the delivery of cache updates while there are loads in progress.
     */
    @Inject
    @Named("org.xwiki.security.authorization.internal.DefaultSecurityCacheRulesInvalidator")
    private ReadWriteLock readWriteLock;

    @Inject
    private SecurityCache securityCache;

    @Inject
    private SecurityReferenceFactory securityReferenceFactory;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private DocumentReferenceResolver<EntityReference> documentReferenceResolver;

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
            securityCache.remove(
                securityReferenceFactory.newEntityReference(null));
        } finally {
            if (locked) {
                readWriteLock.writeLock().unlock();
            }
        }
    }

    @Override
    public void invalidate(XarInstalledExtension extension)
    {
        boolean locked = acquireLock();
        try {
            for (String namespace : extension.getNamespaces()) {
                if (namespace.startsWith("wiki:")) {
                    WikiReference wikiRef = new WikiReference(namespace.substring(5));
                    for (XarEntry entry : extension.getXarPackage().getEntries()) {
                        securityCache.remove(
                            securityReferenceFactory.newEntityReference(
                                documentReferenceResolver.resolve(entry, wikiRef)
                            )
                        );
                    }
                }
            }
        } finally {
            if (locked) {
                readWriteLock.writeLock().unlock();
            }
        }
    }

    private boolean acquireLock()
    {
        // This class is used by the LicenseManager component at initialization time or when a new license is added
        // (more generally when the license cache is modified). However setting the write lock would block if the
        // Authorization Manager is currently loading security rules (DefaultSecurityCacheLoader#load() sets a read
        // lock preventing the write lock from being acquired and resulting in a dead lock).
        // Thus to prevent this we only acquire the lock if it's not already locked.
        boolean locked = readWriteLock.writeLock().tryLock();
        if (!locked) {
            this.logger.warn("Failed to acquire read lock in LicensingSecurityCacheRuleInvalidator. "
                + "Continuing without lock.");
        }
        return locked;
    }
}
