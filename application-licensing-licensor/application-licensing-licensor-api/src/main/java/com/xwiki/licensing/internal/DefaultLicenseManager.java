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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;

import com.xwiki.licensing.FileLicenseStoreReference;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseId;
import com.xwiki.licensing.LicenseManager;
import com.xwiki.licensing.LicenseStore;
import com.xwiki.licensing.LicenseStoreReference;
import com.xwiki.licensing.LicenseValidator;
import com.xwiki.licensing.LicensedFeatureId;
import com.xwiki.licensing.LicensingConfiguration;
import com.xwiki.licensing.internal.enforcer.LicensingSecurityCacheRuleInvalidator;
import com.xwiki.licensing.internal.enforcer.LicensingUtils;

/**
 * Default implementation of the {@link LicenseManager} role.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultLicenseManager implements LicenseManager, Initializable
{
    @Inject
    private Logger logger;

    @Inject
    private LicensingConfiguration configuration;

    @Inject
    @Named("FileSystem")
    private LicenseStore store;

    @Inject
    private InstalledExtensionRepository installedExtensionRepository;

    @Inject
    @Named(XarExtensionHandler.TYPE)
    private InstalledExtensionRepository xarInstalledExtensionRepository;

    @Inject
    private LicenseValidator licenseValidator;

    @Inject
    private LicensingSecurityCacheRuleInvalidator licensingSecurityCacheRuleInvalidator;

    private final Map<LicenseId, License> licenses = new HashMap<>();

    private final Map<LicenseId, Integer> licensesUsage = new HashMap<>();

    private final Map<LicensedFeatureId, License> featureToLicense = new HashMap<>();

    private final Map<ExtensionId, License> extensionToLicense = new HashMap<>();

    private ExtensionId licensorExtensionId;

    private LicenseStoreReference storeReference;

    @Override
    public void initialize() throws InitializationException
    {
        if (!LicensingUtils.isPristineImpl(licenseValidator)) {
            licenseValidator = LicenseValidator.INVALIDATOR;
        }

        this.licensorExtensionId = installedExtensionRepository
            .getInstalledExtension("com.xwiki.licensing:application-licensing-licensor-api", null).getId();

        logger.debug("About to load registered licenses");
        this.storeReference = new FileLicenseStoreReference(configuration.getLocalStorePath(), true);
        for (License license : store.getIterable(storeReference)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Registering license [{}]", license.getId());
            }
            try {
                linkLicenseToLicensedFeature(license);
            } catch (RuntimeException e) {
                logger.warn("Error retrieving license, license has been skipped.", e.getCause());
            }
        }

        for (Map.Entry<LicensedFeatureId, License> entry : featureToLicense.entrySet()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Associating license [{}] for feature [{}]", entry.getValue().getId(), entry.getKey());
            }
            linkLicenseToInstalledExtensions(entry.getKey(), entry.getValue());
        }

        Collection<ExtensionId> licensed = getLicensedExtensions();

        // Add Unlicensed licenses to all extension installed that are under license
        for (ExtensionId id : licensed) {
            if (extensionToLicense.putIfAbsent(id, License.UNLICENSED) == null) {
                logger.debug("Mark extension [{}] unlicensed", id);
            }
        }

        // Important: don't clear the security cache here by calling clearSecurityCacheForXarExtensions(licensed)
        // since that would lock XWiki at startup since this method is called when the Security system is initializing
        // and thus a lock is set up and the implementation of LicensingSecurityCacheRuleInvalidator is also asking
        // for the same lock...
        // See LICENSING-57
    }

    private Collection<ExtensionId> linkLicenseToInstalledExtensions(Collection<LicensedFeatureId> licIds,
        License licenseTolink)
    {
        Set<ExtensionId> extensionIds = new HashSet<>();
        for (LicensedFeatureId licId : licIds) {
            extensionIds.addAll(linkLicenseToInstalledExtensions(licId, licenseTolink));
        }
        return extensionIds;
    }

    private Collection<ExtensionId> linkLicenseToInstalledExtensions(LicensedFeatureId licId, License licenseTolink)
    {
        Set<ExtensionId> extensionIds = new HashSet<>();
        License license = licenseTolink;
        for (Extension extension : getInstalledExtensions(licId)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Analyze license [{}] for extension [{}]", license.getId(), extension.getId());
            }
            License existingLicense = extensionToLicense.get(extension.getId());

            // If already licensed using another license, get the best of both license
            if (existingLicense != null && license != existingLicense) {
                license = License.getOptimumLicense(existingLicense, license);
            }

            // If the new license is better
            if (license != existingLicense) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Register license [{}] for extension [{}]", license.getId(), extension.getId());
                }
                // Register the new license for this extension
                registerLicense(extension.getId(), license);
                extensionIds.add(extension.getId());
            }
        }
        return extensionIds;
    }

    private synchronized Collection<LicensedFeatureId> linkLicenseToLicensedFeature(License license)
    {
        Collection<LicensedFeatureId> licensedFeatureIds = new ArrayList<>();
        if (licenseValidator.isApplicable(license) && licenseValidator.isSigned(license)) {
            if (logger.isDebugEnabled()) {
                logger.debug("License [{}] is applicable to this wiki instance", license.getId());
            }
            for (LicensedFeatureId extId : license.getFeatureIds()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Analyze license [{}] for feature [{}]", license.getId(), extId);
                }
                License existingLicense = featureToLicense.get(extId);
                License newLicense = license;

                // If already licensed somehow, get the best of both license
                if (existingLicense != null) {
                    newLicense = License.getOptimumLicense(existingLicense, license);
                }

                // If the new license is the best
                if (newLicense != existingLicense) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Linking license [{}] to feature [{}]", newLicense.getId(), extId);
                    }
                    replaceLicense(extId, existingLicense, newLicense);
                    licensedFeatureIds.add(extId);
                }
            }
        } else if (logger.isDebugEnabled()) {
            logger.debug("License [{}] is NOT applicable to this wiki instance", license.getId());
        }
        return licensedFeatureIds;
    }

    private List<Extension> getInstalledExtensions(LicensedFeatureId licensedFeatureId)
    {
        List<Extension> coveredExtensions = new ArrayList<>();
        ExtensionDependency extensionDependency = licensedFeatureId.getExtensionDependency();
        for (Extension extension : this.installedExtensionRepository.getInstalledExtensions()) {
            if (extensionDependency.isCompatible(extension)) {
                coveredExtensions.add(extension);
            }
        }
        return coveredExtensions;
    }

    private void registerLicense(ExtensionId extId, License license)
    {
        extensionToLicense.put(extId, license);
        clearSecurityCacheForXarExtension(extId);
    }

    private void replaceLicense(LicensedFeatureId extId, License existingLicense, License newLicense)
    {
        // Register the new license for this extension
        featureToLicense.put(extId, newLicense);

        Integer usage = licensesUsage.get(newLicense.getId());
        if (usage == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Initialize usage of license [{}] to [1]", newLicense.getId());
            }
            // Initialize the first usage of this new license
            licenses.put(newLicense.getId(), newLicense);
            licensesUsage.put(newLicense.getId(), 1);
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Increment usage of license [{}] to [{}]", newLicense.getId(), usage + 1);
            }
            // Increment the usage of this new license
            licensesUsage.put(newLicense.getId(), usage + 1);
        }

        if (existingLicense != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Decrement usage of license [{}] to [{}]", existingLicense.getId(), usage - 1);
            }
            // Decrement the usage of the replaced license
            usage = licensesUsage.get(existingLicense.getId());
            licensesUsage.put(existingLicense.getId(), usage - 1);
            if (usage < 1) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Remove license [{}] from in-use licenses", existingLicense.getId());
                }
                // If the replaced license is no more in use, drop it from the license set to free memory
                licenses.remove(existingLicense.getId());
            }
        }
    }

    void installExtensionLicense(String namespace, InstalledExtension extension)
    {
        ExtensionId extensionId = extension.getId();
        Collection<InstalledExtension> backwardDeps = getLicensorBackwardDependencies().get(namespace);

        if (backwardDeps != null && backwardDeps.contains(extension) && extensionToLicense.get(extensionId) == null) {
            registerLicense(extensionId, resolveLicenseForExtension(extension));
        }
    }

    private License resolveLicenseForExtension(Extension extension)
    {
        Set<License> candidateLicenses = new HashSet<>();
        ExtensionId extId = extension.getId();
        Collection<ExtensionId> features = extension.getExtensionFeatures();

        for (Map.Entry<LicensedFeatureId, License> entry : featureToLicense.entrySet()) {
            License license = entry.getValue();
            if (candidateLicenses.contains(license)) {
                continue;
            }

            LicensedFeatureId licId = entry.getKey();
            if (licId.isCompatible(extId)) {
                candidateLicenses.add(license);
                continue;
            }
            for (ExtensionId feature : features) {
                if (licId.isCompatible(feature)) {
                    candidateLicenses.add(license);
                    break;
                }
            }
        }

        return (candidateLicenses.size() > 0) ? License.getOptimumLicense(candidateLicenses) : License.UNLICENSED;
    }

    void uninstallExtensionLicense(InstalledExtension extension)
    {
        ExtensionId extensionId = extension.getId();

        // Remove the license binding only if the specified extension version has been uninstalled from all namespaces.
        if (!extension.isInstalled()) {
            extensionToLicense.remove(extensionId);
        }
    }

    private Collection<ExtensionId> getLicensedExtensions()
    {
        Set<ExtensionId> extIds = new HashSet<>();
        for (Collection<InstalledExtension> extensions : getLicensorBackwardDependencies().values()) {
            for (InstalledExtension extension : extensions) {
                extIds.add(extension.getId());
            }
        }
        return extIds;
    }

    private Map<String, Collection<InstalledExtension>> getLicensorBackwardDependencies()
    {
        try {
            return installedExtensionRepository.getBackwardDependencies(licensorExtensionId);
        } catch (ResolveException e) {
            logger.warn("Licensor is unable to properly register licensed extensions.", e);
            return Collections.emptyMap();
        }
    }

    @Override
    public License get(ExtensionId extensionId)
    {
        // Check if there is a license available that covers a specific version of the extension.
        License license = extensionToLicense.get(extensionId);

        if (license != null) {
            return license;
        }

        // Check if there is a license available that covers any version of the extension.
        ExtensionId extId = new ExtensionId(extensionId.getId());
        return extensionToLicense.get(extId);
    }

    @Override
    public boolean add(License license)
    {
        Collection<LicensedFeatureId> licIds = linkLicenseToLicensedFeature(license);
        if (licIds.size() > 0) {
            try {
                store.store(storeReference, license);
            } catch (IOException e) {
                logger.warn("Licensor was unable to persist license [{}].", license.getId());
            }

            clearSecurityCacheForXarExtensions(linkLicenseToInstalledExtensions(licIds, license));
            return true;
        }
        return false;
    }

    private void clearSecurityCacheForXarExtensions(Collection<ExtensionId> extensionIds)
    {
        for (ExtensionId extId : extensionIds) {
            clearSecurityCacheForXarExtension(extId);
        }
    }

    private void clearSecurityCacheForXarExtension(ExtensionId extensionId)
    {
        // We need to clear the cache because its content might be wrong after a licensing state change
        InstalledExtension extension = xarInstalledExtensionRepository.getInstalledExtension(extensionId);
        if (extension != null && extension instanceof XarInstalledExtension) {
            logger.debug("Clearing security cache for extension [{}]", extension);
            licensingSecurityCacheRuleInvalidator.invalidate((XarInstalledExtension) extension);
        }
    }

    @Override
    public void delete(LicenseId licenseId)
    {
        store.delete(storeReference, licenseId);
    }

    @Override
    public Collection<License> getActiveLicenses()
    {
        return Collections.unmodifiableCollection(licenses.values());
    }

    @Override
    public Collection<LicenseId> getPersistedLicenses()
    {
        return Collections.unmodifiableCollection(licensesUsage.keySet());
    }

    @Override
    public Collection<LicenseId> getUnusedPersistedLicenses()
    {
        Collection<LicenseId> licenseIds = new ArrayList<>();
        for (Map.Entry<LicenseId, Integer> entry : licensesUsage.entrySet()) {
            if (entry.getValue() == 0) {
                licenseIds.add(entry.getKey());
            }
        }
        return licenseIds;
    }

    @Override
    public Collection<License> getUsedLicenses()
    {
        Collection<License> usedLicenses = new HashSet<>();
        for (License license : extensionToLicense.values()) {
            if (license == License.UNLICENSED) {
                continue;
            }
            usedLicenses.add(license);
        }
        return usedLicenses;
    }
}
