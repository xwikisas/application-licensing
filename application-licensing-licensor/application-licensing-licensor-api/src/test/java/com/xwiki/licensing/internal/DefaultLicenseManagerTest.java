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
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.instance.InstanceId;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xwiki.licensing.FileLicenseStoreReference;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseId;
import com.xwiki.licensing.LicenseManager;
import com.xwiki.licensing.LicenseStore;
import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.LicenseValidator;
import com.xwiki.licensing.LicensedFeatureId;
import com.xwiki.licensing.LicensingConfiguration;
import com.xwiki.licensing.internal.test.LicenseValidatorWrapper;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
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

    @Before
    public void configure() throws Exception
    {
        InstalledExtension testApp = mock(InstalledExtension.class);
        when(testApp.getId()).thenReturn(this.testAppId);

        List<InstalledExtension> installedExtensions = new ArrayList<>();
        installedExtensions.add(testApp);

        this.validLicense.setId(new LicenseId());
        this.validLicense.setType(LicenseType.PAID);
        this.validLicense.addInstanceId(new InstanceId("ec9adc8a-cb98-4d5e-803a-5746fc8330c5"));
        this.validLicense.addFeatureId(new LicensedFeatureId(testAppId.getId()));
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

        LicenseValidator licenseValidator = mock(LicenseValidator.class);
        this.mocker.registerComponent(LicenseValidator.class, new LicenseValidatorWrapper(licenseValidator));
        when(licenseValidator.isApplicable(this.validLicense)).thenReturn(true);
        when(licenseValidator.isSigned(this.validLicense)).thenReturn(true);
        when(licenseValidator.isValid(this.validLicense)).thenReturn(true);

        InstalledExtensionRepository installedExtensionRepository =
            this.mocker.getInstance(InstalledExtensionRepository.class);
        when(installedExtensionRepository.getInstalledExtensions()).thenReturn(installedExtensions);
    }

    @Test
    public void initBeforeLicensorIsMarkedAsInstalled() throws Exception
    {
        LicenseManager licenseManager = this.mocker.getComponentUnderTest();
        assertSame(this.validLicense, licenseManager.get(this.testAppId));
        assertTrue(CollectionUtils.isEqualCollection(Collections.singletonList(this.validLicense),
            licenseManager.getActiveLicenses()));
        assertTrue(CollectionUtils.isEqualCollection(Collections.singletonList(this.validLicense),
            licenseManager.getUsedLicenses()));

        verify(this.mocker.getMockedLogger()).warn(
            "The Licensor API extension ({}) is not installed on the root namespace as it should."
                + " Licensed extensions won't be detected correctly as a conseuence.",
            "com.xwiki.licensing:application-licensing-licensor-api");
    }
}
