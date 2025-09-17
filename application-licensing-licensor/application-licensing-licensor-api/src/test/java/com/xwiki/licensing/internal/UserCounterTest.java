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
package com.xwiki.licensing.internal;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.observation.EventListener;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.licensing.internal.UserCounter.UserListener;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserCounter}.
 * 
 * @version $Id$
 * @since 1.6
 */
@ComponentList(UserListener.class)
public class UserCounterTest
{
    @Rule
    public MockitoComponentMockingRule<UserCounter> mocker = new MockitoComponentMockingRule<>(UserCounter.class);

    private WikiDescriptorManager wikiDescriptorManager;

    private QueryManager queryManager;

    private QueryFilter countFilter;

    @Before
    public void configure() throws Exception
    {
        this.wikiDescriptorManager = this.mocker.getInstance(WikiDescriptorManager.class);
        this.queryManager = this.mocker.getInstance(QueryManager.class);
        this.countFilter = this.mocker.getInstance(QueryFilter.class, "count");
    }

    @Test
    public void getUserCountIsCached() throws Exception
    {
        assertEquals(0, this.mocker.getComponentUnderTest().getUserCount());
        assertEquals(0, this.mocker.getComponentUnderTest().getUserCount());

        verify(this.wikiDescriptorManager, times(1)).getAllIds();
    }

    @Test
    public void getUserCount() throws Exception
    {
        when(this.wikiDescriptorManager.getAllIds()).thenReturn(Arrays.asList("foo", "bar"));

        Query fooQuery = mock(Query.class, "foo");
        Query barQuery = mock(Query.class, "bar");
        when(this.queryManager.createQuery(UserCounter.BASE_USER_QUERY, Query.HQL)).thenReturn(fooQuery, barQuery);
        when(fooQuery.addFilter(this.countFilter)).thenReturn(fooQuery);
        when(barQuery.addFilter(this.countFilter)).thenReturn(barQuery);
        when(fooQuery.execute()).thenReturn(Collections.singletonList(3L));
        when(barQuery.execute()).thenReturn(Collections.singletonList(4L));

        assertEquals(7L, this.mocker.getComponentUnderTest().getUserCount());

        verify(fooQuery).addFilter(this.countFilter);
        verify(fooQuery).setWiki("foo");

        verify(barQuery).addFilter(this.countFilter);
        verify(barQuery).setWiki("bar");
    }

    @Test
    public void getUserCountThrowsQueryException() throws Exception
    {
        when(this.wikiDescriptorManager.getAllIds()).thenReturn(Arrays.asList("foo", "bar"));

        Query fooQuery = mock(Query.class, "foo");
        Query barQuery = mock(Query.class, "bar");
        when(this.queryManager.createQuery(UserCounter.BASE_USER_QUERY, Query.HQL)).thenReturn(fooQuery, barQuery);
        when(fooQuery.addFilter(this.countFilter)).thenReturn(fooQuery);
        when(barQuery.addFilter(this.countFilter)).thenReturn(barQuery);
        when(fooQuery.execute()).thenReturn(Collections.singletonList(3L));
        when(barQuery.execute()).thenThrow(new QueryException("message", barQuery, null));

        try {
            this.mocker.getComponentUnderTest().getUserCount();
            fail();
        } catch (Exception expected) {
            assertEquals("Failed to count the users.", expected.getMessage());
            assertEquals("message. Query statement = [null]", expected.getCause().getMessage());
        }
    }

    public void getUserCountWithCacheInvalidation() throws Exception
    {
        when(this.wikiDescriptorManager.getAllIds()).thenReturn(Collections.singletonList("foo"));

        Query fooQuery = mock(Query.class, "foo");
        when(this.queryManager.createQuery(UserCounter.BASE_USER_QUERY, Query.HQL)).thenReturn(fooQuery);
        when(fooQuery.addFilter(this.countFilter)).thenReturn(fooQuery);
        when(fooQuery.execute()).thenReturn(Collections.singletonList(3L));

        assertEquals(3L, this.mocker.getComponentUnderTest().getUserCount());
        assertEquals(3L, this.mocker.getComponentUnderTest().getUserCount());

        XWikiDocument userProfile = mock(XWikiDocument.class, "userProfile");
        when(userProfile.getOriginalDocument()).thenReturn(mock(XWikiDocument.class));
        when(userProfile.getXObject(UserListener.USER_CLASS)).thenReturn(mock(BaseObject.class));
        EventListener userListener = this.mocker.getInstance(EventListener.class, UserListener.class.getName());
        userListener.onEvent(null, userProfile, null);

        assertEquals(3L, this.mocker.getComponentUnderTest().getUserCount());

        XWikiDocument blankPage = mock(XWikiDocument.class);
        when(blankPage.getOriginalDocument()).thenReturn(blankPage);
        userListener.onEvent(null, blankPage, null);

        assertEquals(3L, this.mocker.getComponentUnderTest().getUserCount());

        verify(this.wikiDescriptorManager, times(2)).getAllIds();
        verify(fooQuery, times(2)).execute();
    }
}
