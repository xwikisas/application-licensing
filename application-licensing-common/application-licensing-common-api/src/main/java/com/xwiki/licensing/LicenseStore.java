package com.xwiki.licensing;

import java.io.IOException;

import org.xwiki.component.annotation.Role;

/**
 * Persistent storage for licenses.
 *
 * @version $Id$
 */
@Role
public interface LicenseStore
{
    void store(LicenseStoreReference store, License license) throws IOException;

    License retrieve(LicenseStoreReference store) throws IOException;

    License retrieve(LicenseStoreReference store, LicenseId license) throws IOException;

    void delete(LicenseStoreReference store);

    void delete(LicenseStoreReference store, LicenseId license);

    Iterable<License> getIterable(LicenseStoreReference store);
}
