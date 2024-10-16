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

        when(installedExtension.getId()).thenReturn(extensionId);
        when(licensor.getLicense(extensionId)).thenReturn(license);

        renewListener.onEvent(event, installedExtension, null);

        verify(licenseUpdater).getLicensesUpdates();
        verify(licenseUpdater).renewLicense(extensionId);
    }

    @Test
    void onEventWithExtensionUpgradedEventAndChanges() throws Exception
    {
        ExtensionId extensionId = new ExtensionId("application-test", "1.2");
        ExtensionUpgradedEvent event = new ExtensionUpgradedEvent(extensionId, null);
        when(licensedExtensionManager.getLicensedExtensions()).thenReturn(Collections.singletonList(extensionId));

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

        verify(licenseUpdater).getLicensesUpdates();
        verify(licenseUpdater).renewLicense(extensionId);
    }

    @Test
    void onEventWithExtensionUpgradedEventAndNoChanges() throws Exception
    {
        ExtensionId extensionId = new ExtensionId("application-test", "1.2");
        ExtensionUpgradedEvent event = new ExtensionUpgradedEvent(extensionId, null);
        when(licensedExtensionManager.getLicensedExtensions()).thenReturn(Collections.singletonList(extensionId));

        when(installedExtension.getId()).thenReturn(extensionId);
        Set<ExtensionId> dependencies = new HashSet<>();
        dependencies.add(new ExtensionId("application-dep2", "2.0"));
        when(licensedExtensionManager.getLicensedDependencies(installedExtension, null)).thenReturn(dependencies);

        ExtensionId prevExtensionId = new ExtensionId("application-test", "1.1");
        when(prevInstalledExtension.getId()).thenReturn(prevExtensionId);
        when(licensedExtensionManager.getLicensedDependencies(prevInstalledExtension, null)).thenReturn(dependencies);

        renewListener.onEvent(event, installedExtension, Collections.singletonList(prevInstalledExtension));

        verify(licenseUpdater).getLicensesUpdates();
        verify(licenseUpdater, never()).renewLicense(extensionId);
    }
}
