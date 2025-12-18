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
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.AbstractDocumentEvent;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.event.filter.EventFilter;
import org.xwiki.observation.event.filter.RegexEventFilter;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseManager;
import com.xwiki.licensing.LicensingConfiguration;
import com.xwiki.licensing.internal.UserCounter;
import com.xwiki.licensing.internal.helpers.events.LicenseAddedEvent;

import static org.xwiki.user.internal.UserPropertyConstants.ACTIVE;

/**
 * Listener which sends notifications when a license user limit is almost exceeded.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named(LicenseUserLimitWarningEvent.EVENT_TYPE)
public class LicenseUserLimitListener extends AbstractEventListener
{
    private static final LocalDocumentReference USER_CLASS = new LocalDocumentReference("XWiki", "XWikiUsers");

    private static final EventFilter XWIKI_SPACE_FILTER = new RegexEventFilter("(.*:)?XWiki\\..*");

    private static final long USER_LIMIT_THRESHOLD_DEFAULT = 5;

    private static final String PERCENT = "%";

    @Inject
    private Logger logger;

    @Inject
    private LicenseManager licenseManager;

    @Inject
    private UserCounter userCounter;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private LicensingConfiguration licensingConfiguration;

    /**
     * Default constructor.
     */
    public LicenseUserLimitListener()
    {
        super(LicenseUserLimitListener.class.getName(),
            List.of(new DocumentUpdatedEvent(XWIKI_SPACE_FILTER), new DocumentCreatedEvent(XWIKI_SPACE_FILTER),
                new LicenseAddedEvent()));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        Collection<License> potentiallyInvalidatedLicenses = List.of();
        if (event instanceof AbstractDocumentEvent && shouldTriggerOnDocumentEvent(source)) {
            // If the event was triggered because of a user page update, we need to search for all licenses that
            // might be nearing their user limit.
            potentiallyInvalidatedLicenses = getUsedLicensesWithUserLimits();
        } else if (event instanceof LicenseAddedEvent) {
            // We know exactly which license was added, so check if it almost exceeds the user limit.
            potentiallyInvalidatedLicenses = List.of(((LicenseAddedEvent) event).getLicense());
        }

        if (potentiallyInvalidatedLicenses.isEmpty()) {
            return;
        }

        try {
            long userCount = userCounter.getUserCount();
            notifyForLicenses(potentiallyInvalidatedLicenses, userCount);
        } catch (Exception e) {
            logger.error("Failed to get count of users for license user limit warning. Cause: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private boolean shouldTriggerOnDocumentEvent(Object source)
    {
        XWikiDocument newDocument = (XWikiDocument) source;
        XWikiDocument oldDocument = newDocument.getOriginalDocument();

        BaseObject newObject = newDocument.getXObject(USER_CLASS);
        BaseObject oldObject = oldDocument.getXObject(USER_CLASS);

        boolean newDocumentIsUser = newObject != null;
        boolean oldDocumentIsUser = oldObject != null;

        // Set defaults to -1 to avoid nulls.
        int newActive = newDocumentIsUser ? newObject.getIntValue(ACTIVE) : -1;
        int oldActive = oldDocumentIsUser ? oldObject.getIntValue(ACTIVE) : -1;

        if (newDocumentIsUser != oldDocumentIsUser) {
            // User page was just created.
            return true;
        } else {
            // User was enabled.
            return newDocumentIsUser && newActive == 1 && oldActive == 0;
        }
    }

    private void notifyForLicenses(Collection<License> licenses, long userCount)
    {
        Collection<License> filteredLicenses = licenses.stream().filter(license -> {
            long userDiff = license.getMaxUserCount() - userCount;
            return 0 <= userDiff && userDiff < getUserNotificationThresholdForLicense(license);
        }).collect(Collectors.toSet());

        if (!filteredLicenses.isEmpty()) {
            // Unless the user watches the Licenses.WebHome page, they won't receive the notification. This may be
            // helpful if the user wants to have an exclusive filter on the entire wiki but still receive licensing
            // notifications.
            // TODO: The Licenses.WebHome page is not visible for guests, so email notifications list the page as $title
            //  Would need to pick a public marker page? Or replace the $title with something else by means of
            //  velocity workarounds.
            observationManager.notify(new LicenseUserLimitWarningEvent(filteredLicenses, userCount),
                "com.xwiki.licensing:application-licensing-licensor-api",
                new XWikiDocument(new DocumentReference("xwiki", "Licenses", "WebHome")));
        }
    }

    private long getUserNotificationThresholdForLicense(License license)
    {
        String threshold = licensingConfiguration.getUserLimitWarningThreshold().trim();
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
        return licenseManager.getUsedLicenses().stream()
            .filter(license -> 0 <= license.getMaxUserCount() && license.getMaxUserCount() < Long.MAX_VALUE)
            .collect(Collectors.toSet());
    }
}
