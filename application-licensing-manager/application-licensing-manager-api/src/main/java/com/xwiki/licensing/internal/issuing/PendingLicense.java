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

import com.xwiki.licensing.SignedLicense;

/**
 * A license signed in memory, and its record, not written yet.
 *
 * @version $Id$
 * @since 1.34
 */
public class PendingLicense
{
    private final LicenseRecord licenseRecord;

    private final SignedLicense signedLicense;

    private final String key;

    /**
     * @param licenseRecord the record to create
     * @param signedLicense the signed license to store
     * @param key the license key returned to the client
     */
    public PendingLicense(LicenseRecord licenseRecord, SignedLicense signedLicense, String key)
    {
        this.licenseRecord = licenseRecord;
        this.signedLicense = signedLicense;
        this.key = key;
    }

    /**
     * @return the record to create
     */
    public LicenseRecord getRecord()
    {
        return this.licenseRecord;
    }

    /**
     * @return the signed license to store
     */
    public SignedLicense getSignedLicense()
    {
        return this.signedLicense;
    }

    /**
     * @return the license key returned to the client
     */
    public String getKey()
    {
        return this.key;
    }
}
