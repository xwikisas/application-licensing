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

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicensedFeatureId;

/**
 * An event triggered after a license is updated.
 *
 * @version $Id$
 * @since 1.31
 */
public class LicenseUpdatedEvent extends LicenseEvent
{
    private License oldLicense;

    /**
     * Match any license updated event.
     */
    public LicenseUpdatedEvent()
    {
        super();
    }

    /**
     * Match any license managing the given feature.
     *
     * @param featureId
     */
    public LicenseUpdatedEvent(LicensedFeatureId featureId)
    {
        super(featureId);
    }

    /**
     * Default constructor for events.
     *
     * @param license the new version of the license which was updated
     */
    public LicenseUpdatedEvent(License license)
    {
        super(license);
    }

    /**
     * Constructor for events.
     *
     * @param newLicense the new version of the license which was updated
     * @param oldLicense the old version of the license which was updated
     */
    public LicenseUpdatedEvent(License newLicense, License oldLicense)
    {
        super(newLicense);
        this.oldLicense = oldLicense;
    }

    /**
     * @return the old version of the license which was updated
     */
    public License getOldLicense()
    {
        return this.oldLicense;
    }

    /**
     * @param oldLicense the old version of the license which was updated
     */
    public void setOldLicense(License oldLicense)
    {
        this.oldLicense = oldLicense;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        if (!(otherEvent instanceof LicenseUpdatedEvent)) {
            return false;
        }
        return super.matches(otherEvent);
    }
}
