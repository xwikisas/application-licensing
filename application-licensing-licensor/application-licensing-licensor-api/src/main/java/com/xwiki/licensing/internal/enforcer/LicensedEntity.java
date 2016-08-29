package com.xwiki.licensing.internal.enforcer;

import com.xwiki.licensing.License;

/**
 * Simple interface that allow getting the license of a licenced entity.
 *
 * @version $Id$
 */
public interface LicensedEntity
{
    /**
     * @return the license applicable on this entity or NULL if no license should be applied. If no license is
     * available but one should be applied, this method will return a License.UNLICENSED license.
     */
    License getLicense();
}
