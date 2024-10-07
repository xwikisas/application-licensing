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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.instance.InstanceIdManager;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicensedExtensionManager;
import com.xwiki.licensing.LicensingConfiguration;
import com.xwiki.licensing.Licensor;

/**
 * Helper methods for generating a trial license and updating it.
 *
 * @version $Id$
 * @since 1.17
 */
@Component(roles = TrialLicenseGenerator.class)
@Singleton
public class TrialLicenseGenerator
{
    private static final String FEATURE_ID = "featureId";

    private static final String INSTANCE_ID = "instanceId";

    @Inject
    private Logger logger;

    @Inject
    private Provider<InstanceIdManager> instanceIdManagerProvider;

    @Inject
    private UserCounter userCounter;

    @Inject
    private LicensedExtensionManager licensedExtensionManager;

    @Inject
    private LicensingConfiguration licensingConfig;

    @Inject
    private Provider<Licensor> licensorProvider;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private DefaultLicenseUpdater licenseUpdater;

    /**
     * Generate trial license for the given extension.
     *
     * @param extensionId the extension for which to generate a trial license
     */
    public void generateTrialLicense(ExtensionId extensionId)
    {
        try {
            URL trialURL = getTrialURL(extensionId);
            if (trialURL == null) {
                logger.warn("Failed to add trial license for [{}] because the licensor configuration is not complete. "
                    + "Check your store trial URL and owner details.", extensionId.getId());
                return;
            }

            XWikiContext xcontext = contextProvider.get();
            String getTrialResponse = xcontext.getWiki().getURLContent(trialURL.toString(), xcontext);

            if (getTrialResponse.contains("error")) {
                logger.warn("Failed to generate trial license for [{}] on store.", extensionId.getId());
            } else {
                logger.debug("Trial license added for [{}]", extensionId.getId());
                licenseUpdater.getLicensesUpdates();
            }
        } catch (Exception e) {
            logger.warn("Failed to get trial license for [{}]. Root cause is [{}]", extensionId,
                ExceptionUtils.getRootCauseMessage(e));
        }
    }

    /**
     * Check if the given extension is a mandatory licensed extension, if there isn't already an active license for it
     * and the licensing owner information is complete.
     *
     * @param extensionId extension to be checked
     * @return true if a trial license can be generated for the given extension, false otherwise
     */
    public Boolean canGenerateTrialLicense(ExtensionId extensionId)
    {
        return License.UNLICENSED.equals(licensorProvider.get().getLicense(extensionId)) && isOwnerDataComplete()
            && isMandatoryLicensedExtension(extensionId);
    }

    /**
     * Create the URL for getting a trial license of a given extension.
     *
     * @param extensionId extension for which the trial license is needed
     * @return the URL for getting a trial license, or null if it couldn't be constructed
     * @throws Exception if an error occured while constructing the url
     */
    private URL getTrialURL(ExtensionId extensionId) throws Exception
    {
        String storeTrialURL = licensingConfig.getStoreTrialURL();
        // In case the property has no filled value, the URL cannot be constructed.
        if (storeTrialURL == null) {
            return null;
        }

        URIBuilder builder = new URIBuilder(storeTrialURL);

        builder.addParameter("firstName", licensingConfig.getLicensingOwnerFirstName());
        builder.addParameter("lastName", licensingConfig.getLicensingOwnerLastName());
        builder.addParameter("email", licensingConfig.getLicensingOwnerEmail());
        builder.addParameter(INSTANCE_ID, instanceIdManagerProvider.get().getInstanceId().toString());
        builder.addParameter(FEATURE_ID, extensionId.getId());
        builder.addParameter("extensionVersion", extensionId.getVersion().getValue());
        builder.addParameter("licenseType", "TRIAL");
        builder.addParameter("userCount", String.valueOf(userCounter.getUserCount()));

        return builder.build().toURL();
    }

    /**
     * Check if for the given licensed extension a license is mandatory, meaning that it is not covered by the license
     * of other extension (it's not dependency of other licensed extension).
     *
     * @param extensionId the extension to be checked
     * @return true if is a mandatory licensed extension, false otherwise
     */
    private Boolean isMandatoryLicensedExtension(ExtensionId extensionId)
    {
        return licensedExtensionManager.getMandatoryLicensedExtensions().contains(extensionId);
    }

    /**
     * @return false if owner information is not complete filled up and true otherwise.
     */
    private Boolean isOwnerDataComplete()
    {
        return !(StringUtils.isEmpty(licensingConfig.getLicensingOwnerLastName()) || StringUtils.isEmpty(
            licensingConfig.getLicensingOwnerFirstName()) || StringUtils.isEmpty(
            licensingConfig.getLicensingOwnerEmail()));
    }
}
