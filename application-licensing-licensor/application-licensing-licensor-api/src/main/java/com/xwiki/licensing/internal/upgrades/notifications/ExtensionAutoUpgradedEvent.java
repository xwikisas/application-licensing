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

import java.util.Set;

import com.xwiki.licensing.internal.upgrades.AbstractLicensorTargetableEvent;

/**
 * The event send when an application is automatically upgraded. Used in UpgradeExtensionHandler.
 *
 * @version $Id$
 * @since 1.17
 */
public class ExtensionAutoUpgradedEvent extends AbstractLicensorTargetableEvent
{
    /**
     * The event type used for this component.
     */
    public static final String EVENT_TYPE = "ExtensionAutoUpgradedEvent";

    /**
     * The default constructor.
     */
    public ExtensionAutoUpgradedEvent()
    {
    }

    /**
     * Created a new instance with the given data.
     *
     * @param notifiedGroups the groups that should be notified about the new upgrade. An empty {@link Set} means
     *     all users will be notified, no matter the group
     */
    public ExtensionAutoUpgradedEvent(Set<String> notifiedGroups)
    {
        this.setNotifiedGroups(notifiedGroups);
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof ExtensionAutoUpgradedEvent;
    }
}
