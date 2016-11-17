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
package com.xwiki.licensing;

import java.io.Serializable;
import java.util.UUID;

/**
 * License unique identifier.
 *
 * @version $Id$
 */
public class LicenseId implements Serializable, Comparable<LicenseId>
{
    private final UUID uuid;

    /**
     * Default constructor which create a random identifier.
     */
    public LicenseId()
    {
        uuid = UUID.randomUUID();
    }

    /**
     * Constructor to create a license identifier from a string.
     * @param id the identifier as a string.
     */
    public LicenseId(String id)
    {
        uuid = UUID.fromString(id);
    }

    public String getId()
    {
        return this.uuid.toString();
    }

    @Override
    public String toString()
    {
        return getId();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof LicenseId)) {
            return false;
        }

        LicenseId licenseId = (LicenseId) o;

        return uuid.equals(licenseId.uuid);
    }

    @Override
    public int hashCode()
    {
        return uuid.hashCode();
    }

    @Override
    public int compareTo(LicenseId o)
    {
        return this.uuid.compareTo(o.uuid);
    }
}
