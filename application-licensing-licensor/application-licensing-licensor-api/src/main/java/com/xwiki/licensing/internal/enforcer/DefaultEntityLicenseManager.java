package com.xwiki.licensing.internal.enforcer;

import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtensionRepository;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseManager;

/**
 * Default implementation of the {@link EntityLicenseManager} role.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultEntityLicenseManager implements EntityLicenseManager, Initializable
{
    @Inject
    private Logger logger;

    @Inject
    @Named(XarExtensionHandler.TYPE)
    private InstalledExtensionRepository installedExtensionRepository;

    @Inject
    private LicenseManager licenseManager;

    @Override
    public void initialize() throws InitializationException
    {
        LicensingUtils.checkIntegrity(licenseManager);
    }

    @Override
    public License get(EntityReference reference)
    {
        for (XarInstalledExtension extension : getMatchingExtensions(reference)) {
            License license = licenseManager.get(extension.getId());
            if (license != null) {
                // TODO: improve potential conflict resolution
                return license;
            }
        }

        return null;
    }

    private Collection<XarInstalledExtension> getMatchingExtensions(EntityReference reference)
    {
        if (reference.getType() == EntityType.DOCUMENT) {
            return ((XarInstalledExtensionRepository) installedExtensionRepository)
                .getXarInstalledExtensions(new DocumentReference(reference));
        }
        return Collections.emptyList();
    }
}
