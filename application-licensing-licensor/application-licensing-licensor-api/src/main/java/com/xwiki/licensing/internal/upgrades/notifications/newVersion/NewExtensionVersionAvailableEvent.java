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

import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.extension.ExtensionId;

/**
 * The event send when a new version of a licensed extension is available.
 * 
 * @version $Id$
 * @since 1.23
 */
public class NewExtensionVersionAvailableEvent implements RecordableEvent
{
    /**
     * The name of this component.
     */
    public static final String EVENT_TYPE = "NewExtensionVersionAvailableEvent";

    private ExtensionId extensionId;

    private String namespace;

    /**
     * The default constructor.
     */
    public NewExtensionVersionAvailableEvent()
    {
    }

    /**
     * Created a new instance with the given data.
     *
     * @param extensionId the extension id of the new extension version detected
     * @param namespace the namespace where the new extension version was detected, where {@code null} means root
     *            namespace (i.e. all namespaces)
     */
    public NewExtensionVersionAvailableEvent(ExtensionId extensionId, String namespace)
    {
        this.extensionId = extensionId;
        this.namespace = namespace;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof NewExtensionVersionAvailableEvent;
    }

    /**
     * @return the extension id of the new version detected, containing information like id and version
     */
    public ExtensionId getExtensionId()
    {
        return this.extensionId;
    }

    /**
     * @return the namespace where the new extension version was detected. {@code null} means root namespace (i.e all
     *         namespaces)
     */
    public String getNamespace()
    {
        return this.namespace;
    }
}
