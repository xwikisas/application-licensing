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

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.environment.Environment;
import org.xwiki.properties.ConverterManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.stability.Unstable;

import com.xwiki.licensing.FileLicenseStoreReference;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseStore;
import com.xwiki.licensing.LicenseStoreReference;
import com.xwiki.licensing.SignedLicense;

/**
 * Script services related to Licensing Common API.
 *
 * @version $Id$
 * @since 1.1
 */
@Component
@Named("licensing.license")
@Singleton
@Unstable
public class LicenseScriptService implements ScriptService
{
    @Inject
    private ConverterManager converterManager;

    @Inject
    @Named("Base64")
    private BinaryStringEncoder encoder;

    @Inject
    private Environment environment;

    @Inject
    @Named("FileSystem")
    private LicenseStore filesystemLicenseStore;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    /**
     * @return an empty license object. Caller then needs to call setters on it to configure it.
     */
    public License createLicense()
    {
        return new License();
    }

    /**
     * @param filename the location where the licenses are stored on the filesystem (relative paths point inside the
     *        permanent directory, absolute paths can be anywhere on the machine).
     * @return a license store, configured to point to a path on the filesystem. Requires Programming Rights.
     */
    public ScriptLicenseStore getFileLicenseStore(String filename)
    {
        return getFileLicenseStore(filename, true);
    }

    /**
     * @param filename the location where the licenses are stored on the filesystem (relative paths point inside the
     *        permanent directory, absolute paths can be anywhere on the machine).
     * @param multi if true then the store can be store more than one license, false otherwise
     * @return a license store, configured to point to a path on the filesystem. Requires Programming Rights.
     */
    public ScriptLicenseStore getFileLicenseStore(String filename, boolean multi)
    {
        return new ScriptLicenseStore(this.filesystemLicenseStore, getFileLicenseStoreReference(filename, multi),
            this.contextualAuthorizationManager);
    }

    /**
     * @param license the license to encode as a string
     * @return the licensed encoded as a base64 string, and presented on lines of 64 characters long
     * @throws IOException when an encoding error occurs
     */
    public String encode(SignedLicense license) throws IOException
    {
        return this.encoder.encode(license.getEncoded(), 64);
    }

    private LicenseStoreReference getFileLicenseStoreReference(String filename, boolean multi)
    {
        File file;
        if (!filename.startsWith("/")) {
            file = new File(this.environment.getPermanentDirectory(), filename);
        } else {
            file = new File(filename);
        }

        return new FileLicenseStoreReference(file, multi);
    }
}
