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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.internal.installed.DefaultInstalledExtension;
import org.xwiki.extension.version.Version;
import org.xwiki.job.Request;
import org.xwiki.job.event.JobFinishedEvent;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Unit tests for {@link GetTrialLicenseListener}.
 * 
 * @version $Id$
 */
public class GetTrialLicenseListenerTest
{
    @Rule
    public MockitoComponentMockingRule<GetTrialLicenseListener> mocker =
        new MockitoComponentMockingRule<>(GetTrialLicenseListener.class);

    TrialLicenseGenerator trialLicenseGenerator;

    DefaultInstalledExtension extension;

    ExtensionId extensionId;

    ExtensionId dependencyId;

    JobFinishedEvent job;

    InstalledExtensionRepository installedExtensionRepository;

    List<String> namespaces;

    InstalledExtension installedExtension;

    InstalledExtension installedDependency;

    @Before
    public void configure() throws Exception
    {
        this.job = mock(JobFinishedEvent.class);
        this.extension = mock(DefaultInstalledExtension.class);
        this.extensionId = new ExtensionId("application-test", "1.0");
        this.trialLicenseGenerator = this.mocker.getInstance(TrialLicenseGenerator.class);

        Request request = mock(Request.class);
        List<ExtensionId> extensions = Arrays.asList(this.extensionId);

        when(request.getProperty("extensions")).thenReturn(extensions);
        when(this.job.getRequest()).thenReturn(request);

        this.installedExtensionRepository = this.mocker.getInstance(InstalledExtensionRepository.class);
        this.installedExtension = mock(InstalledExtension.class);

        when(this.installedExtensionRepository.getInstalledExtension(this.extensionId))
            .thenReturn(this.installedExtension);

        this.installedDependency = mock(InstalledExtension.class);
        ExtensionDependency dependency = mock(ExtensionDependency.class);
        Collection<ExtensionDependency> dependencies = Arrays.asList(dependency);
        this.dependencyId = new ExtensionId("application-dependency", mock(Version.class));
        this.namespaces = Arrays.asList("main");

        when(dependency.getId()).thenReturn("dependencyId");
        when(this.installedExtension.getDependencies()).thenReturn(dependencies);
        when(this.installedExtension.getNamespaces()).thenReturn(this.namespaces);
        when(this.installedExtensionRepository.getInstalledExtension("dependencyId", this.namespaces.get(0)))
            .thenReturn(this.installedDependency);
        when(this.installedDependency.getId()).thenReturn(this.dependencyId);
    }

    @Test
    public void onEventWithCompleteData() throws Exception
    {
        when(this.trialLicenseGenerator.canGenerateTrialLicense(this.extensionId)).thenReturn(true);
        when(this.trialLicenseGenerator.canGenerateTrialLicense(this.dependencyId)).thenReturn(false);

        this.mocker.getComponentUnderTest().onEvent(this.job, null, null);

        verify(this.trialLicenseGenerator, times(1)).generateTrialLicense(this.extensionId);
        verify(this.trialLicenseGenerator, never()).generateTrialLicense(this.dependencyId);
    }

    @Test
    public void onEventWithoutCompleteData() throws Exception
    {
        when(this.trialLicenseGenerator.canGenerateTrialLicense(this.extensionId)).thenReturn(false);
        when(this.trialLicenseGenerator.canGenerateTrialLicense(this.dependencyId)).thenReturn(false);

        this.mocker.getComponentUnderTest().onEvent(this.job, null, null);

        verify(this.trialLicenseGenerator, never()).generateTrialLicense(this.extensionId);
    }

    @Test
    public void onEventWithPaidAppDependency() throws Exception
    {
        when(this.trialLicenseGenerator.canGenerateTrialLicense(this.extensionId)).thenReturn(false);
        when(this.trialLicenseGenerator.canGenerateTrialLicense(this.dependencyId)).thenReturn(true);

        this.mocker.getComponentUnderTest().onEvent(this.job, null, null);

        verify(this.trialLicenseGenerator, never()).generateTrialLicense(this.extensionId);
        verify(this.trialLicenseGenerator, times(1)).generateTrialLicense(this.dependencyId);
    }

    @Test
    public void onEventWithPaidAppTransitiveDependency() throws Exception
    {
        when(this.trialLicenseGenerator.canGenerateTrialLicense(this.extensionId)).thenReturn(false);
        when(this.trialLicenseGenerator.canGenerateTrialLicense(this.dependencyId)).thenReturn(false);

        InstalledExtension installedDependencyExtension = mock(InstalledExtension.class);

        when(this.installedExtensionRepository.getInstalledExtension(this.dependencyId))
            .thenReturn(installedDependencyExtension);
        when(installedDependencyExtension.getNamespaces()).thenReturn(this.namespaces);

        InstalledExtension installedTransitiveDependency = mock(InstalledExtension.class);
        ExtensionDependency transitiveDependency = mock(ExtensionDependency.class);
        Collection<ExtensionDependency> transitiveDependencies = Arrays.asList(transitiveDependency);
        ExtensionId transitiveDependencyId = new ExtensionId("application-dependency", mock(Version.class));

        when(installedDependencyExtension.getDependencies()).thenReturn(transitiveDependencies);
        when(transitiveDependency.getId()).thenReturn(transitiveDependencyId.getId());
        when(this.installedExtensionRepository.getInstalledExtension(transitiveDependencyId.getId(),
            this.namespaces.get(0))).thenReturn(installedTransitiveDependency);
        when(installedTransitiveDependency.getId()).thenReturn(transitiveDependencyId);

        when(this.trialLicenseGenerator.canGenerateTrialLicense(transitiveDependencyId)).thenReturn(true);

        this.mocker.getComponentUnderTest().onEvent(this.job, null, null);

        verify(this.trialLicenseGenerator, never()).generateTrialLicense(this.extensionId);
        verify(this.trialLicenseGenerator, never()).generateTrialLicense(this.dependencyId);
        verify(this.trialLicenseGenerator, times(1)).generateTrialLicense(transitiveDependencyId);
    }

    @Test
    public void onEventWithPaidAppDependencyOnRootNamespace() throws Exception
    {
        when(this.trialLicenseGenerator.canGenerateTrialLicense(this.extensionId)).thenReturn(false);
        when(this.installedExtension.getNamespaces()).thenReturn(null);
        when(this.trialLicenseGenerator.canGenerateTrialLicense(this.dependencyId)).thenReturn(true);
        when(this.installedExtensionRepository.getInstalledExtension("dependencyId", null))
            .thenReturn(this.installedDependency);

        this.mocker.getComponentUnderTest().onEvent(this.job, null, null);

        verify(this.trialLicenseGenerator, never()).generateTrialLicense(this.extensionId);
        verify(this.trialLicenseGenerator, times(1)).generateTrialLicense(this.dependencyId);
    }
}
