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
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
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
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.ObservationManager;

import com.xwiki.licensing.internal.upgrades.notifications.ExtensionAutoUpgradedEvent;

/**
 * Upgrades an extension from a namespace to the last compatible version and sends a notification. The notification will
 * be displayed only for users that subscribed to it and disabled the System filter since it is send by superadmin user.
 * 
 * @version $Id$
 * @since 1.17
 */
@Component(roles = UpgradeExtensionHandler.class)
@Singleton
public class UpgradeExtensionHandler
{
    /**
     * The id of application-licensing-licensor-api module.
     */
    private static final String LICENSOR_API_ID = "com.xwiki.licensing:application-licensing-licensor-api";

    @Inject
    protected DocumentAccessBridge documentAccessBridge;

    @Inject
    protected JobExecutor jobExecutor;

    @Inject
    private Logger logger;

    @Inject
    private ExtensionRepositoryManager extensionRepositoryManager;

    /** The default factory for creating event objects. */
    @Inject
    private ObservationManager observationManager;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @Inject
    private ContextualLocalizationManager localization;

    /**
     * Try upgrading an extension inside a namespace to the last compatible version.
     * 
     * @param installedExtension the already installed extension
     * @param namespace the namespace in which the extension is installed
     */
    public void tryUpgradeExtensionToLastVersion(InstalledExtension installedExtension, String namespace)
    {
        ExtensionId installedExtensionId = installedExtension.getId();
        // Use the reversed list of versions since the last compatible version is targeted.
        List<Version> versions = getInstallableVersions(installedExtensionId);

        for (Version version : versions) {
            ExtensionId toInstallExtensionId = new ExtensionId(installedExtensionId.getId(), version);
            Boolean upgradeDone = true;
            try {
                installExtension(toInstallExtensionId, namespace);

                String doneUpgradeMessage = this.localization.getTranslationPlain("licensor.notifications.event.done",
                    installedExtension.getName(), installedExtensionId.getVersion().getValue(),
                    toInstallExtensionId.getVersion().getValue());

                this.observationManager.notify(new ExtensionAutoUpgradedEvent(), LICENSOR_API_ID, doneUpgradeMessage);
            } catch (JobException | InterruptedException e) {
                String failedUpgradeMessage = this.localization.getTranslationPlain(
                    "licensor.notifications.event.failed", installedExtension.getName(),
                    installedExtensionId.getVersion().getValue(), toInstallExtensionId.getVersion().getValue());

                this.observationManager.notify(new ExtensionAutoUpgradedEvent(), LICENSOR_API_ID, failedUpgradeMessage);
                upgradeDone = false;
            }

            if (upgradeDone) {
                break;
            }
        }
    }

    /**
     * Install the given extension inside a namespace.
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
     * @param extensionId ExtensionId of the application that is needed
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
