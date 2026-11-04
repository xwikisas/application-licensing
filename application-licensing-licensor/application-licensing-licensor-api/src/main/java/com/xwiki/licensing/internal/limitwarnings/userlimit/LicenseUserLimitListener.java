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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseManager;
import com.xwiki.licensing.LicensingConfiguration;
import com.xwiki.licensing.internal.UserCounter;
import com.xwiki.licensing.internal.helpers.events.ActiveUserAddedEvent;
import com.xwiki.licensing.internal.helpers.events.LicenseAddedEvent;

/**
 * Listener which sends notifications when a license user limit is almost exceeded.
 *
 * @version $Id$
 * @since 1.34
 */
@Component
@Singleton
@Named(LicenseUserLimitListener.HINT)
public class LicenseUserLimitListener extends AbstractEventListener
{
    /**
     * The event listener component hint.
     */
    public static final String HINT = "com.xwiki.licensing.internal.limitwarnings.userlimit.LicenseUserLimitListener";

    private static final long USER_LIMIT_THRESHOLD_DEFAULT = 5;

    private static final String PERCENT = "%";

    @Inject
    private Logger logger;

    @Inject
    private Provider<LicenseManager> licenseManagerProvider;

    @Inject
    private UserCounter userCounter;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private LicensingConfiguration licensingConfiguration;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Default constructor.
     */
    public LicenseUserLimitListener()
    {
        super(HINT, List.of(new ActiveUserAddedEvent(), new LicenseAddedEvent()));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        Collection<License> potentiallyInvalidatedLicenses = List.of();
        if (event instanceof ActiveUserAddedEvent) {
            // A new active user was added, so we need to search for all licenses that might be nearing their user
            // limit.
            potentiallyInvalidatedLicenses = getUsedLicensesWithUserLimits();
        } else if (event instanceof LicenseAddedEvent) {
            // We know exactly which license was added, so check if it almost exceeds the user limit.
            potentiallyInvalidatedLicenses = List.of(((LicenseAddedEvent) event).getLicense());
        }

        if (potentiallyInvalidatedLicenses.isEmpty()) {
            return;
        }

        // Licenses are managed from the main wiki, so read the licensing configuration from there even when a subwiki
        // user triggered the event.
        XWikiContext xcontext = contextProvider.get();
        String currentWikiId = xcontext.getWikiId();
        try {
            xcontext.setWikiId(xcontext.getMainXWiki());
            long userCount = userCounter.getUserCount();
            notifyForLicenses(potentiallyInvalidatedLicenses, userCount);
        } catch (Exception e) {
            logger.error("Failed to get count of users for license user limit warning. Cause: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
        } finally {
            xcontext.setWikiId(currentWikiId);
        }
    }

    private void notifyForLicenses(Collection<License> licenses, long userCount)
    {
        Collection<License> filteredLicenses = licenses.stream().filter(license -> {
            long userDiff = license.getMaxUserCount() - userCount;
            return 0 <= userDiff && userDiff < getUserNotificationThresholdForLicense(license);
        }).collect(Collectors.toSet());

        if (!filteredLicenses.isEmpty()) {
            // The event is attached to Licenses.WebHome because the table of contents of notification emails labels
            // each event with the title of its document; without a document the label would be empty. Since the event
            // is targeted, recipients don't need to watch this page (only an explicit exclusive filter blocks it), but
            // they need view right on it, and the Licenses space is restricted to the XWikiAdminGroup. On XWiki
            // versions older than 15.10.12 the email label is displayed as "$title" (see XWIKI-20187).
            String mainWikiId = contextProvider.get().getMainXWiki();
            observationManager.notify(
                new LicenseUserLimitWarningEvent(filteredLicenses, userCount, getTargetGroups(mainWikiId)),
                "com.xwiki.licensing:application-licensing-licensor-api",
                new XWikiDocument(new DocumentReference(mainWikiId, "Licenses", "WebHome")));
        }
    }

    private Set<String> getTargetGroups(String mainWikiId)
    {
        Set<String> notifiedGroups = licensingConfiguration.getNotifiedGroupsSet();
        notifiedGroups.add(serializer.serialize(new DocumentReference(mainWikiId, "XWiki", "XWikiAdminGroup")));
        return notifiedGroups;
    }

    private long getUserNotificationThresholdForLicense(License license)
    {
        String threshold = licensingConfiguration.getUserLimitWarningThreshold();
        if (null == threshold) {
            // The configuration object is missing or can't be retrieved.
            return USER_LIMIT_THRESHOLD_DEFAULT;
        }

        threshold = threshold.trim();
        try {
            if (threshold.endsWith(PERCENT)) {
                // The threshold is specified as a % of the license user limit.
                long thresh = Long.parseLong(threshold.replace(PERCENT, ""));
                if (!(thresh >= 0 && thresh <= 100)) {
                    throw new NumberFormatException("Invalid Percentage: " + thresh + PERCENT);
                } else {
                    return license.getMaxUserCount() * thresh / 100;
                }
            } else {
                return Long.parseLong(threshold);
            }
        } catch (NumberFormatException e) {
            logger.warn("Could not parse user limit threshold [{}] for license [{}]. Cause: [{}]", threshold,
                license.getId().getId(), ExceptionUtils.getRootCauseMessage(e));
            return USER_LIMIT_THRESHOLD_DEFAULT;
        }
    }

    private Collection<License> getUsedLicensesWithUserLimits()
    {
        return licenseManagerProvider.get().getUsedLicenses().stream()
            .filter(license -> 0 <= license.getMaxUserCount() && license.getMaxUserCount() < Long.MAX_VALUE)
            .collect(Collectors.toSet());
    }
}
