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
import org.xwiki.extension.ExtensionId;
import org.xwiki.model.reference.EntityReference;

/**
 * The licensor component used while running functional tests for licensed applications.
 *
 * @version $Id$
 * @since 1.21
 */
@Role
public interface Licensor
{
    /**
     * Check if the given entity is covered by a valid license.
     *
     * @param reference the reference of the entity for which licensure should be checked.
     * @return true if the given reference has a valid license or is not subject to licensing.
     */
    boolean hasLicensure(EntityReference reference);

    /**
     * Check if the given extension is covered by a valid license.
     *
     * @param extensionId the identifier of the extension for which licensure should be checked.
     * @return true if the given extension has a valid license or is not subject to licensing.
     */
    boolean hasLicensure(ExtensionId extensionId);

    /**
     * Check if the given extension is covered by a valid license.
     *
     * @param extensionId the name of the extension for which licensure should be checked.
     * @return true if the given extension has a valid license or is not subject to licensing.
     */
    boolean hasLicensure(String extensionId);

    /**
     * @return true if the current document has a valid license or is not subject to licensing.
     */
    boolean hasLicensure();

    /**
     * Retrieve the currently applicable license for the current context document if any.
     *
     * @return a license, or null if there is no current document, or the current document is not subject to licensing.
     */
    License getLicense();

    /**
     * Retrieve the currently applicable license for the given installed extension.
     *
     * @param extensionId identifier of an installed extension
     * @return a license, or null if the given installed extension is not subject to licensing.
     */
    License getLicense(ExtensionId extensionId);

    /**
     * Get the license applicable to the given reference.
     *
     * @param reference the reference to get the license from.
     * @return a license, or null if the given reference is not subject to licensing.
     */
    License getLicense(EntityReference reference);

    /**
     * Retrieve the currently applicable license for the given installed extension.
     *
     * @param extensionId name of an installed extension. This method automatically resolves the version of the
     *     extension which is installed
     * @return a license, or null if the given installed extension is not subject to licensing.
     */
    License getLicense(String extensionId);

    /**
     * @param extensionId identifier of an installed extension
     * @param licenseType the type of License to add (FREE, TRIAL, PAID)
     * @return the newly created License
     */
    License addLicense(ExtensionId extensionId, LicenseType licenseType);

    /**
     * @param extensionId identifier of an installed extension
     * @param licenseType the type of License to add (FREE, TRIAL, PAID)
     * @param expirationDays the number of days until the license expires
     * @param maxUserCount the maximum number of users allowed under this license
     * @return the newly created License
     */
    License addLicense(ExtensionId extensionId, LicenseType licenseType, int expirationDays, long maxUserCount);

    /**
     * @param entityReference the reference of the entity for which the license applies
     * @param licenseType the type of License to add (FREE, TRIAL, PAID)
     * @return the newly created License
     */
    License addLicense(EntityReference entityReference, LicenseType licenseType);

    /**
     * @param entityReference the reference of the entity for which the license applies
     * @param licenseType the type of License to add (FREE, TRIAL, PAID)
     * @param expirationDays the number of days until the license expires
     * @param maxUserCount the maximum number of users allowed under this license
     * @return the newly created License
     */
    License addLicense(EntityReference entityReference, LicenseType licenseType, int expirationDays, long maxUserCount);
}
