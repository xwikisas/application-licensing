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
import java.util.Collection;
import java.util.Collections;
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
    public Collection<ExtensionId> getMandatoryLicensedExtensions()
    {
        Collection<ExtensionId> paidExtensions = getLicensedExtensions();

        Set<ExtensionId> visiblePaidExtensions = new HashSet<ExtensionId>(paidExtensions);
        for (ExtensionId extensionId : paidExtensions) {
            InstalledExtension installedExtension = installedExtensionRepository.getInstalledExtension(extensionId);

            Collection<ExtensionDependency> dependencies = installedExtension.getDependencies();
            for (ExtensionId paidExtension : paidExtensions) {
                if (dependencies.stream().filter(dependency -> dependency.getId().equals(paidExtension.getId()))
                    .findFirst().isPresent()) {
                    visiblePaidExtensions.remove(paidExtension);
                }
            }
        }

        return visiblePaidExtensions;
    }
}
