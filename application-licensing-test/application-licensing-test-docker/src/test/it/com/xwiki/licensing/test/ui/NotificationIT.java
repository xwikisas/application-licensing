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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.platform.notifications.test.po.NotificationsTrayPage;
import org.xwiki.platform.notifications.test.po.NotificationsUserProfilePage;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.scheduler.test.po.SchedulerHomePage;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.BootstrapSwitch;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.test.po.LicensesAdminPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional tests for the notifications of the Licensing application.
 *
 * @version $Id$
 */
@UITest(sshPorts = {
    // Open the GreenMail port so that the XWiki instance can use the SMTP server provided by GreenMail running on the
    // host.
    3025}, properties = {
        // Needed to send emails.
        "xwikiDbHbmCommonExtraMappings=mailsender.hbm.xml",

        // Needed to trigger the notification email jobs.
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.scheduler.SchedulerPlugin"}, extraJARs = {
            // Required for the mailsender.hbm.xml file.
            "org.xwiki.platform:xwiki-platform-mail-send-storage:15.10",

            // The Scheduler plugin needs to be in WEB-INF/lib since it's defined in xwiki.cfg and plugins are loaded by
            // XWiki at startup, i.e. before extensions are provisioned for the tests.
            "org.xwiki.platform:xwiki-platform-scheduler-api:15.10"}, resolveExtraJARs = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotificationIT
{
    private static final String USER_LIMIT_WARNING_EVENT =
        "com.xwiki.licensing.internal.limitwarnings.userlimit.LicenseUserLimitWarningEvent";

    private static final String USER_LIMIT_WARNING_EMAIL_HINT = "This table might not be up to date anymore.";

    private static final String PASSWORD = "test";

    private static final String LICENSE_ADMIN = "licenseAdmin";

    private static final String LICENSE_ADMIN_EMAIL = "licenseadmin@xwiki.org";

    private static final String SIMPLE_USER = "carol";

    /**
     * Provides the license setup (certificates, store URLs) and adds the licenses.
     */
    private static final LicensingIT LICENSING = new LicensingIT();

    private static GreenMail mail;

    @BeforeAll
    static void beforeAll(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        LICENSING.configure(setup);
        mail = startMailServer(setup, testConfiguration);

        // Workaround: The test XARs are imported before the notification renderer classes exist (unlike on a normal
        // instance), so the renderers are registered without their templates. Saving their page with a real change (an
        // unchanged page isn't saved) registers them again.
        setRenderersEventType(setup, "temporary");
        setRenderersEventType(setup, USER_LIMIT_WARNING_EVENT);

        // An administrator who receives the notification emails (daily, by default), and a simple user.
        setup.createUser(LICENSE_ADMIN, PASSWORD, "", "email", LICENSE_ADMIN_EMAIL);
        setup.addObject("XWiki", "XWikiAdminGroup", "XWiki.XWikiGroups", "member", "XWiki." + LICENSE_ADMIN);
        setup.createUser(SIMPLE_USER, PASSWORD, "");
    }

    @AfterAll
    static void afterAll()
    {
        if (mail != null) {
            mail.stop();
        }
    }

    @Test
    @Order(1)
    void userLimitWarningEnabledByDefault(TestUtils setup) throws Exception
    {
        // The user limit warning is enabled by default, for both the alerts and the emails.
        setup.loginAndGotoPage(LICENSE_ADMIN, PASSWORD, setup.getURL("Main", "WebHome"));
        NotificationsUserProfilePage preferences = NotificationsUserProfilePage.gotoPage(LICENSE_ADMIN);
        assertEquals(BootstrapSwitch.State.ON,
            preferences.getEventTypeState("admin.licenses", USER_LIMIT_WARNING_EVENT, "alert"));
        assertEquals(BootstrapSwitch.State.ON,
            preferences.getEventTypeState("admin.licenses", USER_LIMIT_WARNING_EVENT, "email"));
    }

    @Test
    @Order(2)
    void userLimitWarning(TestUtils setup) throws Exception
    {
        // Adding a license with a single seat left warns the administrators.
        setup.loginAsSuperAdmin();
        LICENSING.instanceId = LicensesAdminPage.gotoPage().getInstanceId();
        // The rendered content is returned as HTML (e.g. "<p>3</p>"), so remove the markup.
        long userCount = Long.parseLong(
            setup.executeWiki("{{velocity}}$services.licensing.licensor.userCount{{/velocity}}", Syntax.XWIKI_2_1)
                .replaceAll("<[^>]*>", "").trim());
        LICENSING.addLicense(LicenseType.PAID, null, "silver", String.valueOf(userCount + 1), setup);
        waitForUserLimitWarningAlert(setup);
        sendDailyEmails(setup, 1);

        // A new user reaches the user limit, which warns the administrators again. Clear the previous warning so that
        // we can wait for the new one.
        setup.loginAndGotoPage(LICENSE_ADMIN, PASSWORD, setup.getURL("Main", "WebHome"));
        new NotificationsTrayPage().clearAllNotifications();
        setup.loginAsSuperAdmin();
        setup.createUser("bob", PASSWORD, "");
        waitForUserLimitWarningAlert(setup);
        sendDailyEmails(setup, 2);

        // The simple users are not warned.
        setup.loginAndGotoPage(SIMPLE_USER, PASSWORD, setup.getURL("Main", "WebHome"));
        assertFalse(hasUserLimitWarningAlert());
    }

    /**
     * Set the event type of the user limit warning alert and email renderers, which registers them again. See the
     * workaround comment in {@link #beforeAll(TestUtils, TestConfiguration)}.
     */
    private static void setRenderersEventType(TestUtils setup, String eventType)
    {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("XWiki.Notifications.Code.NotificationDisplayerClass_0_eventType", eventType);
        parameters.put("XWiki.Notifications.Code.NotificationEmailRendererClass_0_eventType", eventType);
        setup.gotoPage(new LocalDocumentReference(List.of("Licenses", "Code"),
            "LicenseUserLimitWarningEventNotificationRenderers"), "save", parameters);
    }

    /**
     * Configure XWiki to send its emails to a GreenMail SMTP server running on the host, and start that server.
     */
    private static GreenMail startMailServer(TestUtils setup, TestConfiguration testConfiguration)
    {
        setup.loginAsSuperAdmin();
        setup.updateObject("Mail", "MailConfig", "Mail.SendMailConfigClass", 0,
            "host", testConfiguration.getServletEngine().getHostIP(),
            "port", "3025",
            "sendWaitTime", "0",
            "from", "admin@xwiki.org");

        GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();
        return greenMail;
    }

    /**
     * Wait until the license administrator has an unread user limit warning alert. The events are stored and dispatched
     * asynchronously, and the email job only sends the events dated after its previous run, so it must not run before
     * the event is ready.
     */
    private void waitForUserLimitWarningAlert(TestUtils setup)
    {
        setup.loginAndGotoPage(LICENSE_ADMIN, PASSWORD, setup.getURL("Main", "WebHome"));
        NotificationsTrayPage.waitOnNotificationCount("xwiki:XWiki." + LICENSE_ADMIN, "xwiki", 1);
        setup.gotoPage("Main", "WebHome");
        assertTrue(hasUserLimitWarningAlert());
    }

    /**
     * Trigger the daily notification emails job and check how many user limit warning emails were received in total.
     */
    private void sendDailyEmails(TestUtils setup, int expectedCount) throws Exception
    {
        setup.loginAsSuperAdmin();
        int receivedCount = mail.getReceivedMessages().length;
        SchedulerHomePage.gotoPage().clickJobActionTrigger("Notifications daily email");
        // Wait 30s at most, so that the email has enough time to arrive even if the CI is slow.
        mail.waitForIncomingEmail(30000L, receivedCount + 1);
        assertEquals(expectedCount, getUserLimitWarningEmails().size());
    }

    /**
     * @return the user limit warning emails received by the license administrator, recognized by a hint that only the
     *         user limit warning email template writes, so that other notification emails (e.g. page updates) are
     *         ignored
     */
    private List<MimeMessage> getUserLimitWarningEmails() throws Exception
    {
        List<MimeMessage> emails = new ArrayList<>();
        for (MimeMessage message : mail.getReceivedMessages()) {
            if (isSentTo(message, LICENSE_ADMIN_EMAIL) && getText(message).contains(USER_LIMIT_WARNING_EMAIL_HINT)) {
                emails.add(message);
            }
        }
        return emails;
    }

    /**
     * @return whether the given email address is one of the message recipients
     */
    private boolean isSentTo(MimeMessage message, String emailAddress) throws Exception
    {
        for (Address recipient : message.getAllRecipients()) {
            if (emailAddress.equals(((InternetAddress) recipient).getAddress())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the text of all the text parts (plain text and HTML) of the given message/email part
     */
    private String getText(Part part) throws Exception
    {
        if (part.isMimeType("text/*")) {
            return (String) part.getContent();
        } else if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                text.append(getText(multipart.getBodyPart(i)));
            }
            return text.toString();
        }
        return "";
    }

    /**
     * @return whether the notifications tray of the current user shows a user limit warning alert
     */
    private boolean hasUserLimitWarningAlert()
    {
        NotificationsTrayPage tray = new NotificationsTrayPage();
        tray.showNotificationTray();
        for (int i = 0; i < tray.getNotificationsCount(); i++) {
            if (USER_LIMIT_WARNING_EVENT.equals(tray.getNotificationType(i))) {
                return true;
            }
        }
        return false;
    }
}
