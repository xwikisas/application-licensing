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
package com.xwiki.licensing.internal.upgrades;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.version.internal.DefaultVersion;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xwiki.licensing.LicensedExtensionManager;
import com.xwiki.licensing.LicensingConfiguration;

/**
 * Unit tests for {@link LicensedExtensionUpgradeManager}.
 *
 * @version $Id$
 * @since 1.17
 */
public class LicensedExtensionUpgradeManagerTest
{
    @Rule
    public MockitoComponentMockingRule<LicensedExtensionUpgradeManager> mocker =
        new MockitoComponentMockingRule<>(LicensedExtensionUpgradeManager.class);

    private InstalledExtensionRepository installedRepository;

    private UpgradeExtensionHandler upgradeExtensionHandler;

    private LicensedExtensionManager licensedExtensionManager;

    private LicensingConfiguration licensingConfig;

    private ExtensionId extensionId1;

    private ExtensionId extensionId2;

    @Before
    public void configure() throws Exception
    {
        this.installedRepository = this.mocker.getInstance(InstalledExtensionRepository.class);
        this.upgradeExtensionHandler = this.mocker.getInstance(UpgradeExtensionHandler.class);
        this.licensedExtensionManager = this.mocker.getInstance(LicensedExtensionManager.class);
        this.licensingConfig = this.mocker.getInstance(LicensingConfiguration.class);

        this.extensionId1 = new ExtensionId("extensionId1", new DefaultVersion("1.0"));
        this.extensionId2 = new ExtensionId("extensionId2", new DefaultVersion("2.0"));

    }

    @Test
    public void upgradeLicensedExtensionsOnNamespaceWithAllowlist() throws Exception
    {
        String namespace = "wiki:test";
        when(this.licensingConfig.getAutoUpgradeAllowlist()).thenReturn(Arrays.asList(this.extensionId1.getId()));

        when(this.licensedExtensionManager.getLicensedExtensions())
            .thenReturn(Arrays.asList(this.extensionId1, this.extensionId2));

        InstalledExtension installedExtension1 = mock(InstalledExtension.class);
        InstalledExtension installedExtension2 = mock(InstalledExtension.class);
        when(this.installedRepository.getInstalledExtension(this.extensionId1)).thenReturn(installedExtension1);
        when(this.installedRepository.getInstalledExtension(this.extensionId2)).thenReturn(installedExtension2);

        when(installedExtension1.getNamespaces()).thenReturn(Arrays.asList(namespace));
        when(installedExtension2.getNamespaces()).thenReturn(Arrays.asList(namespace));

        mocker.getComponentUnderTest().upgradeLicensedExtensions();

        verify(this.upgradeExtensionHandler, times(1)).tryUpgradeExtensionToLastVersion(eq(installedExtension1),
            eq(namespace));
        verify(this.upgradeExtensionHandler, never()).tryUpgradeExtensionToLastVersion(eq(installedExtension2),
            eq(namespace));
    }

    @Test
    public void upgradeLicensedExtensionsWithoutAllowlist() throws Exception
    {
        String namespace = "wiki:test";
        when(this.licensingConfig.getAutoUpgradeAllowlist()).thenReturn(Collections.emptyList());

        when(this.licensedExtensionManager.getLicensedExtensions())
            .thenReturn(Arrays.asList(this.extensionId1, this.extensionId2));

        InstalledExtension installedExtension1 = mock(InstalledExtension.class);
        InstalledExtension installedExtension2 = mock(InstalledExtension.class);
        when(this.installedRepository.getInstalledExtension(this.extensionId1)).thenReturn(installedExtension1);
        when(this.installedRepository.getInstalledExtension(this.extensionId2)).thenReturn(installedExtension2);

        when(installedExtension1.getNamespaces()).thenReturn(Arrays.asList(namespace));
        when(installedExtension2.getNamespaces()).thenReturn(Arrays.asList(namespace));

        mocker.getComponentUnderTest().upgradeLicensedExtensions();

        verify(this.upgradeExtensionHandler, never()).tryUpgradeExtensionToLastVersion(eq(installedExtension1),
            eq(namespace));

        verify(this.upgradeExtensionHandler, never()).tryUpgradeExtensionToLastVersion(eq(installedExtension2),
            eq(namespace));

    }

    @Test
    public void upgradeLicensedExtensionsOnRootNamespace() throws Exception
    {
        when(this.licensingConfig.getAutoUpgradeAllowlist()).thenReturn(Arrays.asList(this.extensionId1.getId()));
        when(this.licensedExtensionManager.getLicensedExtensions()).thenReturn(Arrays.asList(this.extensionId1));

        InstalledExtension installedExtension1 = mock(InstalledExtension.class);
        when(this.installedRepository.getInstalledExtension(this.extensionId1)).thenReturn(installedExtension1);

        when(installedExtension1.getNamespaces()).thenReturn(null);

        mocker.getComponentUnderTest().upgradeLicensedExtensions();

        verify(this.upgradeExtensionHandler, times(1)).tryUpgradeExtensionToLastVersion(eq(installedExtension1),
            isNull());
    }
}
