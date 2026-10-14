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

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
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

    private static final String STORE_CONNECTION_FIELDSET = "#licenses-settings .licensing-store-connection";

    private static final By ONLINE_CHECKS_CHECKBOX =
        By.cssSelector(STORE_CONNECTION_FIELDSET + " input[type='checkbox']");

    @FindBy(id = "firstName")
    private WebElement firstNameInput;

    @FindBy(id = "lastName")
    private WebElement lastNameInput;

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "instanceId")
    private WebElement instanceIdInput;

    @FindBy(css = "#licenseManager .licenseButton-trial")
    private WebElement getTrialButton;

    @FindBy(css = "textarea#license")
    private WebElement licenseTextArea;

    @FindBy(css = "form#addLicense input[type='submit']")
    private WebElement addLicenseButton;

    @FindBy(id = "updateLicenses")
    private WebElement updateLicensesButton;

    @FindBy(css = "#licenses-settings input[name='formactionsac']")
    private WebElement saveSettingsButton;

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

    public LicensesAdminPage clickGetTrialButton()
    {
        this.getTrialButton.click();
        return this;
    }

    /**
     * Clicks the "Get Trial" button and waits for the request to be handled.
     *
     * @return the message of the notification displayed when the request is handled, with the {@code info} level
     */
    public String getTrialLicense()
    {
        clickGetTrialButton();
        return waitForInfoNotification();
    }

    /**
     * Clicks the "Check for License Updates" button and waits for the request to be handled.
     *
     * @return the message of the notification displayed when the request is handled, with the {@code info} level
     */
    public String checkForLicenseUpdates()
    {
        this.updateLicensesButton.click();
        return waitForInfoNotification();
    }

    /**
     * @return {@code true} if the "Enable online license checks" checkbox is displayed, {@code false} when it is
     *     replaced by a message explaining why online license checks are disabled
     */
    public boolean hasOnlineChecksOption()
    {
        return getDriver().hasElementWithoutWaiting(ONLINE_CHECKS_CHECKBOX);
    }

    public boolean isOnlineChecksEnabled()
    {
        return getDriver().findElementWithoutWaiting(ONLINE_CHECKS_CHECKBOX).isSelected();
    }

    public LicensesAdminPage setOnlineChecksEnabled(boolean enabled)
    {
        WebElement checkbox = getDriver().findElementWithoutWaiting(ONLINE_CHECKS_CHECKBOX);
        if (checkbox.isSelected() != enabled) {
            checkbox.click();
        }
        return this;
    }

    /**
     * @return the message displayed instead of the "Enable online license checks" checkbox
     */
    public String getOnlineChecksMessage()
    {
        return getDriver().findElementWithoutWaiting(By.cssSelector(STORE_CONNECTION_FIELDSET + " .infomessage"))
            .getText();
    }

    /**
     * Saves the settings form of the Licenses section (online license checks and notification settings).
     *
     * @return the Licenses section, reloaded after the save
     */
    public LicensesAdminPage saveSettings()
    {
        getDriver().addPageNotYetReloadedMarker();
        this.saveSettingsButton.click();
        getDriver().waitUntilPageIsReloaded();
        return new LicensesAdminPage();
    }

    public String addLicense(String license)
    {
        // The license is pretty long and it kills the browser if we generate keyboard events for each character.
        getDriver().executeScript("arguments[0].value = arguments[1]", this.licenseTextArea, license);
        this.addLicenseButton.click();

        // Wait for the Add License button to be re-enabled.
        getDriver().waitUntilCondition(ExpectedConditions.elementToBeClickable(this.addLicenseButton));

        // Get the notification message before it disappears.
        WebElement notificationElement = getDriver().findElementWithoutWaiting(By.cssSelector(".xnotification"));
        boolean successful = notificationElement.getAttribute("class").contains("xnotification-success");
        String notificationMessage = notificationElement.getText();
        hideNotification(notificationElement);

        if (successful) {
            // Wait for the live table to be reloaded.
            getLiveTable();
        }

        return notificationMessage;
    }

    private String waitForInfoNotification()
    {
        By infoNotification = By.cssSelector(".xnotification-info");
        getDriver().waitUntilElementIsVisible(infoNotification);
        WebElement notificationElement = getDriver().findElementWithoutWaiting(infoNotification);
        String notificationMessage = notificationElement.getText();
        hideNotification(notificationElement);
        return notificationMessage;
    }

    private void hideNotification(WebElement notificationElement)
    {
        // In order to improve test speed, clicking on the notification will make it disappear. This also ensures that
        // this method always waits for the last notification message of the specified level.
        try {
            // The notification message may disappear before we get to click on it.
            notificationElement.click();
        } catch (WebDriverException e) {
            // Ignore.
        }
    }
}
