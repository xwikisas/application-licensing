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
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
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
import org.xwiki.extension.version.Version.Type;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.ObservationManager;

import com.xwiki.licensing.LicensingConfiguration;
import com.xwiki.licensing.internal.upgrades.notifications.ExtensionAutoUpgradedEvent;
import com.xwiki.licensing.internal.upgrades.notifications.ExtensionAutoUpgradedFailedEvent;

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
    protected static final String LICENSOR_API_ID = "com.xwiki.licensing:application-licensing-licensor-api";

    @Inject
    protected DocumentAccessBridge documentAccessBridge;

    @Inject
    protected JobExecutor jobExecutor;

    @Inject
    private Logger logger;

    @Inject
    private ExtensionRepositoryManager extensionRepositoryManager;

    /** Used for managing event notifications. */
    @Inject
    private ObservationManager observationManager;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @Inject
    private ContextualLocalizationManager localization;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private LicensingConfiguration licensingConfig;


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
            try {
                installExtension(toInstallExtensionId, namespace);

                String doneUpgradeMessage = this.localization.getTranslationPlain(
                    "licensor.notification.autoUpgrade.done", installedExtension.getName(),
                    installedExtensionId.getVersion().getValue(), toInstallExtensionId.getVersion().getValue());
                Set<String> notifiedGroups = getTargetGroups();
                this.observationManager.notify(new ExtensionAutoUpgradedEvent(notifiedGroups), LICENSOR_API_ID,
                    doneUpgradeMessage);

                // If the execution gets here, it means that the upgrade was done.
                break;
            } catch (JobException | InterruptedException e) {
                String failedUpgradeMessage = this.localization.getTranslationPlain(
                    "licensor.notification.autoUpgrade.failed", installedExtension.getName(),
                    installedExtensionId.getVersion().getValue(), toInstallExtensionId.getVersion().getValue());

                this.observationManager.notify(new ExtensionAutoUpgradedFailedEvent(), LICENSOR_API_ID,
                    failedUpgradeMessage);
            }
        }
    }

    private Set<String> getTargetGroups()
    {
        Set<String> notifiedGroups = licensingConfig.getNotifiedGroupsSet();
        DocumentReference adminGroupDoc = currentDocumentReferenceResolver.resolve("XWiki.XWikiAdminGroup");
        String adminGroup = serializer.serialize(adminGroupDoc);
        notifiedGroups.add(adminGroup);
        return notifiedGroups;
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
        InstallRequest installRequest = getInstallRequest(extensionId, namespace);

        Job job = this.jobExecutor.execute(InstallJob.JOBTYPE, installRequest);
        job.join();
        return job;
    }

    /**
     * Create an install plan.
     *
     * @param extensionId the extension to be installed
     * @param namespace the namespace where the extension will be installed
     * @return the install request
     */
    protected InstallRequest getInstallRequest(ExtensionId extensionId, String namespace)
    {
        // Create install plan.
        InstallRequest installRequest = new InstallRequest();
        installRequest
            .setId(ExtensionRequest.getJobId(ExtensionRequest.JOBID_ACTION_PREFIX, extensionId.getId(), namespace));
        installRequest.addExtension(extensionId);
        installRequest.addNamespace(namespace);

        // Indicate it's allowed to do modification on root namespace.
        installRequest.setRootModificationsAllowed(true);

        // Prevent the install job from asking questions because we want the install to be automatic, without user
        // interaction.
        installRequest.setInteractive(false);

        // Set the author to use.
        installRequest.setProperty(AbstractExtensionValidator.PROPERTY_USERREFERENCE,
            this.documentAccessBridge.getCurrentUserReference());
        // We set the string value because the extension repository doesn't know how to serialize/parse an extension
        // property whose value is a DocumentReference.
        installRequest.setExtensionProperty(AbstractExtensionValidator.PROPERTY_USERREFERENCE,
            this.serializer.serialize(this.documentAccessBridge.getCurrentUserReference()));
        return installRequest;
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

            // Take only stable versions greater than the already installed one.
            for (Version version : iterableVersions) {
                if (extensionId.getVersion().compareTo(version) < 0 && version.getType() == Type.STABLE) {
                    versions.add(version);
                }
            }

            Collections.reverse(versions);
        } catch (ResolveException e) {
            logger.warn("Failed to resolve versions of extension [{}]. Root cause is [{}]", extensionId.getId(),
                ExceptionUtils.getRootCauseMessage(e));
        }

        return versions;
    }
}
