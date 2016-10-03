package com.xwiki.licensing.internal.enforcer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.security.authorization.AuthorizationSettler;
import org.xwiki.security.authorization.SecurityEntryReader;

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

    /** The context component manager. */
    @Inject
    private ComponentManager componentManager;

    /**
     * Default constructor.
     */
    public LicensingInitializer()
    {
        super("LicensingInitializerListener");
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Never call, we use this Listener just to get instanciated !
    }

    @Override
    public void initialize() throws InitializationException
    {
        logger.debug("Initializing the licensing system.");
        try {
            if (!LicensingUtils.isPristineImpl(componentManager.getInstance(AuthorizationSettler.class,
                LicensingAuthorizationSettler.HINT))
                || !LicensingUtils.isPristineImpl(componentManager.getInstance(SecurityEntryReader.class,
                LicensingSecurityEntryReader.HINT))) {
                logger.debug("Integrity check failed when getting authorization settler.");
                throw new Exception();
            }
        } catch (Exception e) {
            logger.error("The licensure engine has failed to be properly registered, this could affect your ability "
                + "to use licensed extensions.");
        }
    }
}
