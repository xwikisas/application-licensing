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
package com.xwiki.licensing.internal.enforcer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtensionRepository;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseManager;

/**
 * Default implementation of the {@link EntityLicenseManager} role.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultEntityLicenseManager implements EntityLicenseManager
{
    /**
     * The license used to grant access to the public pages of a licensed extension.
     */
    protected static final License FREE = new License();

    private static final Pattern LIST_SEPARATOR = Pattern.compile("\\s*,\\s*");

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @Inject
    @Named(XarExtensionHandler.TYPE)
    private InstalledExtensionRepository installedExtensionRepository;

    @Inject
    private Provider<LicenseManager> licenseManagerProvider;

    private LicenseManager licenseManager;

    private LicenseManager getLicenseManager()
    {
        if (licenseManager == null) {
            licenseManager = licenseManagerProvider.get();
            if (!LicensingUtils.isPristineImpl(licenseManager)) {
                throw new RuntimeException(
                    "The licensure engine has been tampered. " + "Your XWiki instance will be seriously impacted.");
            }
        }
        return licenseManager;
    }

    @Override
    public License get(EntityReference reference)
    {
        List<License> licenses = getLicenses(reference);
        if (licenses.isEmpty()) {
            return null;
        } else {
            return License.getOptimumLicense(licenses);
        }
    }

    private List<License> getLicenses(EntityReference reference)
    {
        List<License> licenses = new ArrayList<>();
        for (XarInstalledExtension extension : getMatchingExtensions(reference)) {
            License license = getLicenseManager().get(extension.getId());
            if (license != null) {
                licenses.add(isExcluded(reference, extension) ? FREE : license);
            }
        }
        return licenses;
    }

    private boolean isExcluded(EntityReference reference, XarInstalledExtension extension)
    {
        EntityReference documentReference = reference.extractReference(EntityType.DOCUMENT);
        if (documentReference != null) {
            String excludedDocuments = extension.getProperty("xwiki.extension.licensing.excludedDocuments", "");
            List<String> excludedDocumentsList = Arrays.asList(LIST_SEPARATOR.split(excludedDocuments.trim()));
            return excludedDocumentsList.contains(this.localEntityReferenceSerializer.serialize(documentReference));
        }

        return false;
    }

    private Collection<XarInstalledExtension> getMatchingExtensions(EntityReference reference)
    {
        EntityReference documentReference = reference.extractReference(EntityType.DOCUMENT);
        if (documentReference != null) {
            return ((XarInstalledExtensionRepository) installedExtensionRepository)
                .getXarInstalledExtensions(new DocumentReference(documentReference));
        }
        return Collections.emptyList();
    }
}
