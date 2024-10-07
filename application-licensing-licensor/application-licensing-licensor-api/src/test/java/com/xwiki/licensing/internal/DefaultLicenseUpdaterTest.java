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
import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.instance.InstanceId;
import org.xwiki.instance.InstanceIdManager;
import org.xwiki.properties.converter.Converter;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseManager;
import com.xwiki.licensing.LicensedExtensionManager;
import com.xwiki.licensing.LicensingConfiguration;
import com.xwiki.licensing.Licensor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
public class DefaultLicenseUpdaterTest
{
    @RegisterExtension
    private LogCaptureExtension logCaptureWarn = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private DefaultLicenseUpdater licenseUpdater;

    @MockComponent
    private LicensedExtensionManager licensedExtensionManager;

    @MockComponent
    private InstalledExtensionRepository installedExtensionRepository;

    @MockComponent
    private LicensingConfiguration licensingConfig;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private Provider<InstanceIdManager> instanceIdManagerProvider;

    @MockComponent
    private Provider<Licensor> licensorProvider;

    @MockComponent
    private Provider<LicenseManager> licenseManagerProvider;

    @MockComponent
    @Named("Base64")
    private BinaryStringEncoder base64decoder;

    @MockComponent
    private Converter<License> converter;

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private LicenseManager licenseManager;

    @Mock
    private InstanceIdManager instanceIdManager;

    @Mock
    private Licensor licensor;

    @Mock
    private License license;

    @BeforeEach
    void configure()
    {
        when(contextProvider.get()).thenReturn(xcontext);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(licenseManagerProvider.get()).thenReturn(licenseManager);
        when(instanceIdManagerProvider.get()).thenReturn(instanceIdManager);
        when(instanceIdManager.getInstanceId()).thenReturn(new InstanceId("7237b65d-e5d6-4249-aa4f-7c732cba27e2"));
        when(licensorProvider.get()).thenReturn(licensor);
    }

    @Test
    void renewLicenseWithNullURL() throws Exception
    {
        when(licensingConfig.getStoreLicenseRenewURL()).thenReturn(null);

        ExtensionId extensionId = new ExtensionId("application-test", "1.0");
        licenseUpdater.renewLicense(extensionId);

        assertEquals(String.format(
                "Failed to renew license for [%s] because the licensor configuration is not complete."
                    + " Check your store license renew URL and owner details.", extensionId.getId()),
            logCaptureWarn.getMessage(0));
    }

    @Test
    void renewLicenseWithSuccess() throws Exception
    {
        when(licensingConfig.getStoreLicenseRenewURL()).thenReturn("https://storeRenew.com");
        when(licensingConfig.getLicensingOwnerFirstName()).thenReturn("John");
        when(licensingConfig.getLicensingOwnerLastName()).thenReturn("Doe");
        String renewURL =
            "https://storeRenew.com?firstName=John&lastName=Doe&email&instanceId=7237b65d-e5d6-4249-aa4f-7c732cba27e2"
                + "&featureId=application-test&extensionVersion=1.0&licenseType=TRIAL";
        String getLicenseRenewResponse = "success";
        when(xwiki.getURLContent(renewURL, xcontext)).thenReturn(getLicenseRenewResponse);

        ExtensionId extensionId = new ExtensionId("application-test", "1.0");
        licenseUpdater.renewLicense(extensionId);

        // After renewing the license on store, right now we retrieve new updates from store. Maybe we should simply
        // return the updated license from the start, to not make a new request?
    }

    @Test
    void renewLicenseWithError() throws Exception
    {
        when(licensingConfig.getStoreLicenseRenewURL()).thenReturn("https://storeRenew.com");
        when(licensingConfig.getLicensingOwnerFirstName()).thenReturn("John");
        when(licensingConfig.getLicensingOwnerLastName()).thenReturn("Doe");
        String renewURL =
            "https://storeRenew.com?firstName=John&lastName=Doe&email&instanceId=7237b65d-e5d6-4249-aa4f-7c732cba27e2"
                + "&featureId=application-test&extensionVersion=1.0&licenseType=TRIAL";
        String getLicenseRenewResponse = "error";
        when(xwiki.getURLContent(renewURL, xcontext)).thenReturn(getLicenseRenewResponse);

        ExtensionId extensionId = new ExtensionId("application-test", "1.0");
        licenseUpdater.renewLicense(extensionId);

        assertEquals(String.format("Failed to renew license for [%s] on store.", extensionId.getId()),
            logCaptureWarn.getMessage(0));
        // After renewing the license on store, right now we retrieve new updates from store. Maybe we should simply
        // return the updated license from the start, to not make a new request?
    }

    @Test
    void getLicensesUpdatesWithNullURL() throws Exception
    {
        when(licensingConfig.getStoreUpdateURL()).thenReturn(null);

        licenseUpdater.getLicensesUpdates();

        assertEquals("Failed to update licenses because the licensor configuration is not complete. "
            + "Check your store update URL.", logCaptureWarn.getMessage(0));
    }

    @Test
    void getLicensesUpdatesWithUpdates() throws Exception
    {
        when(licensingConfig.getStoreUpdateURL()).thenReturn("https://storeUpdate.com");

        String updateURL =
            "https://storeUpdate.com?instanceId=7237b65d-e5d6-4249-aa4f-7c732cba27e2&outputSyntax=plain&featureId"
                + "=application-test&expirationDate%3Aapplication-test=12";
        String licenseResponse = "[\"license\"]";
        when(xwiki.getURLContent(updateURL, xcontext)).thenReturn(licenseResponse);

        ExtensionId licensedExtension = new ExtensionId("application-test", "1.0");
        List<ExtensionId> mandatoryExtensions = Collections.singletonList(licensedExtension);
        when(licensedExtensionManager.getMandatoryLicensedExtensions()).thenReturn(mandatoryExtensions);
        when(licensor.getLicense(licensedExtension)).thenReturn(license);
        when(license.getExpirationDate()).thenReturn(Long.valueOf("12"));

        byte[] licenseBytes = "license".getBytes();
        when(base64decoder.decode("license")).thenReturn(licenseBytes);
        License convertedLicense = new License();
        when(converter.convert(License.class, licenseBytes)).thenReturn(convertedLicense);

        licenseUpdater.getLicensesUpdates();

        verify(licenseManager).add(convertedLicense);
    }

    @Test
    void getLicensesUpdatesWithoutUpdates() throws IOException
    {
        when(licensingConfig.getStoreUpdateURL()).thenReturn("https://storeUpdate.com");

        String updateURL =
            "https://storeUpdate.com?instanceId=7237b65d-e5d6-4249-aa4f-7c732cba27e2&outputSyntax=plain&featureId"
                + "=application-test&expirationDate%3Aapplication-test=12";
        String licenseResponse = "[]";
        when(xwiki.getURLContent(updateURL, xcontext)).thenReturn(licenseResponse);

        ExtensionId licensedExtension = new ExtensionId("application-test", "1.0");
        List<ExtensionId> mandatoryExtensions = Collections.singletonList(licensedExtension);
        when(licensedExtensionManager.getMandatoryLicensedExtensions()).thenReturn(mandatoryExtensions);
        when(licensor.getLicense(licensedExtension)).thenReturn(license);
        when(license.getExpirationDate()).thenReturn(Long.valueOf("12"));

        licenseUpdater.getLicensesUpdates();

        verify(licenseManager, times(0)).add(any(License.class));
    }
}
