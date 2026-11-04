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

import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.notifications.preferences.internal.DefaultTargetableNotificationPreferenceBuilder;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LicenseUserLimitWarningEventDescriptor}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList(DefaultTargetableNotificationPreferenceBuilder.class)
class LicenseUserLimitWarningEventDescriptorTest
{
    private static final WikiReference WIKI = new WikiReference("xwiki");

    @InjectMockComponents
    private LicenseUserLimitWarningEventDescriptor descriptor;

    @MockComponent
    private NotificationPreferenceManager notificationPreferenceManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @BeforeComponent
    void beforeComponent() throws Exception
    {
        XWikiContext context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(context);
        when(context.getWikiReference()).thenReturn(WIKI);

        // The wiki already has the email preference, e.g. disabled by an admin.
        NotificationPreference existingEmail = mock(NotificationPreference.class);
        when(existingEmail.getFormat()).thenReturn(NotificationFormat.EMAIL);
        when(existingEmail.getProperties()).thenReturn(
            Map.of(NotificationPreferenceProperty.EVENT_TYPE, LicenseUserLimitWarningEvent.EVENT_TYPE));
        when(this.notificationPreferenceManager.getAllPreferences(WIKI)).thenReturn(List.of(existingEmail));
    }

    @Test
    void initializeOnlySavesMissingFormats() throws Exception
    {
        ArgumentCaptor<List<NotificationPreference>> saved = ArgumentCaptor.forClass(List.class);
        verify(this.notificationPreferenceManager).savePreferences(saved.capture());

        assertEquals(1, saved.getValue().size());
        assertEquals(NotificationFormat.ALERT, saved.getValue().get(0).getFormat());
    }
}
