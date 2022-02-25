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

import com.xwiki.licensing.LicensedExtensionManager;
import com.xwiki.licensing.LicensingConfiguration;

/**
 * Verifies extensions that have a license for possible upgrades and tries the install of the last compatible version.
 *
 * @since 1.17
 * @version $Id$
 */
@Component(roles = LicensedExtensionUpgradeManager.class)
@Singleton
public class LicensedExtensionUpgradeManager
{
    @Inject
    private InstalledExtensionRepository installedRepository;

    @Inject
    private UpgradeExtensionHandler upgradeExtensionHandler;

    @Inject
    private LicensedExtensionManager licensedExtensionManager;

    @Inject
    private LicensingConfiguration licensingConfig;

    /**
     * Triggers the upgrade for licensed applications in all namespaces where they are enabled, skipping the ones that
     * are blocked for upgrade.
     */
    public void upgradeLicensedExtensions()
    {
        List<String> allowlist = licensingConfig.getAutoUpgradeAllowList();

        for (ExtensionId extensionId : licensedExtensionManager.getLicensedExtensions()) {
            if (!allowlist.contains(extensionId.getId())) {
                continue;
            }

            InstalledExtension installedExtension = installedRepository.getInstalledExtension(extensionId);
            Collection<String> namespaces = installedExtension.getNamespaces();
            if (namespaces == null) {
                upgradeExtensionHandler.tryUpgradeExtensionToLastVersion(installedExtension, null);
            } else {
                for (String namespace : installedExtension.getNamespaces()) {
                    upgradeExtensionHandler.tryUpgradeExtensionToLastVersion(installedExtension, namespace);
                }
            }

        }
    }
}
