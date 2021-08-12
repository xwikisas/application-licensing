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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.SecurityReferenceFactory;
import org.xwiki.security.authorization.cache.SecurityCache;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.xar.XarEntry;

/**
 * Default implementation of the {@link LicensingSecurityCacheRuleInvalidator}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultLicensingSecurityCacheRuleInvalidator implements LicensingSecurityCacheRuleInvalidator
{
    private static final String WIKI_NAMESPACE = "wiki:";

    @Inject
    private SecurityCache securityCache;

    @Inject
    private SecurityReferenceFactory securityReferenceFactory;

    @Inject
    private DocumentReferenceResolver<EntityReference> documentReferenceResolver;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Logger logger;

    @Override
    public void invalidate(XarInstalledExtension extension)
    {
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
                logger.error("Failed to invalidate the cache for the extension [{}] on the farm", extension.getId(), e);
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
}
