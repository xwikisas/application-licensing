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
package com.xwiki.licensing.test.ui;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;
import org.xwiki.text.StringUtils;

import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.test.po.LicenseDetailsEditPage;
import com.xwiki.licensing.test.po.LicenseDetailsViewPage;
import com.xwiki.licensing.test.po.LicenseNotificationPane;
import com.xwiki.licensing.test.po.LicensesAdminPage;
import com.xwiki.licensing.test.po.LicensesHomePage;

import static org.junit.Assert.assertEquals;

/**
 * Functional tests for the Licensing application.
 *
 * @version $Id$
 */
@UITest
class LicensingIT
{
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    private String instanceId;

    @BeforeEach
    void configure(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();

        // Generate Certificates and Keys.

        LocalDocumentReference generateCertificatesAndKeys =
            new LocalDocumentReference(Arrays.asList("License", "Code"), "GenerateCertificatesAndKeys");
        if (!setup.rest().exists(generateCertificatesAndKeys)) {
            try (InputStream content = getClass().getResourceAsStream("/GenerateCertificatesAndKeys.wiki")) {
                setup.rest().savePage(generateCertificatesAndKeys,
                    IOUtils.toString(content, StandardCharsets.UTF_8), "");
            }

            setup.gotoPage(generateCertificatesAndKeys, "view", "proceed=1");
        }

        // Use the generated certificates.

        LocalDocumentReference useCertificates =
            new LocalDocumentReference(Arrays.asList("License", "Code"), "UseCertificates");
        if (!setup.rest().exists(useCertificates)) {
            try (InputStream content = getClass().getResourceAsStream("/UseCertificates.wiki")) {
                setup.rest().savePage(useCertificates, IOUtils.toString(content, StandardCharsets.UTF_8), "");
            }
        }

        setup.gotoPage(useCertificates, "view", "proceed=1");
        assertEquals("OK", new ViewPage().getContent());

        // Configure the store URLs (buy and trial) to point to the current wiki.

        setup.updateObject(Arrays.asList("Licenses", "Code"), "LicensingConfig",
            "Licenses.Code.LicensingStoreClass", 0, "storeTrialURL",
            "http://localhost:8080/xwiki/bin/get/Store/GetTrialLicense", "storeBuyURL",
            "http://localhost:8080/xwiki/bin/view/Store/BuyLicense");
    }

    @Test
    void generateLicense(TestReference testReference, TestUtils setup) throws Exception
    {
        // Verify that there's no license for Example.WebHome
        ViewPage viewPage = setup.gotoPage("Example", "WebHome");
        // The superadmin user can still see the page.
        assertEquals("Missing license", viewPage.getContent());
        // The rest of the users are not allowed view it.
        setup.createUserAndLoginWithRedirect("alice", "test", setup.getURL("Example", "WebHome"));
        assertEquals("You are not allowed to view this page or perform this action.",
            setup.getDriver().findElementByCssSelector("p.xwikimessage").getText());

        // Verify that the public page is accessible without a license.
        setup.gotoPage("Example", "ApplicationsPanelEntry");
        assertEquals("No license is needed to view this page.", viewPage.getContent());

        // Verify that the excluded page is editable without a license.
        setup.gotoPage("Example", "Config", "edit", "editor=wiki&force=true");
        assertEquals("No license is needed to edit this page.", new WikiEditPage().getContent());

        // Verify the notification for the missing license.
        LicenseNotificationPane notification = new LicenseNotificationPane();
        // The simple users should not see the notification.
        setup.gotoPage(testReference);
        viewPage.toggleNotificationsMenu();
        assertEquals(0, notification.getExtensions().size());

        // Users with administration rights should see it though.
        setup.loginAsSuperAdminAndGotoPage(setup.getURL(testReference, "view", null));
        viewPage.toggleNotificationsMenu();
        assertEquals(Collections.singletonList("Licensed Application Example"), notification.getExtensions());

        // Navigate to the Licenses administration section.
        LicensesAdminPage licensesAdminSection = notification.clickLicensesSectionLink();

        // Initially expiration date should display "No license available".
        LiveTableElement liveTable = licensesAdminSection.getLiveTable();
        assertEquals(1, liveTable.getRowCount());
        WebElement firstRow = liveTable.getRow(1);
        assertEquals("No license available", liveTable.getCell(firstRow, 3).getText());
        assertEquals("Support level should not be specified.", "-", liveTable.getCell(firstRow, 4).getText());
        assertEquals("User limit should not be specified.", "-", liveTable.getCell(firstRow, 5).getText());

        // Import an invalid license.
        assertEquals("Failed! The provided license could not be decoded. Please contact sales@xwiki.com.",
            licensesAdminSection.addLicense("foo"));

        // Import a license that is not meant for the current XWiki instance.
        try (InputStream incompatibleLicense = getClass().getResourceAsStream("/incompatible.license")) {
            assertEquals("Failed! License is not compatible or useful for your server. Please contact sales@xwiki.com",
                licensesAdminSection.addLicense(IOUtils.toString(incompatibleLicense, StandardCharsets.UTF_8)));
        }

        this.instanceId = licensesAdminSection.getInstanceId();

        // Generate and import an expired license with gold support and unspecified number of users.
        addLicense(LicenseType.FREE, "21/06/2017", "gold", "", setup);

        // Check the license live table.
        assertEquals(1, liveTable.getRowCount());
        firstRow = liveTable.getRow(1);
        assertEquals("21/06/2017", liveTable.getCell(firstRow, 3).getText());
        assertEquals("Gold", liveTable.getCell(firstRow, 4).getText());
        assertEquals("-", liveTable.getCell(firstRow, 5).getText());

        // Check the license notification message.
        // We need to refresh the page in order for the notification menu to be updated.
        setup.getDriver().navigate().refresh();
        licensesAdminSection.toggleNotificationsMenu();
        assertEquals(Collections.singletonList("Licensed Application Example"), notification.getExtensions());

        // Generate and import a license for 0 users and unspecified support level.
        addLicense(LicenseType.TRIAL, null, "", "0", setup);

        // Check the license live table.
        assertEquals(1, liveTable.getRowCount());
        firstRow = liveTable.getRow(1);
        assertEquals(DATE_FORMAT.format(new DateTime().plusDays(11).toDate()),
            liveTable.getCell(firstRow, 3).getText());
        assertEquals("-", liveTable.getCell(firstRow, 4).getText());
        assertEquals("1 / 0", liveTable.getCell(firstRow, 5).getText());

        // Check the license notification message.
        // We need to refresh the page in order for the notification menu to be updated.
        licensesAdminSection.toggleNotificationsMenu();
        assertEquals(Collections.singletonList("Licensed Application Example"), notification.getExtensions());

        // Generate and import a license for unlimited users
        addLicense(LicenseType.PAID, null, "silver", "-1", setup);

        // Check the license live table.
        assertEquals(1, liveTable.getRowCount());
        firstRow = liveTable.getRow(1);
        assertEquals(DATE_FORMAT.format(new DateTime().plusDays(365).toDate()),
            liveTable.getCell(firstRow, 3).getText());
        assertEquals("Silver", liveTable.getCell(firstRow, 4).getText());
        assertEquals("Unlimited", liveTable.getCell(firstRow, 5).getText());

        // Verify that the Example page now has a license.
        viewPage = setup.gotoPage("Example", "WebHome");
        assertEquals("Hello", viewPage.getContent());

        // Check the license notification message.
        licensesAdminSection.toggleNotificationsMenu();
        assertEquals(0, notification.getExtensions().size());

        // Try also with a simple user.
        setup.loginAndGotoPage("alice", "test", setup.getURL("Example", "WebHome"));
        assertEquals("Hello", viewPage.getContent());
    }

    private void addLicense(LicenseType type, String expirationDate, String support, String userLimit, TestUtils setup)
    {
        LicenseDetailsEditPage licenseDetails = LicensesHomePage.gotoPage().clickAddLicenseDetails();
        licenseDetails.setLicenseeFirstName("John").setLicenseeLastName("Doe").setLicenseeEmail("john@acme.com")
            .setInstanceId(this.instanceId).setExtensionId("com.xwiki.licensing:application-licensing-test-example")
            .setSupportLevel(support).setUserLimit(userLimit).setLicenseType(type.name().toLowerCase());
        LicenseDetailsViewPage licenseDetailsView = licenseDetails.clickSaveAndView();
        if (!StringUtils.isEmpty(expirationDate)) {
            String licenseId = licenseDetailsView.getHTMLMetaDataValue("page");
            setup.updateObject(Arrays.asList("License", "Data"), licenseId, "License.Code.LicenseDetailsClass", 0,
                "expirationDate", expirationDate);
        }
        String license = licenseDetailsView.generateLicense();
        assertEquals("License successfully added!", LicensesAdminPage.gotoPage().addLicense(license));
    }
}
