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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.LongUnaryOperator;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
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
            // The cached user count was just invalidated, so this recounts the active users on all wikis. When many
            // users are created in a row (e.g. an LDAP or SSO import) this means one full count per user, but only
            // while a license with a user limit is in use, since we return early otherwise.
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
        LongUnaryOperator threshold = getUserNotificationThreshold();
        Collection<License> filteredLicenses = licenses.stream().filter(license -> {
            long userDiff = license.getMaxUserCount() - userCount;
            // Inclusive, so that reaching the limit always warns, even with a threshold of 0 (or a % that rounds to 0).
            return 0 <= userDiff && userDiff <= threshold.applyAsLong(license.getMaxUserCount());
        }).collect(Collectors.toSet());

        if (!filteredLicenses.isEmpty()) {
            // The event has no target page set as data, so all users in the target groups will receive the
            // notifications, but the email renderer will show a raw velocity $title.
            // If a target page is ever added, then users who can't view that page won't receive the notifications.
            String mainWikiId = contextProvider.get().getMainXWiki();
            observationManager.notify(
                new LicenseUserLimitWarningEvent(filteredLicenses, userCount, getTargetGroups(mainWikiId)),
                "com.xwiki.licensing:application-licensing-licensor-api",
                null);
        }
    }

    private Set<String> getTargetGroups(String mainWikiId)
    {
        // Copy, since the returned set is not guaranteed to be modifiable.
        Set<String> notifiedGroups = new HashSet<>(licensingConfiguration.getNotifiedGroupsSet());
        notifiedGroups.add(serializer.serialize(new DocumentReference(mainWikiId, "XWiki", "XWikiAdminGroup")));
        return notifiedGroups;
    }

    /**
     * Parse the configured threshold once, so that an invalid value is logged once instead of once per license.
     *
     * @return the function computing the warning threshold of a license from its user limit
     */
    private LongUnaryOperator getUserNotificationThreshold()
    {
        String threshold = licensingConfiguration.getUserLimitWarningThreshold();
        if (StringUtils.isBlank(threshold)) {
            // The configuration object is missing or can't be retrieved, or the field is left empty.
            return maxUserCount -> USER_LIMIT_THRESHOLD_DEFAULT;
        }

        threshold = threshold.trim();
        try {
            if (threshold.endsWith(PERCENT)) {
                // The threshold is specified as a % of the license user limit. Only the last character is removed, so
                // that values like "10%%" are rejected.
                long thresh = Long.parseLong(threshold.substring(0, threshold.length() - 1));
                if (!(thresh >= 0 && thresh <= 100)) {
                    throw new NumberFormatException("Invalid Percentage: " + thresh + PERCENT);
                }
                return maxUserCount -> maxUserCount * thresh / 100;
            } else {
                long thresh = Long.parseLong(threshold);
                if (thresh < 0) {
                    // A negative threshold would silently disable the warnings, so reject it like a negative %.
                    throw new NumberFormatException("Negative threshold: " + thresh);
                }
                return maxUserCount -> thresh;
            }
        } catch (NumberFormatException e) {
            logger.warn("Could not parse user limit threshold [{}], using the default of [{}] users. Cause: [{}]",
                threshold, USER_LIMIT_THRESHOLD_DEFAULT, ExceptionUtils.getRootCauseMessage(e));
            return maxUserCount -> USER_LIMIT_THRESHOLD_DEFAULT;
        }
    }

    private Collection<License> getUsedLicensesWithUserLimits()
    {
        return licenseManagerProvider.get().getUsedLicenses().stream()
            .filter(license -> 0 <= license.getMaxUserCount() && license.getMaxUserCount() < Long.MAX_VALUE)
            .collect(Collectors.toSet());
    }
}
