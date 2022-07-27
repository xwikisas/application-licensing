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
import java.util.Collections;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NewVersionNotificationManager}.
 *
 * @version $Id$
 * @since 1.23
 */
@ComponentTest
class NewVersionNotificationManagerTest
{
    @InjectMockComponents
    private NewVersionNotificationManager newVersionNotificationManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private Logger logger;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiDocument licensingDoc;

    @Mock
    private BaseObject newVersionObject1;

    @Mock
    private BaseObject newVersionObject2;

    @BeforeEach
    void configure() throws Exception
    {
        when(this.xcontextProvider.get()).thenReturn(xcontext);
        when(this.xcontext.getWiki()).thenReturn(xwiki);
        when(this.xwiki.getDocument(NewVersionNotificationManager.LICENSING_CONFIG_DOC, xcontext)).thenReturn(
            licensingDoc);
    }

    @Test
    void isNotificationAlreadySent() throws Exception
    {
        when(this.licensingDoc.getXObjects(NewVersionNotificationManager.NEW_VERSION_NOTIFICATION_CLASS)).thenReturn(
            Arrays.asList(newVersionObject1, newVersionObject2));

        when(newVersionObject1.getStringValue(NewVersionNotificationManager.EXTENSION_ID)).thenReturn("extension1");
        when(newVersionObject1.getStringValue(NewVersionNotificationManager.NAMESPACE)).thenReturn("root");
        when(newVersionObject1.getStringValue(NewVersionNotificationManager.VERSION)).thenReturn("1.1");

        when(newVersionObject2.getStringValue(NewVersionNotificationManager.EXTENSION_ID)).thenReturn("extension1");
        when(newVersionObject2.getStringValue(NewVersionNotificationManager.NAMESPACE)).thenReturn("xwiki:test");
        when(newVersionObject2.getStringValue(NewVersionNotificationManager.VERSION)).thenReturn("1.1");

        assertTrue(this.newVersionNotificationManager.isNotificationAlreadySent("extension1", "root", "1.1"));
    }

    @Test
    void isNotificationAlreadySentWithDifferentInfo() throws Exception
    {
        when(this.licensingDoc.getXObjects(NewVersionNotificationManager.NEW_VERSION_NOTIFICATION_CLASS)).thenReturn(
            Arrays.asList(newVersionObject1, null, newVersionObject2));

        when(newVersionObject1.getStringValue(NewVersionNotificationManager.EXTENSION_ID)).thenReturn("extension1");
        when(newVersionObject1.getStringValue(NewVersionNotificationManager.NAMESPACE)).thenReturn("root");
        when(newVersionObject1.getStringValue(NewVersionNotificationManager.VERSION)).thenReturn("1.0");

        when(newVersionObject2.getStringValue(NewVersionNotificationManager.EXTENSION_ID)).thenReturn("extension1");
        when(newVersionObject2.getStringValue(NewVersionNotificationManager.NAMESPACE)).thenReturn("xwiki:test");
        when(newVersionObject2.getStringValue(NewVersionNotificationManager.VERSION)).thenReturn("1.1");

        assertFalse(this.newVersionNotificationManager.isNotificationAlreadySent("extension1", "root", "1.1"));
    }

    @Test
    void markNotificationAsSent() throws Exception
    {
        when(this.licensingDoc.getXObjects(NewVersionNotificationManager.NEW_VERSION_NOTIFICATION_CLASS)).thenReturn(
            null);

        when(this.licensingDoc.createXObject(NewVersionNotificationManager.NEW_VERSION_NOTIFICATION_CLASS,
            xcontext)).thenReturn(0);
        when(this.licensingDoc.getXObject(NewVersionNotificationManager.NEW_VERSION_NOTIFICATION_CLASS, 0)).thenReturn(
            this.newVersionObject1);

        this.newVersionNotificationManager.markNotificationAsSent("extension1", "root", "2.1");

        verify(this.newVersionObject1, times(1)).setStringValue(NewVersionNotificationManager.EXTENSION_ID,
            "extension1");
        verify(this.newVersionObject1, times(1)).setStringValue(NewVersionNotificationManager.NAMESPACE, "root");
        verify(this.newVersionObject1, times(1)).setStringValue(NewVersionNotificationManager.VERSION, "2.1");
        verify(this.xwiki, times(1)).saveDocument(any(XWikiDocument.class),
            eq("Added NewVersionNotificationClass object for extension1."), any(XWikiContext.class));
    }

    @Test
    void markNotificationAsSentOnSameNamespace() throws Exception
    {
        when(this.licensingDoc.getXObjects(NewVersionNotificationManager.NEW_VERSION_NOTIFICATION_CLASS)).thenReturn(
            Collections.singletonList(newVersionObject1));

        when(newVersionObject1.getStringValue(NewVersionNotificationManager.EXTENSION_ID)).thenReturn("extension1");
        when(newVersionObject1.getStringValue(NewVersionNotificationManager.NAMESPACE)).thenReturn("root");
        when(newVersionObject1.getStringValue(NewVersionNotificationManager.VERSION)).thenReturn("1.0");

        this.newVersionNotificationManager.markNotificationAsSent("extension1", "root", "1.1");

        verify(this.newVersionObject1, times(1)).setStringValue(NewVersionNotificationManager.VERSION, "1.1");
        verify(this.xwiki, times(1)).saveDocument(any(XWikiDocument.class),
            eq("Added NewVersionNotificationClass object for extension1."), any(XWikiContext.class));
    }

    @Test
    void markNotificationAsSentOnDifferentNamespace() throws Exception
    {
        when(this.licensingDoc.getXObjects(NewVersionNotificationManager.NEW_VERSION_NOTIFICATION_CLASS)).thenReturn(
            Arrays.asList(newVersionObject1, null));

        when(newVersionObject1.getStringValue(NewVersionNotificationManager.EXTENSION_ID)).thenReturn("extension1");
        when(newVersionObject1.getStringValue(NewVersionNotificationManager.NAMESPACE)).thenReturn("root");
        when(newVersionObject1.getStringValue(NewVersionNotificationManager.VERSION)).thenReturn("1.0");

        when(this.licensingDoc.createXObject(NewVersionNotificationManager.NEW_VERSION_NOTIFICATION_CLASS,
            xcontext)).thenReturn(1);
        when(this.licensingDoc.getXObject(NewVersionNotificationManager.NEW_VERSION_NOTIFICATION_CLASS, 1)).thenReturn(
            this.newVersionObject2);

        this.newVersionNotificationManager.markNotificationAsSent("extension1", "xwiki:test", "1.1");

        verify(this.newVersionObject1, never()).setStringValue(NewVersionNotificationManager.VERSION, "1.1");
        verify(this.newVersionObject2, times(1)).setStringValue(NewVersionNotificationManager.EXTENSION_ID,
            "extension1");
        verify(this.newVersionObject2, times(1)).setStringValue(NewVersionNotificationManager.NAMESPACE, "xwiki:test");
        verify(this.newVersionObject2, times(1)).setStringValue(NewVersionNotificationManager.VERSION, "1.1");
        verify(this.xwiki, times(1)).saveDocument(any(XWikiDocument.class),
            eq("Added NewVersionNotificationClass object for extension1."), any(XWikiContext.class));
    }
}
