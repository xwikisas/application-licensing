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
import java.util.List;

import org.xwiki.component.annotation.Role;

/**
 * Configuration of the licensing module.
 *
 * @version $Id$
 */
@Role
public interface LicensingConfiguration
{
    /**
     * @return the configured path where to store licenses.
     */
    File getLocalStorePath();

    /**
     * Get the list of extensions that should be upgraded automatically.
     *
     * @return the list of extensions to be upgraded
     */
    List<String> getAutoUpgradeAllowlist();

    /**
     * Get the URL used in retrieving a trial license of an extension from store.
     *
     * @return the URL used in getting a trial license or null if the value of the property is not filled up
     */
    String getStoreTrialURL();

    /**
     * Get the URL used in updating the licenses.
     *
     * @return the store update URL for updating the licenses or null if the value of the property is not filled up
     */
    String getStoreUpdateURL();

    /**
     * @return the first name of the licensing owner or null if the value of the property is not filled up
     */
    String getLicensingOwnerFirstName();

    /**
     * @return the last name of the licensing owner or null if the value of the property is not filled up
     */
    String getLicensingOwnerLastName();

    /**
     * @return the email of the licensing owner or null if the value of the property is not filled up
     */
    String getLicensingOwnerEmail();
}
