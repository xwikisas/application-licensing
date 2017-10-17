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

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.FindBys;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the notification message shown when the license required by an extension is missing, has expired or is
 * invalid for any reason.
 * 
 * @version $Id$
 * @since 1.6
 */
public class LicenseNotificationPane extends BaseElement
{
    @FindBy(css = "li.notifications-payingapps-item a")
    private WebElement licensesSectionLink;

    @FindBys({@FindBy(css = "li.notifications-payingapps-item li")})
    private List<WebElement> extensionListItems;

    public LicensesAdminPage clickLicensesSectionLink()
    {
        this.licensesSectionLink.click();
        return new LicensesAdminPage();
    }

    public List<String> getExtensions()
    {
        List<String> extensions = new ArrayList<>();
        for (WebElement extensionListItem : this.extensionListItems) {
            extensions.add(extensionListItem.getText());
        }
        return extensions;
    }
}
