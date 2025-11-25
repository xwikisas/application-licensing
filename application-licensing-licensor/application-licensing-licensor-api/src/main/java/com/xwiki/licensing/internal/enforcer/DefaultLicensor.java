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
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseManager;
import com.xwiki.licensing.LicenseValidator;
import com.xwiki.licensing.Licensor;
import com.xwiki.licensing.internal.UserCounter;

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

    /**
     * Display the 'expiring license' message when the license has this many users left until the user limit is
     * reached.
     */
    private static final int USERLIMIT_EXPIRATION_THRESHOLD = 5;

    @Inject
    private LicenseManager licenseManager;

    @Inject
    private LicenseValidator licenseValidator;

    @Inject
    private EntityLicenseManager entityLicenseManager;

    @Inject
    private Provider<XWikiContext> context;

    @Inject
    private UserCounter userCounter;

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

    private boolean isLicenseExpiringDate(License license)
    {
        LocalDate expirationDate =
            Instant.ofEpochMilli(license.getExpirationDate()).atZone(ZoneId.systemDefault()).toLocalDate();
        long daysUntilExpiration = ChronoUnit.DAYS.between(LocalDate.now(), expirationDate);
        return daysUntilExpiration <= EXPIRATION_THRESHOLD && daysUntilExpiration > 0;
    }

    private boolean isLicenseExpiringUserLimit(License license)
    {
        try {
            long instanceUserCount = userCounter.getUserCount();
            long userDifference = instanceUserCount - license.getMaxUserCount();
            return userDifference <= USERLIMIT_EXPIRATION_THRESHOLD && userDifference > 0;
        } catch (Exception ignored) {
            // If we can't count the users, consider the license not expiring from the user limit point of view.
        }
        return false;
    }

    @Override
    public boolean isLicenseExpiring(ExtensionId extensionId)
    {
        License license = getLicense(extensionId);
        if (license == null) {
            return false;
        }
        return isLicenseExpiringDate(license) || isLicenseExpiringUserLimit(license);
    }
}
