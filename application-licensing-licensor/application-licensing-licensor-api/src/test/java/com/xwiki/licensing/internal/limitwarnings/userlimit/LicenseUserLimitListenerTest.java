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
package com.xwiki.licensing.internal.limitwarnings.userlimit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseId;
import com.xwiki.licensing.LicenseManager;
import com.xwiki.licensing.LicensingConfiguration;
import com.xwiki.licensing.internal.UserCounter;
import com.xwiki.licensing.internal.helpers.events.ActiveUserAddedEvent;
import com.xwiki.licensing.internal.helpers.events.LicenseAddedEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LicenseUserLimitListener}.
 *
 * @version $Id$
 */
@ComponentTest
class LicenseUserLimitListenerTest
{
    private static final String MAIN_WIKI = "mainwiki";

    private static final String ADMIN_GROUP = MAIN_WIKI + ":XWiki.XWikiAdminGroup";

    private static final String NOTIFIED_GROUP = MAIN_WIKI + ":XWiki.LicenseManagers";

    @InjectMockComponents
    private LicenseUserLimitListener listener;

    @MockComponent
    private Provider<LicenseManager> licenseManagerProvider;

    @MockComponent
    private UserCounter userCounter;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private LicensingConfiguration licensingConfiguration;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private LicenseManager licenseManager = mock(LicenseManager.class);

    private XWikiContext xcontext = mock(XWikiContext.class);

    @BeforeEach
    void configure()
    {
        when(this.licenseManagerProvider.get()).thenReturn(this.licenseManager);
        when(this.contextProvider.get()).thenReturn(this.xcontext);
        // Use a main wiki id other than "xwiki" to make sure it's not hardcoded, and trigger the event from a subwiki.
        when(this.xcontext.getMainXWiki()).thenReturn(MAIN_WIKI);
        when(this.xcontext.getWikiId()).thenReturn("subwiki");
        when(this.serializer.serialize(new DocumentReference(MAIN_WIKI, "XWiki", "XWikiAdminGroup")))
            .thenReturn(ADMIN_GROUP);
        when(this.licensingConfiguration.getNotifiedGroupsSet()).thenReturn(new HashSet<>(Set.of(NOTIFIED_GROUP)));
        when(this.licensingConfiguration.getUserLimitWarningThreshold()).thenReturn("5");
    }

    @Test
    void activeUserAddedNearUserLimit() throws Exception
    {
        License license = createLicense(10);
        when(this.licenseManager.getUsedLicenses()).thenReturn(List.of(license));
        when(this.userCounter.getUserCount()).thenReturn(7L);

        this.listener.onEvent(new ActiveUserAddedEvent(), null, null);

        ArgumentCaptor<XWikiDocument> document = ArgumentCaptor.forClass(XWikiDocument.class);
        LicenseUserLimitWarningEvent event = verifyNotificationSent(document);
        assertEquals(List.of(license), List.copyOf(event.getLicenses()));
        assertEquals(7L, event.getUserCount());
        assertEquals(Set.of(NOTIFIED_GROUP, ADMIN_GROUP), event.getTarget());

        // The users are counted from the main wiki, then the current wiki is restored.
        verify(this.xcontext).setWikiId(MAIN_WIKI);
        verify(this.xcontext).setWikiId("subwiki");
    }

    @ParameterizedTest
    @CsvSource(value = {
        // userCount, threshold, notificationSent, logged threshold
        // At the limit.
        "10, 5, true, false",
        // Far from the limit.
        "4, 5, false, false",
        // Over the limit.
        "11, 5, false, false",
        // 20% of 10 users is 2 users.
        "9, 20%, true, false",
        "8, 20%, false, false",
        // 33% of 10 users is 3.3 users (which should be treated as 3).
        "8, 33%, true, false",
        "7, 33%, false, false",
        // Invalid thresholds fall back to 5.
        "7, abc, true, true",
        "7, 150%, true, true",
        "7, 10 %, true, true",
        "7, 10%%, true, true",
        "7, 3.3, true, true",
        "7, -3, true, true",
        "7, -5%, true, true",
        // No threshold configured, or an empty one, falls back to 5.
        "7, NULL, true, false",
        "7, '', true, false",
        "4, NULL, false, false" }, nullValues = "NULL")
    void thresholdValues(long userCount, String threshold, boolean notificationSent, boolean invalidThreshold)
        throws Exception
    {
        License license = createLicense(10);
        when(this.licenseManager.getUsedLicenses()).thenReturn(List.of(license));
        when(this.userCounter.getUserCount()).thenReturn(userCount);
        when(this.licensingConfiguration.getUserLimitWarningThreshold()).thenReturn(threshold);

        this.listener.onEvent(new ActiveUserAddedEvent(), null, null);

        // Name the parameters in the failure messages.
        String parameters = String.format("userCount=%d, threshold=%s", userCount, threshold);
        verify(this.observationManager, times(notificationSent ? 1 : 0).description(parameters))
            .notify(any(LicenseUserLimitWarningEvent.class), any(), any());
        if (invalidThreshold) {
            assertEquals(1, this.logCapture.size(), parameters);
            assertTrue(
                this.logCapture.getMessage(0).startsWith(String.format(
                    "Could not parse user limit threshold [%s] for license [%s].", threshold, license.getId().getId())),
                parameters);
        }
    }

    @Test
    void activeUserAddedIgnoresLicensesWithoutUserLimit() throws Exception
    {
        License limitedLicense = createLicense(10);
        when(this.licenseManager.getUsedLicenses())
            .thenReturn(List.of(createLicense(-1), createLicense(Long.MAX_VALUE), limitedLicense));
        when(this.userCounter.getUserCount()).thenReturn(9L);

        this.listener.onEvent(new ActiveUserAddedEvent(), null, null);

        assertEquals(List.of(limitedLicense),
            List.copyOf(verifyNotificationSent(ArgumentCaptor.forClass(XWikiDocument.class)).getLicenses()));
    }

    @Test
    void licenseAddedOnlyChecksTheAddedLicense() throws Exception
    {
        License addedLicense = createLicense(10);
        when(this.userCounter.getUserCount()).thenReturn(9L);

        this.listener.onEvent(new LicenseAddedEvent(addedLicense), null, null);

        assertEquals(List.of(addedLicense),
            List.copyOf(verifyNotificationSent(ArgumentCaptor.forClass(XWikiDocument.class)).getLicenses()));
        verify(this.licenseManager, never()).getUsedLicenses();
    }

    @Test
    void userCountFailure() throws Exception
    {
        when(this.licenseManager.getUsedLicenses()).thenReturn(List.of(createLicense(10)));
        when(this.userCounter.getUserCount()).thenThrow(new Exception("Query failed"));

        this.listener.onEvent(new ActiveUserAddedEvent(), null, null);

        verify(this.observationManager, never()).notify(any(), any(), any());
        assertEquals("Failed to get count of users for license user limit warning. Cause: [Exception: Query failed]",
            this.logCapture.getMessage(0));
        // The current wiki is restored even when counting the users fails.
        verify(this.xcontext).setWikiId("subwiki");
    }

    private LicenseUserLimitWarningEvent verifyNotificationSent(ArgumentCaptor<XWikiDocument> document)
    {
        ArgumentCaptor<LicenseUserLimitWarningEvent> event =
            ArgumentCaptor.forClass(LicenseUserLimitWarningEvent.class);
        verify(this.observationManager).notify(event.capture(),
            eq("com.xwiki.licensing:application-licensing-licensor-api"), document.capture());
        return event.getValue();
    }

    private License createLicense(long maxUserCount)
    {
        License license = new License();
        license.setId(new LicenseId());
        license.setMaxUserCount(maxUserCount);
        return license;
    }
}
