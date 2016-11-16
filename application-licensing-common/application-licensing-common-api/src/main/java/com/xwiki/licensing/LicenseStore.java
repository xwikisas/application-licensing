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
