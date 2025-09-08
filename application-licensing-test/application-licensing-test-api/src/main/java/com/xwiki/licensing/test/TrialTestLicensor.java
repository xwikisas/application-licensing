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

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.model.reference.EntityReference;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.LicensedFeatureId;
import com.xwiki.licensing.Licensor;

/**
 * Trial licensor implementation for testing with a trial license.
 *
 * @version $Id$
 */
@Component
@Singleton
public class TrialTestLicensor implements Licensor
{
    private static final Map<String, License> TRIAL_LICENSES = new ConcurrentHashMap<>();

    private static volatile boolean noLicenseMode = false;

    private static volatile License currentTrialLicense = null;

    public static void setNoLicenseMode(boolean enabled)
    {
        noLicenseMode = enabled;
    }

    public static License addTrialLicense(ExtensionId extensionId, int expirationDays, long maxUserCount)
    {
        License license = createTrialLicense(expirationDays, maxUserCount);
        license.addFeatureId(new LicensedFeatureId(extensionId.getId()));

        TRIAL_LICENSES.put(extensionId.getId(), license);
        currentTrialLicense = license;

        return license;
    }

    public static void clearCurrentTrialLicense()
    {
        if (currentTrialLicense != null) {
            TRIAL_LICENSES.remove(currentTrialLicense.getFeatureIds().iterator().next().getId());
            currentTrialLicense = null;
        }
    }

    @Override
    public boolean hasLicensure(EntityReference reference)
    {
        if (noLicenseMode) {
            return false;
        }
        return true;
    }

    @Override
    public boolean hasLicensure(ExtensionId extensionId)
    {
        if (noLicenseMode) {
            return false;
        }
        return true;
    }

    @Override
    public boolean hasLicensure()
    {
        if (noLicenseMode) {
            return false;
        }
        return true;
    }

    @Override
    public License getLicense()
    {
        if (noLicenseMode) {
            return null;
        }
        return currentTrialLicense != null ? currentTrialLicense : createDefaultTrialLicense();
    }

    @Override
    public License getLicense(ExtensionId extensionId)
    {
        if (noLicenseMode) {
            return null;
        }
        return TRIAL_LICENSES.getOrDefault(extensionId.getId(),
            currentTrialLicense != null ? currentTrialLicense : createDefaultTrialLicense());
    }

    @Override
    public License getLicense(EntityReference reference)
    {
        if (noLicenseMode) {
            return null;
        }
        return TRIAL_LICENSES.getOrDefault(reference.toString(),
            currentTrialLicense != null ? currentTrialLicense : createDefaultTrialLicense());
    }

    private static License createTrialLicense(int expirationDays, long maxUserCount)
    {
        License license = new License();
        license.setType(LicenseType.TRIAL);
        license.setExpirationDate(System.currentTimeMillis() + (expirationDays * 24L * 60 * 60 * 1000));
        license.setMaxUserCount(maxUserCount);

        license.addLicenseeInfo(License.LICENSEE_FIRST_NAME, "Test");
        license.addLicenseeInfo(License.LICENSEE_LAST_NAME, "User");
        license.addLicenseeInfo(License.LICENSEE_EMAIL, "test@example.com");

        return license;
    }

    private static License createDefaultTrialLicense()
    {
        return createTrialLicense(30, 10L);
    }
}
