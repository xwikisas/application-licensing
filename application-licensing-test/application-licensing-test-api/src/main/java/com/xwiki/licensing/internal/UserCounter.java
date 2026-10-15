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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Component used to count the existing active users.
 * <br>
 * Added to the test module since some external apps depend on the UserCounter class.
 * 
 * @version $Id$
 * @since 1.33.0
 */
@Component(roles = UserCounter.class)
@Singleton
public class UserCounter
{
    protected static final String BASE_USER_QUERY = ", BaseObject as obj, IntegerProperty as prop "
            + "where doc.fullName = obj.name and obj.className = 'XWiki.XWikiUsers' and prop.id.id = obj.id "
            + "and prop.id.name = 'active' and prop.value = '1'";

    @Inject
    private Logger logger;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Provider<XWikiContext> xWikiContextProvider;

    @Inject
    private QueryManager queryManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    @Named("count")
    private QueryFilter countFilter;

    @Inject
    @Named("unique")
    private QueryFilter uniqueFilter;

    private Long cachedUserCount;

    // The position of each active user of the instance, from all subwikis, in the creation date order.
    private Map<DocumentReference, Integer> cachedUserIndexes;

    /**
     * Event listener that invalidates the cached user count when an user is added, deleted or the active property's
     * value is changed.
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

        @Inject
        private UserCounter userCounter;

        /**
         * Default constructor.
         */
        public UserListener()
        {
            super(HINT,
                Arrays.asList(new DocumentCreatedEvent(), new DocumentUpdatedEvent(), new DocumentDeletedEvent()));
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
     *
     * @since 1.33.0
     */
    public void flushCache()
    {
        this.cachedUserCount = null;
        this.cachedUserIndexes = null;
    }

    /**
     * Get the position of each active user of the instance, from all subwikis, when sorted by creation date. Users
     * with the same creation date (dates are stored with a precision of one second) are sorted by their serialized
     * reference, so that every user is kept and the order is stable.
     *
     * @return the 0-based position of each active user, oldest first; the iteration order is the creation date order
     * @throws WikiManagerException if the list of wikis cannot be retrieved
     * @throws QueryException if the users of a wiki cannot be retrieved
     * @since 1.33.0
     */
    protected Map<DocumentReference, Integer> getUserIndexes() throws WikiManagerException, QueryException
    {
        Map<DocumentReference, Integer> userIndexes = this.cachedUserIndexes;
        if (userIndexes == null) {
            List<Pair<Date, DocumentReference>> users = new ArrayList<>();
            for (String wikiId : this.wikiDescriptorManager.getAllIds()) {
                users.addAll(getUsersOnWiki(wikiId));
            }
            users.sort(Comparator.comparing((Pair<Date, DocumentReference> user) -> user.getLeft())
                .thenComparing(user -> user.getRight().toString()));
            Map<DocumentReference, Integer> indexes = new LinkedHashMap<>();
            for (Pair<Date, DocumentReference> user : users) {
                // A translated user profile is returned once per translation: keep the oldest (first) one.
                indexes.putIfAbsent(user.getRight(), indexes.size());
            }
            userIndexes = Collections.unmodifiableMap(indexes);
            this.cachedUserIndexes = userIndexes;
            this.cachedUserCount = (long) userIndexes.size();
        }
        return userIndexes;
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
     * Check whether the given user is among the first {@code userLimit} active users of the instance, all wikis
     * included, ordered by creation date (see {@link #getUserIndexes()}). For example, with a limit of {@code 10}, the
     * 10 oldest active users are under the limit and every newer one is over it.
     *
     * @param user the user to check, e.g. {@code xwiki:XWiki.JohnDoe}
     * @param userLimit the maximum number of users allowed by the license; a negative value (e.g. {@code -1}) means
     *     unlimited
     * @return {@code true} if the limit is negative, whatever the user; otherwise {@code true} only if the user is an
     *     active user of the instance within the first {@code userLimit} ones, and {@code false} for a {@code null},
     *     inactive or nonexistent user (this includes the guest user and superadmin, which have no user profile)
     * @throws Exception if the users of the instance cannot be retrieved
     * @since 1.33.0
     */
    public boolean isUserUnderLimit(DocumentReference user, long userLimit) throws Exception
    {
        if (user == null) {
            return false;
        }
        if (userLimit < 0) {
            return true;
        }
        Integer index = getUserIndexes().get(user);
        return index != null && index < userLimit;
    }

    private long getUserCountOnWiki(String wikiId) throws QueryException
    {
        Query query = this.queryManager.createQuery(BASE_USER_QUERY, Query.HQL);
        query.addFilter(this.uniqueFilter).addFilter(this.countFilter).setWiki(wikiId);
        List<Long> results = query.execute();
        return results.get(0);
    }

    private List<Pair<Date, DocumentReference>> getUsersOnWiki(String wikiId) throws QueryException
    {
        List<Object[]> results = this.queryManager
            .createQuery("select doc.fullName, doc.creationDate from XWikiDocument doc" + BASE_USER_QUERY, Query.HQL)
            .addFilter(this.uniqueFilter).setWiki(wikiId).execute();
        WikiReference wikiReference = new WikiReference(wikiId);
        List<Pair<Date, DocumentReference>> users = new ArrayList<>(results.size());
        for (Object[] result : results) {
            DocumentReference userReference = this.documentReferenceResolver.resolve((String) result[0], wikiReference);
            users.add(Pair.of((Date) result[1], userReference));
        }
        return users;
    }
}
