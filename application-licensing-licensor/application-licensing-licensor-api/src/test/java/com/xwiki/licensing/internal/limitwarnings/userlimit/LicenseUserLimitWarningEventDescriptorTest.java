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
import java.util.stream.Collectors;

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
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    private WikiDescriptorManager wikiDescriptorManager;

    @BeforeComponent
    void beforeComponent() throws Exception
    {
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn(WIKI.getName());

        // The wiki already has the email preference, e.g. disabled by an admin.
        List<NotificationPreference> existing =
            List.of(mockPreference(LicenseUserLimitWarningEvent.EVENT_TYPE, NotificationFormat.EMAIL));
        when(this.notificationPreferenceManager.getAllPreferences(WIKI)).thenReturn(existing);
    }

    @Test
    void initializeOnlySavesMissingFormats() throws Exception
    {
        ArgumentCaptor<List<NotificationPreference>> saved = ArgumentCaptor.forClass(List.class);
        verify(this.notificationPreferenceManager).savePreferences(saved.capture());

        assertEquals(1, saved.getValue().size());
        assertEquals(NotificationFormat.ALERT, saved.getValue().get(0).getFormat());
    }

    @Test
    void initializeSavesBothFormatsWhenMissing() throws Exception
    {
        clearInvocations(this.notificationPreferenceManager);
        // A preference of another event type doesn't count.
        List<NotificationPreference> existing = List.of(mockPreference("otherEventType", NotificationFormat.ALERT));
        when(this.notificationPreferenceManager.getAllPreferences(WIKI)).thenReturn(existing);

        this.descriptor.initialize();

        ArgumentCaptor<List<NotificationPreference>> saved = ArgumentCaptor.forClass(List.class);
        verify(this.notificationPreferenceManager).savePreferences(saved.capture());
        assertEquals(List.of(NotificationFormat.ALERT, NotificationFormat.EMAIL),
            saved.getValue().stream().map(NotificationPreference::getFormat).collect(Collectors.toList()));
    }

    @Test
    void initializeSavesNothingWhenBothFormatsExist() throws Exception
    {
        clearInvocations(this.notificationPreferenceManager);
        List<NotificationPreference> existing =
            List.of(mockPreference(LicenseUserLimitWarningEvent.EVENT_TYPE, NotificationFormat.ALERT),
                mockPreference(LicenseUserLimitWarningEvent.EVENT_TYPE, NotificationFormat.EMAIL));
        when(this.notificationPreferenceManager.getAllPreferences(WIKI)).thenReturn(existing);

        this.descriptor.initialize();

        verify(this.notificationPreferenceManager, never()).savePreferences(any());
    }

    private NotificationPreference mockPreference(String eventType, NotificationFormat format)
    {
        NotificationPreference preference = mock(NotificationPreference.class);
        when(preference.getFormat()).thenReturn(format);
        when(preference.getProperties()).thenReturn(Map.of(NotificationPreferenceProperty.EVENT_TYPE, eventType));
        return preference;
    }
}
