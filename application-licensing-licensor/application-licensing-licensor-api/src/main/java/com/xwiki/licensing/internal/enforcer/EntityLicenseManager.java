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
    License get(EntityReference reference);
}
