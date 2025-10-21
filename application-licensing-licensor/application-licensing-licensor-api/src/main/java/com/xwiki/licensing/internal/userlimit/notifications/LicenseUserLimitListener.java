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
package com.xwiki.licensing.internal.userlimit.notifications;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseManager;
import com.xwiki.licensing.internal.UserCounter;
import com.xwiki.licensing.internal.helpers.events.LicenseEvent;

/**
 * Listener which sends notifications when a license user limit is almost exceeded.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named("com.xwiki.licensing.internal.userlimit.notifications.LicenseUserLimitListener")
public class LicenseUserLimitListener extends AbstractEventListener
{
    private static final LocalDocumentReference USER_CLASS = new LocalDocumentReference("XWiki", "XWikiUsers");

    private static final EventFilter XWIKI_SPACE_FILTER = new RegexEventFilter("(.*:)?XWiki\\..*");

    @Inject
    private Logger logger;

    @Inject
    private LicenseManager licenseManager;

    @Inject
    private UserCounter userCounter;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private ObservationManager observationManager;

    /**
     * Default constructor.
     */
    public LicenseUserLimitListener()
    {
        super(LicenseUserLimitListener.class.getName(),
            List.of(new DocumentUpdatedEvent(XWIKI_SPACE_FILTER), new DocumentCreatedEvent(XWIKI_SPACE_FILTER),
                new LicenseEvent()));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        long userCount;
        // source = XWikiDocument; data = XWikiContext;
        try {
            userCount = userCounter.getUserCount();
        } catch (Exception e) {
            logger.error("Failed to get count of users for license user limit warning. Cause: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            return;
        }

        if (event instanceof AbstractDocumentEvent) {
            AbstractDocumentEvent documentEvent = (AbstractDocumentEvent) event;
            try {
                if (null == xcontextProvider.get().getWiki()
                    .getDocument(documentEvent.getDocumentReference(), xcontextProvider.get()).getXObject(USER_CLASS))
                {
                    // If the document is not a user page, return.
                    return;
                }
            } catch (XWikiException e) {
                logger.error("Failed to verify user page [{}] for license user limit warning. Cause: [{}]",
                    documentEvent.getDocumentReference(), ExceptionUtils.getRootCauseMessage(e));
                return;
                // TODO: Continue execution for any page?
            }
            Collection<License> potentiallyInvalidatedLicenses = getActiveLicensesWithUserLimits();
            for (License license : potentiallyInvalidatedLicenses) {
                notifyForLicense(license, userCount, source, data);
            }
        } else if (event instanceof LicenseEvent) {
            // See if the new license exceeds the user limit.
            notifyForLicense(((LicenseEvent) event).getLicense(), userCount, source, data);
        }
    }

    private void notifyForLicense(License license, long userCount, Object source, Object data)
    {
        long userDiff = license.getMaxUserCount() - userCount;
        if (userDiff < 0) {
            // User count exceeded.
        } else if (userDiff < getUserNotificationThreshold()) {
            observationManager.notify(new LicenseUserLimitWarningEvent(license, userDiff),
                "com.xwiki.licensing:application-licensing-licensor-api",
                new XWikiDocument(new DocumentReference("xwiki", "Licenses", "WebHome")));
        }
    }

    private long getUserNotificationThreshold()
    {
        // TODO: Is it worth it to have customized/multiple thresholds?
        // Example: On 10 users left, notify XWikiAdminGroup. On 5 users left, notify regular users too.
        return 5;
    }

    private Collection<License> getActiveLicensesWithUserLimits()
    {
        return licenseManager.getUsedLicenses().stream()
            .filter(license -> 0 <= license.getMaxUserCount() && license.getMaxUserCount() < Long.MAX_VALUE)
            .collect(Collectors.toSet());
    }
}
