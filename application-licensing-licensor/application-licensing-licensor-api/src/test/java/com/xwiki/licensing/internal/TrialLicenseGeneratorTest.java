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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.extension.ExtensionId;
import org.xwiki.instance.InstanceId;
import org.xwiki.instance.InstanceIdManager;
import org.xwiki.properties.converter.Converter;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseManager;
import com.xwiki.licensing.LicensedExtensionManager;
import com.xwiki.licensing.LicensingConfiguration;
import com.xwiki.licensing.Licensor;

/**
 * Unit tests for {@link GetTrialLicenseHandler}.
 * 
 * @version $Id$
 */
public class TrialLicenseGeneratorTest
{
    @Rule
    public MockitoComponentMockingRule<TrialLicenseGenerator> mocker =
        new MockitoComponentMockingRule<>(TrialLicenseGenerator.class);

    InstanceId instanceId;

    LicensedExtensionManager licensedExtensionManager;

    LicenseManager licenseManager;

    XWikiContext xcontext;

    XWiki xwiki;

    LicensingConfiguration licensingConfig;

    Licensor licensor;

    Converter<License> converter;

    BinaryStringEncoder decoder;

    ExtensionId extension1 = new ExtensionId("application-test1", "1.0");

    ExtensionId extension2 = new ExtensionId("application-test2", "2.0");

    @Before
    public void configure() throws Exception
    {
        this.licensingConfig = this.mocker.getInstance(LicensingConfiguration.class);
        when(this.licensingConfig.getLicensingOwnerFirstName()).thenReturn("Doe");
        when(this.licensingConfig.getLicensingOwnerLastName()).thenReturn("John");
        when(this.licensingConfig.getLicensingOwnerEmail()).thenReturn("test@mail.com");

        this.licensedExtensionManager = this.mocker.getInstance(LicensedExtensionManager.class);
        when(this.licensedExtensionManager.getMandatoryLicensedExtensions()).thenReturn(Arrays.asList(this.extension1));

        this.instanceId = new InstanceId("7237b65d-e5d6-4249-aa4f-7c732cba27e2");
        InstanceIdManager instanceIdManager = mock(InstanceIdManager.class);
        DefaultParameterizedType instanceIdManagerType =
            new DefaultParameterizedType(null, Provider.class, InstanceIdManager.class);
        Provider<InstanceIdManager> instanceIdManagerProvider =
            this.mocker.registerMockComponent(instanceIdManagerType);
        when(instanceIdManagerProvider.get()).thenReturn(instanceIdManager);
        when(instanceIdManager.getInstanceId()).thenReturn(instanceId);

        UserCounter userCounter = this.mocker.getInstance(UserCounter.class);
        when(userCounter.getUserCount()).thenReturn(Long.valueOf(5));

        this.licensor = mock(Licensor.class);
        DefaultParameterizedType licensorType = new DefaultParameterizedType(null, Provider.class, Licensor.class);
        Provider<Licensor> licensorProvider = this.mocker.registerMockComponent(licensorType);
        when(licensorProvider.get()).thenReturn(this.licensor);

        this.xcontext = mock(XWikiContext.class);
        this.xwiki = mock(XWiki.class);
        Provider<XWikiContext> xcontextProvider = this.mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);

        this.licenseManager = mock(LicenseManager.class);
        DefaultParameterizedType licenseManagerType =
            new DefaultParameterizedType(null, Provider.class, LicenseManager.class);
        Provider<LicenseManager> licenseManagerProvider = this.mocker.registerMockComponent(licenseManagerType);
        when(licenseManagerProvider.get()).thenReturn(this.licenseManager);

        DefaultParameterizedType licenseConverterType =
            new DefaultParameterizedType(null, Converter.class, License.class);
        this.converter = this.mocker.getInstance(licenseConverterType);
        this.decoder = this.mocker.getInstance(BinaryStringEncoder.class, "Base64");
    }

    @Test
    public void generateTrialLicense() throws Exception
    {
        when(this.licensingConfig.getStoreTrialURL()).thenReturn("https://storeTrial.com");
        when(this.licensingConfig.getStoreUpdateURL()).thenReturn("https://storeUpdate.com");

        String trialUrl =
            "https://storeTrial.com?firstName=Doe&lastName=John&email=test%40mail.com&instanceId=7237b65d-e5d6-4249-aa4f-7c732cba27e2&featureId=application-test1&extensionVersion=1.0&licenseType=TRIAL&userCount=5";
        when(this.xwiki.getURLContent(trialUrl, this.xcontext)).thenReturn("success");

        License license = mock(License.class, "oldLicense");
        when(this.licensor.getLicense(this.extension1)).thenReturn(license);
        when(license.getExpirationDate()).thenReturn(Long.valueOf("12"));

        String updateUrl =
            "https://storeUpdate.com?instanceId=7237b65d-e5d6-4249-aa4f-7c732cba27e2&outputSyntax=plain&featureId=application-test1&expirationDate%3Aapplication-test1=12";
        String licenseResponse = "[\"trialLicense\"]";
        when(this.xwiki.getURLContent(updateUrl, this.xcontext)).thenReturn(licenseResponse);

        License retrivedLicense = mock(License.class, "newLicense");
        byte[] trialLicenseBytes = "trialLicense".getBytes();
        when(this.decoder.decode("trialLicense")).thenReturn(trialLicenseBytes);
        when(this.converter.convert(License.class, trialLicenseBytes)).thenReturn(retrivedLicense);

        this.mocker.getComponentUnderTest().generateTrialLicense(this.extension1);

        verify(this.licenseManager, times(1)).add(retrivedLicense);
    }

    @Test
    public void generateTrialLicenseWithNullTrialURL() throws Exception
    {
        when(this.licensingConfig.getStoreTrialURL()).thenReturn(null);

        this.mocker.getComponentUnderTest().generateTrialLicense(this.extension1);

        verify(this.mocker.getMockedLogger(), times(1)).info("Failed to add trial license");
    }

    @Test
    public void generateTrialLicenseWithGetTrialError() throws Exception
    {
        when(this.licensingConfig.getStoreTrialURL()).thenReturn("https://storeTrial.com");

        String trialUrl =
            "https://storeTrial.com?firstName=Doe&lastName=John&email=test%40mail.com&instanceId=7237b65d-e5d6-4249-aa4f-7c732cba27e2&featureId=application-test1&extensionVersion=1.0&licenseType=TRIAL&userCount=5";
        when(this.xwiki.getURLContent(trialUrl, this.xcontext)).thenReturn("error");

        this.mocker.getComponentUnderTest().generateTrialLicense(this.extension1);

        verify(this.mocker.getMockedLogger(), times(1)).info("Failed to add trial license");
    }

    @Test
    public void generateTrialLicenseWithNullUpdateURL() throws Exception
    {
        when(this.licensingConfig.getStoreTrialURL()).thenReturn("https://storeTrial.com");

        String trialUrl =
            "https://storeTrial.com?firstName=Doe&lastName=John&email=test%40mail.com&instanceId=7237b65d-e5d6-4249-aa4f-7c732cba27e2&featureId=application-test1&extensionVersion=1.0&licenseType=TRIAL&userCount=5";
        when(this.xwiki.getURLContent(trialUrl, this.xcontext)).thenReturn("success");

        when(this.licensingConfig.getStoreUpdateURL()).thenReturn(null);

        this.mocker.getComponentUnderTest().generateTrialLicense(this.extension1);

        verify(this.mocker.getMockedLogger(), times(1)).info("Added trial license");
        verify(this.mocker.getMockedLogger(), times(1)).warn("Failed to update licenses");
    }

    @Test
    public void canGenerateTrialLicenseWithExtensionAndCompleteData() throws Exception
    {
        this.mocker.getComponentUnderTest().canGenerateTrialLicense(this.extension1);

        assertTrue(this.mocker.getComponentUnderTest().canGenerateTrialLicense(this.extension1));
    }

    @Test
    public void canGenerateTrialLicenseWithoutCompleteData() throws Exception
    {
        when(this.licensingConfig.getLicensingOwnerEmail()).thenReturn(null);

        this.mocker.getComponentUnderTest().canGenerateTrialLicense(this.extension1);

        assertFalse(this.mocker.getComponentUnderTest().canGenerateTrialLicense(extension1));
    }

    @Test
    public void canGenerateTrialLicenseWithoutLicensedExtension() throws Exception
    {
        this.mocker.getComponentUnderTest().canGenerateTrialLicense(this.extension1);

        assertFalse(this.mocker.getComponentUnderTest().canGenerateTrialLicense(extension2));
    }
}
