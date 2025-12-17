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

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceCategory;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.notifications.preferences.TargetableNotificationPreferenceBuilder;
import org.xwiki.notifications.preferences.internal.WikiNotificationPreferenceProvider;

import com.xpn.xwiki.XWikiContext;

/**
 * Descriptor for the event {@link LicenseUserLimitWarningEvent}.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named(LicenseUserLimitWarningEvent.EVENT_TYPE)
public class LicenseUserLimitWarningEventDescriptor implements RecordableEventDescriptor, Initializable
{
    @Inject
    private TargetableNotificationPreferenceBuilder targetableNotificationPreferenceBuilder;

    @Inject
    private NotificationPreferenceManager notificationPreferenceManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    @Override
    public void initialize()
    {
        WikiReference wikiReference = this.contextProvider.get().getWikiReference();
        Map<NotificationPreferenceProperty, Object> properties =
            Map.of(NotificationPreferenceProperty.EVENT_TYPE, LicenseUserLimitWarningEvent.EVENT_TYPE);
        // Create the preference for alerts and emails.
        List<NotificationPreference> notificationPreferences = List.of(
            this.targetableNotificationPreferenceBuilder.prepare().setCategory(NotificationPreferenceCategory.DEFAULT)
                .setEnabled(true).setFormat(NotificationFormat.ALERT).setProperties(properties)
                .setProviderHint(WikiNotificationPreferenceProvider.NAME).setStartDate(new Date())
                .setTarget(wikiReference).build(),
            this.targetableNotificationPreferenceBuilder.setFormat(NotificationFormat.EMAIL).build());

        // Save it.
        try {
            this.notificationPreferenceManager.savePreferences(notificationPreferences);
        } catch (NotificationException e) {
            // We don't throw an InitializationException since it doesn't prevent the component to be used.
            this.logger.warn("Error while enabling LicenseUserLimitWarningEvent for the wiki {}: {}", wikiReference,
                ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @Override
    public String getEventType()
    {
        return LicenseUserLimitWarningEvent.class.getCanonicalName();
    }

    @Override
    public String getApplicationName()
    {
        return "licensor.extension.name";
    }

    @Override
    public String getDescription()
    {
        return "licensor.events.LicenseUserLimitWarningEvent.description";
    }

    @Override
    public String getApplicationIcon()
    {
        return "warning";
    }

    @Override
    public String getEventTitle()
    {
        return "licensor.events.LicenseUserLimitWarningEvent.title";
    }
}
