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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.job.AbstractJob;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.licensing.Licensor;
import com.xwiki.licensing.internal.AuthExtensionUserManager;
import com.xwiki.licensing.internal.helpers.events.LicenseUpdatedEvent;

/**
 * Disable users who are over the license user limit, and re-enable those below the limit.
 *
 * @version $Id$
 * @since 1.31
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Named(UserLicenseDisablerJob.JOB_TYPE)
public class UserLicenseDisablerJob extends AbstractJob<UserLicenseDisablerJobRequest, UserLicenseDisablerJobStatus>
{
    /**
     * The job type string.
     */
    public static final String JOB_TYPE = "licensor.userlimit.disableAuthUsersOverLimit";

    private static final String EDIT_MESSAGE_DISABLE = "User disabled to enforce license.";

    private static final String EDIT_MESSAGE_ENABLE = "User activated to enforce license.";

    private static final String ACTIVE = "active";

    private static final String MAIN_WIKI_NAME = "xwiki";

    private static final DocumentReference XWIKI_USER_CLASS_REFERENCE =
        new DocumentReference(MAIN_WIKI_NAME, "XWiki", "XWikiUsers");

    private static final DocumentReference LICENSE_USER_CHECKPOINT_CLASS_REFERENCE =
        new DocumentReference(MAIN_WIKI_NAME, List.of("Licenses", "Code"), "LicensingUserCheckpoint");

    @Inject
    private Licensor licensor;

    @Inject
    private Provider<ObservationManager> observationManagerProvider;

    @Inject
    private Provider<XWikiContext> xWikiContextProvider;

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    protected void runInternal() throws Exception
    {
        Event event = request.getProperty(UserLicenseDisablerJobRequest.PROPERTY_EVENT);
        Object data = request.getProperty(UserLicenseDisablerJobRequest.PROPERTY_DATA);
        Object source = request.getProperty(UserLicenseDisablerJobRequest.PROPERTY_SOURCE);

        Map<AuthExtensionUserManager, Map<XWikiDocument, Boolean>> managedUsers;
        Map<String, AuthExtensionUserManager> userManagerMap;

        try {
            userManagerMap = componentManager.getInstanceMap(AuthExtensionUserManager.class);
        } catch (ComponentLookupException e) {
            return;
        }

        observationManagerProvider.get().notify(new UserLicenseBeginFoldEvent(), source, data);
        if (event instanceof LicenseUpdatedEvent) {
            LicenseUpdatedEvent licenseUpdatedEvent = (LicenseUpdatedEvent) event;
            AuthExtensionUserManager currentExtensionUserManager =
                userManagerMap.get(licenseUpdatedEvent.getLicense().getId().getId());
            if (currentExtensionUserManager != null) {
                managedUsers = new HashMap<>();
                managedUsers.put(currentExtensionUserManager, currentExtensionUserManager.getManagedUsers().stream()
                    .collect(Collectors.toUnmodifiableMap(doc -> doc,
                        doc -> licensor.hasLicensure(doc.getDocumentReference()))));
            } else {
                // This extension doesn't need the custom user limit enforcing.
                observationManagerProvider.get().notify(new UserLicenseEndFoldEvent(), source, data);
                return;
            }
        } else {
            // If a user is updated, we need to check all licenses.
            managedUsers = userManagerMap.values().stream()
                .collect(Collectors.toUnmodifiableMap(v -> v, this::getUserManagerActiveUsers));
        }

        managedUsers.forEach((authManager, userMap) -> userMap.forEach(
            (user, shouldBeActive) -> updateUserPage(user, shouldBeActive, authManager.getClass().getName(),
                xWikiContextProvider.get())));
        observationManagerProvider.get().notify(new UserLicenseEndFoldEvent(), source, data);
    }

    private Map<XWikiDocument, Boolean> getUserManagerActiveUsers(AuthExtensionUserManager userManager)
    {
        return userManager.getManagedUsers().stream().collect(
            Collectors.toUnmodifiableMap(doc -> doc, doc -> userManager.shouldBeActive(doc.getDocumentReference())));
    }

    private void updateUserPage(XWikiDocument user, boolean shouldBeActive, String extensionName, XWikiContext xcontext)
    {
        XWikiDocument userDoc;
        try {
            userDoc = xcontext.getWiki().getDocument(user, xcontext);
        } catch (XWikiException e) {
            logger.warn("Oops [{}]", ExceptionUtils.getRootCauseMessage(e));
            return;
        }
        String editMessage = shouldBeActive ? EDIT_MESSAGE_ENABLE : EDIT_MESSAGE_DISABLE;
        if (shouldBeActive) {
            BaseObject checkpoint = userDoc.getXObject(LICENSE_USER_CHECKPOINT_CLASS_REFERENCE);
            if (checkpoint != null) {
                userDoc.getXObject(XWIKI_USER_CLASS_REFERENCE).set(ACTIVE, checkpoint.getIntValue(ACTIVE), xcontext);
                userDoc.removeXObjects(LICENSE_USER_CHECKPOINT_CLASS_REFERENCE);
            } else {
                logger.debug("No license checkpoint object found for user [{}].", userDoc);
                return;
            }
        } else {
            int previousActiveStatus = userDoc.getXObject(XWIKI_USER_CLASS_REFERENCE).getIntValue(ACTIVE);
            try {
                userDoc.createXObject(LICENSE_USER_CHECKPOINT_CLASS_REFERENCE, xcontext);
            } catch (XWikiException e) {
                logger.error(
                    "Failed to create XObject on user profile for [{}] when enforcing the license for [{}]. Cause:"
                        + " [{}]", userDoc, extensionName, ExceptionUtils.getRootCauseMessage(e));
            }
            userDoc.getXObject(LICENSE_USER_CHECKPOINT_CLASS_REFERENCE).set(ACTIVE, previousActiveStatus, xcontext);

            userDoc.getXObject(XWIKI_USER_CLASS_REFERENCE).set(ACTIVE, 0, xcontext);
        }
        try {
            xcontext.getWiki().saveDocument(userDoc, editMessage, xcontext);
            logger.info("Disabled user [{}] to enforce license for [{}].", userDoc, extensionName);
        } catch (XWikiException e) {
            logger.error("Failed to save user profile for [{}] when enforcing the license for [{}]. Cause: [{}]",
                userDoc, extensionName, ExceptionUtils.getRootCauseMessage(e));
        }
    }
}
