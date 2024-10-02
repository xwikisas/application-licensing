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

import java.net.URL;
import java.util.Collection;
import java.util.Set;
import java.util.Stack;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.instance.InstanceIdManager;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.licensing.LicensedExtensionManager;
import com.xwiki.licensing.LicensingConfiguration;

/**
 * In progress: these methods might be moved to other components at the refactoring step.
 */
@Component(roles = LicenseRenew.class)
@Singleton
public class LicenseRenew
{
    private static final String FEATURE_ID = "featureId";

    private static final String INSTANCE_ID = "instanceId";

    @Inject
    private Logger logger;

    @Inject
    private LicensedExtensionManager licensedExtensionManager;

    @Inject
    private InstalledExtensionRepository installedExtensionRepository;

    @Inject
    private LicensingConfiguration licensingConfig;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Provider<InstanceIdManager> instanceIdManagerProvider;

    @Inject
    private TrialLicenseGenerator trialLicenseGenerator;

    public void getLicensedDependencies(Set<ExtensionId> licensedDependencies, Stack<ExtensionId> dependencyPath,
        InstalledExtension installedExtension, String namespace)
    {
        Collection<ExtensionDependency> dependencies = installedExtension.getDependencies();

        for (ExtensionDependency dep : dependencies) {
            InstalledExtension installedDep =
                installedExtensionRepository.getInstalledExtension(dep.getId(), namespace);
            if (installedDep == null || licensedDependencies.contains(installedDep.getId())
                || dependencyPath.search(installedDep.getId()) > 0)
            {
                return;
            }

            if (licensedExtensionManager.getLicensedExtensions().contains(installedDep.getId())) {
                licensedDependencies.add(installedDep.getId());
            }

            dependencyPath.push(installedDep.getId());
            getLicensedDependencies(licensedDependencies, dependencyPath, installedDep, namespace);
        }
    }

    public void renewLicense(ExtensionId extensionId)
    {
        try {
            URL licenseRenewURL = getLicenseRenewURL(extensionId);
            if (licenseRenewURL == null) {
                logger.debug("Failed to add renew license for [{}] because the licensor configuration is not complete. "
                    + "Check your store license renew URL and owner details.", extensionId.getId());
                return;
            }

            XWikiContext xcontext = contextProvider.get();
            String getLicenseRenewResponse = xcontext.getWiki().getURLContent(licenseRenewURL.toString(), xcontext);

            if (getLicenseRenewResponse.contains("error")) {
                logger.debug("Failed to renew license for [{}] on store.", extensionId.getId());
            } else {
                logger.debug("License renewed for [{}]", extensionId.getId());
                // This will be moved to other component
                //trialLicenseGenerator.updateLicenses();
            }
        } catch (Exception e) {
            logger.warn("Failed to update license for [{}]. Root cause is [{}]", extensionId,
                ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private URL getLicenseRenewURL(ExtensionId extensionId) throws Exception
    {
        String storeLicenseRenewURL = licensingConfig.getStoreLicenseRenewURL();
        // In case the property has no filled value, the URL cannot be constructed.
        if (storeLicenseRenewURL == null) {
            return null;
        }

        URIBuilder builder = new URIBuilder(storeLicenseRenewURL);

        builder.addParameter("firstName", licensingConfig.getLicensingOwnerFirstName());
        builder.addParameter("lastName", licensingConfig.getLicensingOwnerLastName());
        builder.addParameter("email", licensingConfig.getLicensingOwnerEmail());
        builder.addParameter(INSTANCE_ID, instanceIdManagerProvider.get().getInstanceId().toString());
        builder.addParameter(FEATURE_ID, extensionId.getId());
        builder.addParameter("extensionVersion", extensionId.getVersion().getValue());
        // In progress: take it from existing license.
        builder.addParameter("licenseType", "TRIAL");

        return builder.build().toURL();
    }
}
