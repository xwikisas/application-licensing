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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.WebElement;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.test.ExtensionTestUtils;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.text.StringUtils;

import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.test.po.LicenseDetailsEditPage;
import com.xwiki.licensing.test.po.LicenseDetailsViewPage;
import com.xwiki.licensing.test.po.LicenseNotificationPane;
import com.xwiki.licensing.test.po.LicensesAdminPage;
import com.xwiki.licensing.test.po.LicensesHomePage;

import static org.junit.Assert.*;

/**
 * Functional tests for the Licensing application.
 *
 * @version $Id$
 */
public class LicensingTest extends AbstractTest
{
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    @Rule
    public SuperAdminAuthenticationRule superAdminAuthenticationRule = new SuperAdminAuthenticationRule(getUtil());

    private String instanceId;

    @Before
    public void configure() throws Exception
    {
        // Install the example application.

        if (!getUtil().pageExists("Example", "WebHome")) {
            ExtensionTestUtils extensionTestUtils = new ExtensionTestUtils(getUtil());
            extensionTestUtils.install(new ExtensionId("com.xwiki.licensing:application-licensing-test-example",
                System.getProperty("licensing.version")));
        }

        // Generate Certificates and Keys.

        LocalDocumentReference generateCertificatesAndKeys =
            new LocalDocumentReference(Arrays.asList("License", "Code"), "GenerateCertificatesAndKeys");
        if (!getUtil().rest().exists(generateCertificatesAndKeys)) {
            try (InputStream content = getClass().getResourceAsStream("/GenerateCertificatesAndKeys.wiki")) {
                getUtil().rest().savePage(generateCertificatesAndKeys,
                    IOUtils.toString(content, StandardCharsets.UTF_8), "");
            }

            getUtil().gotoPage(generateCertificatesAndKeys, "view", "proceed=1");
        }

        // Use the generated certificates.

        LocalDocumentReference useCertificates =
            new LocalDocumentReference(Arrays.asList("License", "Code"), "UseCertificates");
        if (!getUtil().rest().exists(useCertificates)) {
            try (InputStream content = getClass().getResourceAsStream("/UseCertificates.wiki")) {
                getUtil().rest().savePage(useCertificates, IOUtils.toString(content, StandardCharsets.UTF_8), "");
            }
        }

        getUtil().gotoPage(useCertificates, "view", "proceed=1");
        assertEquals("OK", new ViewPage().getContent());

        // Configure the store URLs (buy and trial) to point to the current wiki.

        getUtil().updateObject(Arrays.asList("Licenses", "Code"), "LicensingConfig",
            "Licenses.Code.LicensingStoreClass", 0, "storeTrialURL",
            "http://localhost:8080/xwiki/bin/get/Store/GetTrialLicense", "storeBuyURL",
            "http://localhost:8080/xwiki/bin/view/Store/BuyLicense");
    }

    @Test
    public void generateLicense() throws Exception
    {
        // Verify that there's no license for Example.WebHome
        ViewPage viewPage = getUtil().gotoPage("Example", "WebHome");
        // The superadmin user can still see the page.
        assertEquals("Missing license", viewPage.getContent());
        // The rest of the users are not allowed view it.
        getUtil().createUserAndLoginWithRedirect("alice", "test", getUtil().getURL("Example", "WebHome"));
        assertEquals("You are not allowed to view this page or perform this action.",
            getDriver().findElementByCssSelector("p.xwikimessage").getText());

        // Verify that the excluded page is accessible without a license.
        getUtil().gotoPage("Example", "ApplicationsPanelEntry");
        assertEquals("No license needed.", viewPage.getContent());

        // Verify the notification for the missing license.
        // The simple users should not see the notification.
        getUtil().gotoPage(getTestClassName(), getTestMethodName());
        assertFalse(viewPage.hasNotificationsMenu());
        // Users with administration rights should see it though.
        getUtil().loginAsSuperAdminAndGotoPage(getUtil().getURL(getTestClassName(), getTestMethodName()));
        viewPage.toggleNotificationsMenu();
        LicenseNotificationPane notification = new LicenseNotificationPane();
        assertEquals(Collections.singletonList("Paid Application Example"), notification.getExtensions());

        // Navigate to the Licenses administration section.
        LicensesAdminPage licensesAdminSection = notification.clickLicensesSectionLink();

        // Initially expiration date should display "No license available".
        LiveTableElement liveTable = licensesAdminSection.getLiveTable();
        assertEquals(1, liveTable.getRowCount());
        WebElement firstRow = liveTable.getRow(1);
        assertEquals("No license available", liveTable.getCell(firstRow, 3).getText());
        assertEquals("1 / 0", liveTable.getCell(firstRow, 4).getText());

        // Import an invalid license.
        licensesAdminSection.addLicense("foo");
        assertEquals("Failed! The provided license could not be decoded. Please contact sales@xwiki.com.",
            licensesAdminSection.getErrorMessage());

        // Import a license that is not meant for the current XWiki instance.
        try (InputStream incompatibleLicense = getClass().getResourceAsStream("/incompatible.license")) {
            licensesAdminSection.addLicense(IOUtils.toString(incompatibleLicense, StandardCharsets.UTF_8));
        }
        assertEquals("Failed! License is not compatible or useful for your server. Please contact sales@xwiki.com",
            licensesAdminSection.getErrorMessage());

        this.instanceId = licensesAdminSection.getInstanceId();

        // Generate and import an expired license with unspecified number of users.
        addLicense(LicenseType.FREE, "21/06/2017", "");

        // Check the license live table.
        assertEquals(1, liveTable.getRowCount());
        firstRow = liveTable.getRow(1);
        assertEquals("21/06/2017", liveTable.getCell(firstRow, 3).getText());
        assertEquals("-", liveTable.getCell(firstRow, 4).getText());

        // Check the license notification message.
        licensesAdminSection.toggleNotificationsMenu();
        notification = new LicenseNotificationPane();
        assertEquals(Collections.singletonList("Paid Application Example"), notification.getExtensions());

        // Generate and import a license for 0 users
        addLicense(LicenseType.TRIAL, null, "0");

        // Check the license live table.
        assertEquals(1, liveTable.getRowCount());
        firstRow = liveTable.getRow(1);
        assertEquals(DATE_FORMAT.format(new DateTime().plusDays(11).toDate()),
            liveTable.getCell(firstRow, 3).getText());
        assertEquals("1 / 0", liveTable.getCell(firstRow, 4).getText());

        // Check the license notification message.
        licensesAdminSection.toggleNotificationsMenu();
        notification = new LicenseNotificationPane();
        assertEquals(Collections.singletonList("Paid Application Example"), notification.getExtensions());

        // Generate and import a license for unlimited users
        addLicense(LicenseType.PAID, null, "-1");

        // Check the license live table.
        assertEquals(1, liveTable.getRowCount());
        firstRow = liveTable.getRow(1);
        assertEquals(DATE_FORMAT.format(new DateTime().plusDays(365).toDate()),
            liveTable.getCell(firstRow, 3).getText());
        assertEquals("Unlimited", liveTable.getCell(firstRow, 4).getText());

        // Check the license notification message.
        assertFalse(licensesAdminSection.hasNotificationsMenu());

        // Verify that the Example page now has a license.
        viewPage = getUtil().gotoPage("Example", "WebHome");
        assertEquals("Hello", viewPage.getContent());

        // Try also with a simple user.
        getUtil().loginAndGotoPage("alice", "test", getUtil().getURL("Example", "WebHome"));
        assertEquals("Hello", viewPage.getContent());
    }

    private void addLicense(LicenseType type, String expirationDate, String userLimit)
    {
        LicenseDetailsEditPage licenseDetails = LicensesHomePage.gotoPage().clickAddLicenseDetails();
        licenseDetails.setLicenseeFirstName("John").setLicenseeLastName("Doe").setLicenseeEmail("john@acme.com")
            .setInstanceId(this.instanceId).setExtensionName("Paid Application Example")
            .setExtensionId("com.xwiki.licensing:application-licensing-test-example").setUserLimit(userLimit)
            .setLicenseType(type.name().toLowerCase());
        LicenseDetailsViewPage licenseDetailsView = licenseDetails.clickSaveAndView();
        if (!StringUtils.isEmpty(expirationDate)) {
            String licenseId = licenseDetailsView.getHTMLMetaDataValue("page");
            getUtil().updateObject(Arrays.asList("License", "Data"), licenseId, "License.Code.LicenseDetailsClass", 0,
                "expirationDate", expirationDate);
        }
        String license = licenseDetailsView.generateLicense();
        LicensesAdminPage.gotoPage().addLicense(license);
    }
}
