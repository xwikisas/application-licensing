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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.security.authorization.AuthorizationSettler;
import org.xwiki.security.authorization.SecurityEntryReader;

import com.xwiki.licensing.LicenseManager;
import com.xwiki.licensing.internal.DefaultLicensedExtensionManager;

/**
 * Initialize the licensing system:
 * <ul>
 * <li>right after the licensor API extension has been installed for the first time (we need it to be marked as
 * installed so that we can detect the licensed extensions)</li>
 * <li>as soon as the XWiki application (database) is ready (usually after a server restart), because we need to read
 * the XWiki instance id from the database.</li>
 * </ul>
 *
 * @version $Id$
 */
@Component
@Singleton
@Named("LicensingInitializerListener")
public class LicensingInitializer extends AbstractEventListener
{
    @Inject
    private Logger logger;

    /**
     * Used to initialize the license manager as soon as the XWiki database is ready (it needs to read the XWiki
     * instance id from the database) and as soon as the licensor API extension is marked as installed (in order to be
     * able to detect the licensed extensions).
     */
    @Inject
    private Provider<LicenseManager> licenseManagerProvider;

    @Inject
    @Named(LicensingAuthorizationSettler.HINT)
    private Provider<AuthorizationSettler> licensingAuthorizationSettlerProvider;

    @Inject
    @Named(LicensingSecurityEntryReader.HINT)
    private Provider<SecurityEntryReader> licensingSecurityEntryReaderProvider;

    /**
     * Default constructor.
     */
    public LicensingInitializer()
    {
        super("LicensingInitializerListener", new ApplicationReadyEvent(),
            new ExtensionInstalledEvent(new ExtensionId(DefaultLicensedExtensionManager.LICENSOR_EXTENSION_ID), null));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        this.logger.debug("Initializing the licensing system.");
        try {
            if (!LicensingUtils.isPristineImpl(this.licensingAuthorizationSettlerProvider.get())
                || !LicensingUtils.isPristineImpl(this.licensingSecurityEntryReaderProvider.get())) {
                logger.debug("Integrity check failed when getting authorization settler.");
                throw new Exception();
            }

            // Load the licenses from the file system before they are used.
            this.licenseManagerProvider.get();
        } catch (Exception e) {
            this.logger.error("The licensing system has failed to be properly registered,"
                + " this could affect your ability to use licensed extensions.");
        }
    }
}
