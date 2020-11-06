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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.internal.validator.AbstractExtensionValidator;
import org.xwiki.extension.job.ExtensionRequest;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.internal.InstallJob;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.result.CollectionIterableResult;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.internal.DefaultVersion;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xwiki.licensing.internal.upgrades.notifications.ExtensionAutoUpgradedEvent;

public class UpgradeExtensionHandlerTest
{
    @Rule
    public MockitoComponentMockingRule<UpgradeExtensionHandler> mocker =
        new MockitoComponentMockingRule<>(UpgradeExtensionHandler.class);

    private ExtensionRepositoryManager extensionRepositoryManager;

    private InstalledExtension installedExtension = mock(InstalledExtension.class);

    private ObservationManager observationManager;

    private ContextualLocalizationManager localization;

    private JobExecutor jobExecutor;

    private DocumentAccessBridge documentAccessBridge;

    private ExtensionId installedExtensionId;

    private Version installedVersion;

    private Version newVersion1;

    private Version newVersion2;
    
    private DocumentReference userReference;

    @Before
    public void configure() throws Exception
    {
        extensionRepositoryManager = mocker.getInstance(ExtensionRepositoryManager.class);

        observationManager = mocker.getInstance(ObservationManager.class);

        documentAccessBridge = mocker.getInstance(DocumentAccessBridge.class);

        jobExecutor = mocker.getInstance(JobExecutor.class);

        localization = mocker.getInstance(ContextualLocalizationManager.class);

        installedVersion = new DefaultVersion("1.0");
        newVersion1 = new DefaultVersion("2.0");
        newVersion2 = new DefaultVersion("3.0");
        installedExtensionId = new ExtensionId("application", installedVersion);

        when(this.installedExtension.getId()).thenReturn(installedExtensionId);

        userReference = new DocumentReference("wiki", Arrays.asList("XWiki"), "UserName");
        when(this.documentAccessBridge.getCurrentUserReference()).thenReturn(userReference);
    }

    @Test
    public void getInstallableVersionsNullList() throws Exception
    {
        Collection<Version> allVersions = Arrays.asList(installedVersion);
        when(extensionRepositoryManager.resolveVersions(installedExtensionId.getId(), 0, -1))
            .thenReturn(new CollectionIterableResult<>(-1, 0, allVersions));

        assertEquals(Collections.emptyList(),
            this.mocker.getComponentUnderTest().getInstallableVersions(installedExtensionId));
    }

    @Test
    public void getInstallableVersionsWithoutInstalledVersion() throws Exception
    {
        Collection<Version> allVersions = Arrays.asList(installedVersion, newVersion1);
        when(extensionRepositoryManager.resolveVersions(installedExtensionId.getId(), 0, -1))
            .thenReturn(new CollectionIterableResult<>(-1, 0, allVersions));

        assertEquals(Arrays.asList(newVersion1),
            this.mocker.getComponentUnderTest().getInstallableVersions(installedExtensionId));
    }

    @Test
    public void getInstallableVersionsWithCorrectOrder() throws Exception
    {
        Collection<Version> allVersions = Arrays.asList(installedVersion, newVersion1, newVersion2);
        when(extensionRepositoryManager.resolveVersions(installedExtensionId.getId(), 0, -1))
            .thenReturn(new CollectionIterableResult<>(-1, 0, allVersions));

        assertEquals(Arrays.asList(newVersion2, newVersion1),
            this.mocker.getComponentUnderTest().getInstallableVersions(installedExtensionId));
    }

    @Test
    public void getInstallRequest() throws Exception
    {
        InstallRequest installRequest =
            mocker.getComponentUnderTest().getInstallRequest(installedExtensionId, "wiki:test");

        assertEquals(
            Arrays.asList("extension", ExtensionRequest.JOBID_ACTION_PREFIX, installedExtensionId.getId(), "wiki:test"),
            installRequest.getId());
        assertEquals(Collections.singletonList(installedExtensionId), installRequest.getExtensions());
        assertEquals(Collections.singletonList("wiki:test"), installRequest.getNamespaces());
        assertTrue(installRequest.isRootModificationsAllowed());
        assertFalse(installRequest.isInteractive());
        assertEquals(userReference, installRequest.getProperty(AbstractExtensionValidator.PROPERTY_USERREFERENCE));
        assertEquals(userReference.toString(),
            installRequest.getExtensionProperties().get(AbstractExtensionValidator.PROPERTY_USERREFERENCE));
    }

    @Test
    public void tryUpgradeExtensionToLastVersion() throws Exception
    {
        Collection<Version> allVersions = Arrays.asList(installedVersion, newVersion1, newVersion2);
        when(extensionRepositoryManager.resolveVersions(installedExtensionId.getId(), 0, -1))
            .thenReturn(new CollectionIterableResult<>(-1, 0, allVersions));

        Job job = mock(Job.class);
        when(this.jobExecutor.execute(eq(InstallJob.JOBTYPE), any(InstallRequest.class))).thenReturn(job);

        when(this.localization.getTranslationPlain("licensor.notifications.event.done", installedExtension.getName(),
            installedExtensionId.getVersion().getValue(), newVersion2.getValue())).thenReturn("extension upgraded");

        mocker.getComponentUnderTest().tryUpgradeExtensionToLastVersion(installedExtension, "wiki:test");

        verify(job).join();

        verify(observationManager).notify(any(ExtensionAutoUpgradedEvent.class),
            eq(UpgradeExtensionHandler.LICENSOR_API_ID), eq("extension upgraded"));
    }

    @Test
    public void tryUpgradeExtensionToLastVersionWithException() throws Exception
    {
        Collection<Version> allVersions = Arrays.asList(installedVersion, newVersion1, newVersion2);
        when(extensionRepositoryManager.resolveVersions(installedExtensionId.getId(), 0, -1))
            .thenReturn(new CollectionIterableResult<>(-1, 0, allVersions));

        when(this.localization.getTranslationPlain("licensor.notifications.event.failed", installedExtension.getName(),
            installedExtensionId.getVersion().getValue(), newVersion2.getValue())).thenReturn("upgrade failed");

        when(this.jobExecutor.execute(eq(InstallJob.JOBTYPE), any(InstallRequest.class)))
            .thenThrow(new JobException("extension upgrade failed"));

        mocker.getComponentUnderTest().tryUpgradeExtensionToLastVersion(installedExtension, "wiki:test");

        verify(observationManager).notify(any(ExtensionAutoUpgradedEvent.class),
            eq(UpgradeExtensionHandler.LICENSOR_API_ID), eq("upgrade failed"));
    }

    @Test
    public void tryUpgradeExtensionToLastVersionWhenNoVersions() throws Exception
    {
        when(extensionRepositoryManager.resolveVersions(installedExtensionId.getId(), 0, -1))
            .thenReturn(new CollectionIterableResult<>(-1, 0, Collections.emptyList()));

        mocker.getComponentUnderTest().tryUpgradeExtensionToLastVersion(installedExtension, "wiki:test");

        verify(this.jobExecutor, never()).execute(eq(InstallJob.JOBTYPE), any(InstallRequest.class));

        verify(observationManager, never()).notify(any(ExtensionAutoUpgradedEvent.class),
            eq(UpgradeExtensionHandler.LICENSOR_API_ID), any());
    }
}
