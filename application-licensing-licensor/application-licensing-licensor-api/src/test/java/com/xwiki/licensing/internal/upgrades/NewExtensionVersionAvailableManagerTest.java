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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.internal.DefaultVersion;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.licensing.LicensedExtensionManager;
import com.xwiki.licensing.LicensingConfiguration;
import com.xwiki.licensing.internal.upgrades.notifications.newVersion.NewExtensionVersionAvailableEvent;

/**
 * Unit tests for {@link NewExtensionVersionAvailableManager}.
 *
 * @version $Id$
 * @since 1.23
 */
@ComponentTest
public class NewExtensionVersionAvailableManagerTest
{
    @InjectMockComponents
    private NewExtensionVersionAvailableManager newVersionAvailableManager;

    @MockComponent
    private InstalledExtensionRepository installedRepository;

    @MockComponent
    private UpgradeExtensionHandler upgradeExtensionHandler;

    @MockComponent
    private LicensedExtensionManager licensedExtensionManager;

    @MockComponent
    private LicensingConfiguration licensingConfig;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private NewVersionNotificationManager newVersionNotificationManager;

    @MockComponent
    private Provider<XWikiContext> wikiContextProvider;

    @MockComponent
    private XWikiContext wikiContext;

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

        when(this.installedExtension1.getName()).thenReturn("Application 1");
        when(this.installedExtension1.getId()).thenReturn(extensionId1);
        when(this.installedRepository.getInstalledExtension(this.extensionId1)).thenReturn(this.installedExtension1);

        when(this.installedExtension2.getName()).thenReturn("Application 2");
        when(this.installedExtension2.getId()).thenReturn(extensionId2);
        when(this.installedRepository.getInstalledExtension(this.extensionId2)).thenReturn(this.installedExtension2);

        when(wikiContextProvider.get()).thenReturn(wikiContext);
        when(wikiContext.getWikiId()).thenReturn("xwiki");
    }

    @Test
    void checkLicensedExtensionsAvailableVersionsWithMultipleExtensionsAndVersionsNotVerified() throws Exception
    {
        when(this.licensingConfig.getAutoUpgradeAllowList()).thenReturn(Collections.emptyList());
        when(this.licensedExtensionManager.getLicensedExtensions())
            .thenReturn(Arrays.asList(this.extensionId1, this.extensionId2));

        String namespace = "wiki:test";
        when(this.installedExtension1.getNamespaces()).thenReturn(Collections.singletonList(namespace));
        when(this.installedRepository.getInstalledExtension(this.extensionId1.getId(), namespace))
            .thenReturn(this.installedExtension1);

        when(this.installedExtension2.getNamespaces()).thenReturn(null);
        when(this.installedRepository.getInstalledExtension(this.extensionId2.getId(), null))
            .thenReturn(this.installedExtension2);

        when(this.upgradeExtensionHandler.getInstallableVersions(extensionId1))
            .thenReturn(Collections.singletonList((Version) new DefaultVersion("2.1")));
        when(this.upgradeExtensionHandler.getInstallableVersions(extensionId2))
            .thenReturn(Arrays.asList((Version) new DefaultVersion("3.1"), (Version) new DefaultVersion("2.1")));
        when(this.newVersionNotificationManager.isNotificationAlreadySent(this.extensionId1.getId(), namespace,
            "2.1")).thenReturn(false);
        when(this.newVersionNotificationManager.isNotificationAlreadySent(this.extensionId2.getId(), "root",
            "3.1")).thenReturn(false);

        this.newVersionAvailableManager.checkLicensedExtensionsAvailableVersions();

        verify(this.observationManager, times(1)).notify(any(NewExtensionVersionAvailableEvent.class),
            eq(this.extensionId1.getId()),
            eq("{\"extensionName\":\"Application 1\",\"namespace\":\"wiki:test\",\"version\":\"2.1\"}"));
        verify(this.observationManager, times(1)).notify(any(NewExtensionVersionAvailableEvent.class),
            eq(this.extensionId2.getId()),
            eq("{\"extensionName\":\"Application 2\",\"namespace\":\"root\",\"version\":\"3.1\"}"));
    }

    @Test
    void checkLicensedExtensionsAvailableVersionsWithVerifiedVersions() throws Exception
    {
        when(this.licensingConfig.getAutoUpgradeAllowList()).thenReturn(Collections.emptyList());
        when(this.licensedExtensionManager.getLicensedExtensions())
            .thenReturn(Arrays.asList(this.extensionId1, this.extensionId2));

        String namespace = "wiki:test";
        when(this.installedExtension1.getNamespaces()).thenReturn(Collections.singletonList(namespace));
        when(this.installedRepository.getInstalledExtension(this.extensionId1.getId(), namespace))
            .thenReturn(this.installedExtension1);

        when(this.installedExtension2.getNamespaces()).thenReturn(null);
        when(this.installedRepository.getInstalledExtension(this.extensionId2.getId(), null))
            .thenReturn(this.installedExtension2);

        when(this.upgradeExtensionHandler.getInstallableVersions(extensionId1))
            .thenReturn(Arrays.asList((Version) new DefaultVersion("2.2"), (Version) new DefaultVersion("2.1")));
        when(this.upgradeExtensionHandler.getInstallableVersions(extensionId2))
            .thenReturn(Arrays.asList((Version) new DefaultVersion("3.1"), (Version) new DefaultVersion("3.0")));

        when(this.newVersionNotificationManager.isNotificationAlreadySent(this.extensionId1.getId(), namespace,
            "2.2")).thenReturn(true);
        when(this.newVersionNotificationManager.isNotificationAlreadySent(this.extensionId2.getId(), null,
            "3.0")).thenReturn(true);

        this.newVersionAvailableManager.checkLicensedExtensionsAvailableVersions();

        verify(this.observationManager, never()).notify(any(NewExtensionVersionAvailableEvent.class),
            eq(this.extensionId1.getId()), any(String.class));
        verify(this.observationManager, times(1)).notify(any(NewExtensionVersionAvailableEvent.class),
            eq(this.extensionId2.getId()),
            eq("{\"extensionName\":\"Application 2\",\"namespace\":\"root\",\"version\":\"3.1\"}"));
        verify(this.observationManager, never()).notify(any(NewExtensionVersionAvailableEvent.class),
            eq(this.extensionId2.getId()),
            eq("{\"extensionName\":\"Application 2\",\"namespace\":\"root\",\"version\":\"3.0\"}"));
    }

    @Test
    void checkLicensedExtensionsAvailableVersionsWithoutNewVersions() throws Exception
    {
        when(this.licensingConfig.getAutoUpgradeAllowList()).thenReturn(
            Collections.singletonList(this.extensionId2.getId()));
        when(this.licensedExtensionManager.getLicensedExtensions())
            .thenReturn(Arrays.asList(this.extensionId1, this.extensionId2));

        String namespace = "wiki:test";
        when(this.installedExtension1.getNamespaces()).thenReturn(Collections.singletonList(namespace));
        when(this.installedRepository.getInstalledExtension(this.extensionId1.getId(), namespace))
            .thenReturn(this.installedExtension1);

        when(this.upgradeExtensionHandler.getInstallableVersions(extensionId1)).thenReturn(Collections.emptyList());

        this.newVersionAvailableManager.checkLicensedExtensionsAvailableVersions();

        verify(this.observationManager, never()).notify(any(NewExtensionVersionAvailableEvent.class),
            eq(this.extensionId1), any(String.class));
        verify(this.installedRepository, never()).getInstalledExtension(this.extensionId2);
    }
}
