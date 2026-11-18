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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
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
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Descriptor for the event {@link LicenseUserLimitWarningEvent}.
 *
 * @version $Id$
 * @since 1.34
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
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Logger logger;

    @Override
    public void initialize()
    {
        // Use the main wiki rather than the current one: licenses are managed from the main wiki, and this singleton is
        // initialized only once, possibly while a subwiki is the current wiki.
        WikiReference wikiReference = new WikiReference(this.wikiDescriptorManager.getMainWikiId());
        Map<NotificationPreferenceProperty, Object> properties =
            Map.of(NotificationPreferenceProperty.EVENT_TYPE, LicenseUserLimitWarningEvent.EVENT_TYPE);
        try {
            // Only add the default alert and email preferences that are missing, so that an admin who disabled them
            // keeps them disabled after a restart.
            Set<NotificationFormat> existingFormats =
                this.notificationPreferenceManager.getAllPreferences(wikiReference).stream()
                    .filter(preference -> LicenseUserLimitWarningEvent.EVENT_TYPE
                        .equals(preference.getProperties().get(NotificationPreferenceProperty.EVENT_TYPE)))
                    .map(NotificationPreference::getFormat).collect(Collectors.toSet());

            List<NotificationPreference> notificationPreferences = new ArrayList<>();
            for (NotificationFormat format : List.of(NotificationFormat.ALERT, NotificationFormat.EMAIL)) {
                if (!existingFormats.contains(format)) {
                    notificationPreferences.add(createPreference(format, properties, wikiReference));
                }
            }

            if (!notificationPreferences.isEmpty()) {
                this.notificationPreferenceManager.savePreferences(notificationPreferences);
            }
        } catch (NotificationException e) {
            // We don't throw an InitializationException since it doesn't prevent the component to be used.
            this.logger.warn("Error while enabling LicenseUserLimitWarningEvent for the wiki {}: {}", wikiReference,
                ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private NotificationPreference createPreference(NotificationFormat format,
        Map<NotificationPreferenceProperty, Object> properties, WikiReference wikiReference)
    {
        return this.targetableNotificationPreferenceBuilder.prepare()
            .setCategory(NotificationPreferenceCategory.DEFAULT).setEnabled(true).setFormat(format)
            .setProperties(properties).setProviderHint(WikiNotificationPreferenceProvider.NAME).setStartDate(new Date())
            .setTarget(wikiReference).build();
    }

    @Override
    public String getEventType()
    {
        return LicenseUserLimitWarningEvent.class.getCanonicalName();
    }

    @Override
    public String getApplicationName()
    {
        return "admin.licenses";
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
