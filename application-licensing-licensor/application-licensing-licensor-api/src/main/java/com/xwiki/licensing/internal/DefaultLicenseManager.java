package com.xwiki.licensing.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.event.ExtensionEvent;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.event.ExtensionUninstalledEvent;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.version.Version;
import org.xwiki.instance.InstanceIdManager;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xwiki.licensing.FileLicenseStoreReference;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseId;
import com.xwiki.licensing.LicenseManager;
import com.xwiki.licensing.LicenseStore;
import com.xwiki.licensing.LicenseStoreReference;
import com.xwiki.licensing.LicensedFeatureId;
import com.xwiki.licensing.LicensingConfiguration;

/**
 * Default implementation of the {@link LicenseManager} role.
 *
 * @version $Id$
 */
public class DefaultLicenseManager implements LicenseManager, EventListener, Initializable
{
    /**
     * The events observed by this event listener.
     */
    private static final List<Event> EVENTS = new ArrayList<>(Arrays.asList(
        new ExtensionInstalledEvent(),
        new ExtensionUninstalledEvent(),
        new ExtensionUpgradedEvent()));

    @Inject
    private Logger logger;

    @Inject
    LicensingConfiguration configuration;

    @Inject
    @Named("FileSystem")
    LicenseStore store;

    @Inject
    InstalledExtensionRepository installedExtensionRepository;

    @Inject
    private InstanceIdManager instanceIdManager;

    private final Map<LicenseId, License> licenses = new HashMap<>();

    private final Map<LicenseId, Integer> licensesUsage = new HashMap<>();

    private final Map<LicensedFeatureId, License> featureToLicense = new HashMap<>();

    private final Map<ExtensionId, License> extensionToLicense = new HashMap<>();

    private ExtensionId licensorExtensionId;

    private LicenseStoreReference storeReference;

    @Override
    public void initialize() throws InitializationException
    {
        this.licensorExtensionId =
            installedExtensionRepository.getInstalledExtension("com.xwiki.licensing:application-licensing-licensor",
                null).getId();

        this.storeReference = new FileLicenseStoreReference(configuration.getLocalStorePath());
        Iterator<License> it = store.getIterable(storeReference).iterator();
        while (it.hasNext()) {
            try {
                linkLicenseToLicensedFeature(it.next());
            } catch (RuntimeException e) {
                logger.warn("Error retrieving licence, license has been skipped.", e.getCause());
            }
        }

        for (Map.Entry<LicensedFeatureId, License> entry : featureToLicense.entrySet()) {
            linkLicenceToInstalledExtension(entry.getKey(), entry.getValue());
        }

        // Add Unlicensed licenses to all extension installed that are under license
        for (ExtensionId id : getLicensedExtensions()) {
            extensionToLicense.putIfAbsent(id, License.UNLICENSED);
        }
    }

    private Collection<ExtensionId> linkLicenceToInstalledExtension(Collection<LicensedFeatureId> licIds,
        License licenseTolink)
    {
        Set<ExtensionId> extensionIds = new HashSet<>();
        for(LicensedFeatureId licId : licIds) {
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
                License existingLicense = extensionToLicense.get(extension.getId());

                // If already licensed using another license, get the best of both license
                if (existingLicense != null && license != existingLicense) {
                    license = License.getOptimumLicense(existingLicense, license);
                }

                // If the new license is better
                if (license != existingLicense) {
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
            for (LicensedFeatureId extId : license.getFeatureIds()) {
                License existingLicense = featureToLicense.get(extId);

                // If already licensed somehow, get the best of both license
                if (existingLicense != null) {
                    license = License.getOptimumLicense(existingLicense, license);
                }

                // If the new license is the best
                if (license != existingLicense) {

                    // Register the new license for this extension
                    featureToLicense.put(extId, license);

                    Integer usage = licensesUsage.get(license.getId());
                    if (usage == null) {
                        // Initialize the first usage of this new license
                        licenses.put(license.getId(), license);
                        licensesUsage.put(license.getId(), 1);
                    } else {
                        // Increment the usage of this new license
                        licensesUsage.put(license.getId(), usage + 1);
                    }

                    if (existingLicense != null) {
                        // Decrement the usage of the replaced license
                        usage = licensesUsage.get(existingLicense.getId());
                        licensesUsage.put(existingLicense.getId(), usage - 1);
                        if (usage < 1) {
                            // If the replaced license is no more in use, drop it from the license set to free memory
                            licenses.remove(existingLicense.getId());
                        }
                    }
                    licensedFeatureIds.add(extId);
                }
            }
        }
        return licensedFeatureIds;
    }

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public String getName()
    {
        return "LicensorManagerListener";
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        ExtensionEvent extensionEvent = (ExtensionEvent) event;
        InstalledExtension installedExtension = (InstalledExtension) source;

        if (event instanceof ExtensionInstalledEvent) {
            installExtensionLicense(extensionEvent.getNamespace(), installedExtension);
        } else if (event instanceof ExtensionUninstalledEvent) {
            uninstallExtensionLicense(installedExtension);
        } else if (event instanceof ExtensionUpgradedEvent) {
            uninstallExtensionLicense((InstalledExtension) data);
            installExtensionLicense(extensionEvent.getNamespace(), installedExtension);
        }
    }

    private void installExtensionLicense(String namespace, InstalledExtension extension)
    {
        ExtensionId extensionId = extension.getId();

        if (getLicensorBackwardDependencies().get(namespace).contains(extension)
            && extensionToLicense.get(extensionId) == null) {
            extensionToLicense.put(extensionId, resolveLicenseForExtension(extension));
        }
    }

    private License resolveLicenseForExtension(Extension extension) {
        Set<License> licenses = new HashSet<>();
        ExtensionId extId = extension.getId();
        Collection<ExtensionId> features = extension.getExtensionFeatures();

        for(Map.Entry<LicensedFeatureId, License> entry : featureToLicense.entrySet()) {
            License license = entry.getValue();
            if (licenses.contains(license)) {
                continue;
            }

            LicensedFeatureId licId = entry.getKey();
            if (licId.isCompatible(extId)) {
                licenses.add(license);
                continue;
            }
            for (ExtensionId feature : features) {
                if (licId.isCompatible(feature)) {
                    licenses.add(license);
                    break;
                }
            }
        }

        return (licenses.size() > 0) ? License.getOptimumLicense(licenses) : License.UNLICENSED;
    }

    private void uninstallExtensionLicense(InstalledExtension extension)
    {
        ExtensionId extensionId = extension.getId();

        extensionToLicense.remove(extensionId);
    }

    private Collection<ExtensionId> getLicensedExtensions()
    {
        Set<ExtensionId> extIds = new HashSet<>();
        for (Collection<InstalledExtension> extensions : getLicensorBackwardDependencies().values()) {
            for (InstalledExtension extension : extensions) {
                extIds.add(new ExtensionId(extension.getId().getId(), (Version) null));
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
            linkLicenceToInstalledExtension(licIds, license);
            try {
                store.store(storeReference, license);
            } catch (IOException e) {
                logger.warn("Licensor was unable to persist license [{}].", license.getId());
            }
            return true;
        }
        return false;
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
        Collection<License> licenses = new HashSet<>();
        licenses.addAll(extensionToLicense.values());
        return licenses;
    }
}
