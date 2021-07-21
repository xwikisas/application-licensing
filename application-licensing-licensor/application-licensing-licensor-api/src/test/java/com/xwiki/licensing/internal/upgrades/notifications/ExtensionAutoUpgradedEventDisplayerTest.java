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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.inject.Provider;
import javax.script.ScriptContext;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Unit tests for {@link ExtensionAutoUpgradedEventDisplayer}.
 *
 * @version $Id$
 * @since 1.17
 */
public class ExtensionAutoUpgradedEventDisplayerTest
{
    @Rule
    public MockitoComponentMockingRule<ExtensionAutoUpgradedEventDisplayer> mocker =
        new MockitoComponentMockingRule<>(ExtensionAutoUpgradedEventDisplayer.class);

    private TemplateManager templateManager;

    private ScriptContextManager scriptContextManager;

    private XWikiContext xcontext;

    private XWiki xwiki;

    private XWikiDocument licensorDoc;

    CompositeEvent eventNotification = mock(CompositeEvent.class);

    @Before
    public void configure() throws Exception
    {
        this.templateManager = this.mocker.getInstance(TemplateManager.class);
        this.scriptContextManager = this.mocker.getInstance(ScriptContextManager.class);

        Provider<XWikiContext> contextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        this.xcontext = mock(XWikiContext.class);
        when(contextProvider.get()).thenReturn(this.xcontext);

        ScriptContext scriptContext = mock(ScriptContext.class);
        when(this.scriptContextManager.getScriptContext()).thenReturn(scriptContext);

        this.xwiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);

        this.licensorDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(ExtensionAutoUpgradedEventDisplayer.LICENSOR_DOC, this.xcontext))
            .thenReturn(this.licensorDoc);

    }

    @Test
    public void renderNotificationWithCustomTemplate() throws Exception
    {
        Template customTemplate = mock(Template.class);
        XDOM customTemplateXDOM = mock(XDOM.class);

        when(this.templateManager.getTemplate("extensionAutoUpgraded/extensionAutoUpgraded.vm"))
            .thenReturn(customTemplate);
        when(this.templateManager.execute(customTemplate)).thenReturn(customTemplateXDOM);

        assertEquals(customTemplateXDOM,
            this.mocker.getComponentUnderTest().renderNotification(this.eventNotification));
    }

    @Test
    public void renderNotificationWithDefaultTemplate() throws Exception
    {
        XDOM defaultTemplateXDOM = mock(XDOM.class);

        when(this.templateManager.getTemplate("extensionAutoUpgraded/extensionAutoUpgraded.vm")).thenReturn(null);
        when(this.templateManager.execute("notification/default.vm")).thenReturn(defaultTemplateXDOM);

        assertEquals(defaultTemplateXDOM,
            this.mocker.getComponentUnderTest().renderNotification(this.eventNotification));
    }

    @Test
    public void renderNotificationWithNull() throws Exception
    {
        Template customTemplate = mock(Template.class);

        when(this.templateManager.getTemplate("extensionAutoUpgraded/extensionAutoUpgraded.vm"))
            .thenReturn(customTemplate);
        when(this.templateManager.execute(customTemplate)).thenThrow(new Exception());

        assertNull(this.mocker.getComponentUnderTest().renderNotification(this.eventNotification));
    }

    @Test
    public void getSupportedEvents() throws Exception
    {
        assertEquals(ExtensionAutoUpgradedEventDisplayer.EVENTS,
            this.mocker.getComponentUnderTest().getSupportedEvents());
    }
}
