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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
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
                this.userCounter.cachedUserCount = null;
            }
        }
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

    private long getUserCountOnWiki(String wikiId) throws QueryException
    {
        StringBuilder statement = new StringBuilder(", BaseObject as obj, IntegerProperty as prop ");
        statement.append("where doc.fullName = obj.name and obj.className = 'XWiki.XWikiUsers' and ");
        statement.append("prop.id.id = obj.id and prop.id.name = 'active' and prop.value = '1'");

        Query query = this.queryManager.createQuery(statement.toString(), Query.HQL);
        query.addFilter(this.uniqueFilter).setWiki(wikiId);
        List<Long> results = query.addFilter(this.countFilter).execute();
        return results.get(0);
    }
}
