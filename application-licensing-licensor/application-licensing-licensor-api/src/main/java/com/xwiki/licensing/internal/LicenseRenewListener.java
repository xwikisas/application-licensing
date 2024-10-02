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
package com.xwiki.licensing.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.event.ExtensionEvent;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xwiki.licensing.LicensedExtensionManager;

/**
 * Description in progress.
 *
 * @version $Id$
 * @since 1.27
 */
@Component
@Named(LicenseRenewListener.NAME)
@Singleton
public class LicenseRenewListener implements EventListener
{
    protected static final String NAME = "com.xwiki.licensing.internal.LicenseRenewListener";

    protected static final List<Event> EVENTS =
        new ArrayList<>(Arrays.asList(new ExtensionInstalledEvent(), new ExtensionUpgradedEvent()));

    @Inject
    private LicensedExtensionManager licensedExtensionManager;

    @Inject
    private InstalledExtensionRepository installedExtensionRepository;

    @Inject
    private DefaultLicenseUpdater licenseUpdater;

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
        // Retrieve license updates from store.
        licenseUpdater.getLicensesUpdates();

        ExtensionEvent extensionEvent = (ExtensionEvent) event;
        if (licensedExtensionManager.getLicensedExtensions().contains(extensionEvent.getExtensionId())) {
            InstalledExtension installedExtension = (InstalledExtension) source;
            if (installedExtension == null) {
                return;
            }
            Set<ExtensionId> licensedDependencies =
                licensedExtensionManager.getLicensedDependencies(installedExtension, extensionEvent.getNamespace());
            System.out.println(licensedDependencies);

            Set<ExtensionId> previousDependencies =
                getPreviousDependencies(data, installedExtension, extensionEvent.getNamespace());
            System.out.println(previousDependencies);

            // In progress: ! removed for testing purposes.
            if (licensedDependencies.equals(previousDependencies)) {
                licenseUpdater.renewLicense(installedExtension.getId());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Set<ExtensionId> getPreviousDependencies(Object data, InstalledExtension installedExtension,
        String namespace)
    {
        if (data == null) {
            return new HashSet<>();
        }

        for (InstalledExtension previousInstalledExtension : (Collection<InstalledExtension>) data) {
            if (previousInstalledExtension.getId().getId().equals(installedExtension.getId().getId())) {
                return licensedExtensionManager.getLicensedDependencies(previousInstalledExtension, namespace);
            }
        }

        return new HashSet<>();
    }
}
