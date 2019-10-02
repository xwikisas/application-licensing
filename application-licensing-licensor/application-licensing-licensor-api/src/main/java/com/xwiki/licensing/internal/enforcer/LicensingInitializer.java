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
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.security.authorization.AuthorizationSettler;
import org.xwiki.security.authorization.SecurityEntryReader;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.licensing.LicenseManager;
import com.xwiki.licensing.internal.DefaultLicensedExtensionManager;

/**
 * Initialize the licensing system:
 * <ul>
 * <li>when XWiki is restarted, as soon as the database is ready,</li>
 * <li>when the Licensor API extension is installed or upgraded,</li>
 * <li>when the component manager reloads its components (e.g. when the XWiki distribution is upgraded).</li>
 * </ul>
 *
 * @version $Id$
 */
@Component
@Singleton
@Named(LicensingInitializer.HINT)
public class LicensingInitializer extends AbstractEventListener implements Initializable
{
    /**
     * The hint of this component.
     */
    public static final String HINT = "LicensingInitializerListener";

    @Inject
    private Logger logger;

    @Inject
    private Provider<LicenseManager> licenseManagerProvider;

    @Inject
    @Named(LicensingAuthorizationSettler.HINT)
    private Provider<AuthorizationSettler> licensingAuthorizationSettlerProvider;

    @Inject
    @Named(LicensingSecurityEntryReader.HINT)
    private Provider<SecurityEntryReader> licensingSecurityEntryReaderProvider;

    /**
     * Used to check if the XWiki database is ready since this component can be initialized before. We can't read the
     * XWiki instance id from the database otherwise.
     */
    @Inject
    @Named("readonly")
    private Provider<XWikiContext> readOnlyXWikiContextProvider;

    /**
     * Used to check if the Licensor API extension is installed. We can't detect licensed extensions otherwise (we need
     * to retrieve the backward dependencies).
     */
    @Inject
    private Provider<InstalledExtensionRepository> installedExtensionRepositoryProvider;

    /**
     * Default constructor.
     */
    public LicensingInitializer()
    {
        super(HINT, new ApplicationReadyEvent(),
            new ExtensionInstalledEvent(new ExtensionId(DefaultLicensedExtensionManager.LICENSOR_EXTENSION_ID), null));
    }

    @Override
    public void initialize() throws InitializationException
    {
        // The component manager can reload its components at runtime (e.g. when upgrading the XWiki distribution) so we
        // need to re-initialize the licensing system when that happens, if the XWiki database is ready and the Licensor
        // API extension is marked as installed.
        boolean databaseReady = this.readOnlyXWikiContextProvider.get() != null;
        InstalledExtension licensorExtension = this.installedExtensionRepositoryProvider.get()
            .getInstalledExtension(DefaultLicensedExtensionManager.LICENSOR_EXTENSION_ID, null);
        if (databaseReady && licensorExtension != null) {
            initializeLicensingSystem();
        }
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // If we get here then either XWiki has just been restarted and its database is ready now (we can't read the
        // XWiki instance id otherwise) or the Licensor API extension has just been marked as installed (we can't
        // retrieve its backward dependencies otherwise).
        initializeLicensingSystem();
    }

    private void initializeLicensingSystem()
    {
        this.logger.debug("Initializing the licensing system.");
        try {
            // Overwrite default authorization components in order to add the license check.
            if (!LicensingUtils.isPristineImpl(this.licensingAuthorizationSettlerProvider.get())
                || !LicensingUtils.isPristineImpl(this.licensingSecurityEntryReaderProvider.get())) {
                logger.debug("Integrity check failed when getting authorization settler.");
                throw new Exception();
            }

            // Load the licenses from the file system before they are used.
            this.licenseManagerProvider.get();
        } catch (Exception e) {
            this.logger.error("The licensing system has failed to be properly initialized,"
                + " this could affect your ability to use licensed extensions.");
        }
    }
}
