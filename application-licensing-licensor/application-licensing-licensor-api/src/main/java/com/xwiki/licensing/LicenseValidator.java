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
    /**
     * Check if a license is valid, meaning that it is properly signed, and its constrains are respected.
     * The instance constraint is NOT checked.
     *
     * @param license the license to be checked.
     * @return true if the license is valid, false otherwise.
     */
    boolean isValid(License license);
}
