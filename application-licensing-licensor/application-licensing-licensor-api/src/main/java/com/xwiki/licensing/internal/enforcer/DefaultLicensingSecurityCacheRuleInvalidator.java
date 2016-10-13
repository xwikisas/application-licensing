package com.xwiki.licensing.internal.enforcer;

import java.util.concurrent.locks.ReadWriteLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

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

    @Override
    public void invalidateAll()
    {
        // Check that the security factory has enough context
        // TODO: this is really fragile, should be improved by securing the factory itself
        if (xcontextProvider.get() == null) {
            return;
        }
        
        readWriteLock.writeLock().lock();
        try {
            securityCache.remove(
                securityReferenceFactory.newEntityReference(null));
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public void invalidate(XarInstalledExtension extension)
    {
        readWriteLock.writeLock().lock();
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
            readWriteLock.writeLock().unlock();
        }
    }
}
