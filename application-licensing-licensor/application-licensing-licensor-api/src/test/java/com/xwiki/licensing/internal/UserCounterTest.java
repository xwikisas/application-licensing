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
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Before
    public void configure() throws Exception
    {
        this.wikiDescriptorManager = this.mocker.getInstance(WikiDescriptorManager.class);
        this.queryManager = this.mocker.getInstance(QueryManager.class);
        this.countFilter = this.mocker.getInstance(QueryFilter.class, "count");
        this.uniqueFilter = this.mocker.getInstance(QueryFilter.class, "unique");
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
        when(this.queryManager.createQuery(UserCounter.BASE_USER_QUERY, Query.HQL)).thenReturn(fooQuery, barQuery);
        when(this.queryManager.createQuery(UserCounter.BASE_USER_QUERY, Query.HQL)).thenReturn(fooQuery, barQuery);
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
        when(this.queryManager.createQuery(UserCounter.BASE_USER_QUERY, Query.HQL)).thenReturn(fooQuery, barQuery);
        when(this.queryManager.createQuery(UserCounter.BASE_USER_QUERY, Query.HQL)).thenReturn(fooQuery, barQuery);
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

    @Test
    public void getUserIndexesOrdersByCreationDateAcrossWikis() throws Exception
    {
        Query fooQuery = mockUsers("foo", "bar");
        when(fooQuery.execute()).thenReturn(List.of(user("User1", 100000), user("User2", 50000)),
            Collections.singletonList(user("User3", 75000)));

        Map<DocumentReference, Integer> result = this.mocker.getComponentUnderTest().getUserIndexes();

        assertEquals(Map.of(reference("foo", "User2"), 0, reference("bar", "User3"), 1, reference("foo", "User1"), 2),
            result);
        assertEquals(3L, this.mocker.getComponentUnderTest().getUserCount());

        // Further calls use the cache.
        clearInvocations(fooQuery);
        assertEquals(result, this.mocker.getComponentUnderTest().getUserIndexes());
        verify(fooQuery, times(0)).execute();
    }

    @Test
    public void getUserIndexesKeepsUsersWithTheSameCreationDate() throws Exception
    {
        Query fooQuery = mockUsers("foo");
        when(fooQuery.execute()).thenReturn(List.of(user("UserB", 1000), user("UserC", 1000), user("UserA", 1000)));

        assertEquals(Map.of(reference("foo", "UserA"), 0, reference("foo", "UserB"), 1, reference("foo", "UserC"), 2),
            this.mocker.getComponentUnderTest().getUserIndexes());
        assertEquals(3L, this.mocker.getComponentUnderTest().getUserCount());
    }

    @Test
    public void getUserIndexesKeepsTheOldestTranslationOfAUser() throws Exception
    {
        Query fooQuery = mockUsers("foo");
        // UserA has a translation, created after UserB.
        when(fooQuery.execute()).thenReturn(
            List.of(user("UserA", 1000), user("UserB", 2000), user("UserA", 3000), user("UserC", 4000)));

        assertEquals(Map.of(reference("foo", "UserA"), 0, reference("foo", "UserB"), 1, reference("foo", "UserC"), 2),
            this.mocker.getComponentUnderTest().getUserIndexes());
    }

    @Test
    public void isUserUnderLimit() throws Exception
    {
        Query fooQuery = mockUsers("foo");
        when(fooQuery.execute()).thenReturn(List.of(user("User1", 1000), user("User2", 2000), user("User3", 3000)));
        UserCounter userCounter = this.mocker.getComponentUnderTest();

        // Limit lower than the user count.
        assertTrue(userCounter.isUserUnderLimit(reference("foo", "User1"), 2));
        assertTrue(userCounter.isUserUnderLimit(reference("foo", "User2"), 2));
        assertFalse(userCounter.isUserUnderLimit(reference("foo", "User3"), 2));
        // Limit higher than the user count.
        assertTrue(userCounter.isUserUnderLimit(reference("foo", "User3"), 5));
        // No user is under a limit of 0.
        assertFalse(userCounter.isUserUnderLimit(reference("foo", "User1"), 0));
        // Users that are not active users of the instance, like the guest user or superadmin.
        assertFalse(userCounter.isUserUnderLimit(reference("foo", "XWikiGuest"), 5));
        assertFalse(userCounter.isUserUnderLimit(reference("foo", "superadmin"), 5));
        assertFalse(userCounter.isUserUnderLimit(null, 5));
        // A negative limit means unlimited.
        assertTrue(userCounter.isUserUnderLimit(reference("foo", "User3"), -1));
        assertTrue(userCounter.isUserUnderLimit(reference("foo", "NonExistentUser"), -1));
        assertFalse(userCounter.isUserUnderLimit(null, -1));
    }

    @Test
    public void isUserUnderLimitThrowsQueryException() throws Exception
    {
        Query fooQuery = mockUsers("foo");
        when(fooQuery.execute()).thenThrow(new QueryException("message", fooQuery, null));

        try {
            this.mocker.getComponentUnderTest().isUserUnderLimit(reference("foo", "User1"), 1);
            fail();
        } catch (QueryException expected) {
            assertEquals("message. Query statement = [null]", expected.getMessage());
        }
    }

    /**
     * @return the query returning the active users, one execution per given wiki
     */
    private Query mockUsers(String... wikis) throws Exception
    {
        when(this.wikiDescriptorManager.getAllIds()).thenReturn(List.of(wikis));
        Query query = createMockQuery("users");
        when(this.queryManager.createQuery(any(), any())).thenReturn(query);
        DocumentReferenceResolver<String> resolver = this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING);
        when(resolver.resolve(anyString(), any(WikiReference.class))).thenAnswer(
            invocation -> new DocumentReference(((WikiReference) invocation.getArgument(1)).getName(), "XWiki",
                invocation.<String>getArgument(0).substring("XWiki.".length())));
        return query;
    }

    private Object[] user(String username, long creationDateMillis)
    {
        return new Object[] { "XWiki." + username, new Date(creationDateMillis) };
    }

    private DocumentReference reference(String wiki, String username)
    {
        return new DocumentReference(wiki, "XWiki", username);
    }

    @Test
    public void getUserCountWithCacheInvalidation() throws Exception
    {
        when(this.wikiDescriptorManager.getAllIds()).thenReturn(Collections.singletonList("foo"));

        Query fooQuery = createMockQuery("foo");
        when(this.queryManager.createQuery(UserCounter.BASE_USER_QUERY, Query.HQL)).thenReturn(fooQuery);
        when(this.queryManager.createQuery(UserCounter.BASE_USER_QUERY, Query.HQL)).thenReturn(fooQuery);
        when(fooQuery.execute()).thenReturn(Collections.singletonList(3L));

        assertEquals(3L, this.mocker.getComponentUnderTest().getUserCount());
        assertEquals(3L, this.mocker.getComponentUnderTest().getUserCount());

        XWikiDocument userProfile = mock(XWikiDocument.class, "userProfile");
        when(userProfile.getOriginalDocument()).thenReturn(mock(XWikiDocument.class));
        when(userProfile.getXObject(UserListener.USER_CLASS)).thenReturn(mock(BaseObject.class));
        EventListener userListener = this.mocker.getInstance(EventListener.class, UserListener.HINT);
        userListener.onEvent(null, userProfile, null);

        assertEquals(3L, this.mocker.getComponentUnderTest().getUserCount());

        XWikiDocument blankPage = mock(XWikiDocument.class);
        when(blankPage.getOriginalDocument()).thenReturn(blankPage);
        userListener.onEvent(null, blankPage, null);

        assertEquals(3L, this.mocker.getComponentUnderTest().getUserCount());

        verify(this.wikiDescriptorManager, times(2)).getAllIds();
        verify(fooQuery, times(2)).execute();
    }

    private Query createMockQuery(String queryName) {
        Query query = mock(Query.class, queryName);
        when(query.addFilter(any())).thenReturn(query);
        when(query.setWiki(any())).thenReturn(query);
        when(query.setWiki(any())).thenReturn(query);
        return query;
    }
}
