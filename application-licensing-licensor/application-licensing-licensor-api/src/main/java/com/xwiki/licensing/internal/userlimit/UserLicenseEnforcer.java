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
package com.xwiki.licensing.internal.userlimit;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.BeginEvent;
import org.xwiki.observation.event.BeginFoldEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.event.filter.EventFilter;
import org.xwiki.observation.event.filter.RegexEventFilter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.licensing.internal.AuthExtensionUserManager;

/**
 * Ensure disabled users remain disabled if they have no valid license.
 *
 * @version $Id$
 * @since 1.31
 */
@Component
@Singleton
@Named(UserLicenseEnforcer.ROLE_NAME)
public class UserLicenseEnforcer extends AbstractEventListener
{
    /**
     * The role name of this component.
     */
    public static final String ROLE_NAME = "com.xwiki.licensing.internal.userlimit.UserLicenseEnforcer";

    /**
     * Matcher for {@link UserLicenseBeginFoldEvent}.
     */
    public static final BeginEvent USER_LICENSE_FOLD_EVENT_MATCHER =
        event -> event instanceof BeginFoldEvent;

    private static final EventFilter XWIKI_SPACE_FILTER = new RegexEventFilter("^(.*:)?XWiki\\..*");

    private static final LocalDocumentReference XWIKI_USER_CLASS_REFERENCE =
        new LocalDocumentReference("XWiki", "XWikiUsers");

    @Inject
    private ComponentManager componentManager;

    @Inject
    private ObservationContext observationContext;

    @Inject
    private Provider<ObservationManager> observationManagerProvider;

    /**
     * Default constructor.
     */
    public UserLicenseEnforcer()
    {
        super(ROLE_NAME, Arrays.asList(new DocumentCreatingEvent(XWIKI_SPACE_FILTER),
            new DocumentUpdatingEvent(XWIKI_SPACE_FILTER)));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (observationContext.isIn(USER_LICENSE_FOLD_EVENT_MATCHER)) {
            return;
        }

        observationManagerProvider.get().notify(new UserLicenseBeginFoldEvent(), source, data);
        XWikiDocument sourceDocument = (XWikiDocument) source;
        XWikiContext xcontext = (XWikiContext) data;
        Map<String, AuthExtensionUserManager> userManagerMap;
        try {
            userManagerMap = componentManager.getInstanceMap(AuthExtensionUserManager.class);
        } catch (ComponentLookupException e) {
            observationManagerProvider.get().notify(new UserLicenseEndFoldEvent(), source, data);
            return;
        }
        List<AuthExtensionUserManager> associatedUserManagers =
            userManagerMap.values().stream().filter(a -> a.managesUser(sourceDocument))
                .collect(Collectors.toUnmodifiableList());
        // If no user manager or conflict, skip the user.
        if (associatedUserManagers.size() != 1) {
            observationManagerProvider.get().notify(new UserLicenseEndFoldEvent(), source, data);
            return;
        }

        boolean shouldBeActive = associatedUserManagers.get(0).shouldBeActive(sourceDocument.getDocumentReference());
        if (!shouldBeActive) {
            sourceDocument.getXObject(XWIKI_USER_CLASS_REFERENCE).set("active", 0, xcontext);
            sourceDocument.setComment(sourceDocument.getComment() + " + Enforce license user limit.");
        }
        observationManagerProvider.get().notify(new UserLicenseEndFoldEvent(), source, data);
    }
}
