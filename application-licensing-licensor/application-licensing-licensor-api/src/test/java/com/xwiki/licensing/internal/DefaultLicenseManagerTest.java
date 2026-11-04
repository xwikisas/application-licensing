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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xwiki.extension.ExtensionId;
import org.xwiki.instance.InstanceId;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xwiki.licensing.FileLicenseStoreReference;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseId;
import com.xwiki.licensing.LicenseManager;
import com.xwiki.licensing.LicenseStore;
import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.LicenseValidator;
import com.xwiki.licensing.LicensedExtensionManager;
import com.xwiki.licensing.LicensedFeatureId;
import com.xwiki.licensing.LicensingConfiguration;
import com.xwiki.licensing.internal.helpers.events.LicenseAddedEvent;
import com.xwiki.licensing.internal.test.LicenseValidatorWrapper;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultLicenseManager}.
 * 
 * @version $Id$
 */
public class DefaultLicenseManagerTest
{
    @Rule
    public MockitoComponentMockingRule<LicenseManager> mocker =
        new MockitoComponentMockingRule<>(DefaultLicenseManager.class);

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private List<License> licenses = new ArrayList<>();

    private License validLicense = new License();

    private ExtensionId testAppId = new ExtensionId("com.xwiki.test:application-test", "1.0");

    private LicenseValidator licenseValidator = mock(LicenseValidator.class);

    private ObservationManager observationManager;

    @Before
    public void configure() throws Exception
    {
        this.validLicense.setId(new LicenseId());
        this.validLicense.setType(LicenseType.PAID);
        this.validLicense.addInstanceId(new InstanceId("ec9adc8a-cb98-4d5e-803a-5746fc8330c5"));
        LicensedFeatureId licensedFeatureId = new LicensedFeatureId(testAppId.getId());
        this.validLicense.addFeatureId(licensedFeatureId);
        this.validLicense.addLicenseeInfo("firstName", "John");
        this.validLicense.addLicenseeInfo("lastName", "Doe");
        this.validLicense.addLicenseeInfo("email", "john@doe.com");
        this.licenses.add(this.validLicense);

        LicensingConfiguration configuration = this.mocker.getInstance(LicensingConfiguration.class);
        File licenseStorePath = this.tempFolder.newFolder("licenses");
        when(configuration.getLocalStorePath()).thenReturn(licenseStorePath);

        LicenseStore store = this.mocker.getInstance(LicenseStore.class, "FileSystem");
        FileLicenseStoreReference storeReference = new FileLicenseStoreReference(licenseStorePath, true);
        when(store.getIterable(storeReference)).thenReturn(this.licenses);

        this.mocker.registerComponent(LicenseValidator.class, new LicenseValidatorWrapper(this.licenseValidator));
        when(this.licenseValidator.isApplicable(this.validLicense)).thenReturn(true);
        when(this.licenseValidator.isSigned(this.validLicense)).thenReturn(true);
        when(this.licenseValidator.isValid(this.validLicense)).thenReturn(true);

        this.observationManager = this.mocker.registerMockComponent(ObservationManager.class);

        LicensedExtensionManager licensedExtensionManager = this.mocker.getInstance(LicensedExtensionManager.class);
        when(licensedExtensionManager.getLicensedExtensions()).thenReturn(Collections.singleton(testAppId));
        when(licensedExtensionManager.getLicensedExtensions(licensedFeatureId))
            .thenReturn(Collections.singleton(testAppId));
    }

    @Test
    public void init() throws Exception
    {
        LicenseManager licenseManager = this.mocker.getComponentUnderTest();

        // The extension is marked as Unlicensed until the License Manager initialization thread is done.
        assertSame(License.UNLICENSED, licenseManager.get(this.testAppId));
        assertTrue(CollectionUtils.isEqualCollection(Collections.singletonList(this.validLicense),
            licenseManager.getActiveLicenses()));

        // Wait for the License Manager initialization thread to execute.
        Thread.sleep(1000);

        assertSame(this.validLicense, licenseManager.get(this.testAppId));
        assertTrue(CollectionUtils.isEqualCollection(Collections.singletonList(this.validLicense),
            licenseManager.getUsedLicenses()));
    }

    @Test
    public void licenseAddedEventIsOnlySentForAddedLicenses() throws Exception
    {
        LicenseManager licenseManager = this.mocker.getComponentUnderTest();

        // Wait for the License Manager initialization thread to execute.
        Thread.sleep(1000);

        // Loading the stored licenses at startup is not an addition.
        verify(this.observationManager, never()).notify(any(), any(), any());

        License newLicense = new License();
        newLicense.setId(new LicenseId());
        newLicense.setType(LicenseType.PAID);
        newLicense.addFeatureId(new LicensedFeatureId("com.xwiki.test:other-application"));
        when(this.licenseValidator.isApplicable(newLicense)).thenReturn(true);
        when(this.licenseValidator.isSigned(newLicense)).thenReturn(true);

        assertTrue(licenseManager.add(newLicense));

        verify(this.observationManager).notify(
            argThat(
                event -> event instanceof LicenseAddedEvent && ((LicenseAddedEvent) event).getLicense() == newLicense),
            isNull(), isNull());
    }
}
