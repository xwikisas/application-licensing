package com.xwiki.licensing.internal.enforcer;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;

import com.xwiki.licensing.License;

/**
 * Manage licenses.
 *
 * @version $Id$
 */
@Role
public interface EntityLicenseManager
{
    /**
     * Retrieve the license applicable for a given entity.
     *
     * @param reference the reference of the entity.
     * @return the best applicable license or NULL if no license need to be applied. If no license is available but one
     * should be applied, this method will return a License.UNLICENSED license.
     */
    License get(EntityReference reference);
}
