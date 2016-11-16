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

import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.instance.InstanceId;
import org.xwiki.stability.Unstable;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.LicensedFeatureId;

/**
 * Entry point for License script services.
 *
 * @version $Id$
 * @since 1.1
 */
@Component
@Named("licensing.license")
@Singleton
@Unstable
public class LicenseScriptService extends AbstractLicenseScriptService
{
    public License createLicense(LicenseType licenseType, long expirationDate,
        List<LicensedFeatureId> featureIds, InstanceId instanceId, Map<String, String> licenseInfo)
    {
        License license = new License();
        license.setType(licenseType);
        for (LicensedFeatureId featureId : featureIds) {
            license.addFeatureId(featureId);
        }
        license.addInstanceId(instanceId);
        license.setExpirationDate(expirationDate);
        for (Map.Entry<String, String> info : licenseInfo.entrySet()) {
            license.addLicenseeInfo(info.getKey(), info.getValue());
        }
        return license;
    }

    public ScriptLicenseStore getFileLicenseStore(String filename, boolean multi)
    {
        return new ScriptLicenseStore(this.filesystemLicenseStore, getFileLicenseStoreReference(filename, multi));
    }
}
