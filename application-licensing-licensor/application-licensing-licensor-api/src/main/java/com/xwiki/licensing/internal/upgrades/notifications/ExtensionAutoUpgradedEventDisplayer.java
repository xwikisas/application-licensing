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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.NotificationDisplayer;
import org.xwiki.rendering.block.Block;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Displaying a custom template for ExtensionAutoUpgradedEvent or ExtensionAutoUpgradedFailedEvent. The
 * NotificationDisplayerClass XObject should work independently of this class, but it is unstable on older versions and
 * sometimes the default template is rendered. After upgrading to 10.x XWiki parent only the XObject should be used and
 * this class removed.
 * 
 * @version $Id$
 * @since 1.17
 */
@Component
@Singleton
@Named(ExtensionAutoUpgradedEventDisplayer.NAME)
public class ExtensionAutoUpgradedEventDisplayer implements NotificationDisplayer
{
    protected static final String CODE_SPACE = "Code";

    protected static final String NAME = "ExtensionAutoUpgradedEventDisplayer";

    protected static final List<String> EVENTS = Arrays.asList(ExtensionAutoUpgradedEvent.class.getCanonicalName(),
        ExtensionAutoUpgradedFailedEvent.class.getCanonicalName());

    protected static final LocalDocumentReference DISPLAYER_DOC =
        new LocalDocumentReference(Arrays.asList("Licenses", CODE_SPACE), NAME);

    protected static final LocalDocumentReference DISPLAYER_OBJ =
        new LocalDocumentReference(Arrays.asList("XWiki", "Notifications", CODE_SPACE), "NotificationDisplayerClass");

    @Inject
    private TemplateManager templateManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    @Override
    public Block renderNotification(CompositeEvent eventNotification) throws NotificationException
    {
        XWikiContext xcontext = contextProvider.get();

        try {
            XWikiDocument displayerDoc = xcontext.getWiki().getDocument(DISPLAYER_DOC, xcontext);
            BaseObject displayerObj = displayerDoc.getXObject(DISPLAYER_OBJ, 0);

            Template customTemplate = templateManager.createStringTemplate(
                displayerObj.getStringValue("notificationTemplate"), displayerDoc.getAuthorReference());
            return (customTemplate != null) ? this.templateManager.execute(customTemplate)
                : this.templateManager.execute("notification/default.vm");
        } catch (Exception e) {
            logger.warn("Failed to render the template for ExtensionAutoUpgradedEvent. Root cause is: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
        }
        return null;
    }

    @Override
    public List<String> getSupportedEvents()
    {
        return EVENTS;
    }
}
