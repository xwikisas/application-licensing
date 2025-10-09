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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseManager;
import com.xwiki.licensing.LicenseValidator;
import com.xwiki.licensing.Licensor;

/**
 * Default implementation for {@link Licensor}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultLicensor implements Licensor, Initializable
{
    private static final int EXPIRATION_THRESHOLD = 10;

    @Inject
    private LicenseManager licenseManager;

    @Inject
    private LicenseValidator licenseValidator;

    @Inject
    private EntityLicenseManager entityLicenseManager;

    @Inject
    private Provider<XWikiContext> context;

    @Override
    public void initialize() throws InitializationException
    {
        LicensingUtils.checkIntegrity(licenseManager, licenseValidator, entityLicenseManager);
    }

    @Override
    public License getLicense()
    {
        try {
            return getLicense(context.get().getDoc().getDocumentReference());
        } catch (Throwable e) {
            // Ignored
        }
        return null;
    }

    @Override
    public License getLicense(EntityReference reference)
    {
        try {
            return entityLicenseManager.get(reference);
        } catch (Throwable e) {
            // Ignored
        }
        return null;
    }

    @Override
    public License getLicense(ExtensionId extensionId)
    {
        try {
            return licenseManager.get(extensionId);
        } catch (Throwable e) {
            // Ignored
        }
        return null;
    }

    @Override
    public boolean hasLicensure()
    {
        License license = getLicense();
        return license == null || licenseValidator.isValid(license);
    }

    @Override
    public boolean hasLicensure(EntityReference reference)
    {
        License license = getLicense(reference);
        return license == null || licenseValidator.isValid(license);
    }

    @Override
    public boolean hasLicensure(ExtensionId extensionId)
    {
        License license = getLicense(extensionId);
        return license == null || licenseValidator.isValid(license);
    }

    @Override
    public boolean hasLicensure(EntityReference reference, DocumentReference userReference)
    {
        License license = getLicense(reference);
        return license == null || licenseValidator.isValid(license, userReference);
    }

    @Override
    public boolean hasLicensure(ExtensionId extensionId, DocumentReference userReference)
    {
        License license = getLicense(extensionId);
        return license == null || licenseValidator.isValid(license, userReference);
    }

    public boolean isLicenseExpiring(ExtensionId extensionId)
    {
        License license = getLicense(extensionId);
        if (license == null) {
            return false;
        }
        LocalDate expirationDate =
            Instant.ofEpochMilli(license.getExpirationDate()).atZone(ZoneId.systemDefault()).toLocalDate();
        long daysUntilExpiration = ChronoUnit.DAYS.between(LocalDate.now(), expirationDate);
        return daysUntilExpiration <= EXPIRATION_THRESHOLD && daysUntilExpiration > 0;
    }
}
