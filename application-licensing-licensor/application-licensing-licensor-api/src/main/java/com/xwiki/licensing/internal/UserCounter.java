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
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.event.filter.EventFilter;
import org.xwiki.observation.event.filter.RegexEventFilter;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Component used to count the existing active users.
 * 
 * @version $Id$
 * @since 1.6
 */
@Component(roles = UserCounter.class)
@Singleton
public class UserCounter
{
    protected static final String BASE_USER_QUERY = ", BaseObject as obj, IntegerProperty as prop "
        + "where doc.space = 'XWiki' "
        + "and doc.fullName = obj.name and obj.className = 'XWiki.XWikiUsers' and prop.id.id = obj.id "
        + "and prop.id.name = 'active' and prop.value = '1'";

    @Inject
    private Logger logger;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("count")
    private QueryFilter countFilter;

    @Inject
    @Named("unique")
    private QueryFilter uniqueFilter;

    private Long cachedUserCount;

    // A set of users on the instance, sorted by creation date.
    private SortedSet<XWikiDocument> cachedSortedUsers;

    // Helper to find users in constant time.
    private Map<DocumentReference, XWikiDocument> cachedSortedUsersLookupTable;

    /**
     * Event listener that invalidates the cached user count when an user is added, deleted or the active property's
     * value is changed.
     * 
     * @version $Id$
     * @since 1.6
     */
    @Component
    @Singleton
    @Named(UserListener.HINT)
    public static class UserListener extends AbstractEventListener
    {
        /**
         * The event listener component hint.
         */
        public static final String HINT = "com.xwiki.licensing.internal.UserCounter.UserListener";

        protected static final String ACTIVE = "active";

        protected static final LocalDocumentReference USER_CLASS = new LocalDocumentReference("XWiki", "XWikiUsers");

        private static final EventFilter XWIKI_SPACE_FILTER = new RegexEventFilter("(.*:)?XWiki\\..*");

        @Inject
        private UserCounter userCounter;

        /**
         * Default constructor.
         */
        public UserListener()
        {
            super(HINT, Arrays.asList(new DocumentCreatedEvent(XWIKI_SPACE_FILTER),
                new DocumentUpdatedEvent(XWIKI_SPACE_FILTER), new DocumentDeletedEvent(XWIKI_SPACE_FILTER)));
        }

        @Override
        public void onEvent(Event event, Object source, Object data)
        {
            XWikiDocument newDocument = (XWikiDocument) source;
            XWikiDocument oldDocument = newDocument.getOriginalDocument();

            BaseObject newObject = newDocument.getXObject(USER_CLASS);
            BaseObject oldObject = oldDocument.getXObject(USER_CLASS);

            boolean newDocumentIsUser = newObject != null;
            boolean oldDocumentIsUser = oldObject != null;

            // Set defaults to -1 to avoid nulls.
            int newActive = newDocumentIsUser ? newObject.getIntValue(ACTIVE) : -1;
            int oldActive = oldDocumentIsUser ? oldObject.getIntValue(ACTIVE) : -1;

            if (newDocumentIsUser != oldDocumentIsUser || newActive != oldActive) {
                // The user object is either added/removed or set to active/inactive. Invalidate the cached user count.
                this.userCounter.flushCache();
            }
        }
    }

    /**
     * Flush the cache of the user counter.
     */
    public void flushCache()
    {
        this.cachedUserCount = null;
        this.cachedSortedUsers = null;
        this.cachedSortedUsersLookupTable = null;
    }

    /**
     * Get the users sorted by creation date.
     *
     * @return the users, sorted by creation date.
     */
    public SortedSet<XWikiDocument> getSortedUsers() throws WikiManagerException, QueryException
    {
        if (cachedSortedUsers == null) {
            cachedSortedUsers = new TreeSet<>(
                (e1, e2) -> new CompareToBuilder().append(e1.getCreationDate(), e2.getCreationDate()).build());
            for (String wikiId : wikiDescriptorManager.getAllIds()) {
                cachedSortedUsers.addAll(getUsersOnWiki(wikiId));
            }
            cachedSortedUsersLookupTable =
                cachedSortedUsers.stream().collect(Collectors.toMap(XWikiDocument::getDocumentReference, e -> e));
        }
        return cachedSortedUsers;
    }

    /**
     * Counts the existing active users.
     * 
     * @return the user count
     * @throws Exception if we fail to count the users
     */
    public long getUserCount() throws Exception
    {
        if (cachedUserCount == null) {
            try {
                long userCount = 0;
                for (String wikiId : this.wikiDescriptorManager.getAllIds()) {
                    userCount += getUserCountOnWiki(wikiId);
                }
                this.logger.debug("User count is [{}].", userCount);
                this.cachedUserCount = userCount;
            } catch (WikiManagerException | QueryException e) {
                throw new Exception("Failed to count the users.", e);
            }
        }

        return cachedUserCount;
    }

    /**
     * Return whether the given user is under the specified license user limit.
     *
     * @param user the user to check
     * @param userLimit the license max user limit
     * @return whether the given user is under the specified license user limit
     */
    public boolean isUserUnderLimit(DocumentReference user, long userLimit) throws Exception
    {
        if (userLimit < 0 || userLimit <= getUserCount()) {
            // Unlimited licenses should always return true.
            // Also, skip the checks for instances with fewer users than the limit.
            return true;
        }
        SortedSet<XWikiDocument> sortedUsers = getSortedUsers();
        // Lookup table is initialized in getSortedUsers().
        XWikiDocument userDocument = cachedSortedUsersLookupTable.get(user);
        if (userDocument == null) {
            return false;
        } else {
            return sortedUsers.headSet(userDocument).size() < userLimit;
        }
    }

    private long getUserCountOnWiki(String wikiId) throws QueryException
    {
        Query query = this.queryManager.createQuery(BASE_USER_QUERY, Query.HQL);
        query.addFilter(this.uniqueFilter).addFilter(this.countFilter).setWiki(wikiId);
        List<Long> results = query.execute();
        return results.get(0);
    }

    private List<XWikiDocument> getUsersOnWiki(String wikiId) throws QueryException
    {
        return this.queryManager.createQuery("select doc from XWikiDocument doc" + BASE_USER_QUERY, Query.HQL)
            .setWiki(wikiId).execute();
    }
}
