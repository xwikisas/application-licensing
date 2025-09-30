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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.event.filter.EventFilter;
import org.xwiki.observation.event.filter.RegexEventFilter;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.licensing.LicenseManager;

/**
 * Invalidate the security cache when any user is updated, so the {@link LicensingAuthorizationSettler} is
 * called to compute the correct rights for all users on licensed extensions.
 * This ensures that users under the user limit still have view rights on licensed pages, and those above the limit
 * don't.
 *
 * @version $Id$
 * @since 1.31
 */
@Component
@Singleton
@Named(UserLicenseSecurityCacheInvalidator.ROLE_NAME)
public class UserLicenseSecurityCacheInvalidator extends AbstractEventListener
{
    /**
     * The role name of this component.
     */
    public static final String ROLE_NAME = "com.xwiki.licensing.internal.enforcer.UserLicenseSecurityCacheInvalidator";

    private static final EventFilter XWIKI_SPACE_FILTER = new RegexEventFilter("^(.*:)?XWiki\\..*");

    private static final String MAIN_WIKI_NAME = "xwiki";

    private static final DocumentReference XWIKI_USER_CLASS_REFERENCE =
        new DocumentReference(MAIN_WIKI_NAME, "XWiki", "XWikiUsers");

    @Inject
    private Logger logger;

    @Inject
    private LicensingSecurityCacheRuleInvalidator licensingSecurityCacheRuleInvalidator;

    @Inject
    @Named(XarExtensionHandler.TYPE)
    private InstalledExtensionRepository xarInstalledExtensionRepository;

    @Inject
    private LicenseManager licenseManager;

    /**
     * Default constructor.
     */
    public UserLicenseSecurityCacheInvalidator()
    {
        super(ROLE_NAME,
            Arrays.asList(new DocumentCreatedEvent(XWIKI_SPACE_FILTER), new DocumentUpdatedEvent(XWIKI_SPACE_FILTER),
                new DocumentDeletedEvent(XWIKI_SPACE_FILTER)));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Only invalidate security cache when users are modified.
        if (((XWikiDocument) source).getXObject(XWIKI_USER_CLASS_REFERENCE) != null) {
            xarInstalledExtensionRepository.getInstalledExtensions().parallelStream().filter(
                a -> a instanceof XarInstalledExtension
                        && licenseManager.get(a.getId()) != null
                        && isValidLicenseUserCount(licenseManager.get(a.getId()).getMaxUserCount())
            ).forEach(extension -> {
                logger.warn("Clearing security cache for licensed extension [{}] after user status change.", extension);
                licensingSecurityCacheRuleInvalidator.invalidate((XarInstalledExtension) extension);
            });
        }
    }

    private boolean isValidLicenseUserCount(long count)
    {
        return !(count < 0 || count == Long.MAX_VALUE);
    }
}
