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
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
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
import com.xwiki.licensing.internal.helpers.events.ActiveUserAddedEvent;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
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

    private QueryFilter uniqueFilter;

    private String statement;

    @Before
    public void configure() throws Exception
    {
        this.wikiDescriptorManager = this.mocker.getInstance(WikiDescriptorManager.class);
        this.queryManager = this.mocker.getInstance(QueryManager.class);
        this.countFilter = this.mocker.getInstance(QueryFilter.class, "count");
        this.uniqueFilter = this.mocker.getInstance(QueryFilter.class, "unique");

        StringBuilder stringBuilder = new StringBuilder(", BaseObject as obj, IntegerProperty as prop ");
        stringBuilder.append("where doc.fullName = obj.name and obj.className = 'XWiki.XWikiUsers' and ");
        stringBuilder.append("prop.id.id = obj.id and prop.id.name = 'active' and prop.value = '1'");
        this.statement = stringBuilder.toString();
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

        Query fooQuery = createMockQuery("foo");
        Query barQuery = createMockQuery("bar");
        when(this.queryManager.createQuery(this.statement, Query.HQL)).thenReturn(fooQuery, barQuery);
        when(fooQuery.execute()).thenReturn(Collections.singletonList(3L));
        when(barQuery.execute()).thenReturn(Collections.singletonList(4L));

        assertEquals(7L, this.mocker.getComponentUnderTest().getUserCount());

        verify(fooQuery).addFilter(this.countFilter);
        verify(fooQuery).addFilter(this.uniqueFilter);
        verify(fooQuery).setWiki("foo");

        verify(barQuery).addFilter(this.countFilter);
        verify(barQuery).addFilter(this.uniqueFilter);
        verify(barQuery).setWiki("bar");
    }

    @Test
    public void getUserCountThrowsQueryException() throws Exception
    {
        when(this.wikiDescriptorManager.getAllIds()).thenReturn(Arrays.asList("foo", "bar"));

        Query fooQuery = createMockQuery("foo");
        Query barQuery = createMockQuery("bar");
        when(this.queryManager.createQuery(this.statement, Query.HQL)).thenReturn(fooQuery, barQuery);
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

        Query fooQuery = createMockQuery("foo");
        when(this.queryManager.createQuery(this.statement, Query.HQL)).thenReturn(fooQuery);
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

    @Test
    public void notifyActiveUserAdded() throws Exception
    {
        ObservationManager observationManager = this.mocker.registerMockComponent(ObservationManager.class);
        this.mocker.registerMockComponent(RemoteObservationManagerContext.class);
        EventListener userListener = this.mocker.getInstance(EventListener.class, UserListener.HINT);

        XWikiDocument createdActiveUser = mockUserDocument(null, 1);
        XWikiDocument enabledUser = mockUserDocument(0, 1);
        XWikiDocument updatedActiveUser = mockUserDocument(1, 1);
        XWikiDocument createdInactiveUser = mockUserDocument(null, 0);
        XWikiDocument blankPage = mock(XWikiDocument.class, "blankPage");
        when(blankPage.getOriginalDocument()).thenReturn(blankPage);
        userListener.onEvent(null, blankPage, null);
        userListener.onEvent(null, createdActiveUser, null);
        userListener.onEvent(null, enabledUser, null);
        userListener.onEvent(null, updatedActiveUser, null);
        userListener.onEvent(null, createdInactiveUser, null);

        verify(observationManager).notify(any(ActiveUserAddedEvent.class), same(createdActiveUser), isNull());
        verify(observationManager).notify(any(ActiveUserAddedEvent.class), same(enabledUser), isNull());
        verifyNoMoreInteractions(observationManager);
    }

    @Test
    public void notifyActiveUserAddedOnlyOnLocalEvents() throws Exception
    {
        ObservationManager observationManager = this.mocker.registerMockComponent(ObservationManager.class);
        RemoteObservationManagerContext remoteContext =
            this.mocker.registerMockComponent(RemoteObservationManagerContext.class);
        when(remoteContext.isRemoteState()).thenReturn(true);
        when(this.wikiDescriptorManager.getAllIds()).thenReturn(Collections.emptyList());
        EventListener userListener = this.mocker.getInstance(EventListener.class, UserListener.HINT);

        // Fill the cache, so that we can check it is still invalidated on remote events.
        this.mocker.getComponentUnderTest().getUserCount();
        userListener.onEvent(null, mockUserDocument(null, 1), null);
        this.mocker.getComponentUnderTest().getUserCount();

        verify(this.wikiDescriptorManager, times(2)).getAllIds();
        verifyNoInteractions(observationManager);
    }

    /**
     * @param oldActive the value of the active property before the save, or {@code null} if the document was not a user
     * @param newActive the value of the active property after the save
     */
    private XWikiDocument mockUserDocument(Integer oldActive, int newActive)
    {
        XWikiDocument oldDocument = mock(XWikiDocument.class);
        if (oldActive != null) {
            BaseObject oldObject = mock(BaseObject.class);
            when(oldObject.getIntValue(UserListener.ACTIVE)).thenReturn(oldActive);
            when(oldDocument.getXObject(UserListener.USER_CLASS)).thenReturn(oldObject);
        }

        BaseObject newObject = mock(BaseObject.class);
        when(newObject.getIntValue(UserListener.ACTIVE)).thenReturn(newActive);
        XWikiDocument newDocument = mock(XWikiDocument.class);
        when(newDocument.getOriginalDocument()).thenReturn(oldDocument);
        when(newDocument.getXObject(UserListener.USER_CLASS)).thenReturn(newObject);
        return newDocument;
    }

    private Query createMockQuery(String queryName)
    {
        Query query = mock(Query.class, queryName);
        when(query.addFilter(any())).thenReturn(query);
        return query;
    }
}
