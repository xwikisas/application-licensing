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
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xwiki.licensing.internal.upgrades.notifications.ExtensionAutoUpgradedEvent;
import com.xwiki.licensing.internal.upgrades.notifications.ExtensionAutoUpgradedFailedEvent;

/**
 * Unit tests for {@link UpgradeExtensionHandler}.
 *
 * @version $Id$
 * @since 1.17
 */
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

    private ExtensionId installedExtensionId;

    private Version installedVersion;

    private Version newVersion1;

    private Version newVersion2;

    private DocumentReference userReference;

    @Before
    public void configure() throws Exception
    {
        this.extensionRepositoryManager = this.mocker.getInstance(ExtensionRepositoryManager.class);

        this.observationManager = this.mocker.getInstance(ObservationManager.class);

        this.jobExecutor = this.mocker.getInstance(JobExecutor.class);

        this.localization = this.mocker.getInstance(ContextualLocalizationManager.class);

        this.installedVersion = new DefaultVersion("1.0");
        this.installedExtensionId = new ExtensionId("application", this.installedVersion);
        this.newVersion1 = new DefaultVersion("2.0");
        this.newVersion2 = new DefaultVersion("3.0");

        when(this.installedExtension.getId()).thenReturn(this.installedExtensionId);

        DocumentAccessBridge documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);
        this.userReference = new DocumentReference("wiki", Arrays.asList("XWiki"), "UserName");
        when(documentAccessBridge.getCurrentUserReference()).thenReturn(this.userReference);

        EntityReferenceSerializer<String> serializer = this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        when(serializer.serialize(userReference)).thenReturn("wiki:XWiki.UserName");
    }

    @Test
    public void getInstallableVersionsWithEmptyList() throws Exception
    {
        Collection<Version> allVersions = Arrays.asList(this.installedVersion);
        when(extensionRepositoryManager.resolveVersions(this.installedExtensionId.getId(), 0, -1))
            .thenReturn(new CollectionIterableResult<>(-1, 0, allVersions));

        assertEquals(Collections.emptyList(),
            this.mocker.getComponentUnderTest().getInstallableVersions(this.installedExtensionId));
    }

    @Test
    public void getInstallableVersionsWithoutInstalledVersion() throws Exception
    {
        Collection<Version> allVersions = Arrays.asList(this.installedVersion, this.newVersion1);
        when(extensionRepositoryManager.resolveVersions(this.installedExtensionId.getId(), 0, -1))
            .thenReturn(new CollectionIterableResult<>(-1, 0, allVersions));

        assertEquals(Arrays.asList(this.newVersion1),
            this.mocker.getComponentUnderTest().getInstallableVersions(this.installedExtensionId));
    }

    @Test
    public void getInstallableVersionsInCorrectOrder() throws Exception
    {
        Collection<Version> allVersions = Arrays.asList(this.installedVersion, this.newVersion1, this.newVersion2);
        when(extensionRepositoryManager.resolveVersions(this.installedExtensionId.getId(), 0, -1))
            .thenReturn(new CollectionIterableResult<>(-1, 0, allVersions));

        assertEquals(Arrays.asList(this.newVersion2, this.newVersion1),
            this.mocker.getComponentUnderTest().getInstallableVersions(this.installedExtensionId));
    }

    @Test
    public void getInstallableVersionsWithoutBetaVersion() throws Exception
    {
        Version betaVersion = new DefaultVersion("3.0-rc-1");
        Collection<Version> allVersions = Arrays.asList(this.installedVersion, betaVersion);

        when(extensionRepositoryManager.resolveVersions(this.installedExtensionId.getId(), 0, -1))
            .thenReturn(new CollectionIterableResult<>(-1, 0, allVersions));

        assertEquals(Collections.emptyList(),
            this.mocker.getComponentUnderTest().getInstallableVersions(this.installedExtensionId));
    }

    @Test
    public void getInstallRequest() throws Exception
    {
        InstallRequest installRequest =
            this.mocker.getComponentUnderTest().getInstallRequest(this.installedExtensionId, "wiki:test");

        assertEquals(Arrays.asList("extension", ExtensionRequest.JOBID_ACTION_PREFIX, this.installedExtensionId.getId(),
            "wiki:test"), installRequest.getId());
        assertEquals(Collections.singletonList(this.installedExtensionId), installRequest.getExtensions());
        assertEquals(Collections.singletonList("wiki:test"), installRequest.getNamespaces());
        assertTrue(installRequest.isRootModificationsAllowed());
        assertFalse(installRequest.isInteractive());
        assertEquals(this.userReference, installRequest.getProperty(AbstractExtensionValidator.PROPERTY_USERREFERENCE));
        assertEquals(this.userReference.toString(),
            installRequest.getExtensionProperties().get(AbstractExtensionValidator.PROPERTY_USERREFERENCE));
    }

    @Test
    public void tryUpgradeExtensionToLastVersionWithCorrectVersion() throws Exception
    {
        Collection<Version> allVersions = Arrays.asList(this.installedVersion, this.newVersion1, this.newVersion2);
        when(this.extensionRepositoryManager.resolveVersions(this.installedExtensionId.getId(), 0, -1))
            .thenReturn(new CollectionIterableResult<>(-1, 0, allVersions));

        Job job = mock(Job.class);
        when(this.jobExecutor.execute(eq(InstallJob.JOBTYPE), any(InstallRequest.class))).thenReturn(job);

        when(this.localization.getTranslationPlain("licensor.notification.autoUpgrade.done",
            this.installedExtension.getName(), this.installedExtensionId.getVersion().getValue(),
            this.newVersion2.getValue())).thenReturn("extension upgraded");

        this.mocker.getComponentUnderTest().tryUpgradeExtensionToLastVersion(this.installedExtension, "wiki:test");

        verify(job).join();

        verify(this.observationManager).notify(any(ExtensionAutoUpgradedEvent.class),
            eq(UpgradeExtensionHandler.LICENSOR_API_ID), eq("extension upgraded"));
    }

    @Test
    public void tryUpgradeExtensionToLastVersionWithException() throws Exception
    {
        Collection<Version> allVersions = Arrays.asList(this.installedVersion, this.newVersion1, this.newVersion2);
        when(this.extensionRepositoryManager.resolveVersions(this.installedExtensionId.getId(), 0, -1))
            .thenReturn(new CollectionIterableResult<>(-1, 0, allVersions));

        when(this.localization.getTranslationPlain("licensor.notification.autoUpgrade.failed",
            this.installedExtension.getName(), this.installedExtensionId.getVersion().getValue(),
            this.newVersion2.getValue())).thenReturn("upgrade failed");

        when(this.jobExecutor.execute(eq(InstallJob.JOBTYPE), any(InstallRequest.class)))
            .thenThrow(new JobException("extension upgrade failed"));

        this.mocker.getComponentUnderTest().tryUpgradeExtensionToLastVersion(this.installedExtension, "wiki:test");

        verify(this.observationManager).notify(any(ExtensionAutoUpgradedFailedEvent.class),
            eq(UpgradeExtensionHandler.LICENSOR_API_ID), eq("upgrade failed"));
    }

    @Test
    public void tryUpgradeExtensionToLastVersionWhenNoVersions() throws Exception
    {
        when(this.extensionRepositoryManager.resolveVersions(this.installedExtensionId.getId(), 0, -1))
            .thenReturn(new CollectionIterableResult<>(-1, 0, Collections.emptyList()));

        this.mocker.getComponentUnderTest().tryUpgradeExtensionToLastVersion(this.installedExtension, "wiki:test");

        verify(this.jobExecutor, never()).execute(eq(InstallJob.JOBTYPE), any(InstallRequest.class));

        verify(this.observationManager, never()).notify(any(ExtensionAutoUpgradedEvent.class),
            eq(UpgradeExtensionHandler.LICENSOR_API_ID), any());
    }
}
