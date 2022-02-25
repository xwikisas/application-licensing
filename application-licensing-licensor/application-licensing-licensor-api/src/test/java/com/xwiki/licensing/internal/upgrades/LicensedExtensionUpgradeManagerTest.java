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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.version.internal.DefaultVersion;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.licensing.LicensedExtensionManager;
import com.xwiki.licensing.LicensingConfiguration;

/**
 * Unit tests for {@link LicensedExtensionUpgradeManager}.
 *
 * @version $Id$
 * @since 1.17
 */
@ComponentTest
class LicensedExtensionUpgradeManagerTest
{
    @InjectMockComponents
    private LicensedExtensionUpgradeManager licensedExtensionUpgradeManager;

    @MockComponent
    private InstalledExtensionRepository installedRepository;

    @MockComponent
    private UpgradeExtensionHandler upgradeExtensionHandler;

    @MockComponent
    private LicensedExtensionManager licensedExtensionManager;

    @MockComponent
    private LicensingConfiguration licensingConfig;

    @Mock
    private InstalledExtension installedExtension1;

    @Mock
    private InstalledExtension installedExtension2;

    private ExtensionId extensionId1;

    private ExtensionId extensionId2;

    @BeforeEach
    public void configure() throws Exception
    {
        this.extensionId1 = new ExtensionId("extensionId1", new DefaultVersion("1.0"));
        this.extensionId2 = new ExtensionId("extensionId2", new DefaultVersion("2.0"));
    }

    @Test
    void upgradeLicensedExtensionsOnNamespaceWithAllowList() throws Exception
    {
        String namespace = "wiki:test";
        when(this.licensingConfig.getAutoUpgradeAllowList()).thenReturn(Arrays.asList(this.extensionId1.getId()));

        when(this.licensedExtensionManager.getLicensedExtensions())
            .thenReturn(Arrays.asList(this.extensionId1, this.extensionId2));

        when(this.installedRepository.getInstalledExtension(this.extensionId1)).thenReturn(this.installedExtension1);
        when(this.installedRepository.getInstalledExtension(this.extensionId2)).thenReturn(this.installedExtension2);

        when(this.installedExtension1.getNamespaces()).thenReturn(Arrays.asList(namespace));
        when(this.installedExtension2.getNamespaces()).thenReturn(Arrays.asList(namespace));

        this.licensedExtensionUpgradeManager.upgradeLicensedExtensions();

        verify(this.upgradeExtensionHandler, times(1)).tryUpgradeExtensionToLastVersion(eq(this.installedExtension1),
            eq(namespace));
        verify(this.upgradeExtensionHandler, never()).tryUpgradeExtensionToLastVersion(eq(this.installedExtension2),
            eq(namespace));
    }

    @Test
    void upgradeLicensedExtensionsWithoutAllowList() throws Exception
    {
        String namespace = "wiki:test";
        when(this.licensingConfig.getAutoUpgradeAllowList()).thenReturn(Collections.emptyList());

        when(this.licensedExtensionManager.getLicensedExtensions())
            .thenReturn(Arrays.asList(this.extensionId1, this.extensionId2));

        when(this.installedRepository.getInstalledExtension(this.extensionId1)).thenReturn(this.installedExtension1);
        when(this.installedRepository.getInstalledExtension(this.extensionId2)).thenReturn(this.installedExtension2);

        when(this.installedExtension1.getNamespaces()).thenReturn(Arrays.asList(namespace));
        when(this.installedExtension2.getNamespaces()).thenReturn(Arrays.asList(namespace));

        this.licensedExtensionUpgradeManager.upgradeLicensedExtensions();

        verify(this.upgradeExtensionHandler, never()).tryUpgradeExtensionToLastVersion(eq(this.installedExtension1),
            eq(namespace));

        verify(this.upgradeExtensionHandler, never()).tryUpgradeExtensionToLastVersion(eq(this.installedExtension2),
            eq(namespace));

    }

    @Test
    void upgradeLicensedExtensionsOnRootNamespace() throws Exception
    {
        when(this.licensingConfig.getAutoUpgradeAllowList()).thenReturn(Arrays.asList(this.extensionId1.getId()));
        when(this.licensedExtensionManager.getLicensedExtensions()).thenReturn(Arrays.asList(this.extensionId1));

        when(this.installedRepository.getInstalledExtension(this.extensionId1)).thenReturn(this.installedExtension1);

        when(this.installedExtension1.getNamespaces()).thenReturn(null);

        this.licensedExtensionUpgradeManager.upgradeLicensedExtensions();

        verify(this.upgradeExtensionHandler, times(1)).tryUpgradeExtensionToLastVersion(eq(this.installedExtension1),
            isNull());
    }
}
