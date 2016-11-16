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
package com.xwiki.licensing.script;

import java.io.IOException;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseId;
import com.xwiki.licensing.LicenseStore;
import com.xwiki.licensing.LicenseStoreReference;

public class ScriptLicenseStore
{
    private LicenseStore licenseStore;

    private LicenseStoreReference licenseStoreReference;

    public ScriptLicenseStore(LicenseStore licenseStore, LicenseStoreReference licenseStoreReference)
    {
        this.licenseStore = licenseStore;
        this.licenseStoreReference = licenseStoreReference;
    }

    public License retrieve(LicenseId licenseId) throws IOException
    {
        return this.licenseStore.retrieve(this.licenseStoreReference, licenseId);
    }

    public void store(License license) throws IOException
    {
        this.licenseStore.store(this.licenseStoreReference, license);
    }
}
