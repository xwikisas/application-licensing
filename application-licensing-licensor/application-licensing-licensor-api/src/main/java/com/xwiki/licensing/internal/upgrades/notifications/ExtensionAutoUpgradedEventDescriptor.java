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

package com.xwiki.licensing.internal.upgrades.notifications;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.RecordableEventDescriptor;

/**
 * Description of the ExtensionAutoUpgraded notification. Used for displaying settings in Notifications Preferences.
 * 
 * @version $Id$
 * @since 1.17
 */
@Component
@Singleton
@Named(ExtensionAutoUpgradedEventDescriptor.NAME)
public class ExtensionAutoUpgradedEventDescriptor implements RecordableEventDescriptor
{
    /**
     * The name used for this component.
     */
    public static final String NAME = "ExtensionAutoUpgradedEventDescriptor";

    @Override
    public String getApplicationIcon()
    {
        return "arrow_up";
    }

    @Override
    public String getApplicationName()
    {
        return "licensor.notifications.event.type";
    }

    @Override
    public String getDescription()
    {
        return "licensor.notifications.event.type.description";
    }

    @Override
    public String getEventType()
    {
        return ExtensionAutoUpgradedEvent.class.getCanonicalName();
    }

}
