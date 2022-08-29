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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.eventstream.RecordableEventConverter;

import com.xwiki.licensing.internal.upgrades.notifications.newVersion.NewExtensionVersionAvailableEvent;

/**
 * Add additional information to {@link ExtensionAutoUpgradedEvent}, {@link ExtensionAutoUpgradedFailedEvent} or
 * {@link NewExtensionVersionAvailableEvent}.
 *
 * @version $Id$
 * @since 1.17
 */
@Singleton
@Named(ExtensionEventConverter.NAME)
@Component
public class ExtensionEventConverter implements RecordableEventConverter
{
    /**
     * The name of this component.
     */
    public static final String NAME = "ExtensionEventConverter";

    @Inject
    private RecordableEventConverter defaultConverter;

    @Override
    public Event convert(RecordableEvent recordableEvent, String source, Object data) throws Exception
    {
        String upgradeMessage = (String) data;
        Event convertedEvent = this.defaultConverter.convert(recordableEvent, source, data);

        // Before 12.6 or if you use hibernate event store, the parameters you put in an event are not stored so
        // instead the message is stored directly on the notification body.
        convertedEvent.setBody(upgradeMessage);

        return convertedEvent;
    }

    @Override
    public List<RecordableEvent> getSupportedEvents()
    {
        return Arrays.asList(new ExtensionAutoUpgradedEvent(), new ExtensionAutoUpgradedFailedEvent(),
            new NewExtensionVersionAvailableEvent());
    }
}
