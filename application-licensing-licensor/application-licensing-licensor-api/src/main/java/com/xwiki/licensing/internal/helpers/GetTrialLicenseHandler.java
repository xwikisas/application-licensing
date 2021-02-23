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
package com.xwiki.licensing.internal.helpers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Scanner;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.extension.ExtensionId;
import org.xwiki.instance.InstanceIdManager;
import org.xwiki.properties.converter.Converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseManager;
import com.xwiki.licensing.LicensedExtensionManager;
import com.xwiki.licensing.LicensingConfiguration;
import com.xwiki.licensing.Licensor;
import com.xwiki.licensing.internal.UserCounter;

/**
 * Helper methods for generating a trial license and updating it.
 *
 * @since 1.17
 * @version $Id$
 */
@Component(roles = GetTrialLicenseHandler.class)
@Singleton
public class GetTrialLicenseHandler
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
    private Provider<LicenseManager> licenseManagerProvider;

    @Inject
    @Named("Base64")
    private BinaryStringEncoder base64decoder;

    @Inject
    private Converter<License> converter;

    /**
     * Create the URL for getting a trial license of a given extension.
     *
     * @param extensionId extension for which the trial license is needed
     * @return the url for getting a trial license
     * @throws Exception if an error occured while constructing the url
     */
    public URL getTrialURL(ExtensionId extensionId) throws Exception
    {
        URIBuilder builder = new URIBuilder(licensingConfig.getStoreTrialURL());

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
     * Retrieve license updates from the XWiki Store.
     */
    @SuppressWarnings("unchecked")
    public void updateLicenses()
    {
        try {
            String licensesUpdateResponse = getURLContent(getLicensesUpdateURL());
            ObjectMapper objectMapper = new ObjectMapper();

            List<String> retrivedLicenses = (List<String>) objectMapper.readValue(licensesUpdateResponse, Object.class);
            for (String license : retrivedLicenses) {
                licenseManagerProvider.get().add(converter.convert(License.class, base64decoder.decode(license)));
            }
        } catch (URISyntaxException | IOException e) {
            logger.warn("Error while updating licenses. Root cause [{}]", ExceptionUtils.getRootCauseMessage(e));
        }

    }

    /**
     * Construct the url for updating licenses.
     *
     * @return the url for updating licenses
     * @throws URISyntaxException if the url is not valid
     * @throws MalformedURLException if an error occured while constructing the URL
     */
    public URL getLicensesUpdateURL() throws URISyntaxException, MalformedURLException
    {
        URIBuilder builder = new URIBuilder(licensingConfig.getStoreUpdateURL());
        builder.addParameter(INSTANCE_ID, instanceIdManagerProvider.get().getInstanceId().toString());
        builder.addParameter("outputSyntax", "plain");

        for (ExtensionId paidExtensionId : licensedExtensionManager.getVisibleLicensedExtensions()) {
            builder.addParameter(FEATURE_ID, paidExtensionId.getId());

            License license = licensorProvider.get().getLicense(paidExtensionId);
            if (license != null) {
                builder.addParameter(String.format("expirationDate:%s", paidExtensionId.getId()),
                    Long.toString(license.getExpirationDate()));
            }
        }
        URL updateURL = builder.build().toURL();
        return updateURL;
    }

    /**
     * Returns the content of an URL.
     *
     * @param trialURL URL to retrieve
     * @return content of the specified URL
     * @throws IOException if an I/O exception occurs
     */
    public String getURLContent(URL trialURL) throws IOException
    {
        Scanner sc = new Scanner(trialURL.openStream());
        StringBuffer sb = new StringBuffer();
        while (sc.hasNext()) {
            sb.append(sc.next());
        }
        sc.close();

        return sb.toString();
    }

    /**
     * Check if the given extension is licensed.
     * 
     * @param extensionId the extension to be checked
     * @return true if the extension is licensed, false otherwise
     */
    public Boolean isLicensedExtension(ExtensionId extensionId)
    {
        return licensedExtensionManager.getVisibleLicensedExtensions().stream()
            .filter(o -> o.getId().contentEquals(extensionId.getId())).findFirst().isPresent();
    }

    /**
     * @return false if owner information is not complete filled up and true otherwise.
     */
    public Boolean isOwnerDataComplete()
    {
        return !(licensingConfig.getLicensingOwnerLastName().isEmpty()
            || licensingConfig.getLicensingOwnerFirstName().isEmpty()
            || licensingConfig.getLicensingOwnerEmail().isEmpty());
    }
}
