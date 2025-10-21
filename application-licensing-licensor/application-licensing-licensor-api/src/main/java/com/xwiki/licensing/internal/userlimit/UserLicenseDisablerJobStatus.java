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

import org.xwiki.job.DefaultJobStatus;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.logging.LoggerManager;
import org.xwiki.observation.ObservationManager;

import static com.xwiki.licensing.internal.userlimit.UserLicenseDisablerJob.JOB_TYPE;

/**
 * The status associated with the job created by {@link UserLicenseDisablerJobRequest}.
 *
 * @version $Id$
 * @since 3.5.2
 */
public class UserLicenseDisablerJobStatus extends DefaultJobStatus<UserLicenseDisablerJobRequest>
{
    /**
     * @param request the request provided when started the job
     * @param parentJobStatus the status of the parent job
     * @param observationManager the observation manager component
     * @param loggerManager the logger manager component
     */
    public UserLicenseDisablerJobStatus(UserLicenseDisablerJobRequest request, JobStatus parentJobStatus,
        ObservationManager observationManager, LoggerManager loggerManager)
    {
        super(JOB_TYPE, request, parentJobStatus, observationManager,
            loggerManager);
    }
}
