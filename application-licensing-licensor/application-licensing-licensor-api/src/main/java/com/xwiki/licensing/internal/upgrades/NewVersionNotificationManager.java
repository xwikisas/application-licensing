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
package com.xwiki.licensing.internal.upgrades;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Check and update information about notifications sent when {@code NewExtensionVersionAvailableEvent} events are
 * triggered.
 *
 * @version $Id$
 * @since 1.23
 */
@Component(roles = NewVersionNotificationManager.class)
@Singleton
public class NewVersionNotificationManager
{
    protected static final String EXTENSION_NAME = "extensionName";

    protected static final String NAMESPACE = "namespace";

    protected static final String VERSION = "version";

    protected static final List<String> CODE_SPACE = Arrays.asList("Licenses", "Code");

    protected static final LocalDocumentReference LICENSING_CONFIG_DOC =
        new LocalDocumentReference(CODE_SPACE, "LicensingConfig");

    protected static final LocalDocumentReference NEW_VERSION_NOTIFICATION_CLASS =
        new LocalDocumentReference(CODE_SPACE, "NewVersionNotificationClass");

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Check if a notification for this new extension version was already sent. Consider also the namespace since
     * different versions can be installed on different namespaces.
     *
     * @param extensionName the name of the extension
     * @param namespaceName the targeted namespace name
     * @param version the new version of the extension
     * @return {@code true} if a notification was already sent with this exact information, or {@code false} otherwise
     */
    public boolean isNotificationAlreadySent(String extensionName, String namespaceName, String version)
    {
        try {
            XWikiContext xcontext = contextProvider.get();
            List<BaseObject> versionNotifObjects = xcontext.getWiki().getDocument(LICENSING_CONFIG_DOC, xcontext)
                .getXObjects(NEW_VERSION_NOTIFICATION_CLASS);

            return versionNotifObjects != null && versionNotifObjects.stream().filter(
                obj -> obj != null && objectHasValue(obj, EXTENSION_NAME, extensionName) && objectHasValue(obj,
                    NAMESPACE, namespaceName) && objectHasValue(obj, VERSION, version)).count() > 0;
        } catch (XWikiException e) {
            logger.warn("Failed to check if a NewVersionNotification was already sent for [{}]. Root cause is: [{}]",
                extensionName, ExceptionUtils.getRootCauseMessage(e));
        }

        return false;
    }

    /**
     * Mark that a notification was sent for this new extension version.
     *
     * @param extensionName the name of the extension
     * @param namespaceName the targeted namespace name
     * @param version the new version of the extension
     */
    public void markNotificationAsSent(String extensionName, String namespaceName, String version)
    {
        try {
            XWikiContext xcontext = contextProvider.get();
            XWikiDocument configDoc = xcontext.getWiki().getDocument(LICENSING_CONFIG_DOC, xcontext);
            List<BaseObject> versionNotifObjects = configDoc.getXObjects(NEW_VERSION_NOTIFICATION_CLASS);

            boolean notificationObjectExists = false;
            if (versionNotifObjects != null) {
                notificationObjectExists =
                    updateExistingNotification(versionNotifObjects, extensionName, namespaceName, version);
            }

            if (!notificationObjectExists) {
                int id = configDoc.createXObject(NEW_VERSION_NOTIFICATION_CLASS, xcontext);
                BaseObject obj = configDoc.getXObject(NEW_VERSION_NOTIFICATION_CLASS, id);
                obj.setStringValue(EXTENSION_NAME, extensionName);
                obj.setStringValue(NAMESPACE, namespaceName);
                obj.setStringValue(VERSION, version);
            }

            xcontext.getWiki().saveDocument(configDoc,
                String.format("Added NewVersionNotificationClass object for %s.", extensionName), xcontext);
        } catch (XWikiException e) {
            logger.warn("Failed add NewVersionNotificationClass object for [{}]. Root cause is: [{}]", extensionName,
                ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private boolean updateExistingNotification(List<BaseObject> versionNotifObjects, String extensionName,
        String namespaceName, String version)
    {
        boolean notificationObjectExists = false;
        for (BaseObject obj : versionNotifObjects) {
            if (obj == null) {
                continue;
            }
            // If there is already a notification sent for this extensionId and namespace, update only the
            // version.
            if (objectHasValue(obj, EXTENSION_NAME, extensionName) && objectHasValue(obj, NAMESPACE, namespaceName)) {
                if (!objectHasValue(obj, VERSION, version)) {
                    obj.setStringValue(VERSION, version);
                }
                notificationObjectExists = true;
            }
        }
        return notificationObjectExists;
    }

    private boolean objectHasValue(BaseObject obj, String name, String value)
    {
        return obj.getStringValue(name) != null && obj.getStringValue(name).equals(value);
    }
}
