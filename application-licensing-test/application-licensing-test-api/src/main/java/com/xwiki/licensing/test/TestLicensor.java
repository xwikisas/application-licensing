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
package com.xwiki.licensing.test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.Licensor;

/**
 * Licensor implementation used when running the functional tests.
 *
 * @version $Id$
 * @since 1.21
 */
@Component
@Singleton
public class TestLicensor implements Licensor
{
    // This map is used for storing custom licenses (FREE, TRIAL, PAID) used for tests.
    private static final Map<String, License> CUSTOM_LICENSES = new ConcurrentHashMap<>();

    private final License freeLicense;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    public TestLicensor()
    {
        freeLicense = new License();
        freeLicense.setType(LicenseType.FREE);
    }

    @Override
    public boolean hasLicensure(EntityReference reference)
    {
        return true;
    }

    @Override
    public boolean hasLicensure(ExtensionId extensionId)
    {
        return true;
    }

    @Override
    public boolean hasLicensure(String extensionId)
    {
        return true;
    }

    @Override
    public boolean hasLicensure()
    {
        return true;
    }

    @Override
    public License getLicense()
    {
        return freeLicense;
    }

    @Override
    public License getLicense(ExtensionId extensionId)
    {
        License custom = CUSTOM_LICENSES.get(extensionId.toString());
        return custom != null ? custom : freeLicense;
    }

    @Override
    public License getLicense(EntityReference reference)
    {
        License custom = CUSTOM_LICENSES.get(this.serializer.serialize(reference));
        return custom != null ? custom : freeLicense;
    }
    
    @Override
    public License getLicense(String extensionId)
    {
        return CUSTOM_LICENSES.get(
            CUSTOM_LICENSES.keySet().stream().filter(key -> key.startsWith(extensionId)).findFirst().orElse(""));
    }

    public static void clearCustomLicenses()
    {
        CUSTOM_LICENSES.clear();
    }

    public License addLicense(ExtensionId extensionId, LicenseType licenseType)
    {
        License license = new License();
        license.setType(licenseType);
        CUSTOM_LICENSES.put(extensionId.toString(), license);

        return license;
    }

    public License addLicense(ExtensionId extensionId, LicenseType licenseType, int expirationDays, long maxUserCount)
    {
        License license = new License();
        license.setType(licenseType);
        license.setExpirationDate(System.currentTimeMillis() + (expirationDays * 24L * 60 * 60 * 1000));
        license.setMaxUserCount(maxUserCount);
        CUSTOM_LICENSES.put(extensionId.toString(), license);

        return license;
    }

    public License addLicense(EntityReference entityReference, LicenseType licenseType)
    {
        License license = new License();
        license.setType(licenseType);
        String entityString = this.serializer.serialize(entityReference);
        CUSTOM_LICENSES.put(entityString, license);

        return license;
    }

    public License addLicense(EntityReference entityReference, LicenseType licenseType, int expirationDays,
        long maxUserCount)
    {
        License license = new License();
        license.setType(licenseType);
        license.setExpirationDate(System.currentTimeMillis() + (expirationDays * 24L * 60 * 60 * 1000));
        license.setMaxUserCount(maxUserCount);
        String entityString = this.serializer.serialize(entityReference);
        CUSTOM_LICENSES.put(entityString, license);

        return license;
    }
}
