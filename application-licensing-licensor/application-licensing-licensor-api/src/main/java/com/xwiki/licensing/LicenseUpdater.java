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
import org.xwiki.stability.Unstable;

/**
 * Receive licenses updates from store or renew existing licenses if properties have changed.
 *
 * @version $Id$
 * @since 1.27
 */
@Role
@Unstable
public interface LicenseUpdater
{
    /**
     * Renew this extension's license for including new properties (e.g. new licensed feature ids after changed
     * dependencies). Request the license renewal to store and update the license locally as well.
     *
     * @param extensionId extension for which the license needs an update
     */
    void renewLicense(ExtensionId extensionId);

    /**
     * Retrieve licenses updates from the XWiki Store.
     */
    void updateLicenses();
}
