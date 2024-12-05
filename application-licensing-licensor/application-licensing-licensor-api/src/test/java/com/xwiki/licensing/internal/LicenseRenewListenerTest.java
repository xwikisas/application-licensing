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
import java.util.HashSet;
import java.util.Set;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseUpdater;
import com.xwiki.licensing.LicensedExtensionManager;
import com.xwiki.licensing.LicensedFeatureId;
import com.xwiki.licensing.Licensor;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
public class LicenseRenewListenerTest
{
    @InjectMockComponents
    private LicenseRenewListener renewListener;

    @MockComponent
    private LicensedExtensionManager licensedExtensionManager;

    @MockComponent
    private InstalledExtensionRepository installedExtensionRepository;

    @MockComponent
    private Provider<Licensor> licensorProvider;

    @MockComponent
    private LicenseUpdater licenseUpdater;

    @Mock
    private InstalledExtension installedExtension;

    @Mock
    private InstalledExtension prevInstalledExtension;

    @Mock
    private Licensor licensor;

    @Mock
    private License license;

    @BeforeEach
    void config() throws Exception
    {
        when(licensorProvider.get()).thenReturn(licensor);
    }

    @Test
    void onEventWithExtensionInstalledEvent() throws Exception
    {
        ExtensionId extensionId = new ExtensionId("application-test", "1.0");
        ExtensionInstalledEvent event = new ExtensionInstalledEvent(extensionId, null);
        when(licensedExtensionManager.getLicensedExtensions()).thenReturn(Collections.singletonList(extensionId));
        when(licensedExtensionManager.getMandatoryLicensedExtensions()).thenReturn(Collections.singletonList(extensionId));

        when(installedExtension.getId()).thenReturn(extensionId);
        when(licensor.getLicense(extensionId)).thenReturn(license);

        renewListener.onEvent(event, installedExtension, null);

        verify(licenseUpdater).updateLicenses();
        verify(licenseUpdater).renewLicense(extensionId);
    }

    @Test
    void onEventWithExtensionUpgradedEventAndChangesOnVersions() throws Exception
    {
        ExtensionId extensionId = new ExtensionId("application-test", "1.2");
        ExtensionUpgradedEvent event = new ExtensionUpgradedEvent(extensionId, null);
        when(licensedExtensionManager.getLicensedExtensions()).thenReturn(Collections.singletonList(extensionId));
        // This extension should be considered mandatory no matter the specified version.
        ExtensionId extensionIdV2 = new ExtensionId("application-test", "2.0");
        when(licensedExtensionManager.getMandatoryLicensedExtensions()).thenReturn(Collections.singletonList(extensionIdV2));
        when(licensor.getLicense(extensionId)).thenReturn(license);

        when(installedExtension.getId()).thenReturn(extensionId);
        Set<ExtensionId> dependencies = new HashSet<>();
        dependencies.add(new ExtensionId("application-dep2", "2.0"));
        when(licensedExtensionManager.getLicensedDependencies(installedExtension, null)).thenReturn(dependencies);

        ExtensionId prevExtensionId = new ExtensionId("application-test", "1.1");
        when(prevInstalledExtension.getId()).thenReturn(prevExtensionId);
        Set<ExtensionId> prevDependencies = new HashSet<>();
        prevDependencies.add(new ExtensionId("application-dep1", "1.2"));
        when(licensedExtensionManager.getLicensedDependencies(prevInstalledExtension, null)).thenReturn(
            prevDependencies);

        renewListener.onEvent(event, installedExtension, Collections.singletonList(prevInstalledExtension));

        verify(licenseUpdater).updateLicenses();
        verify(licenseUpdater).renewLicense(extensionId);
        verify(license, never()).getFeatureIds();
    }

    @Test
    void onEventWithExtensionUpgradedEventAndChangesOnLicense() throws Exception
    {
        ExtensionId extensionId = new ExtensionId("application-test", "1.2");
        ExtensionUpgradedEvent event = new ExtensionUpgradedEvent(extensionId, null);
        when(licensedExtensionManager.getLicensedExtensions()).thenReturn(Collections.singletonList(extensionId));
        when(licensedExtensionManager.getMandatoryLicensedExtensions()).thenReturn(Collections.singletonList(extensionId));
        when(licensor.getLicense(extensionId)).thenReturn(license);

        when(installedExtension.getId()).thenReturn(extensionId);
        Set<ExtensionId> dependencies = new HashSet<>();
        dependencies.add(new ExtensionId("application-dep1", "2.0"));
        when(licensedExtensionManager.getLicensedDependencies(installedExtension, null)).thenReturn(dependencies);

        ExtensionId prevExtensionId = new ExtensionId("application-test", "1.1");
        when(prevInstalledExtension.getId()).thenReturn(prevExtensionId);
        when(licensedExtensionManager.getLicensedDependencies(prevInstalledExtension, null)).thenReturn(
            dependencies);

        // Since dependencies are the same between the previous installed extension and the current one, the licensed
        // feature ids are checked.
        LicensedFeatureId featureId = new LicensedFeatureId("application-dep2", "2.1");
        when(license.getFeatureIds()).thenReturn(Collections.singletonList(featureId));

        renewListener.onEvent(event, installedExtension, Collections.singletonList(prevInstalledExtension));

        verify(licenseUpdater).updateLicenses();
        verify(licenseUpdater).renewLicense(extensionId);
    }

    @Test
    void onEventWithExtensionDowngradeEvent() throws Exception
    {
        ExtensionId extensionId = new ExtensionId("application-test", "1.2");
        ExtensionUpgradedEvent event = new ExtensionUpgradedEvent(extensionId, null);
        when(licensedExtensionManager.getLicensedExtensions()).thenReturn(Collections.singletonList(extensionId));
        when(licensedExtensionManager.getMandatoryLicensedExtensions()).thenReturn(Collections.singletonList(extensionId));
        when(licensor.getLicense(extensionId)).thenReturn(license);

        when(installedExtension.getId()).thenReturn(extensionId);

        ExtensionId prevExtensionId = new ExtensionId("application-test", "1.3");
        when(prevInstalledExtension.getId()).thenReturn(prevExtensionId);

        renewListener.onEvent(event, installedExtension, Collections.singletonList(prevInstalledExtension));

        verify(licenseUpdater).updateLicenses();
        verify(licenseUpdater, never()).renewLicense(extensionId);
    }

    @Test
    void onEventWithExtensionUpgradedEventAndNoChanges() throws Exception
    {
        ExtensionId extensionId = new ExtensionId("application-test", "1.2");
        ExtensionUpgradedEvent event = new ExtensionUpgradedEvent(extensionId, null);
        when(licensedExtensionManager.getLicensedExtensions()).thenReturn(Collections.singletonList(extensionId));
        when(licensedExtensionManager.getMandatoryLicensedExtensions()).thenReturn(Collections.singletonList(extensionId));
        when(licensor.getLicense(extensionId)).thenReturn(license);

        when(installedExtension.getId()).thenReturn(extensionId);
        Set<ExtensionId> dependencies = new HashSet<>();
        ExtensionId dependency = new ExtensionId("application-dep", "2.0");
        dependencies.add(dependency);
        when(licensedExtensionManager.getLicensedDependencies(installedExtension, null)).thenReturn(dependencies);

        ExtensionId prevExtensionId = new ExtensionId("application-test", "1.1");
        when(prevInstalledExtension.getId()).thenReturn(prevExtensionId);
        when(licensedExtensionManager.getLicensedDependencies(prevInstalledExtension, null)).thenReturn(dependencies);

        LicensedFeatureId featureId = new LicensedFeatureId(dependency.getId(), dependency.getVersion().getValue());
        when(license.getFeatureIds()).thenReturn(Collections.singletonList(featureId));

        renewListener.onEvent(event, installedExtension, Collections.singletonList(prevInstalledExtension));

        verify(licenseUpdater).updateLicenses();
        verify(licenseUpdater, never()).renewLicense(extensionId);
    }

    @Test
    void onEventWithExtensionUpgradedEventAndNoLicense() throws Exception
    {
        ExtensionId extensionId = new ExtensionId("application-test", "1.2");
        ExtensionUpgradedEvent event = new ExtensionUpgradedEvent(extensionId, null);
        when(licensedExtensionManager.getLicensedExtensions()).thenReturn(Collections.singletonList(extensionId));
        when(licensedExtensionManager.getMandatoryLicensedExtensions()).thenReturn(Collections.singletonList(extensionId));
        when(licensor.getLicense(extensionId)).thenReturn(null);
        when(installedExtension.getId()).thenReturn(extensionId);

        renewListener.onEvent(event, installedExtension, Collections.singletonList(prevInstalledExtension));

        verify(licenseUpdater).updateLicenses();
        verify(licenseUpdater, never()).renewLicense(extensionId);
    }
}
