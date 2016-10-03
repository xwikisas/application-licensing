package com.xwiki.licensing.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.event.ExtensionEvent;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.event.ExtensionUninstalledEvent;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xwiki.licensing.LicenseManager;

/**
 * Listener of Extension event for the management of Licenses.
 *
 * @version $Id$
 */
@Component
@Named(LicenseManagerEventListener.NAME)
@Singleton
public class LicenseManagerEventListener implements EventListener
{
    static final String NAME = "licenseManager";

    /**
     * The events observed by this event listener.
     */
    private static final List<Event> EVENTS = new ArrayList<>(Arrays.asList(
        new ExtensionInstalledEvent(),
        new ExtensionUninstalledEvent(),
        new ExtensionUpgradedEvent()));

    @Inject
    private Provider<LicenseManager> licenseManagerProvider;

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        ExtensionEvent extensionEvent = (ExtensionEvent) event;
        InstalledExtension installedExtension = (InstalledExtension) source;
        DefaultLicenseManager licenseManager = (DefaultLicenseManager) licenseManagerProvider.get();

        if (event instanceof ExtensionInstalledEvent) {
            licenseManager.installExtensionLicense(extensionEvent.getNamespace(), installedExtension);
        } else if (event instanceof ExtensionUninstalledEvent) {
            licenseManager.uninstallExtensionLicense(installedExtension);
        } else if (event instanceof ExtensionUpgradedEvent) {
            licenseManager.uninstallExtensionLicense((InstalledExtension) data);
            licenseManager.installExtensionLicense(extensionEvent.getNamespace(), installedExtension);
        }
    }
}
