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

    public LicenseId()
    {
        uuid = UUID.randomUUID();
    }

    public LicenseId(String id)
    {
        uuid = UUID.fromString(id);
    }

    @Override
    public String toString()
    {
        return uuid.toString();
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
