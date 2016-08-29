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
    /**
     * Store a given license into a given store.
     *
     * @param store the store where to store the license.
     * @param license the license to be stored.
     * @throws IOException when an error occurs.
     */
    void store(LicenseStoreReference store, License license) throws IOException;

    /**
     * Retrieve a license from a single license store.
     *
     * @param store the store from which the license should be retrieved.
     * @return the retrieved license or NULL if no license has been found in the store.
     * @throws IOException when an error occurs.
     */
    License retrieve(LicenseStoreReference store) throws IOException;

    /**
     * Retrieve a identified license from a multi-license store.
     *
     * @param store the store from which the license should be retrieved.
     * @param license the identifier of the license to be retrieved.
     * @return the retrieved license or NULL if the license has not be found in the store.
     * @throws IOException when an error occurs.
     */
    License retrieve(LicenseStoreReference store, LicenseId license) throws IOException;

    /**
     * Delete a license from a single license store.
     *
     * @param store the store from which the license should be deleted.
     */
    void delete(LicenseStoreReference store);

    /**
     * Delete a identifier license from a multiple licenses store.
     *
     * @param store the store from which the license should be deleted.
     * @param license the identifier of the license to be deleted.
     */
    void delete(LicenseStoreReference store, LicenseId license);

    /**
     * Get an iterable over all licenses stored in a given store.
     *
     * @param store the store from which the licenses should be retrieved.
     * @return an iterable over all license currently stored.
     */
    Iterable<License> getIterable(LicenseStoreReference store);
}
