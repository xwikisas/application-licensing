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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultLicensedExtensionManager}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultLicensedExtensionManagerTest
{
    public static final String MAIN = "main";

    @InjectMockComponents
    DefaultLicensedExtensionManager licensedExtensionManager;

    @MockComponent
    InstalledExtensionRepository installedExtensionRepository;

    @MockComponent
    InstalledExtension licensorExtension;

    @MockComponent
    InstalledExtension pollsExtension;

    @MockComponent
    InstalledExtension flashV1Extension;

    @MockComponent
    ExtensionDependency flashV1Dependency;

    @MockComponent
    InstalledExtension flashV2Extension;

    @MockComponent
    ExtensionDependency flashV2Dependency;

    @MockComponent
    InstalledExtension ideasExtension;

    @MockComponent
    ExtensionDependency ideasDependency;

    @MockComponent
    InstalledExtension freeExtension;

    @MockComponent
    ExtensionDependency freeExtensionDependency;

    Map<String, Collection<InstalledExtension>> licensorDependencies;

    ExtensionId flashV2ExtensionId;

    List<String> pollsNamespaces;

    @BeforeEach
    public void configure()
    {
        this.licensorDependencies = new HashMap<>();
        when(this.installedExtensionRepository.getInstalledExtension(
            DefaultLicensedExtensionManager.LICENSOR_EXTENSION_ID, null)).thenReturn(this.licensorExtension);

        ExtensionId pollsExtensionId = new ExtensionId("application-xpoll", "1.0");
        when(this.pollsExtension.getId()).thenReturn(pollsExtensionId);
        this.pollsNamespaces = Collections.singletonList(MAIN);
        when(this.pollsExtension.getNamespaces()).thenReturn(this.pollsNamespaces);
        when(this.installedExtensionRepository.getInstalledExtension(pollsExtensionId)).thenReturn(this.pollsExtension);
        when(this.installedExtensionRepository.getInstalledExtension(pollsExtensionId.getId(),
            this.pollsNamespaces.get(0))).thenReturn(this.pollsExtension);

        ExtensionId flashV1ExtensionId = new ExtensionId("application-flash", "1.1");
        when(this.flashV1Extension.getId()).thenReturn(flashV1ExtensionId);
        when(this.flashV1Dependency.getId()).thenReturn(flashV1ExtensionId.getId());
        when(this.flashV1Extension.getNamespaces()).thenReturn(this.pollsNamespaces);
        when(this.installedExtensionRepository.getInstalledExtension(this.flashV1Dependency.getId(),
            this.pollsNamespaces.get(0))).thenReturn(this.flashV1Extension);
        when(this.installedExtensionRepository.getInstalledExtension(this.flashV1Extension.getId())).thenReturn(
            this.flashV1Extension);

        this.flashV2ExtensionId = new ExtensionId("application-flash", "2.1");
        when(this.flashV2Extension.getId()).thenReturn(this.flashV2ExtensionId);
        when(this.flashV2Dependency.getId()).thenReturn(this.flashV2ExtensionId.getId());

        Collection<ExtensionDependency> pollsDependencies = Collections.singletonList(this.flashV1Dependency);
        when(this.pollsExtension.getDependencies()).thenReturn(pollsDependencies);

        ExtensionId ideasExtensionId = new ExtensionId("application-ideas", "1.0");
        when(this.ideasExtension.getId()).thenReturn(ideasExtensionId);
        when(this.ideasDependency.getId()).thenReturn(ideasExtensionId.getId());
        when(this.installedExtensionRepository.getInstalledExtension(this.ideasDependency.getId(),
            this.pollsNamespaces.get(0))).thenReturn(this.ideasExtension);

        ExtensionId freeExtensionId = new ExtensionId("application-free", "1.1");
        when(freeExtension.getId()).thenReturn(freeExtensionId);
        when(freeExtensionDependency.getId()).thenReturn(freeExtensionId.getId());
        when(this.installedExtensionRepository.getInstalledExtension(freeExtensionDependency.getId(),
            this.pollsNamespaces.get(0))).thenReturn(freeExtension);
    }

    @Test
    public void getMandatoryLicensedExtensions() throws Exception
    {

        this.licensorDependencies.put(null, Arrays.asList(this.pollsExtension, this.flashV1Extension));
        when(this.installedExtensionRepository.getBackwardDependencies(this.licensorExtension.getId())).thenReturn(
            this.licensorDependencies);

        Set<ExtensionId> expected = new HashSet<>();
        expected.add(this.pollsExtension.getId());

        Set<ExtensionId> result = this.licensedExtensionManager.getMandatoryLicensedExtensions();

        assertEquals(expected, result);
    }

    @Test
    public void getMandatoryLicensedExtensionsOnRootNamespace() throws Exception
    {
        this.licensorDependencies.put(null, Arrays.asList(this.pollsExtension, this.flashV1Extension));
        when(this.installedExtensionRepository.getBackwardDependencies(this.licensorExtension.getId())).thenReturn(
            this.licensorDependencies);

        when(this.pollsExtension.getNamespaces()).thenReturn(null);
        when(this.installedExtensionRepository.getInstalledExtension(this.pollsExtension.getId().getId(),
            null)).thenReturn(this.pollsExtension);

        when(this.flashV1Extension.getNamespaces()).thenReturn(null);
        when(this.installedExtensionRepository.getInstalledExtension(this.flashV1Extension.getId().getId(),
            null)).thenReturn(this.flashV1Extension);

        Set<ExtensionId> expected = new HashSet<>();
        expected.add(this.pollsExtension.getId());

        Set<ExtensionId> result = this.licensedExtensionManager.getMandatoryLicensedExtensions();

        assertEquals(expected, result);
    }

    @Test
    public void getMandatoryLicensedExtensionsWithDifferentVersionOnNamespaces() throws Exception
    {
        this.licensorDependencies.put(null,
            Arrays.asList(this.pollsExtension, this.flashV1Extension, this.flashV2Extension));
        when(this.installedExtensionRepository.getBackwardDependencies(this.licensorExtension.getId())).thenReturn(
            this.licensorDependencies);

        List<String> flashV2Namespaces = Arrays.asList("wiki2");
        when(this.flashV2Extension.getNamespaces()).thenReturn(flashV2Namespaces);
        when(this.installedExtensionRepository.getInstalledExtension(this.flashV2ExtensionId)).thenReturn(
            this.flashV2Extension);
        when(this.installedExtensionRepository.getInstalledExtension(this.flashV2ExtensionId.getId(),
            flashV2Namespaces.get(0))).thenReturn(this.flashV2Extension);

        Set<ExtensionId> expected = new HashSet<>();
        expected.add(this.pollsExtension.getId());
        expected.add(this.flashV2ExtensionId);

        Set<ExtensionId> result = this.licensedExtensionManager.getMandatoryLicensedExtensions();

        assertEquals(expected, result);
    }

    @Test
    public void getMandatoryLicensedExtensionsWithTransitivePaidDependency() throws Exception
    {
        this.licensorDependencies.put(null, Arrays.asList(this.pollsExtension, this.flashV1Extension));
        when(this.installedExtensionRepository.getBackwardDependencies(this.licensorExtension.getId())).thenReturn(
            this.licensorDependencies);

        ExtensionId freeExtensionId = new ExtensionId("application-free", "1.1");
        when(freeExtension.getId()).thenReturn(freeExtensionId);
        when(freeExtensionDependency.getId()).thenReturn(freeExtensionId.getId());
        when(this.installedExtensionRepository.getInstalledExtension(freeExtensionDependency.getId(),
            this.pollsNamespaces.get(0))).thenReturn(freeExtension);

        when(this.pollsExtension.getDependencies()).thenReturn(Arrays.asList(freeExtensionDependency));
        when(freeExtension.getDependencies()).thenReturn(Arrays.asList(this.flashV1Dependency));

        Set<ExtensionId> expected = new HashSet<>();
        expected.add(this.pollsExtension.getId());

        Set<ExtensionId> result = this.licensedExtensionManager.getMandatoryLicensedExtensions();

        assertEquals(expected, result);
    }

    @Test
    public void getMandatoryLicensedExtensionsIsCached() throws Exception
    {
        this.licensorDependencies.put(null, Arrays.asList(this.pollsExtension));
        when(this.installedExtensionRepository.getBackwardDependencies(this.licensorExtension.getId())).thenReturn(
            this.licensorDependencies);
        when(this.pollsExtension.getDependencies()).thenReturn(Collections.<ExtensionDependency>emptyList());

        Set<ExtensionId> expected = new HashSet<>();
        expected.add(this.pollsExtension.getId());

        assertEquals(expected, this.licensedExtensionManager.getMandatoryLicensedExtensions());
        assertEquals(expected, this.licensedExtensionManager.getMandatoryLicensedExtensions());

        verify(this.installedExtensionRepository, times(1)).getInstalledExtension(
            DefaultLicensedExtensionManager.LICENSOR_EXTENSION_ID, null);
    }

    @Test
    public void invalidateMandatoryLicensedExtensionsCache() throws Exception
    {
        this.licensorDependencies.put(null, Arrays.asList(this.pollsExtension));
        when(this.installedExtensionRepository.getBackwardDependencies(this.licensorExtension.getId())).thenReturn(
            this.licensorDependencies);
        when(this.pollsExtension.getDependencies()).thenReturn(Collections.<ExtensionDependency>emptyList());

        Set<ExtensionId> expected = new HashSet<>();
        expected.add(this.pollsExtension.getId());

        assertEquals(expected, this.licensedExtensionManager.getMandatoryLicensedExtensions());
        this.licensedExtensionManager.invalidateMandatoryLicensedExtensionsCache();
        assertEquals(expected, this.licensedExtensionManager.getMandatoryLicensedExtensions());

        verify(this.installedExtensionRepository, times(2)).getInstalledExtension(
            DefaultLicensedExtensionManager.LICENSOR_EXTENSION_ID, null);
    }

    @Test
    public void getLicensedDependencies() throws Exception
    {
        this.licensorDependencies.put(null,
            Arrays.asList(this.pollsExtension, this.flashV1Extension, this.ideasExtension));
        when(this.installedExtensionRepository.getBackwardDependencies(this.licensorExtension.getId())).thenReturn(
            this.licensorDependencies);

        // The apps dependency is the following: Polls depends on FreeExtension and Ideas Pro, while FreeExtension
        // depends on FlashV1 Pro.
        when(this.pollsExtension.getDependencies()).thenReturn(
            Arrays.asList(this.freeExtensionDependency, this.ideasDependency));
        when(freeExtension.getDependencies()).thenReturn(Collections.singletonList(this.flashV1Dependency));

        Set<ExtensionId> result =
            licensedExtensionManager.getLicensedDependencies(pollsExtension, pollsNamespaces.get(0));

        Set<ExtensionId> expected = new HashSet<>();
        expected.add(flashV1Extension.getId());
        expected.add(ideasExtension.getId());

        assertEquals(expected, result);
    }

    @Test
    public void getLicensedDependenciesWithExtensionsOnRootNamespaces() throws Exception
    {
        this.licensorDependencies.put(null, Arrays.asList(this.pollsExtension, this.flashV1Extension));
        when(this.installedExtensionRepository.getBackwardDependencies(this.licensorExtension.getId())).thenReturn(
            this.licensorDependencies);

        when(this.pollsExtension.getNamespaces()).thenReturn(null);
        when(this.installedExtensionRepository.getInstalledExtension(this.pollsExtension.getId().getId(),
            null)).thenReturn(this.pollsExtension);

        when(this.flashV1Extension.getNamespaces()).thenReturn(null);
        when(this.installedExtensionRepository.getInstalledExtension(this.flashV1Extension.getId().getId(),
            null)).thenReturn(this.flashV1Extension);

        Set<ExtensionId> result = licensedExtensionManager.getLicensedDependencies(pollsExtension, null);

        Set<ExtensionId> expected = new HashSet<>();
        expected.add(flashV1Extension.getId());

        assertEquals(expected, result);
    }
}
