package com.xwiki.licensing.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.instance.InstanceIdManager;

import com.xwiki.licensing.FileLicenseStoreReference;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseId;
import com.xwiki.licensing.LicenseManager;
import com.xwiki.licensing.LicenseStore;
import com.xwiki.licensing.LicenseStoreReference;
import com.xwiki.licensing.LicensedFeatureId;
import com.xwiki.licensing.LicensingConfiguration;
import com.xwiki.licensing.internal.enforcer.LicensingSecurityCacheRuleInvalidator;

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
    private InstanceIdManager instanceIdManager;

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
        instanceIdManager.initializeInstanceId();

        this.licensorExtensionId =
            installedExtensionRepository.getInstalledExtension("com.xwiki.licensing:application-licensing-licensor-api",
                null).getId();

        logger.debug("About to load registered licences");
        this.storeReference = new FileLicenseStoreReference(configuration.getLocalStorePath(), true);
        for (License license : store.getIterable(storeReference)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Registering licence [{}]", license.getId());
            }
            try {
                linkLicenseToLicensedFeature(license);
            } catch (RuntimeException e) {
                logger.warn("Error retrieving licence, license has been skipped.", e.getCause());
            }
        }

        for (Map.Entry<LicensedFeatureId, License> entry : featureToLicense.entrySet()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Associating licence [{}] for feature [{}]", entry.getValue().getId(), entry.getKey());
            }
            linkLicenceToInstalledExtension(entry.getKey(), entry.getValue());
        }

        // Add Unlicensed licenses to all extension installed that are under license
        for (ExtensionId id : getLicensedExtensions()) {
            if (extensionToLicense.putIfAbsent(id, License.UNLICENSED) == null) {
                logger.debug("Mark extension [{}] unlicensed", id);
            }
        }
    }

    private Collection<ExtensionId> linkLicenceToInstalledExtension(Collection<LicensedFeatureId> licIds,
        License licenseTolink)
    {
        Set<ExtensionId> extensionIds = new HashSet<>();
        for (LicensedFeatureId licId : licIds) {
            ExtensionId extensionId = linkLicenceToInstalledExtension(licId, licenseTolink);
            if (extensionId != null) {
                extensionIds.add(extensionId);
            }
        }
        return extensionIds;
    }

    private ExtensionId linkLicenceToInstalledExtension(LicensedFeatureId licId, License licenseTolink)
    {
        try {
            License license = licenseTolink;
            Extension extension = installedExtensionRepository.resolve(licId.getExtensionDependency());
            if (extension != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Analyze licence [{}] for extension [{}]", license.getId(), extension.getId());
                }
                License existingLicense = extensionToLicense.get(extension.getId());

                // If already licensed using another license, get the best of both license
                if (existingLicense != null && license != existingLicense) {
                    license = License.getOptimumLicense(existingLicense, license);
                }

                // If the new license is better
                if (license != existingLicense) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Register licence [{}] for extension [{}]", license.getId(), extension.getId());
                    }
                    // Register the new license for this extension
                    extensionToLicense.put(extension.getId(), license);
                    return extension.getId();
                }
            }
        } catch (ResolveException e) {
            // ignored
        }
        return null;
    }

    private synchronized Collection<LicensedFeatureId> linkLicenseToLicensedFeature(License license)
    {
        Collection<LicensedFeatureId> licensedFeatureIds = new ArrayList<>();
        if (license.isApplicableTo(instanceIdManager.getInstanceId())) {
            if (logger.isDebugEnabled()) {
                logger.debug("License [{}] is applicable to this wiki instance", license.getId());
            }
            for (LicensedFeatureId extId : license.getFeatureIds()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Analyze licence [{}] for feature [{}]", license.getId(), extId);
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
                        logger.debug("Linking licence [{}] to feature [{}]", newLicense.getId(), extId);
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

        if (getLicensorBackwardDependencies().get(namespace).contains(extension)
            && extensionToLicense.get(extensionId) == null) {
            extensionToLicense.put(extensionId, resolveLicenseForExtension(extension));
        }
    }

    private License resolveLicenseForExtension(Extension extension) {
        Set<License> candidateLicences = new HashSet<>();
        ExtensionId extId = extension.getId();
        Collection<ExtensionId> features = extension.getExtensionFeatures();

        for (Map.Entry<LicensedFeatureId, License> entry : featureToLicense.entrySet()) {
            License license = entry.getValue();
            if (candidateLicences.contains(license)) {
                continue;
            }

            LicensedFeatureId licId = entry.getKey();
            if (licId.isCompatible(extId)) {
                candidateLicences.add(license);
                continue;
            }
            for (ExtensionId feature : features) {
                if (licId.isCompatible(feature)) {
                    candidateLicences.add(license);
                    break;
                }
            }
        }

        return (candidateLicences.size() > 0) ? License.getOptimumLicense(candidateLicences) : License.UNLICENSED;
    }

    void uninstallExtensionLicense(InstalledExtension extension)
    {
        ExtensionId extensionId = extension.getId();

        extensionToLicense.remove(extensionId);
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
        License license = extensionToLicense.get(extensionId);

        if (license != null) {
            return license;
        }

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

            clearSecurityCacheForXarExtensions(linkLicenceToInstalledExtension(licIds, license));
            return true;
        }
        return false;
    }

    private void clearSecurityCacheForXarExtensions(Collection<ExtensionId> extensionIds)
    {
        for (ExtensionId extId : extensionIds) {
            InstalledExtension extension = xarInstalledExtensionRepository.getInstalledExtension(extId);
            if (extension != null && extension instanceof XarInstalledExtension) {
                logger.debug("Clearing security cache for extension [{}]", extension);
                licensingSecurityCacheRuleInvalidator.invalidate((XarInstalledExtension) extension);
            }
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
        Collection<License> usedLicences = new HashSet<>();
        for (License license : extensionToLicense.values()) {
            if (license == License.UNLICENSED) {
                continue;
            }
            usedLicences.add(license);
        }
        return usedLicences;
    }
}
