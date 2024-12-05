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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.instance.InstanceIdManager;
import org.xwiki.properties.converter.Converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpn.xwiki.XWikiContext;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseManager;
import com.xwiki.licensing.LicenseUpdater;
import com.xwiki.licensing.LicensedExtensionManager;
import com.xwiki.licensing.LicensingConfiguration;
import com.xwiki.licensing.Licensor;
import com.xwiki.licensing.internal.helpers.HttpClientUtils;

/**
 * Default implementation of {@link LicenseUpdater}.
 *
 * @version $Id$
 * @since 1.27
 */
@Component
@Singleton
public class DefaultLicenseUpdater implements LicenseUpdater
{
    private static final String OUTPUT_SYNTAX = "outputSyntax";

    private static final String PLAIN = "plain";

    private static final String DATA = "data";

    private static final String ERROR =
        "Failed to update license for [%s]. Please contact your administrator for eventual problems.";

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
    private Provider<Licensor> licensorProvider;

    @Inject
    private Provider<LicenseManager> licenseManagerProvider;

    @Inject
    @Named("Base64")
    private BinaryStringEncoder base64decoder;

    @Inject
    private Converter<License> converter;

    @Inject
    private HttpClientUtils httpUtils;

    @Override
    public void renewLicense(ExtensionId extensionId)
    {
        String errorMsg = String.format(ERROR, extensionId);

        try {
            logger.debug("Try renewing the license of [{}], in order to include new found changes.", extensionId);

            JsonNode licenseRenewResponse = httpUtils.httpPost(initializeLicenseRenewPost(extensionId), errorMsg);
            if (licenseRenewResponse == null) {
                return;
            }

            if (licenseRenewResponse.get("status").textValue().equals("error")) {
                logger.warn("{} Cause: [{}]", errorMsg, licenseRenewResponse.get(DATA).textValue());
            } else {
                logger.debug(
                    "Successful response from store after license renew. Trying to update local licenses too.");

                String license = licenseRenewResponse.get("license").textValue();
                if (license != null) {
                    License retrivedLicense = converter.convert(License.class, base64decoder.decode(license));
                    if (retrivedLicense != null) {
                        licenseManagerProvider.get().add(retrivedLicense);
                        logger.debug("License renewed for [{}].", extensionId.getId());
                    }
                } else {
                    logger.debug("No license received in store response. Updating all licenses in case there might "
                        + "have been updates anyway. Cause: [{}]", licenseRenewResponse.get(DATA));
                    updateLicenses();
                }
            }
        } catch (Exception e) {
            logger.warn("{} Root cause is [{}]", errorMsg, ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void updateLicenses()
    {
        try {
            URL licensesUpdateURL = getLicensesUpdatesURL();
            if (licensesUpdateURL == null) {
                logger.warn("Failed to update licenses because the licensor configuration is not complete. "
                    + "Check your store update URL.");
                return;
            }

            XWikiContext xcontext = contextProvider.get();
            String licensesUpdateResponse = xcontext.getWiki().getURLContent(licensesUpdateURL.toString(), xcontext);
            ObjectMapper objectMapper = new ObjectMapper();

            List<String> retrievedLicenses =
                (List<String>) objectMapper.readValue(licensesUpdateResponse, Object.class);
            for (String license : retrievedLicenses) {
                License retrivedLicense = converter.convert(License.class, base64decoder.decode(license));
                if (retrivedLicense != null) {
                    licenseManagerProvider.get().add(retrivedLicense);
                }
            }
        } catch (URISyntaxException | IOException e) {
            logger.warn("Error while updating licenses. Root cause [{}]", ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private HttpPost initializeLicenseRenewPost(ExtensionId extensionId) throws Exception
    {
        URI licenseRenewURL = getLicenseRenewURL(extensionId);
        if (licenseRenewURL == null) {
            logger.warn("Failed to renew license for [{}] because the licensor configuration is not complete. "
                + "Check your store license renew URL and owner details.", extensionId.getId());
            return null;
        }

        HttpPost httpPost = new HttpPost(licenseRenewURL);
        List<NameValuePair> requestData =
            Arrays.asList(new BasicNameValuePair("firstName", licensingConfig.getLicensingOwnerFirstName()),
                new BasicNameValuePair("lastName", licensingConfig.getLicensingOwnerLastName()),
                new BasicNameValuePair("email", licensingConfig.getLicensingOwnerEmail()),
                new BasicNameValuePair(INSTANCE_ID, instanceIdManagerProvider.get().getInstanceId().toString()),
                new BasicNameValuePair(FEATURE_ID, extensionId.getId()),
                new BasicNameValuePair("extensionVersion", extensionId.getVersion().getValue()));
        httpPost.setEntity(new UrlEncodedFormEntity(requestData));
        return httpPost;
    }

    private URI getLicenseRenewURL(ExtensionId extensionId) throws Exception
    {
        String storeLicenseRenewURL = licensingConfig.getStoreRenewURL();
        // In case the property has no filled value, the URL cannot be constructed.
        if (storeLicenseRenewURL == null) {
            return null;
        }

        URIBuilder builder = new URIBuilder(storeLicenseRenewURL);
        builder.addParameter(OUTPUT_SYNTAX, PLAIN);

        return builder.build();
    }

    /**
     * Construct the URL for updating licenses.
     *
     * @return the URL for updating licenses, or null if it cannot be constructed
     * @throws URISyntaxException if the URL is not valid
     * @throws MalformedURLException if an error occurred while constructing the URL
     */
    private URL getLicensesUpdatesURL() throws URISyntaxException, MalformedURLException
    {
        String storeUpdateURL = licensingConfig.getStoreUpdateURL();
        // In case the property has no filled value, the URL cannot be constructed.
        if (storeUpdateURL == null) {
            return null;
        }

        URIBuilder builder = new URIBuilder(storeUpdateURL);
        builder.addParameter(INSTANCE_ID, instanceIdManagerProvider.get().getInstanceId().toString());
        builder.addParameter(OUTPUT_SYNTAX, PLAIN);

        for (ExtensionId paidExtensionId : licensedExtensionManager.getMandatoryLicensedExtensions()) {
            builder.addParameter(FEATURE_ID, paidExtensionId.getId());

            License license = licensorProvider.get().getLicense(paidExtensionId);
            if (license != null && !License.UNLICENSED.equals(license)) {
                builder.addParameter(String.format("expirationDate:%s", paidExtensionId.getId()),
                    Long.toString(license.getExpirationDate()));
            }
        }
        return builder.build().toURL();
    }
}
