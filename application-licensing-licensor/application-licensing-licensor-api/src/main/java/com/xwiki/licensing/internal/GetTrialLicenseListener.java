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
import java.util.List;
import java.util.Stack;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.job.event.JobFinishedEvent;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xwiki.licensing.LicensedExtensionManager;

/**
 * Generate a trial license for paid extensions after install or upgrade, if the data necessary for it is already filled
 * up and there isn't already an existing license.
 *
 * @since 1.17
 * @version $Id$
 */
@Component
@Named(GetTrialLicenseListener.NAME)
@Singleton
public class GetTrialLicenseListener implements EventListener
{
    protected static final String NAME = "GetTrialLicenseListener";

    protected static final List<Event> EVENTS = Arrays.asList(new JobFinishedEvent("install"));

    @Inject
    private TrialLicenseGenerator trialLicenseGenerator;

    @Inject
    private InstalledExtensionRepository installedExtensionRepository;

    @Inject
    private LicensedExtensionManager licensedExtensionManager;

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        List<ExtensionId> extensions = ((JobFinishedEvent) event).getRequest().getProperty("extensions");
        // Retrieve license updates to be sure that we don't override an existing license.
        trialLicenseGenerator.updateLicenses();

        for (ExtensionId extensionId : extensions) {
            InstalledExtension installedExtension = installedExtensionRepository.getInstalledExtension(extensionId);
            Stack<ExtensionId> dependencyPath = new Stack<>();
            dependencyPath.push(extensionId);
            tryGenerateTrialLicenseRecursive(dependencyPath, installedExtension.getNamespaces());
        }

        licensedExtensionManager.invalidateMandatoryLicensedExtensionsCache();
    }

    /**
     * Try to generate a trial license for the given extension. Since a free extension has no license to cover it's
     * dependencies, check also to see if there aren't any paid dependencies, direct or transitive, that need a trial
     * license.
     *
     * @param dependencyPath the extensions for which to generate a trial license
     * @param extensionNamespaces the namespaces where this extension is installed
     */
    private void tryGenerateTrialLicenseRecursive(Stack<ExtensionId> dependencyPath,
        Collection<String> extensionNamespaces)
    {
        ExtensionId extensionId = dependencyPath.peek();
        if (trialLicenseGenerator.canGenerateTrialLicense(extensionId)) {
            trialLicenseGenerator.generateTrialLicense(extensionId);
        } else {
            if (extensionNamespaces == null) {
                checkDependenciesForTrialLicense(extensionId, null, dependencyPath);
            } else {
                for (String namespace : extensionNamespaces) {
                    checkDependenciesForTrialLicense(extensionId, namespace, dependencyPath);
                }
            }
        }
    }

    private void checkDependenciesForTrialLicense(ExtensionId extensionId, String namespace,
        Stack<ExtensionId> dependencyPath)
    {
        InstalledExtension installedExtension =
            installedExtensionRepository.getInstalledExtension(extensionId.getId(), namespace);
        if (installedExtension == null) {
            return;
        }

        Collection<ExtensionDependency> dependencies = installedExtension.getDependencies();

        for (ExtensionDependency dependency : dependencies) {
            InstalledExtension installedDependency =
                installedExtensionRepository.getInstalledExtension(dependency.getId(), namespace);
            if (installedDependency != null && dependencyPath.search(installedDependency.getId()) == -1) {
                dependencyPath.push(installedDependency.getId());
                tryGenerateTrialLicenseRecursive(dependencyPath,
                    namespace != null ? Arrays.asList(namespace) : null);
            }
        }
    }
}
