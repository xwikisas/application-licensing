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

import java.util.Date;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.administration.test.po.AdministrationSectionPage;
import org.xwiki.test.ui.po.LiveTableElement;

/**
 * Represents the actions that can be done on the "Licenses" section from the administration.
 *
 * @version $Id$
 * @since 1.6
 */
public class LicensesAdminPage extends AdministrationSectionPage
{
    public static final String ADMINISTRATION_SECTION_ID = "Licenses";

    @FindBy(id = "firstName")
    private WebElement firstNameInput;

    @FindBy(id = "lastName")
    private WebElement lastNameInput;

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "instanceId")
    private WebElement instanceIdInput;

    @FindBy(xpath = "//button[contains(@class, 'licenseButton') and . = 'Get Trial']")
    private WebElement getTrialButton;

    @FindBy(css = "textarea#license")
    private WebElement licenseTextArea;

    @FindBy(css = "form#addLicense input[type='submit']")
    private WebElement addLicenseButton;

    public static LicensesAdminPage gotoPage()
    {
        AdministrationSectionPage.gotoPage(ADMINISTRATION_SECTION_ID);
        return new LicensesAdminPage();
    }

    public LicensesAdminPage()
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

    public String getInstanceId()
    {
        return this.instanceIdInput.getAttribute("value");
    }

    public LiveTableElement getLiveTable()
    {
        LiveTableElement liveTable = new LiveTableElement("licenseManager");
        liveTable.waitUntilReady();
        return liveTable;
    }

    /**
     * Note: Need to wait till the page is refreshed.
     */
    public LicensesAdminPage clickGetTrialButton()
    {
        this.getTrialButton.click();
        return new LicensesAdminPage();
    }

    public void addLicense(String license)
    {
        String markerId = "pageNotYetReloadedMarker" + new Date().getTime();
        addPageNotYetReloadedMarker(markerId);

        // The license is pretty long and it kills the browser if we generate keyboard events for each character.
        getDriver().executeScript("arguments[0].value = arguments[1]", this.licenseTextArea, license);
        this.addLicenseButton.click();

        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver input)
            {
                // Wait until either the page is reloaded (license added successfully) or an error message appears.
                return !getDriver().hasElementWithoutWaiting(By.id(markerId))
                    || getDriver().hasElementWithoutWaiting(By.cssSelector(".codeToExecute .box.errormessage"));
            }
        });
    }

    public String getErrorMessage()
    {
        return getDriver().findElementWithoutWaiting(By.cssSelector(".codeToExecute .box.errormessage")).getText();
    }

    private void addPageNotYetReloadedMarker(String id)
    {
        StringBuilder markerScript = new StringBuilder();
        markerScript.append("new function (markerId) {");
        markerScript.append("  var marker = document.createElement('div');");
        markerScript.append("  marker.style.display = 'none';");
        markerScript.append("  marker.id = markerId;");
        markerScript.append("  document.body.appendChild(marker);");
        markerScript.append("}(arguments[0])");

        getDriver().executeScript(markerScript.toString(), id);
    }
}
