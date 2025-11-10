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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.version.Version;
import org.xwiki.observation.ObservationManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xwiki.licensing.LicensedExtensionManager;
import com.xwiki.licensing.LicensingConfiguration;
import com.xwiki.licensing.internal.upgrades.notifications.newVersion.NewExtensionVersionAvailableEvent;

/**
 * Check licensed extensions for new available versions and send a notification, without sending multiple notifications
 * for the same version.
 *
 * @version $Id$
 * @since 1.23
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

    @Inject
    private Logger logger;

    @Inject
    private NewVersionNotificationManager newVersionNotificationManager;

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
                notifyExtensionVersionAvailable(installedExtension.getId(), null);
            } else {
                for (String namespace : installedExtension.getNamespaces()) {
                    notifyExtensionVersionAvailable(installedExtension.getId(), namespace);
                }
            }
        }
    }

    private void notifyExtensionVersionAvailable(ExtensionId extensionId, String namespace)
    {
        InstalledExtension installedExtension =
            installedRepository.getInstalledExtension(extensionId.getId(), namespace);
        // Get the list of versions that can be installed, with the first one being the most recent.
        List<Version> installableVersions = upgradeExtensionHandler.getInstallableVersions(installedExtension.getId());
        if (installableVersions.isEmpty()) {
            return;
        }

        try {
            String namespaceName = namespace != null ? namespace : "root";
            if (!this.newVersionNotificationManager.isNotificationAlreadySent(extensionId.getId(), namespaceName,
                installableVersions.get(0).getValue()))
            {
                Map<String, String> extensionInfo = new HashMap<>();
                extensionInfo.put("extensionName", installedExtension.getName());
                extensionInfo.put("namespace", namespaceName);
                extensionInfo.put("version", installableVersions.get(0).getValue());

                this.observationManager.notify(new NewExtensionVersionAvailableEvent(
                        new ExtensionId(extensionId.getId(), installableVersions.get(0)), namespace,
                        licensingConfig.getNotifiedGroupsSet()), extensionId.getId(),
                    (new ObjectMapper()).writeValueAsString(extensionInfo));
                this.newVersionNotificationManager.markNotificationAsSent(extensionId.getId(), namespaceName,
                    installableVersions.get(0).getValue());
            }
        } catch (JsonProcessingException e) {
            this.logger.warn("Failed to send a NewExtensionVersionAvailableEvent for [{}]. Root cause is [{}]",
                extensionId.getId(), ExceptionUtils.getRootCauseMessage(e));
        }
    }
}
