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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.repository.internal.installed.DefaultInstalledExtension;
import org.xwiki.instance.InstanceIdManager;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.properties.converter.Converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.TextFormat.ParseException;
import com.xpn.xwiki.XWikiException;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseManager;
import com.xwiki.licensing.LicensedExtensionManager;
import com.xwiki.licensing.LicensingConfiguration;
import com.xwiki.licensing.Licensor;

/**
 * Todo.
 * 
 * @version $Id$
 */
@Component
@Named(GenerateTrialListener.NAME)
@Singleton
public class GenerateTrialListener implements EventListener
{
    protected static final String NAME = "GenerateTrialListener";

    protected static final List<Event> EVENTS = Arrays.asList(new ExtensionInstalledEvent());

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
    private Licensor licensor;

    @Inject
    private LicenseManager licenseManager;

    @Inject
    @Named("Base64")
    private BinaryStringEncoder base64decoder;

    @Inject
    private Converter<License> converter;

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
        DefaultInstalledExtension extension = (DefaultInstalledExtension) source;

        try {
            if (!isOwnerDataIncomplete() && isLicensedExtension(extension.getId())) {
                String getTrialResponse = getURLContent(createTrialURL(extension));

                if (getTrialResponse.contains("error")) {
                    logger.warn("Failed to add trial at install step");
                } else {
                    logger.info("Added trial for extension at install step");
                    updateLicenses();
                }
            }
        } catch (XWikiException | URISyntaxException | IOException e) {
            logger.warn("Error 1", e);
        } catch (Exception e) {
            logger.warn("Failed 2", e);
        }
    }

    /**
     * @param extension
     * @return
     * @throws URISyntaxException
     * @throws MalformedURLException
     * @throws Exception
     */
    private URL createTrialURL(DefaultInstalledExtension extension)
        throws URISyntaxException, MalformedURLException, Exception
    {
        URIBuilder builder = new URIBuilder(licensingConfig.getStoreTrialURL());

        builder.addParameter("firstName", licensingConfig.getOwnerFirstName());
        builder.addParameter("lastName", licensingConfig.getOwnerLastName());
        builder.addParameter("email", licensingConfig.getOwnerEmail());
        builder.addParameter("instanceId", instanceIdManagerProvider.get().getInstanceId().toString());
        builder.addParameter("featureId", extension.getId().getId());
        builder.addParameter("extensionVersion", extension.getId().getVersion().toString());
        builder.addParameter("licenseType", "TRIAL");
        builder.addParameter("userCount", String.valueOf(userCounter.getUserCount()));

        return builder.build().toURL();
    }

    /**
     * @throws URISyntaxException
     * @throws IOException
     * @throws ParseException
     */
    @SuppressWarnings("unchecked")
    private void updateLicenses() throws URISyntaxException, IOException, ParseException
    {
        URIBuilder builder = new URIBuilder(licensingConfig.getStoreUpdateURL());
        builder.addParameter("instanceId", instanceIdManagerProvider.get().getInstanceId().toString());
        builder.addParameter("outputSyntax", "plain");

        for (ExtensionId paidExtensionId : licensedExtensionManager.getVisibleLicensedExtensions()) {
            builder.addParameter("featureId", paidExtensionId.getId());

            License license = licensor.getLicense(paidExtensionId);
            if (license != null) {
                builder.addParameter(String.format("expirationDate:%s", paidExtensionId.getId()),
                    Long.toString(license.getExpirationDate()));
            }
        }
        URL updateURL = builder.build().toURL();

        ObjectMapper objectMapper = new ObjectMapper();

        String content = getURLContent(updateURL);
        List<String> licenses = (List<String>) objectMapper.readValue(content, Object.class);

        for (String license : licenses) {
            licenseManager.add(converter.convert(License.class, base64decoder.decode(license)));
        }
    }

    /**
     * Returns the content of an URL.
     * 
     * @param trialURL URL to retrieve
     * @return content of the specified URL
     * @throws IOException is an I/O exception occurs
     */
    private String getURLContent(URL trialURL) throws IOException
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
     * @param extension the extension to be checked
     * @return true is the extension is licensed, false otherwise
     */
    private Boolean isLicensedExtension(ExtensionId extensionId)
    {
        return licensedExtensionManager.getVisibleLicensedExtensions().stream()
            .filter(o -> o.getId().contentEquals(extensionId.getId())).findFirst().isPresent();
    }

    /**
     * @return todo
     */
    public Boolean isOwnerDataIncomplete()
    {
        return licensingConfig.getOwnerLastName().isEmpty() || licensingConfig.getOwnerFirstName().isEmpty()
            || licensingConfig.getOwnerEmail().isEmpty();
    }

}
