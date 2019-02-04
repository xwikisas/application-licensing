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
package com.xwiki.licensing.internal.test;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseValidator;
import com.xwiki.licensing.internal.enforcer.LicensingUtils;

/**
 * {@link LicenseValidator} implementation that is used in unit tests to wrap a mock validator that we can't use
 * directly otherwise because the {@link LicensingUtils#isPristineImpl(Object)} check would fail. Note that this class
 * is excluded from the final jar. We use it only for unit testing.
 * 
 * @version $Id$
 * @since 1.13.2
 */
public class LicenseValidatorWrapper implements LicenseValidator
{
    private LicenseValidator licenseValidator;

    /**
     * Wraps the given license validator (usually a mock validator).
     * 
     * @param licenseValidator the license validator to wrap
     */
    public LicenseValidatorWrapper(LicenseValidator licenseValidator)
    {
        this.licenseValidator = licenseValidator;
    }

    @Override
    public boolean isApplicable(License license)
    {
        return this.licenseValidator.isApplicable(license);
    }

    @Override
    public boolean isSigned(License license)
    {
        return this.licenseValidator.isSigned(license);
    }

    @Override
    public boolean isValid(License license)
    {
        return this.licenseValidator.isValid(license);
    }
}
