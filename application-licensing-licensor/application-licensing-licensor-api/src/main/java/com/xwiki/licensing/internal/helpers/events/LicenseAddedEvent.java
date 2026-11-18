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
package com.xwiki.licensing.internal.helpers.events;

import org.xwiki.observation.event.Event;

import com.xwiki.licensing.License;

/**
 * An event triggered after a license is added.
 *
 * @version $Id$
 * @since 1.34
 */
public class LicenseAddedEvent implements Event
{
    private License license;

    /**
     * For use in listeners to match any license added event.
     */
    public LicenseAddedEvent()
    {
    }

    /**
     * Default constructor for events.
     *
     * @param license the license which was added
     */
    public LicenseAddedEvent(License license)
    {
        this.license = license;
    }

    /**
     * @return the license which was added
     */
    public License getLicense()
    {
        return this.license;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof LicenseAddedEvent
            && (this.license == null || this.license.equals(((LicenseAddedEvent) otherEvent).license));
    }
}
