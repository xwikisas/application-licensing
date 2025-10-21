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
import com.xwiki.licensing.LicensedFeatureId;

/**
 * Event regarding licenses.
 *
 * @version $Id$
 * @since 1.31
 */
public class LicenseEvent implements Event
{
    private License license;

    private LicensedFeatureId featureId;

    /**
     * Constructor for use in listeners to match any license.
     */
    public LicenseEvent()
    {
    }

    /**
     * Constructor for use in listeners to match licenses which manage a specific extension.
     *
     * @param featureId the feature id to match
     */
    public LicenseEvent(LicensedFeatureId featureId)
    {
        this.featureId = featureId;
    }

    /**
     * Default constructor for the event.
     *
     * @param license the license which was updated
     */
    public LicenseEvent(License license)
    {
        this.license = license;
    }

    /**
     * @return license which generated the event
     */
    public License getLicense()
    {
        return this.license;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        if (!(otherEvent instanceof LicenseEvent)) {
            return false;
        }
        if (this.license == null && this.featureId == null) {
            return true;
        } else if (this.featureId != null) {
            return this.getLicense().getFeatureIds().stream().anyMatch(fId -> fId.equals(featureId));
        } else {
            return this.license.equals(((LicenseEvent) otherEvent).license);
        }
    }
}
