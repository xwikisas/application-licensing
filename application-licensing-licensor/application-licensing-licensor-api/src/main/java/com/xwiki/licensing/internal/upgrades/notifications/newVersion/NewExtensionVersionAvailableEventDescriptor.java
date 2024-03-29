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

package com.xwiki.licensing.internal.upgrades.notifications.newVersion;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.RecordableEventDescriptor;

/**
 * Description of the {@link NewExtensionVersionAvailableEvent}. Used for displaying settings in Notifications
 * Preferences.
 *
 * @since 1.23
 * @version $Id$
 */
@Component
@Singleton
@Named(NewExtensionVersionAvailableEventDescriptor.NAME)
public class NewExtensionVersionAvailableEventDescriptor implements RecordableEventDescriptor
{
    /**
     * The name of this component.
     */
    public static final String NAME = "NewExtensionVersionAvailableEventDescriptor";

    @Override
    public String getApplicationIcon()
    {
        return "arrow_up";
    }

    @Override
    public String getApplicationName()
    {
        return "licensor.notification.newVersion.name";
    }

    @Override
    public String getDescription()
    {
        return "licensor.notification.newVersion.description";
    }

    @Override
    public String getEventType()
    {
        return NewExtensionVersionAvailableEvent.class.getCanonicalName();
    }

}
