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

import java.util.Arrays;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

public class ExtensionAutoUpgradedEventDisplayerTest
{
    @Rule
    public MockitoComponentMockingRule<ExtensionAutoUpgradedEventDisplayer> mocker =
        new MockitoComponentMockingRule<>(ExtensionAutoUpgradedEventDisplayer.class);

    private TemplateManager templateManager;

    private XWikiContext xcontext;

    private XWiki xwiki;

    private BaseObject displayerObj;

    private XWikiDocument displayerDoc;

    @Before
    public void configure() throws Exception
    {
        this.templateManager = this.mocker.getInstance(TemplateManager.class);

        Provider<XWikiContext> contextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        this.xcontext = mock(XWikiContext.class);
        when(contextProvider.get()).thenReturn(this.xcontext);

        xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);

        displayerDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(ExtensionAutoUpgradedEventDisplayer.DISPLAYER_DOC, xcontext)).thenReturn(displayerDoc);

        displayerObj = mock(BaseObject.class);
        when(displayerDoc.getXObject(ExtensionAutoUpgradedEventDisplayer.DISPLAYER_OBJ, 0)).thenReturn(displayerObj);

    }

    @Test
    public void renderNotificationWithCustomTemplate() throws Exception
    {
        CompositeEvent eventNotification = mock(CompositeEvent.class);

        DocumentReference displayerDocRef = new DocumentReference("wiki", Arrays.asList("Notification"), "Displayer");
        when(displayerDoc.getDocumentReference()).thenReturn(displayerDocRef);

        String content = "content";
        when(displayerObj.getStringValue("notificationTemplate")).thenReturn(content);

        Template template = mock(Template.class);
        when(this.templateManager.createStringTemplate(content, displayerDocRef)).thenReturn(template);

        XDOM customTemplate = mock(XDOM.class);
        when(this.templateManager.execute(template)).thenReturn(customTemplate);

        assertEquals(customTemplate, this.mocker.getComponentUnderTest().renderNotification(eventNotification));
    }
    
    @Test
    public void renderNotificationWithDefaultTemplate() throws Exception
    {
        CompositeEvent eventNotification = mock(CompositeEvent.class);

        DocumentReference displayerDocRef = new DocumentReference("wiki", Arrays.asList("Notification"), "Displayer");
        when(displayerDoc.getDocumentReference()).thenReturn(displayerDocRef);

        String content = "content";
        when(displayerObj.getStringValue("notificationTemplate")).thenReturn(content);

        when(this.templateManager.createStringTemplate(content, displayerDocRef)).thenReturn(null);

        XDOM defaultTemplate = mock(XDOM.class);
        when(this.templateManager.execute("notification/default.vm")).thenReturn(defaultTemplate);

        assertEquals(defaultTemplate, this.mocker.getComponentUnderTest().renderNotification(eventNotification));
    }
    
    @Test
    public void renderNotificationWithNull() throws Exception
    {
        CompositeEvent eventNotification = mock(CompositeEvent.class);

        DocumentReference displayerDocRef = new DocumentReference("wiki", Arrays.asList("Notification"), "Displayer");
        when(displayerDoc.getDocumentReference()).thenReturn(displayerDocRef);

        String content = "content";
        when(displayerObj.getStringValue("notificationTemplate")).thenReturn(content);

        when(this.templateManager.createStringTemplate(content, displayerDocRef)).thenThrow(new Exception());

        assertNull(this.mocker.getComponentUnderTest().renderNotification(eventNotification));
    }
    
    @Test
    public void getSupportedEvents() throws Exception
    {
        assertEquals(ExtensionAutoUpgradedEventDisplayer.EVENTS, this.mocker.getComponentUnderTest().getSupportedEvents());
    }
}
