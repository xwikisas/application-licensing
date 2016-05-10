package com.xwiki.licensing;

import org.xwiki.component.annotation.Role;

/**
 * Validate the applicability of a license.
 *
 * @version $Id$
 */
@Role
public interface LicenseValidator
{
    boolean isValid(License license);
}
