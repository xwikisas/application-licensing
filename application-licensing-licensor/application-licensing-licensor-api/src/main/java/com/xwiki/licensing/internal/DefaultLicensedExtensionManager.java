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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.InstalledExtensionRepository;

import com.xwiki.licensing.LicensedExtensionManager;
import com.xwiki.licensing.LicensedFeatureId;

/**
 * The default implementation of {@link LicensedExtensionManager} that looks for backward dependencies of the licensor
 * API extension.
 *
 * @version $Id$
 * @since 1.13.6
 */
@Component
@Singleton
public class DefaultLicensedExtensionManager implements LicensedExtensionManager
{
    /**
     * All licensed extensions must declare a dependency on this extension.
     */
    public static final String LICENSOR_EXTENSION_ID = "com.xwiki.licensing:application-licensing-licensor-api";

    /**
     * Cache the list of installed licensed extensions that are not covered by the license of another extension.
     */
    private Set<ExtensionId> cachedMandatoryLicensedExtensions;

    /**
     * The cached map that includes licensed extensions that were installed as dependencies, and on which licensed
     * extension they depend on.
     */
    private Map<String, Set<String>> cachedLicensedDependenciesMap;

    /**
     * to add.
     */
    private boolean licensedDependenciesMapPrettyName;

    @Inject
    private Logger logger;

    @Inject
    private InstalledExtensionRepository installedExtensionRepository;

    @Override
    public Collection<ExtensionId> getLicensedExtensions()
    {
        return getLicensorBackwardDependencies().values().stream().flatMap(Collection::stream).map(Extension::getId)
            .collect(Collectors.toSet());
    }

    @Override
    public Collection<ExtensionId> getLicensedExtensions(String namespace)
    {
        return Optional.ofNullable(getLicensorBackwardDependencies().get(namespace)).orElse(Collections.emptySet())
            .stream().map(Extension::getId).collect(Collectors.toSet());
    }

    @Override
    public Collection<ExtensionId> getLicensedExtensions(LicensedFeatureId licensedFeatureId)
    {
        List<ExtensionId> coveredExtensions = new ArrayList<>();
        ExtensionDependency extensionDependency = licensedFeatureId.getExtensionDependency();
        for (Extension extension : this.installedExtensionRepository.getInstalledExtensions()) {
            if (extensionDependency.isCompatible(extension)) {
                coveredExtensions.add(extension.getId());
            }
        }
        return coveredExtensions;
    }

    private Map<String, Collection<InstalledExtension>> getLicensorBackwardDependencies()
    {
        // The licensor API extension must be installed on the root namespace.
        InstalledExtension licensorExtension =
            this.installedExtensionRepository.getInstalledExtension(LICENSOR_EXTENSION_ID, null);
        if (licensorExtension != null) {
            try {
                return this.installedExtensionRepository.getBackwardDependencies(licensorExtension.getId());
            } catch (ResolveException e) {
                // This shouldn't happen normally because we resolved the licensor API extension just before this call.
                this.logger.error("Failed to detect the licensed extensions.", e);
            }
        } else {
            // This can happen if the licensor API extension is currently being installed (some components are loaded
            // before the extension is marked as installed).
            this.logger.warn("The Licensor API extension ({}) is not installed on the root namespace as it should."
                + " Licensed extensions won't be detected correctly as a conseuence.", LICENSOR_EXTENSION_ID);
        }

        return Collections.emptyMap();
    }

    @Override
    public Set<ExtensionId> getMandatoryLicensedExtensions()
    {
        Set<ExtensionId> mandatoryLicensedExtensions = this.cachedMandatoryLicensedExtensions;
        if (mandatoryLicensedExtensions == null) {
            mandatoryLicensedExtensions = computeMandatoryLicensedExtensions();
            this.cachedMandatoryLicensedExtensions = mandatoryLicensedExtensions;
        }
        return Collections.unmodifiableSet(mandatoryLicensedExtensions);
    }

    private synchronized Set<ExtensionId> computeMandatoryLicensedExtensions()
    {
        Set<ExtensionId> mandatoryLicensedExtensions = this.cachedMandatoryLicensedExtensions;
        if (mandatoryLicensedExtensions != null) {
            return mandatoryLicensedExtensions;
        }

        Collection<ExtensionId> allLicensedExtensions = getLicensedExtensions();
        // Extensions for which it was verified if the dependencies contain licensed extensions.
        Set<ExtensionId> verifiedExtensions = new HashSet<ExtensionId>();
        mandatoryLicensedExtensions = new HashSet<ExtensionId>(allLicensedExtensions);

        for (ExtensionId extensionId : allLicensedExtensions) {
            InstalledExtension installedExtension =
                this.installedExtensionRepository.getInstalledExtension(extensionId);
            if (installedExtension == null) {
                continue;
            }

            verifiedExtensions.add(extensionId);
            Collection<String> namespaces = installedExtension.getNamespaces();
            if (namespaces == null) {
                searchLicensedDependenciesRecursive(
                    this.installedExtensionRepository.getInstalledExtension(extensionId.getId(), null), null,
                    verifiedExtensions, mandatoryLicensedExtensions);
            } else {
                for (String namespace : namespaces) {
                    searchLicensedDependenciesRecursive(
                        this.installedExtensionRepository.getInstalledExtension(extensionId.getId(), namespace),
                        namespace, verifiedExtensions, mandatoryLicensedExtensions);
                }
            }
        }

        return mandatoryLicensedExtensions;
    }

    private void searchLicensedDependenciesRecursive(InstalledExtension installedExtension, String namespace,
        Collection<ExtensionId> verifiedExtensions, Set<ExtensionId> mandatoryLicensedExtensions)
    {
        Collection<ExtensionDependency> dependencies = installedExtension.getDependencies();
        for (ExtensionDependency dependency : dependencies) {
            InstalledExtension installedDependency =
                this.installedExtensionRepository.getInstalledExtension(dependency.getId(), namespace);
            if (installedDependency == null || dependency.isOptional()) {
                continue;
            }

            ExtensionId dependencyId = installedDependency.getId();
            mandatoryLicensedExtensions.remove(dependencyId);

            if (verifiedExtensions.contains(dependencyId)) {
                continue;
            }
            verifiedExtensions.add(dependencyId);
            searchLicensedDependenciesRecursive(installedDependency, namespace, verifiedExtensions,
                mandatoryLicensedExtensions);
        }
    }

    @Override
    public void invalidateMandatoryLicensedExtensionsCache()
    {
        this.cachedMandatoryLicensedExtensions = null;
    }

    @Override
    public Set<ExtensionId> getLicensedDependencies(InstalledExtension installedExtension, String namespace)
    {
        Set<ExtensionId> licensedDependencies = new HashSet<>();
        Set<ExtensionId> verifiedExtensions = new HashSet<>();

        getLicensedDependencies(installedExtension, namespace, getLicensedExtensions(), licensedDependencies,
            verifiedExtensions);
        logger.debug("Found licensed dependencies for extension [{}] : [{}]", installedExtension.getId(),
            licensedDependencies);

        return licensedDependencies;
    }

    @Override
    public Map<String, Set<String>> getLicensedDependenciesMap()
    {
        return getLicensedDependenciesMap(false);
    }

    @Override
    public Map<String, Set<String>> getLicensedDependenciesMap(boolean prettyName)
    {
        Map<String, Set<String>> licensedExtensionsDepChain = this.cachedLicensedDependenciesMap;
        if (licensedExtensionsDepChain == null) {
            this.licensedDependenciesMapPrettyName = prettyName;
            licensedExtensionsDepChain = computeLicensedDependenciesMap(prettyName);
            this.cachedLicensedDependenciesMap = licensedExtensionsDepChain;
        }
        return licensedExtensionsDepChain;
    }

    @Override
    public void invalidateLicensedDependenciesMap()
    {
        this.cachedLicensedDependenciesMap = null;
    }

    private void getLicensedDependencies(InstalledExtension installedExtension, String namespace,
        Collection<ExtensionId> installedLicensedExtensions, Set<ExtensionId> licensedDependencies,
        Set<ExtensionId> verifiedExtensions)
    {
        Collection<ExtensionDependency> dependencies = installedExtension.getDependencies();

        for (ExtensionDependency dep : dependencies) {
            InstalledExtension installedDep =
                installedExtensionRepository.getInstalledExtension(dep.getId(), namespace);
            if (installedDep == null || licensedDependencies.contains(installedDep.getId())
                || verifiedExtensions.contains(installedDep.getId()))
            {
                continue;
            }

            if (installedLicensedExtensions.contains(installedDep.getId()) && !dep.isOptional()) {
                licensedDependencies.add(installedDep.getId());
            }

            verifiedExtensions.add(installedDep.getId());
            getLicensedDependencies(installedDep, namespace, installedLicensedExtensions, licensedDependencies,
                verifiedExtensions);
        }
    }

    private synchronized Map<String, Set<String>> computeLicensedDependenciesMap(boolean prettyName)
    {
        Map<String, Set<String>> backwardLicensedDependencies = this.cachedLicensedDependenciesMap;
        if (backwardLicensedDependencies != null && prettyName == licensedDependenciesMapPrettyName) {
            return backwardLicensedDependencies;
        }

        Collection<ExtensionId> allLicensedExtensions = getLicensedExtensions();
        backwardLicensedDependencies = new HashMap<>();
        // Extensions for which it was verified if the dependencies contain licensed extensions.
        Set<ExtensionId> verifiedExtensions = new HashSet<ExtensionId>();

        for (ExtensionId extensionId : allLicensedExtensions) {
            InstalledExtension installedExtension =
                this.installedExtensionRepository.getInstalledExtension(extensionId);
            if (installedExtension == null) {
                continue;
            }

            verifiedExtensions.add(extensionId);
            Collection<String> namespaces = installedExtension.getNamespaces();
            if (namespaces == null) {
                InstalledExtension installedExtension1 =
                    this.installedExtensionRepository.getInstalledExtension(extensionId.getId(), null);
                getLicensedDependenciesMap(
                    getInstalledExtensionName(installedExtension1, this.licensedDependenciesMapPrettyName),
                    installedExtension1.getDependencies(), null, allLicensedExtensions, verifiedExtensions,
                    backwardLicensedDependencies);
            } else {
                for (String namespace : namespaces) {
                    InstalledExtension installedExtension1 =
                        this.installedExtensionRepository.getInstalledExtension(extensionId.getId(), namespace);
                    getLicensedDependenciesMap(
                        getInstalledExtensionName(installedExtension1, this.licensedDependenciesMapPrettyName),
                        installedExtension1.getDependencies(), namespace, allLicensedExtensions, verifiedExtensions,
                        backwardLicensedDependencies);
                }
            }
        }

        return backwardLicensedDependencies;
    }

    private void getLicensedDependenciesMap(String topLevelExtensionName,
        Collection<ExtensionDependency> dependencies, String namespace,
        Collection<ExtensionId> installedLicensedExtensions, Set<ExtensionId> verifiedExtensions,
        Map<String, Set<String>> licensedDependenciesChain)
    {
        for (ExtensionDependency dep : dependencies) {
            InstalledExtension installedDep =
                installedExtensionRepository.getInstalledExtension(dep.getId(), namespace);
            if (installedDep == null || dep.isOptional()) {
                continue;
            }

            String extensionName = topLevelExtensionName;
            if (installedLicensedExtensions.contains(installedDep.getId())) {
                addBackwardDependency(topLevelExtensionName, licensedDependenciesChain, installedDep.getId().getId());
                // Only if it's mandatory?
                extensionName = getInstalledExtensionName(installedDep, this.licensedDependenciesMapPrettyName);
            }

            // We already checked its dependencies, but we wanted to save its backwards dependency.
            if (verifiedExtensions.contains(installedDep.getId())) {
                continue;
            }

            verifiedExtensions.add(installedDep.getId());
            getLicensedDependenciesMap(extensionName, installedDep.getDependencies(), namespace,
                installedLicensedExtensions, verifiedExtensions, licensedDependenciesChain);
        }
    }

    private void addBackwardDependency(String topLevelExtensionName, Map<String, Set<String>> licensedDependenciesChain,
        String installedDepId)
    {
        if (licensedDependenciesChain.containsKey(installedDepId)) {
            licensedDependenciesChain.get(installedDepId).add(topLevelExtensionName);
        } else {
            licensedDependenciesChain.put(installedDepId, new HashSet<>(Arrays.asList(topLevelExtensionName)));
        }
    }

    private String getInstalledExtensionName(InstalledExtension installedExtension, boolean prettyName)
    {
        return prettyName ? installedExtension.getName() : installedExtension.getId().getId();
    }
}
