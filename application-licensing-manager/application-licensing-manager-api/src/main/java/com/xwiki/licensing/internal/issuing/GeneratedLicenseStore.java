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
package com.xwiki.licensing.internal.issuing;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.BinaryStringEncoder;

import com.xwiki.licensing.FileLicenseStoreReference;
import com.xwiki.licensing.LicenseId;
import com.xwiki.licensing.LicenseStore;
import com.xwiki.licensing.LicenseStoreReference;
import com.xwiki.licensing.SignedLicense;

/**
 * The {@code generated-licenses} file store, where the {@code #generateLicense} macro of
 * {@code License.Code.LicenseDetailsMacros} keeps the signed licenses and where Sales downloads them from. The API
 * stores its licenses there too.
 *
 * @version $Id$
 * @since 1.34
 */
@Component(roles = GeneratedLicenseStore.class)
@Singleton
public class GeneratedLicenseStore
{
    /**
     * The name of the store, in the permanent directory.
     */
    static final String NAME = "generated-licenses";

    @Inject
    @Named("FileSystem")
    private LicenseStore licenseStore;

    @Inject
    @Named("Base64")
    private BinaryStringEncoder base64;

    @Inject
    private org.xwiki.environment.Environment environment;

    /**
     * @param license the signed license to store
     * @throws IOException if the license file can't be written
     */
    public void store(SignedLicense license) throws IOException
    {
        this.licenseStore.store(getReference(), license);
    }

    /**
     * Removes a license from the store. Used to undo a failed issuance.
     *
     * @param licenseId the license id
     */
    public void delete(LicenseId licenseId)
    {
        this.licenseStore.delete(getReference(), licenseId);
    }

    /**
     * @param license a signed license
     * @return the license key: the Base64 encoding of the signed license, on a single line
     * @throws IOException if the license can't be encoded
     */
    public String encode(SignedLicense license) throws IOException
    {
        return this.base64.encode(license.getEncoded());
    }

    private LicenseStoreReference getReference()
    {
        return new FileLicenseStoreReference(new File(this.environment.getPermanentDirectory(), NAME), true);
    }
}
