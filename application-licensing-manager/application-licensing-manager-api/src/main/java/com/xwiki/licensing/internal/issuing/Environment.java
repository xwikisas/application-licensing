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
package com.xwiki.licensing.internal.issuing;

import java.util.Locale;
import java.util.Optional;

import com.xwiki.licensing.LicenseType;

/**
 * The environment of an instance, which decides the type of the licenses it gets.
 *
 * @version $Id$
 * @since 1.34
 */
public enum Environment
{
    /**
     * A production instance, licensed with PAID licenses.
     */
    PROD(LicenseType.PAID),

    /**
     * A test or staging instance, licensed with TRIAL licenses.
     */
    TEST(LicenseType.TRIAL);

    private final LicenseType licenseType;

    Environment(LicenseType licenseType)
    {
        this.licenseType = licenseType;
    }

    /**
     * @return the type of the licenses issued for this environment
     */
    public LicenseType getLicenseType()
    {
        return this.licenseType;
    }

    /**
     * @return the value used in the API and stored in the records ({@code prod} or {@code test})
     */
    public String getValue()
    {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * @param value the value used in the API and stored in the records
     * @return the matching environment, or empty if the value is unknown
     */
    public static Optional<Environment> fromValue(String value)
    {
        for (Environment environment : values()) {
            if (environment.getValue().equals(value)) {
                return Optional.of(environment);
            }
        }
        return Optional.empty();
    }
}
