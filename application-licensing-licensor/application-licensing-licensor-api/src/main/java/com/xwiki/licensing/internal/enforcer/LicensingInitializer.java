package com.xwiki.licensing.internal.enforcer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.extension.version.Version;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
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
public class LicensingInitializer implements EventListener
{
    @Inject
    private Logger logger;

    /** The context component manager. */
    @Inject
    @Named("context")
    private ComponentManager componentManager;

    /**
     * The events observed by this event listener.
     */
    private static final List<Event> EVENTS = new ArrayList<>(Arrays.asList(
        new ApplicationStartedEvent(),
        new ExtensionInstalledEvent(
            new ExtensionId("com.xwiki.licensing:application-licensing-licensor", (Version) null),
            null),
        new ExtensionUpgradedEvent(
            new ExtensionId("com.xwiki.licensing:application-licensing-licensor", (Version) null),
            null)));

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public String getName()
    {
        return "LicensorInitializerListener";
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
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
