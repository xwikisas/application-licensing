package com.xwiki.licensing.internal.enforcer;

import org.xwiki.component.annotation.Role;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;

/**
 * Security cache rule invalidator for licensed extensions.
 *
 * @version $Id$
 */
@Role
public interface LicensingSecurityCacheRuleInvalidator
{
    /**
     * Invalidate the whole security cache.
     */
    void invalidateAll();

    /**
     * Invalidate rules matching documents installed by this extension.
     *
     * @param extension the extension.
     */
    void invalidate(XarInstalledExtension extension);
}
