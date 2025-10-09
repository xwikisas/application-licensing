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

import org.xwiki.job.AbstractRequest;
import org.xwiki.observation.event.Event;

/**
 * The request used to configure {@link UserLicenseDisablerJob}.
 *
 * @version $Id$
 * @since 3.5.2
 */
public class UserLicenseDisablerJobRequest extends AbstractRequest
{
    /**
     * The key of the event property.
     */
    public static final String PROPERTY_EVENT = "event";

    /**
     * The key of the data property.
     */
    public static final String PROPERTY_DATA = "data";

    /**
     * The key of the source property.
     */
    public static final String PROPERTY_SOURCE = "source";

    private static final long serialVersionUID = 1L;

    /**
     * Disable users who are over the license user limit, and re-enable those below the limit.
     *
     * @param event pass this parameter from the event listener which created this job
     * @param source pass this parameter from the event listener which created this job
     * @param data pass this parameter from the event listener which created this job
     */
    public UserLicenseDisablerJobRequest(Event event, Object source, Object data)
    {
        setProperty(PROPERTY_EVENT, event);
        setProperty(PROPERTY_SOURCE, data);
        setProperty(PROPERTY_DATA, source);
        setId(Arrays.asList("licensor", "userlimit", "disableAuthUsersOverLimit"));
        setStatusLogIsolated(false);
    }
}
