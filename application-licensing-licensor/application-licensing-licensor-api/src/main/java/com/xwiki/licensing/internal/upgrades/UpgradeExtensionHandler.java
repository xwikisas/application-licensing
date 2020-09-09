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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.internal.validator.AbstractExtensionValidator;
import org.xwiki.extension.job.ExtensionRequest;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.internal.InstallJob;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.version.Version;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;

/**
 * Upgrades an extension from a namespace to the last compatible version
 * 
 * @since 1.17
 */
@Component(roles = UpgradeExtensionHandler.class)
@Singleton
public class UpgradeExtensionHandler
{
    @Inject
    protected DocumentAccessBridge documentAccessBridge;

    @Inject
    protected JobExecutor jobExecutor;

    @Inject
    private Logger logger;

    @Inject
    private ExtensionRepositoryManager extensionRepositoryManager;

    // private List<ExtensionId> failedInstalledExtensions;

    /**
     * Try upgrading the given extension to the last compatible version.
     * 
     * @param installedExtensionId ExtensionId of the already installed extension
     * @param namespace namespace in which the extension is installed
     */
    public void tryUpgradeExtensionToLastVersion(ExtensionId installedExtensionId, String namespace)
    {
        List<Version> versions = getInstallableVersions(installedExtensionId);

        for (Version version : versions) {
            ExtensionId toInstallExtensionId = new ExtensionId(installedExtensionId.getId(), version);
            Boolean upgradeDone = true;
            try {
                Job job = installExtension(toInstallExtensionId, namespace);
                logger.info("Upgrade done for [{}] from version [{}] to [{}]", installedExtensionId.getId(),
                    installedExtensionId.getVersion().getValue(), version.getValue());
            } catch (JobException | InterruptedException e) {
                logger.error("Error while upgrading [{}] from version [{}] to [{}] ", installedExtensionId.getId(),
                    installedExtensionId.getVersion().getValue(), version.getValue(), e);
                upgradeDone = false;
            }
            if (upgradeDone) {
                break;
            }
        }
    }

    /**
     * Install the given extension.
     * 
     * @param extensionId extension to install
     * @param namespace namespace where the install is done
     * @throws JobException error at job execution
     * @throws InterruptedException if any thread has interrupted the current thread
     */
    protected Job installExtension(ExtensionId extensionId, String namespace) throws JobException, InterruptedException
    {
        // Create install plan.
        InstallRequest installRequest = new InstallRequest();
        installRequest
            .setId(ExtensionRequest.getJobId(ExtensionRequest.JOBID_ACTION_PREFIX, extensionId.getId(), namespace));
        installRequest.addExtension(extensionId);
        installRequest.addNamespace(namespace);

        // Indicate it's allowed to do modification on root namespace.
        installRequest.setRootModificationsAllowed(true);

        // Make sure the job is not interactive.
        installRequest.setInteractive(false);

        // Set the author to use.
        installRequest.setProperty(AbstractExtensionValidator.PROPERTY_USERREFERENCE,
            this.documentAccessBridge.getCurrentUserReference());
        // We set the string value because the extension repository doesn't know how to serialize/parse an extension
        // property whose value is a DocumentReference.
        installRequest.setExtensionProperty(AbstractExtensionValidator.PROPERTY_USERREFERENCE,
            this.documentAccessBridge.getCurrentUserReference().toString());

        Job job = this.jobExecutor.execute(InstallJob.JOBTYPE, installRequest);
        job.join();
        return job;
    }

    /**
     * Get the reversed list of versions that can be installed, considering the already installed version.
     * 
     * @param extensionId ExtensionId of the application that is needed.
     * @return reversed list of versions until the already installed one
     */
    public List<Version> getInstallableVersions(ExtensionId extensionId)
    {
        List<Version> versions = new ArrayList<Version>();

        try {
            IterableResult<Version> iterableVersions =
                extensionRepositoryManager.resolveVersions(extensionId.getId(), 0, -1);
            Boolean found = false;

            // Take only versions greater than the already installed one.
            for (Version version : iterableVersions) {
                if (found) {
                    versions.add(version);
                }
                if (extensionId.getVersion().compareTo(version) == 0) {
                    found = true;
                }
            }

            Collections.reverse(versions);
        } catch (ResolveException e) {
            logger.error("Failed to resolve versions of extension [{}]", extensionId.getId(), e);
        }

        return versions;
    }
}
