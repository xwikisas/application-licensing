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
package com.xwiki.licensing.internal.enforcer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.event.filter.EventFilter;
import org.xwiki.observation.event.filter.RegexEventFilter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.licensing.LicenseValidator;
import com.xwiki.licensing.Licensor;
import com.xwiki.licensing.internal.AuthExtensionUserManager;
import com.xwiki.licensing.internal.helpers.events.LicenseUpdatedEvent;

/**
 * Disable users who are over the user limit of the license.
 *
 * @version $Id$
 * @since 1.31
 */
@Component
@Singleton
@Named(UserLicenseDisabler.ROLE_NAME)
public class UserLicenseDisabler extends AbstractEventListener
{
    /**
     * The role name of this component.
     */
    public static final String ROLE_NAME = "com.xwiki.licensing.internal.enforcer.UserLicenseDisabler";

    private static final String EDIT_MESSAGE_DISABLE = "User disabled to enforce license.";

    private static final String EDIT_MESSAGE_ENABLE = "User activated to enforce license.";

    private static final EventFilter XWIKI_SPACE_FILTER = new RegexEventFilter("^(.*:)?XWiki\\..*");

    private static final String ACTIVE = "active";

    private static final String MAIN_WIKI_NAME = "xwiki";

    private static final DocumentReference XWIKI_USER_CLASS_REFERENCE =
        new DocumentReference(MAIN_WIKI_NAME, "XWiki", "XWikiUsers");

    private static final DocumentReference LICENSE_USER_CHECKPOINT_CLASS_REFERENCE =
        new DocumentReference(MAIN_WIKI_NAME, List.of("Licenses", "Code"), "LicensingUserCheckpoint");

    @Inject
    private Map<String, AuthExtensionUserManager> userManagerMap;

    @Inject
    private Licensor licensor;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public UserLicenseDisabler()
    {
        super(ROLE_NAME,
            Arrays.asList(new DocumentCreatedEvent(XWIKI_SPACE_FILTER), new DocumentUpdatedEvent(XWIKI_SPACE_FILTER),
                new DocumentDeletedEvent(XWIKI_SPACE_FILTER), new LicenseUpdatedEvent()));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        Map<AuthExtensionUserManager, Map<XWikiDocument, Boolean>> managedUsers;
        if (event instanceof LicenseUpdatedEvent) {
            LicenseUpdatedEvent licenseUpdatedEvent = (LicenseUpdatedEvent) event;
            AuthExtensionUserManager currentExtensionUserManager =
                this.userManagerMap.get(licenseUpdatedEvent.getLicense().getId().getId());
            if (currentExtensionUserManager != null) {
                managedUsers = new HashMap<>();
                managedUsers.put(currentExtensionUserManager, currentExtensionUserManager.getManagedUsers().stream()
                    .collect(Collectors.toUnmodifiableMap(doc -> doc,
                        doc -> licensor.hasLicensure(doc.getDocumentReference()))));
            } else {
                // This extension doesn't need the custom user limit enforcing.
                return;
            }
        } else {
            // If a user is updated, we need to check all licenses.
            managedUsers = userManagerMap.values().stream()
                .collect(Collectors.toUnmodifiableMap(v -> v, this::getUserManagerActiveUsers));
        }

        managedUsers.forEach((authManager, userMap) -> userMap.forEach(
            (user, shouldBeActive) -> updateUserPage(user, shouldBeActive, authManager.getClass().getName(),
                (XWikiContext) data)));
    }

    private Map<XWikiDocument, Boolean> getUserManagerActiveUsers(AuthExtensionUserManager userManager)
    {
        return userManager.getManagedUsers().stream().collect(
            Collectors.toUnmodifiableMap(doc -> doc, doc -> userManager.shouldBeActive(doc.getDocumentReference())));
    }

    private void updateUserPage(XWikiDocument user, boolean shouldBeActive, String extensionName, XWikiContext xcontext)
    {
        String editMessage = shouldBeActive ? EDIT_MESSAGE_ENABLE : EDIT_MESSAGE_DISABLE;
        if (shouldBeActive) {
            BaseObject checkpoint = user.getXObject(LICENSE_USER_CHECKPOINT_CLASS_REFERENCE);
            if (checkpoint != null) {
                user.getXObject(XWIKI_USER_CLASS_REFERENCE).set(ACTIVE, checkpoint.getIntValue(ACTIVE), xcontext);
                user.removeXObjects(LICENSE_USER_CHECKPOINT_CLASS_REFERENCE);
            } else {
                // Odd things, maybe add an implicit default here, idk.
                logger.info("No license checkpoint object found for user [{}]. ¯\\_ (ツ)_/¯", user);
                return;
            }
        } else {
            int previousActiveStatus = user.getXObject(XWIKI_USER_CLASS_REFERENCE).getIntValue(ACTIVE);
            try {
                user.createXObject(LICENSE_USER_CHECKPOINT_CLASS_REFERENCE, xcontext);
            } catch (XWikiException e) {
                logger.error(
                    "Failed to create XObject on user profile for [{}] when enforcing the license for [{}]. Cause:"
                        + " [{}]", user, extensionName, ExceptionUtils.getRootCauseMessage(e));
            }
            user.getXObject(LICENSE_USER_CHECKPOINT_CLASS_REFERENCE).set(ACTIVE, previousActiveStatus, xcontext);

            user.getXObject(XWIKI_USER_CLASS_REFERENCE).set(ACTIVE, 0, xcontext);
        }
        try {
            xcontext.getWiki().saveDocument(user, editMessage, xcontext);
            logger.info("Disabled user [{}] to enforce license for [{}].", user, extensionName);
        } catch (XWikiException e) {
            logger.error("Failed to save user profile for [{}] when enforcing the license for [{}]. Cause: [{}]", user,
                extensionName, ExceptionUtils.getRootCauseMessage(e));
        }
    }
}
