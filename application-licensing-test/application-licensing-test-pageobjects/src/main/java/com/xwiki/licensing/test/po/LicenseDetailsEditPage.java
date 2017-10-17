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
package com.xwiki.licensing.test.po;

import org.xwiki.test.ui.po.InlinePage;

/**
 * The form used to add and edit the license details.
 * 
 * @version $Id$
 * @since 1.6
 */
public class LicenseDetailsEditPage extends InlinePage
{
    @Override
    @SuppressWarnings("unchecked")
    protected LicenseDetailsViewPage createViewPage()
    {
        return new LicenseDetailsViewPage();
    }

    public LicenseDetailsEditPage setLicenseeFirstName(String firstName)
    {
        setValue("firstName", firstName);
        return this;
    }

    public LicenseDetailsEditPage setLicenseeLastName(String lastName)
    {
        setValue("lastName", lastName);
        return this;
    }

    public LicenseDetailsEditPage setLicenseeEmail(String email)
    {
        setValue("email", email);
        return this;
    }

    public LicenseDetailsEditPage setInstanceId(String instanceId)
    {
        setValue("instanceId", instanceId);
        return this;
    }

    public LicenseDetailsEditPage setExtensionName(String extensionName)
    {
        setValue("extensionName", extensionName);
        return this;
    }

    public LicenseDetailsEditPage setExtensionId(String extensionId)
    {
        setValue("featureId", extensionId);
        return this;
    }

    public LicenseDetailsEditPage setDependentExtensionIds(String dependentExtensionIds)
    {
        setValue("dependentFeatureIds", dependentExtensionIds);
        return this;
    }

    public LicenseDetailsEditPage setUserLimit(String userLimit)
    {
        setValue("maxUserCount", userLimit);
        return this;
    }

    public LicenseDetailsEditPage setRequestDate(String requestDate)
    {
        setValue("requestDate", requestDate);
        return this;
    }

    public LicenseDetailsEditPage setLicenseType(String type)
    {
        getDriver().findElementByXPath(
            "//input[@name = 'License.Code.LicenseDetailsClass_0_type' and @value = '" + type + "']").click();
        return this;
    }
}
