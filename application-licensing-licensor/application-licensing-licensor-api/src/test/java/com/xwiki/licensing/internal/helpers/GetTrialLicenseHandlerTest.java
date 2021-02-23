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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.extension.ExtensionId;
import org.xwiki.instance.InstanceId;
import org.xwiki.instance.InstanceIdManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicensedExtensionManager;
import com.xwiki.licensing.LicensingConfiguration;
import com.xwiki.licensing.Licensor;
import com.xwiki.licensing.internal.UserCounter;

import io.netty.handler.codec.http.QueryStringDecoder;

/**
 * Unit tests for {@link GetTrialLicenseHandler}.
 * 
 * @version $Id$
 */
public class GetTrialLicenseHandlerTest
{
    @Rule
    public MockitoComponentMockingRule<GetTrialLicenseHandler> mocker =
        new MockitoComponentMockingRule<>(GetTrialLicenseHandler.class);

    InstanceId instanceId;

    LicensedExtensionManager licensedExtensionManager;

    LicensingConfiguration licensingConfig;

    Licensor licensor;

    ExtensionId extension1 = new ExtensionId("application-test1", "1.0");

    ExtensionId extension2 = new ExtensionId("application-test2", "2.0");

    @Before
    public void configure() throws Exception
    {
        licensingConfig = this.mocker.getInstance(LicensingConfiguration.class);
        when(licensingConfig.getStoreTrialURL()).thenReturn("https://storeTrial.com");
        when(licensingConfig.getStoreUpdateURL()).thenReturn("https://storeUpdate.com");
        when(licensingConfig.getLicensingOwnerFirstName()).thenReturn("Doe");
        when(licensingConfig.getLicensingOwnerLastName()).thenReturn("John");
        when(licensingConfig.getLicensingOwnerEmail()).thenReturn("test@mail.com");

        InstanceIdManager instanceIdManager = mock(InstanceIdManager.class);
        DefaultParameterizedType instanceIdManagerType =
            new DefaultParameterizedType(null, Provider.class, InstanceIdManager.class);
        Provider<InstanceIdManager> instanceIdManagerProvider =
            this.mocker.registerMockComponent(instanceIdManagerType);
        when(instanceIdManagerProvider.get()).thenReturn(instanceIdManager);

        this.instanceId = new InstanceId(UUID.randomUUID().toString());
        when(instanceIdManager.getInstanceId()).thenReturn(instanceId);

        UserCounter userCounter = this.mocker.getInstance(UserCounter.class);
        when(userCounter.getUserCount()).thenReturn(Long.valueOf(5));

        this.licensedExtensionManager = this.mocker.getInstance(LicensedExtensionManager.class);

        DefaultParameterizedType licensorType = new DefaultParameterizedType(null, Provider.class, Licensor.class);
        Provider<Licensor> licensorProvider = this.mocker.registerMockComponent(licensorType);
        licensor = this.mocker.getInstance(Licensor.class);
        when(licensorProvider.get()).thenReturn(licensor);

    }

    @Test
    public void getTrialUrl() throws Exception
    {
        URL trialUrl = this.mocker.getComponentUnderTest().getTrialURL(this.extension1);
        QueryStringDecoder decoder = new QueryStringDecoder(trialUrl.toString());
        Map<String, List<String>> parameters = decoder.parameters();

        assertEquals("Doe", parameters.get("firstName").get(0));
        assertEquals("John", parameters.get("lastName").get(0));
        assertEquals("test@mail.com", parameters.get("email").get(0));
        assertEquals(this.instanceId.toString(), parameters.get("instanceId").get(0));
        assertEquals(this.extension1.getId(), parameters.get("featureId").get(0));
        assertEquals(this.extension1.getVersion().getValue(), parameters.get("extensionVersion").get(0));
        assertEquals("TRIAL", parameters.get("licenseType").get(0));
        assertEquals("5", parameters.get("userCount").get(0));
    }

    @Test
    public void getLicensesUpdateUrl() throws Exception
    {
        when(this.licensedExtensionManager.getVisibleLicensedExtensions()).thenReturn(Arrays.asList(this.extension1));

        License license = mock(License.class);
        Long expirationDate = Calendar.getInstance().getTimeInMillis();
        when(license.getExpirationDate()).thenReturn(expirationDate);
        when(licensor.getLicense(this.extension1)).thenReturn(license);

        URL updateUrl = this.mocker.getComponentUnderTest().getLicensesUpdateURL();
        QueryStringDecoder decoder = new QueryStringDecoder(updateUrl.toString());
        Map<String, List<String>> parameters = decoder.parameters();

        assertEquals(this.instanceId.toString(), parameters.get("instanceId").get(0));
        assertEquals("plain", parameters.get("outputSyntax").get(0));
        assertEquals(this.extension1.getId(), parameters.get("featureId").get(0));
        assertEquals(String.valueOf(expirationDate), parameters.get("expirationDate:application-test1").get(0));
    }

    @Test
    public void isLicensedExtensionWithLicensedExtension() throws Exception
    {
        when(this.licensedExtensionManager.getVisibleLicensedExtensions()).thenReturn(Arrays.asList(this.extension1));

        assertTrue(this.mocker.getComponentUnderTest().isLicensedExtension(extension1));
    }

    @Test
    public void isLicensedExtensionWithoutLicensedExtension() throws Exception
    {
        when(this.licensedExtensionManager.getVisibleLicensedExtensions()).thenReturn(Arrays.asList(this.extension1));

        assertFalse(this.mocker.getComponentUnderTest().isLicensedExtension(extension2));
    }

    @Test
    public void isOwnerDataComplete() throws Exception
    {
        assertTrue(this.mocker.getComponentUnderTest().isOwnerDataComplete());
    }

    @Test
    public void isOwnerDataCompleteWithIncompleteData() throws Exception
    {
        when(licensingConfig.getLicensingOwnerLastName()).thenReturn("");

        assertFalse(this.mocker.getComponentUnderTest().isOwnerDataComplete());
    }
}
