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

import org.xwiki.eventstream.RecordableEvent;

import com.xwiki.licensing.License;

/**
 * Notification event for licenses which have almost reached their user limit.
 *
 * @version $Id$
 */
public class LicenseUserLimitWarningEvent implements RecordableEvent
{
    private final License license;

    private final long userDiff;

    /**
     * Constructor for listeners and matchers.
     */
    public LicenseUserLimitWarningEvent()
    {
        this.license = null;
        this.userDiff = 0;
    }

    /**
     * Default constructor.
     *
     * @param license the license with the user limit which was almost exceeded
     * @param userDiff the number of users left until the user limit is exceeded
     */
    public LicenseUserLimitWarningEvent(License license, long userDiff)
    {
        this.license = license;
        this.userDiff = userDiff;
    }

    /**
     * Get the license which generated the event.
     *
     * @return license
     */
    public License getLicense()
    {
        return license;
    }

    /**
     * Get the number of users left for the license.
     *
     * @return the number of users left until the license user limit is exceeded
     */
    public long getUserDiff()
    {
        return userDiff;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof LicenseUserLimitWarningEvent;
//        if (otherEvent instanceof LicenseUserLimitWarningEvent) {
//            return this.license == null || ((LicenseUserLimitWarningEvent) otherEvent).license == null
//                || this.license.getId().equals(((LicenseUserLimitWarningEvent) otherEvent).license.getId());
//        } else {
//            return false;
//        }
    }

//    @Override
//    public Set<String> getTarget()
//    {
//        // TODO: Maybe make this customizable from the licensor Admin Section.
//        return Set.of("xwiki:XWiki.XWikiAdminGroup");
//    }
}
