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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.NotificationDisplayer;
import org.xwiki.rendering.block.Block;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;

import com.xpn.xwiki.XWikiContext;

/**
 * Display a custom template for NewExtensionVersionAvailableEvent.
 *
 * @since 1.23
 * @version $Id$
 */
@Component
@Singleton
@Named(NewExtensionVersionAvailableEventDisplayer.NAME)
public class NewExtensionVersionAvailableEventDisplayer implements NotificationDisplayer
{
    protected static final String NAME = "NewExtensionVersionAvailableEventDisplayer";

    protected static final List<String> EVENTS =
        Arrays.asList(NewExtensionVersionAvailableEvent.class.getCanonicalName());

    protected static final LocalDocumentReference LICENSOR_DOC = new LocalDocumentReference("Licenses", "WebHome");

    protected static final String EVENT_BINDING_NAME = "event";

    @Inject
    private TemplateManager templateManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private Logger logger;

    @Override
    public Block renderNotification(CompositeEvent eventNotification) throws NotificationException
    {
        XWikiContext xcontext = contextProvider.get();
        ScriptContext scriptContext = this.scriptContextManager.getScriptContext();
        Template customTemplate = this.templateManager.getTemplate("newVersionAvailable/newVersionAvailable.vm");

        try {
            // Set a document in the context to act as the current document when the template is rendered.
            xcontext.setDoc(xcontext.getWiki().getDocument(LICENSOR_DOC, xcontext));
            // Bind the event to some variable in the velocity context.
            scriptContext.setAttribute(EVENT_BINDING_NAME, eventNotification, ScriptContext.ENGINE_SCOPE);

            return this.templateManager.execute(customTemplate);
        } catch (Exception e) {
            logger.warn("Failed to render custom template. Root cause is: [{}]", ExceptionUtils.getRootCauseMessage(e));
        }
        return null;
    }

    @Override
    public List<String> getSupportedEvents()
    {
        return EVENTS;
    }
}
