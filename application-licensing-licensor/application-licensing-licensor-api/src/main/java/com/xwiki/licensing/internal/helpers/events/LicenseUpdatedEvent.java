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

import org.xwiki.extension.event.AbstractExtensionEvent;
import org.xwiki.observation.event.Event;

import com.xwiki.licensing.License;

/**
 * Use {@link AbstractExtensionEvent}?
 *
 * @version $Id$
 * @since 1.31
 */
public class LicenseUpdatedEvent implements Event
{
    private EventType eventType;

    private License license;

    private String extensionId;

    /**
     * How the event was generated. Should probably be done through inheritance instead.
     */
    public enum EventType
    {
        /**
         * The license was newly created.
         */
        CREATED,
        /**
         * The license was updated/renewed.
         */
        UPDATED,
        /**
         * The license was newly deleted.
         */
        DELETED
    }

    /**
     * Constructor for use in listeners to match any license.
     */
    public LicenseUpdatedEvent()
    {
    }

    /**
     * Constructor for use in listeners to match on a specific extension license.
     *
     * @param extensionId the extension id to match (without version)
     */
    public LicenseUpdatedEvent(String extensionId)
    {
        this.extensionId = extensionId;
    }

    /**
     * Default constructor for the event.
     *
     * @param license the license which was updated
     * @param eventType type of license event
     */
    public LicenseUpdatedEvent(License license, EventType eventType)
    {
        this.license = license;
        this.eventType = eventType;
    }

    /**
     * @return event type
     */
    public EventType getEventType()
    {
        return eventType;
    }

    /**
     * @return extension id
     */
    public String getExtensionId()
    {
        return extensionId;
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
        if (!(otherEvent instanceof LicenseUpdatedEvent)) {
            return false;
        }
        LicenseUpdatedEvent otherLicenseEvent = (LicenseUpdatedEvent) otherEvent;
        if ((this.license == null && this.extensionId == null) || (otherLicenseEvent.license == null
            && otherLicenseEvent.extensionId == null))
        {
            // Match any event.
            return true;
        }

        if (this.extensionId == null && otherLicenseEvent.extensionId == null) {
            // Match exact event.
            return this.license.getId().equals(otherLicenseEvent.license.getId());
        } else if (this.extensionId != null && otherLicenseEvent.extensionId != null) {
            return this.extensionId.equals(otherLicenseEvent.extensionId);
        } else {
            return this.license.getFeatureIds().stream()
                .anyMatch((licensedFeatureId -> licensedFeatureId.getId().equals(otherLicenseEvent.extensionId)));
        }
    }
}
