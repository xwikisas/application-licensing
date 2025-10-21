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
package com.xwiki.licensing.internal.userlimit;

import java.util.Arrays;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.event.filter.EventFilter;
import org.xwiki.observation.event.filter.RegexEventFilter;

import com.xwiki.licensing.internal.helpers.events.LicenseUpdatedEvent;

import static com.xwiki.licensing.internal.userlimit.UserLicenseDisablerJob.JOB_TYPE;
import static com.xwiki.licensing.internal.userlimit.UserLicenseEnforcer.USER_LICENSE_FOLD_EVENT_MATCHER;

/**
 * Disable users who are over the user limit of the license.
 *
 * @version $Id$
 * @since 1.31
 */
@Component
@Singleton
@Named(UserLicenseDisabler.ROLE_NAME)
public class UserLicenseDisabler extends AbstractEventListener
{
    /**
     * The role name of this component.
     */
    public static final String ROLE_NAME = "com.xwiki.licensing.internal.userlimit.UserLicenseDisabler";

    private static final EventFilter XWIKI_SPACE_FILTER = new RegexEventFilter("^(.*:)?XWiki\\..*");

    @Inject
    private ObservationContext observationContext;

    @Inject
    private JobExecutor jobExecutor;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public UserLicenseDisabler()
    {
        super(ROLE_NAME,
            Arrays.asList(new DocumentCreatedEvent(XWIKI_SPACE_FILTER), new DocumentUpdatedEvent(XWIKI_SPACE_FILTER),
                new DocumentDeletedEvent(XWIKI_SPACE_FILTER), new LicenseUpdatedEvent()));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (observationContext.isIn(USER_LICENSE_FOLD_EVENT_MATCHER)) {
            return;
        }

        UserLicenseDisablerJobRequest jobRequest = new UserLicenseDisablerJobRequest(event, source, data);
        try {
            jobExecutor.execute(JOB_TYPE, jobRequest);
        } catch (JobException e) {
            logger.error("bababooey", e);
        }
    }
}
