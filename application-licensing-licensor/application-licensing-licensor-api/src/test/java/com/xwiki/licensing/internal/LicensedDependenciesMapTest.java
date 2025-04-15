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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LicensedDependenciesMap}.
 *
 * @version $Id$
 * @since 1.6
 */
@ComponentTest
public class LicensedDependenciesMapTest
{
    @InjectMockComponents
    private LicensedDependenciesMap licensedDependenciesMap;

    @MockComponent
    private InstalledExtensionRepository installedExtensionRepository;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.DEBUG);

    @Mock
    InstalledExtension pollExt;

    @Mock
    ExtensionDependency pollDep;

    @Mock
    InstalledExtension meetExt;

    @Mock
    InstalledExtension pdfExt;

    @Mock
    ExtensionDependency pdfDep;

    @Mock
    InstalledExtension diagramExt;

    @Mock
    ExtensionDependency diagramDep;

    @Mock
    InstalledExtension proMacrosExt;

    @Mock
    InstalledExtension contribExt;

    @Mock
    ExtensionDependency contribDep;

    ExtensionId poll;

    ExtensionId meet;

    ExtensionId pdf;

    ExtensionId diagram;

    ExtensionId proMacros;

    ExtensionId contrib;

    @BeforeEach
    void setup()
    {
        // Create an extension structure, following this dependency map: meet -> poll; proMacros -> pdf, diagram
        // (optional), contrib; contrib -> poll.
        poll = new ExtensionId("poll", "1.2");
        meet = new ExtensionId("meet", "2.0");
        pdf = new ExtensionId("pdfViewer", "3.1");
        diagram = new ExtensionId("diagram", "2.1");
        proMacros = new ExtensionId("proMacros", "1.3");
        contrib = new ExtensionId("contrib", "2.2");


        when(installedExtensionRepository.getInstalledExtension(poll)).thenReturn(pollExt);
        when(installedExtensionRepository.getInstalledExtension(poll.getId(), null)).thenReturn(pollExt);
        when(installedExtensionRepository.getInstalledExtension(poll.getId(), "wiki1")).thenReturn(pollExt);
        when(pollExt.getNamespaces()).thenReturn(Arrays.asList(null, "wiki1"));
        when(pollExt.getId()).thenReturn(poll);
        when(pollExt.getDependencies()).thenReturn(Collections.emptyList());
        when(pollDep.getId()).thenReturn(poll.getId());
        when(pollExt.getName()).thenReturn("Poll");

        when(installedExtensionRepository.getInstalledExtension(meet)).thenReturn(meetExt);
        when(installedExtensionRepository.getInstalledExtension(meet.getId(), null)).thenReturn(meetExt);
        when(installedExtensionRepository.getInstalledExtension(meet.getId(), "wiki1")).thenReturn(meetExt);
        when(meetExt.getNamespaces()).thenReturn(Arrays.asList(null, "wiki1"));
        when(meetExt.getId()).thenReturn(meet);
        when(meetExt.getDependencies()).thenReturn(List.of(pollDep));
        when(meetExt.getName()).thenReturn("Meet");

        when(installedExtensionRepository.getInstalledExtension(pdf)).thenReturn(pdfExt);
        when(installedExtensionRepository.getInstalledExtension(pdf.getId(), null)).thenReturn(pdfExt);
        when(installedExtensionRepository.getInstalledExtension(pdf.getId(), "wiki1")).thenReturn(pdfExt);
        when(installedExtensionRepository.getInstalledExtension(pdf.getId(), "wiki2")).thenReturn(pdfExt);
        when(pdfExt.getNamespaces()).thenReturn(Arrays.asList(null, "wiki1", "wiki2"));
        when(pdfExt.getId()).thenReturn(pdf);
        when(pdfExt.getDependencies()).thenReturn(Collections.emptyList());
        when(pdfDep.getId()).thenReturn(pdf.getId());
        when(pdfDep.isOptional()).thenReturn(false);
        when(pdfExt.getName()).thenReturn("Pdf");

        when(installedExtensionRepository.getInstalledExtension(diagram)).thenReturn(diagramExt);
        when(installedExtensionRepository.getInstalledExtension(diagram.getId(), null)).thenReturn(diagramExt);
        when(diagramExt.getNamespaces()).thenReturn(null);
        when(diagramExt.getId()).thenReturn(diagram);
        when(diagramExt.getDependencies()).thenReturn(Collections.emptyList());
        when(diagramDep.getId()).thenReturn(diagram.getId());
        when(diagramDep.isOptional()).thenReturn(true);
        when(diagramExt.getName()).thenReturn("Diagram");

        when(installedExtensionRepository.getInstalledExtension(proMacros)).thenReturn(proMacrosExt);
        when(installedExtensionRepository.getInstalledExtension(proMacros.getId(), null)).thenReturn(proMacrosExt);
        when(proMacrosExt.getNamespaces()).thenReturn(null);
        when(proMacrosExt.getId()).thenReturn(proMacros);
        when(proMacrosExt.getDependencies()).thenReturn(List.of(pdfDep, diagramDep, contribDep));
        when(proMacrosExt.getName()).thenReturn("Pro Macros");

        when(installedExtensionRepository.getInstalledExtension(contrib)).thenReturn(contribExt);
        when(installedExtensionRepository.getInstalledExtension(contrib.getId(), null)).thenReturn(contribExt);
        when(contribExt.getNamespaces()).thenReturn(null);
        when(contribExt.getId()).thenReturn(contrib);
        when(contribExt.getDependencies()).thenReturn(List.of(pollDep));
        when(contribDep.getId()).thenReturn(contrib.getId());
        when(contribExt.getName()).thenReturn("Contrib");
    }

    /**
     * Verify the returned map, with or without cached values.
     */
    @Test
    void getAndInvalidateCache()
    {
        Collection<ExtensionId> allLicensedExtensions =
            new ArrayList<>(Arrays.asList(poll, meet, pdf, diagram, proMacros));

        // First call will compute the cache.
        Map<String, Set<LicensedDependenciesMap.LicensedExtensionParent>> result =
            licensedDependenciesMap.get(allLicensedExtensions);

        assertEquals(2, result.size());
        assertTrue(result.containsKey(poll.getId()));
        // Meet is installed on 2 namespaces, so it will be displayed twice.
        assertEquals(3, result.get(poll.getId()).size());
        assertTrue(result.containsKey(pdf.getId()));
        assertEquals(1, result.get(pdf.getId()).size());

        this.logCapture.ignoreAllMessages();
        // For the future operations, we cannot actually get the cached map, but only the final result, which might
        // be computed or not, so we rely on what is logged.
        // Check that the cached list is returned.
        licensedDependenciesMap.get(allLicensedExtensions);
        assertEquals("Licensed dependencies map is cached, returning it.",
            this.logCapture.getMessage(this.logCapture.size() - 1));

        // Check that cache is invalidated.
        licensedDependenciesMap.invalidateCache();
        assertEquals("Clear licensed dependency map cache.", this.logCapture.getMessage(this.logCapture.size() - 1));
    }
}
