package com.xwiki.licensing.internal.enforcer;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.observation.EventListener;
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
public class LicensingInitializer implements EventListener, Initializable
{
    @Inject
    private Logger logger;

    /** The context component manager. */
    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Override
    public List<Event> getEvents()
    {
        return Collections.emptyList();
    }

    @Override
    public String getName()
    {
        return "LicensorInitializerListener";
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
