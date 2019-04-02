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
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.security.authorization.AuthorizationSettler;
import org.xwiki.security.authorization.SecurityEntryReader;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.licensing.LicenseManager;

/**
 * Initialize the licensing system.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named("LicensingInitializerListener")
public class LicensingInitializer extends AbstractEventListener implements Initializable
{
    @Inject
    private Logger logger;

    @Inject
    private ComponentManager componentManager;

    /**
     * Used to check if the XWiki database is ready.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Used to initialize the license manager as soon as the XWiki database is ready (it needs to read the instance id
     * from the database).
     */
    @Inject
    private Provider<LicenseManager> licenseManagerProvider;

    /**
     * Default constructor.
     */
    public LicensingInitializer()
    {
        super("LicensingInitializerListener", new ApplicationReadyEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        try {
            // Initialize the licensing system as soon as the XWiki application (database) is ready.
            initialize();
        } catch (InitializationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize() throws InitializationException
    {
        // Check if the XWiki database is ready (we need to read the instance id from the database). If it's not ready
        // then the initialization will be performed when the ApplicationReadyEvent is fired (see above).
        if (this.xcontextProvider.get() != null) {
            logger.debug("Initializing the licensing system.");
            try {
                if (!LicensingUtils.isPristineImpl(
                    componentManager.getInstance(AuthorizationSettler.class, LicensingAuthorizationSettler.HINT))
                    || !LicensingUtils.isPristineImpl(
                        componentManager.getInstance(SecurityEntryReader.class, LicensingSecurityEntryReader.HINT))) {
                    logger.debug("Integrity check failed when getting authorization settler.");
                    throw new Exception();
                }

                // Load the licenses from the file system before they are used.
                this.licenseManagerProvider.get();
            } catch (Exception e) {
                logger.error("The licensure engine has failed to be properly registered,"
                    + " this could affect your ability to use licensed extensions.");
            }
        }
    }
}
