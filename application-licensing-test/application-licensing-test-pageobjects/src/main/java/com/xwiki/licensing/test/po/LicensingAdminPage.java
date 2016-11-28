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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.administration.test.po.AdministrationSectionPage;
import org.xwiki.test.ui.po.LiveTableElement;

/**
 * Represents the action can that be done on the Licensing Admin UI page.
 *
 * @version $Id$
 */
public class LicensingAdminPage extends AdministrationSectionPage
{
    public static final String ADMINISTRATION_SECTION_ID = "Licenses";

    @FindBy(id = "firstName")
    private WebElement firstNameInput;

    @FindBy(id = "lastName")
    private WebElement lastNameInput;

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(xpath = "//button[contains(@class, 'licenseButton') and text() = 'Get Trial']")
    private WebElement getTrialButton;

    public static LicensingAdminPage gotoPage()
    {
        AdministrationSectionPage.gotoPage(ADMINISTRATION_SECTION_ID);
        return new LicensingAdminPage();
    }

    public LicensingAdminPage()
    {
        super(ADMINISTRATION_SECTION_ID);
    }

    public void setLicenseOwnershipDetails(String firstName, String lastName, String email)
    {
        this.firstNameInput.clear();
        this.firstNameInput.sendKeys(firstName);
        this.lastNameInput.clear();
        this.lastNameInput.sendKeys(lastName);
        this.emailInput.clear();
        this.emailInput.sendKeys(email);
    }

    public LiveTableElement getLiveTable()
    {
        LiveTableElement lt = new LiveTableElement("licenseManager");
        lt.waitUntilReady();
        return lt;
    }

    /**
     * Note: Need to wait till the page is refreshed
     */
    public LicensingAdminPage clickGetTrialButton()
    {
        this.getTrialButton.click();
        return new LicensingAdminPage();
    }
}
