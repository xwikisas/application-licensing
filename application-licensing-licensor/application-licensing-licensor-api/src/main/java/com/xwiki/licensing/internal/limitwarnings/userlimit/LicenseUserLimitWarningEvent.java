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
import java.util.Set;

import org.xwiki.eventstream.TargetableEvent;

import com.xwiki.licensing.License;

/**
 * Notification event for licenses which have almost reached their user limit.
 * TODO: After the parent is >15.5, implement a Notification Grouping Strategy so licensor emails are not bundled
 *  together with other notifications, similar to the mentions macro emails.
 *
 * @version $Id$
 */
public class LicenseUserLimitWarningEvent implements TargetableEvent
{
    /**
     * The name of components related to this event.
     */
    public static final String EVENT_TYPE =
        "com.xwiki.licensing.internal.limitwarnings.userlimit.LicenseUserLimitWarningEvent";

    /**
     * Licenses which risk to be invalid because of the user limit.
     */
    private final Collection<License> licenses;

    /**
     * The number of users on the instance.
     */
    private final long userCount;

    /**
     * Constructor for listeners and matchers.
     */
    public LicenseUserLimitWarningEvent()
    {
        this.licenses = Set.of();
        this.userCount = 0;
    }

    /**
     * Default constructor.
     *
     * @param licenses the licenses for which the user limit which was almost exceeded
     * @param userCount the number of users on the instance
     */
    public LicenseUserLimitWarningEvent(Collection<License> licenses, long userCount)
    {
        this.licenses = licenses;
        this.userCount = userCount;
    }

    /**
     * Get the license which generated the event.
     *
     * @return license
     */
    public Collection<License> getLicenses()
    {
        return licenses;
    }

    /**
     * Get the number of users left for the license.
     *
     * @return the number of users left until the license user limit is exceeded
     */
    public long getUserCount()
    {
        return userCount;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof LicenseUserLimitWarningEvent;
    }

    @Override
    public Set<String> getTarget()
    {
        // TODO: Make this customizable from the licensor Admin Section.
        return Set.of("xwiki:XWiki.XWikiAdminGroup", "XWiki.XWikiAdminGroup", "XWikiAdminGroup");
    }
}
