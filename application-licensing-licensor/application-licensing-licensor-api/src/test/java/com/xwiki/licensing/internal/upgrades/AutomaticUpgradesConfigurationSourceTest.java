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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Unit tests for {@link AutomaticUpgradesConfigurationSource}.
 *
 * @version $Id$
 * @since 1.17
 */
public class AutomaticUpgradesConfigurationSourceTest
{
    @Rule
    public MockitoComponentMockingRule<AutomaticUpgradesConfigurationSource> mocker =
        new MockitoComponentMockingRule<>(AutomaticUpgradesConfigurationSource.class);

    private BaseObject autoUpgradesObj;

    private XWiki xwiki;

    @Before
    public void configure() throws Exception
    {
        WikiDescriptorManager wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("currentWiki");

        Provider<XWikiContext> contextProvider = mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        XWikiContext xcontext = mock(XWikiContext.class);
        when(contextProvider.get()).thenReturn(xcontext);

        xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);

        XWikiDocument configDoc = mock(XWikiDocument.class);
        DocumentReference configDocRef =
            new DocumentReference("currentWiki", Arrays.asList("Licenses", "Code"), "LicensingConfig");
        when(xwiki.getDocument(configDocRef, xcontext)).thenReturn(configDoc);

        autoUpgradesObj = mock(BaseObject.class);
        when(configDoc.getXObject(AutomaticUpgradesConfigurationSource.AUTO_UPGRADES_CLASS))
            .thenReturn(autoUpgradesObj);
    }

    @Test
    public void getBlocklist() throws Exception
    {
        List<String> blocklist = Arrays.asList("extension1", "extension2");

        when(autoUpgradesObj.getListValue("blocklist")).thenReturn(blocklist);

        assertEquals(blocklist, mocker.getComponentUnderTest().getBlocklist());
    }

    @Test
    public void getBlocklistWithException() throws Exception
    {
        when(xwiki.getDocument(any(EntityReference.class), any(XWikiContext.class))).thenThrow(new XWikiException());

        assertEquals(Collections.emptyList(), mocker.getComponentUnderTest().getBlocklist());
    }
}
