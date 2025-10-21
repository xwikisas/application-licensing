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
package com.xwiki.licensing;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

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

    /**
     * Check if a license is valid for a given user, its constrains are respected.
     * The instance constraint is NOT checked, see {@link #isApplicable(License)}
     *
     * @param license the license to be checked.
     * @param userReference the user to be checked.
     * @return true if the license is valid for the given user, false otherwise.
     */
    default boolean isValid(License license, DocumentReference userReference)
    {
        return isValid(license);
    }
}
