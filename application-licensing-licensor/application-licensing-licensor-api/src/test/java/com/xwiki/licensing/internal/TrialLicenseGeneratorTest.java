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

import java.util.Collections;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.extension.ExtensionId;
import org.xwiki.instance.InstanceId;
import org.xwiki.instance.InstanceIdManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicensedExtensionManager;
import com.xwiki.licensing.LicensingConfiguration;
import com.xwiki.licensing.Licensor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TrialLicenseGenerator}.
 *
 * @version $Id$
 */
@ComponentTest
public class TrialLicenseGeneratorTest
{
    @InjectMockComponents
    TrialLicenseGenerator trialLicenseGenerator;

    @Mock
    InstanceIdManager instanceIdManager;

    @Mock
    XWikiContext xcontext;

    @Mock
    XWiki xwiki;

    @Mock
    Licensor licensor;

    @Mock
    License license;

    InstanceId instanceId;

    ExtensionId extension1 = new ExtensionId("application-test1", "1.0");

    ExtensionId extension2 = new ExtensionId("application-test2", "2.0");

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @MockComponent
    private Provider<InstanceIdManager> instanceIdManagerProvider;

    @MockComponent
    private UserCounter userCounter;

    @MockComponent
    private LicensedExtensionManager licensedExtensionManager;

    @MockComponent
    private LicensingConfiguration licensingConfig;

    @MockComponent
    private Provider<Licensor> licensorProvider;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private DefaultLicenseUpdater licenseUpdater;

    @BeforeEach
    public void configure() throws Exception
    {
        when(this.licensingConfig.getLicensingOwnerFirstName()).thenReturn("Doe");
        when(this.licensingConfig.getLicensingOwnerLastName()).thenReturn("John");
        when(this.licensingConfig.getLicensingOwnerEmail()).thenReturn("test@mail.com");

        when(this.licensedExtensionManager.getMandatoryLicensedExtensions()).thenReturn(
            Collections.singletonList(this.extension1));

        this.instanceId = new InstanceId("7237b65d-e5d6-4249-aa4f-7c732cba27e2");
        when(instanceIdManagerProvider.get()).thenReturn(instanceIdManager);
        when(instanceIdManager.getInstanceId()).thenReturn(instanceId);

        when(userCounter.getUserCount()).thenReturn(Long.valueOf(5));

        when(licensorProvider.get()).thenReturn(this.licensor);

        when(contextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
    }

    @Test
    public void generateTrialLicense() throws Exception
    {
        when(this.licensingConfig.getStoreTrialURL()).thenReturn("https://storeTrial.com");

        String trialUrl = "https://storeTrial.com?firstName=Doe&lastName=John&email=test%40mail"
            + ".com&instanceId=7237b65d-e5d6-4249-aa4f-7c732cba27e2&featureId=application-test1&extensionVersion"
            + "=1.0&licenseType=TRIAL&userCount=5";
        when(this.xwiki.getURLContent(trialUrl, this.xcontext)).thenReturn("success");

        License license = mock(License.class, "oldLicense");
        when(this.licensor.getLicense(this.extension1)).thenReturn(license);
        when(license.getExpirationDate()).thenReturn(Long.valueOf("12"));

        trialLicenseGenerator.generateTrialLicense(this.extension1);

        verify(this.licenseUpdater, times(1)).getLicensesUpdates();
    }

    @Test
    public void generateTrialLicenseWithNullTrialURL() throws Exception
    {
        when(this.licensingConfig.getStoreTrialURL()).thenReturn(null);

        trialLicenseGenerator.generateTrialLicense(this.extension1);

        assertEquals(String.format("Failed to add trial license for [%s] because the licensor configuration is not "
                + "complete. Check your store trial URL and owner details.", this.extension1.getId()),
            logCapture.getMessage(0));
    }

    @Test
    public void generateTrialLicenseWithGetTrialError() throws Exception
    {
        when(this.licensingConfig.getStoreTrialURL()).thenReturn("https://storeTrial.com");

        String trialUrl = "https://storeTrial.com?firstName=Doe&lastName=John&email=test%40mail"
            + ".com&instanceId=7237b65d-e5d6-4249-aa4f-7c732cba27e2&featureId=application-test1&extensionVersion"
            + "=1.0&licenseType=TRIAL&userCount=5";
        when(this.xwiki.getURLContent(trialUrl, this.xcontext)).thenReturn("error");

        trialLicenseGenerator.generateTrialLicense(this.extension1);

        assertEquals(String.format("Failed to generate trial license for [%s] on store.", this.extension1.getId()),
            logCapture.getMessage(0));
    }

    @Test
    public void canGenerateTrialLicense() throws Exception
    {
        when(this.licensor.getLicense(this.extension1)).thenReturn(License.UNLICENSED);

        assertTrue(trialLicenseGenerator.canGenerateTrialLicense(this.extension1));
    }

    @Test
    public void canGenerateTrialLicenseWithoutCompleteData() throws Exception
    {
        when(this.licensor.getLicense(this.extension1)).thenReturn(null);

        when(this.licensingConfig.getLicensingOwnerEmail()).thenReturn(null);

        assertFalse(trialLicenseGenerator.canGenerateTrialLicense(extension1));
    }

    @Test
    public void canGenerateTrialLicenseWithoutLicensedExtension() throws Exception
    {
        when(this.licensor.getLicense(this.extension2)).thenReturn(null);

        assertFalse(trialLicenseGenerator.canGenerateTrialLicense(extension2));
    }

    @Test
    public void canGenerateTrialLicenseWithExistingLicense() throws Exception
    {
        when(this.licensor.getLicense(this.extension1)).thenReturn(license);

        assertFalse(trialLicenseGenerator.canGenerateTrialLicense(this.extension1));
    }
}
