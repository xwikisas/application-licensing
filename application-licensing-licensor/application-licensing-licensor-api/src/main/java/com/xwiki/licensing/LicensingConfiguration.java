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

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.LocalDocumentReference;

/**
 * Configuration of the licensing module.
 *
 * @version $Id$
 */
@Role
public interface LicensingConfiguration
{
    /**
     * Licensing Code space.
     */
    List<String> CODE_SPACE = Arrays.asList("Licenses", "Code");

    /**
     * Licensing configuration document.
     */
    LocalDocumentReference LICENSING_CONFIG_DOC = new LocalDocumentReference(CODE_SPACE, "LicensingConfig");

    /**
     * @return the configured path where to store licenses.
     */
    File getLocalStorePath();

    /**
     * Get the list of extensions that should not be upgraded automatically.
     *
     * @return the list of blocklisted extensions for upgrade
     */
    List<String> getAutoUpgradeBlocklist();

    /**
     * @return the store trial url
     */
    String getStoreTrialURL();

    /**
     * @return the store update url
     */
    String getStoreUpdateURL();

    /**
     * @return the first name of the licensing owner
     */
    String getLicensingOwnerFirstName();

    /**
     * @return the last name of the licensing owner
     */
    String getLicensingOwnerLastName();

    /**
     * @return the email of the licensing owner
     */
    String getLicensingOwnerEmail();
}
