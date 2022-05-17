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
package com.xwiki.licensing.internal.upgrades;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.version.Version;
import org.xwiki.observation.ObservationManager;

import com.xwiki.licensing.LicensedExtensionManager;
import com.xwiki.licensing.LicensingConfiguration;
import com.xwiki.licensing.internal.upgrades.notifications.newVersion.NewExtensionVersionAvailableEvent;

/**
 * Check licensed extensions for new available versions and send a notification.
 *
 * @since 1.23
 * @version $Id$
 */
@Component(roles = NewExtensionVersionAvailableManager.class)
@Singleton
public class NewExtensionVersionAvailableManager
{
    @Inject
    private InstalledExtensionRepository installedRepository;

    @Inject
    private UpgradeExtensionHandler upgradeExtensionHandler;

    @Inject
    private LicensedExtensionManager licensedExtensionManager;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private LicensingConfiguration licensingConfig;

    /**
     * Notify the administrators when one of the installed licensed applications has a new version available. Do nothing
     * for extensions that have auto upgrades enabled.
     */
    public void checkLicensedExtensionsAvailableVersions()
    {
        List<String> allowlist = licensingConfig.getAutoUpgradeAllowList();

        for (ExtensionId extensionId : licensedExtensionManager.getLicensedExtensions()) {
            if (allowlist.contains(extensionId.getId())) {
                continue;
            }

            InstalledExtension installedExtension = installedRepository.getInstalledExtension(extensionId);
            Collection<String> namespaces = installedExtension.getNamespaces();
            if (namespaces == null) {
                notifyExtensionAvailableVersions(installedExtension.getId(), null);
            } else {
                for (String namespace : installedExtension.getNamespaces()) {
                    notifyExtensionAvailableVersions(installedExtension.getId(), namespace);
                }
            }
        }

    }

    private void notifyExtensionAvailableVersions(ExtensionId extensionId, String namespace)
    {
        InstalledExtension installedExtension =
            installedRepository.getInstalledExtension(extensionId.getId(), namespace);
        List<Version> versions = upgradeExtensionHandler.getInstallableVersions(installedExtension.getId());
        if (versions.size() > 0) {
            String message = String.format("%s - %s - %s", installedExtension.getName(),
                namespace != null ? namespace : "root", versions.get(0));
            this.observationManager.notify(new NewExtensionVersionAvailableEvent(), extensionId.getId(), message);
        }
    }

}
