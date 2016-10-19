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
     * A LicenseValidator that always reply false. Used when the license validator received is not pristine.
     */
    LicenseValidator INVALIDATOR = new LicenseValidator() {
        @Override
        public boolean isApplicable(License license)
        {
            return false;
        }

        @Override
        public boolean isSigned(License license)
        {
            return false;
        }

        @Override
        public boolean isValid(License license)
        {
            return false;
        }
    };

    /**
     * Check if a license is applicable to this wiki.
     *
     * @param license the license to be checked.
     * @return true if the license is applicable, false otherwise.
     */
    boolean isApplicable(License license);

    /**
     * Check if a license is properly signed.
     *
     * @param license the license to be checked.
     * @return true if the license is properly signed, false otherwise.
     */
    boolean isSigned(License license);

    /**
     * Check if a license is valid, its constrains are respected.
     * The instance constraint is NOT checked, see {@link #isApplicable(License)}
     *
     * @param license the license to be checked.
     * @return true if the license is valid, false otherwise.
     */
    boolean isValid(License license);
}
