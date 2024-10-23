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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.event.ExtensionEvent;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseUpdater;
import com.xwiki.licensing.LicensedExtensionManager;
import com.xwiki.licensing.LicensedFeatureId;
import com.xwiki.licensing.Licensor;

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
        Arrays.asList(new ExtensionInstalledEvent(), new ExtensionUpgradedEvent());

    @Inject
    private LicensedExtensionManager licensedExtensionManager;

    @Inject
    private InstalledExtensionRepository installedExtensionRepository;

    @Inject
    private Provider<Licensor> licensorProvider;

    @Inject
    private LicenseUpdater licenseUpdater;

    @Inject
    private Logger logger;

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
        // Only treat top level licensed extensions. If it's not a top level extension, then its license it's handled
        // by another extension's license.
        if (licensedExtensionManager.getLicensedExtensions().contains(extensionEvent.getExtensionId())
            && isMandatoryLicensedExtension(extensionEvent.getExtensionId()))
        {
            InstalledExtension installedExtension = (InstalledExtension) source;
            if (installedExtension == null) {
                return;
            }

            License license = licensorProvider.get().getLicense(installedExtension.getId());
            boolean hasLicense = license != null && !License.UNLICENSED.equals(license);

            if (event instanceof ExtensionUpgradedEvent && hasLicense) {
                extensionUpgraded(installedExtension, data, extensionEvent, license);
            } else if (event instanceof ExtensionInstalledEvent) {
                extensionInstalled(installedExtension, extensionEvent, hasLicense);
            }
        }
    }

    private void extensionInstalled(InstalledExtension installedExtension, ExtensionEvent extensionEvent,
        boolean hasLicense)
    {
        if (hasLicense) {
            logger.debug(
                "[{}] has been installed and it has a license associated to it. Check if there are license changes "
                    + "for this new extension version.", extensionEvent.getExtensionId());

            // Since we don't have information on the old installed version, we trigger a license renew and let store
            // check if this license needs changes.
            licenseUpdater.renewLicense(installedExtension.getId());
        }
    }

    private void extensionUpgraded(InstalledExtension installedExtension, Object data, ExtensionEvent extensionEvent,
        License license)
    {
        InstalledExtension prevInstalledExtension =
            getPreviousInstalledExtension(data, installedExtension, extensionEvent.getNamespace());
        // Stop if previous version is actually higher, because we don't want to regenerate a license downgrade.
        if (prevInstalledExtension != null
            && prevInstalledExtension.getId().getVersion().compareTo(installedExtension.getId().getVersion()) > 0)
        {
            logger.debug("The licensed extension [{}] has been downgraded. No license renew it's triggered.",
                extensionEvent.getExtensionId());
            return;
        }

        logger.debug("[{}] has been upgraded and it has a license associated to it. Checking if there are dependencies"
            + " changes, in order to update its license too.", extensionEvent.getExtensionId());

        Set<ExtensionId> licensedDependencies =
            licensedExtensionManager.getLicensedDependencies(installedExtension, extensionEvent.getNamespace());

        Set<ExtensionId> previousDependencies =
            licensedExtensionManager.getLicensedDependencies(prevInstalledExtension, extensionEvent.getNamespace());

        // Besides comparing differences between previous and current version, consider also the licensed feature ids.
        // This is needed because during a license renew there could be issues and only on the licensed feature ids
        // we can check that there were changes at some point, since these 2 versions now have the same
        // dependencies.
        if (!licensedDependencies.equals(previousDependencies) || licensedFeatureIdsChanges(license, installedExtension,
            licensedDependencies))
        {
            logger.debug("New licensed dependencies found.");
            licenseUpdater.renewLicense(installedExtension.getId());
        }
    }

    private boolean licensedFeatureIdsChanges(License license, InstalledExtension installedExtension,
        Set<ExtensionId> licensedDependencies)
    {
        List<String> licenseFeatureIds =
            license.getFeatureIds().stream().map(LicensedFeatureId::getId).collect(Collectors.toList());
        licenseFeatureIds.removeIf(ext -> ext.equals(installedExtension.getId().getId()));
        List<String> licensedDependenciesIds =
            licensedDependencies.stream().map(ExtensionId::getId).collect(Collectors.toList());

        boolean changedDependencies = !licensedDependenciesIds.equals(licenseFeatureIds);
        if (changedDependencies) {
            logger.debug("License contains outdated feature ids [{}]", licenseFeatureIds);
        }
        return changedDependencies;
    }

    @SuppressWarnings("unchecked")
    private InstalledExtension getPreviousInstalledExtension(Object data, InstalledExtension installedExtension,
        String namespace)
    {
        for (InstalledExtension previousInstalledExtension : (Collection<InstalledExtension>) data) {
            if (previousInstalledExtension.getId().getId().equals(installedExtension.getId().getId())) {
                return previousInstalledExtension;
            }
        }
        return null;
    }

    private boolean isMandatoryLicensedExtension(ExtensionId extensionId)
    {
        return licensedExtensionManager.getMandatoryLicensedExtensions().stream().map(ExtensionId::getId)
            .collect(Collectors.toList()).contains(extensionId.getId());
    }
}
