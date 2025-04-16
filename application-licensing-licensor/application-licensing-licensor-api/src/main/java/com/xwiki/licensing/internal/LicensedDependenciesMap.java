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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;

/**
 * Get a dependency map for licensed applications. The scope is to know for licensed extensions that where installed as
 * dependencies, on which licensed application (and so on which license) they depend on, considering transitive
 * dependencies. Optional dependencies will not be included, since that means it's license is not covered by the
 * parent's license.
 *
 * @version $Id$
 * @since 1.29
 */
@Component(roles = LicensedDependenciesMap.class)
@Singleton
public class LicensedDependenciesMap
{
    /**
     * The cached map that includes licensed extensions that were installed as dependencies, and on which licensed
     * extension they depend on.
     */
    private Map<String, Set<LicensedExtensionParent>> cachedLicensedDependenciesMap;

    /**
     * Utility class for holding information about the top level licensed extension. Such an extension will have
     * other licensed extensions as direct or transitive dependencies.
     */
    public static class LicensedExtensionParent
    {
        private final String extensionName;

        private final String extensionId;

        private final String namespace;

        /**
         * Constructor.
         *
         * @param extensionName extension name
         * @param extensionId extension ID
         * @param namespace extension namespace
         */
        public LicensedExtensionParent(String extensionName, String extensionId, String namespace)
        {
            this.extensionName = extensionName;
            this.extensionId = extensionId;
            this.namespace = namespace;
        }

        /**
         * @return extension ID
         */
        public String getExtensionId()
        {
            return extensionId;
        }

        /**
         * @return extension name
         */
        public String getExtensionName()
        {
            return extensionName;
        }

        /**
         * @return extension namespace
         */
        public String getExtensionNamespace()
        {
            return namespace;
        }
    }

    @Inject
    private Logger logger;

    @Inject
    private InstalledExtensionRepository installedExtensionRepository;

    /**
     * @param licensedExtensions the list of all licensed extensions
     * @return the licensed dependencies map
     */
    public Map<String, Set<LicensedExtensionParent>> get(Collection<ExtensionId> licensedExtensions)
    {
        if (this.cachedLicensedDependenciesMap == null) {
            logger.debug("Licensed dependencies map is not cached, computing it.");
            this.cachedLicensedDependenciesMap = computeLicensedDependenciesMap(licensedExtensions);
        } else {
            logger.debug("Licensed dependencies map is cached, returning it.");
        }
        return this.cachedLicensedDependenciesMap;
    }

    /**
     * Invalidate the cached map of licensed dependencies.
     */
    public void invalidateCache()
    {
        logger.debug("Clear licensed dependency map cache.");
        this.cachedLicensedDependenciesMap = null;
    }

    private synchronized Map<String, Set<LicensedExtensionParent>> computeLicensedDependenciesMap(
        Collection<ExtensionId> allLicensedExtensions)
    {
        Map<String, Set<LicensedExtensionParent>> licensedDependenciesMap = new HashMap<>();
        // Extensions for which we already checked dependencies.
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
                logger.debug("Computing licensed dependencies map for [{}] on root namespace.", extensionId);
                installedExtension = this.installedExtensionRepository.getInstalledExtension(extensionId.getId(), null);
                getLicensedDependenciesMapRecursive(
                    new LicensedExtensionParent(installedExtension.getName(), installedExtension.getId().getId(), null),
                    installedExtension.getDependencies(), allLicensedExtensions, verifiedExtensions,
                    licensedDependenciesMap);
            } else {
                for (String namespace : namespaces) {
                    logger.debug("Computing licensed dependencies map for [{}] on namespace [{}]", extensionId,
                        namespace);
                    installedExtension =
                        this.installedExtensionRepository.getInstalledExtension(extensionId.getId(), namespace);
                    getLicensedDependenciesMapRecursive(
                        new LicensedExtensionParent(installedExtension.getName(), installedExtension.getId().getId(),
                            namespace), installedExtension.getDependencies(), allLicensedExtensions, verifiedExtensions,
                        licensedDependenciesMap);
                }
            }
        }

        logger.debug("Computed map of licensed dependencies: [{}]", licensedDependenciesMap);
        return licensedDependenciesMap;
    }

    /**
     * Traverse recursively the dependency tree in order to collect the backward dependency map.
     *
     * @param licensedExtensionParent the licensed application that has the current extension as dependency
     * @param dependencies dependency list to be checked
     * @param installedLicensedExtensions list of all licensed extensions
     * @param verifiedExtensions extensions whose dependencies were already checked
     * @param licensedDependenciesChain a {@code Map} with found licensed dependencies and their parent
     */
    private void getLicensedDependenciesMapRecursive(LicensedExtensionParent licensedExtensionParent,
        Collection<ExtensionDependency> dependencies, Collection<ExtensionId> installedLicensedExtensions,
        Set<ExtensionId> verifiedExtensions, Map<String, Set<LicensedExtensionParent>> licensedDependenciesChain)
    {
        String namespace = licensedExtensionParent.getExtensionNamespace();
        for (ExtensionDependency dep : dependencies) {
            InstalledExtension installedDep =
                installedExtensionRepository.getInstalledExtension(dep.getId(), namespace);
            if (installedDep == null || dep.isOptional()) {
                continue;
            }

            LicensedExtensionParent extensionParent = licensedExtensionParent;
            if (installedLicensedExtensions.contains(installedDep.getId())) {
                addBackwardDependency(licensedExtensionParent, licensedDependenciesChain, installedDep.getId().getId());
                // If A->C and B->C, then save A,B. If A->B->C, keep only B.
                // Only if it's mandatory? If A->B->C , and B is not mandatory, keep A or B? I would say only B, to
                // be checked. Each extension to their own licensed parent.
                extensionParent =
                    new LicensedExtensionParent(installedDep.getName(), installedDep.getId().getId(), namespace);
            }

            // We already checked its dependencies, so stop here.
            if (verifiedExtensions.contains(installedDep.getId())) {
                continue;
            }

            verifiedExtensions.add(installedDep.getId());
            getLicensedDependenciesMapRecursive(extensionParent, installedDep.getDependencies(),
                installedLicensedExtensions, verifiedExtensions, licensedDependenciesChain);
        }
    }

    private void addBackwardDependency(LicensedExtensionParent licensedExtensionParent,
        Map<String, Set<LicensedExtensionParent>> licensedDependenciesMap, String installedDepId)
    {
        if (licensedDependenciesMap.containsKey(installedDepId)) {
            licensedDependenciesMap.get(installedDepId).add(licensedExtensionParent);
        } else {
            // An extension could be a dependency of multiple extensions.
            licensedDependenciesMap.put(installedDepId, new HashSet<>(Arrays.asList(licensedExtensionParent)));
        }
    }
}
