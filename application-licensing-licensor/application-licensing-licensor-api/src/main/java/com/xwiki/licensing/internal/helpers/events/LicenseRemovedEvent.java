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
 * An event triggered after a license is removed.
 *
 * @version $Id$
 * @since 1.31
 */
public class LicenseRemovedEvent extends LicenseOperationEvent
{
    /**
     * For use in listeners to match any license removed event.
     */
    public LicenseRemovedEvent()
    {
        super();
    }

    /**
     * For use in listeners to match any license managing the given feature.
     *
     * @param featureId the feature id to match
     */
    public LicenseRemovedEvent(LicensedFeatureId featureId)
    {
        super(featureId);
    }

    /**
     * Default constructor for events.
     *
     * @param license the license which was removed
     */
    public LicenseRemovedEvent(License license)
    {
        super(license);
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        if (!(otherEvent instanceof LicenseRemovedEvent)) {
            return false;
        }
        return super.matches(otherEvent);
    }
}
