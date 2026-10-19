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

import org.xwiki.component.annotation.Role;
import org.xwiki.extension.ExtensionId;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * Licensor allows licensed extension to check their license.
 *
 * @version $Id$
 */
@Role
public interface Licensor
{
    /**
     * Retrieve the currently applicable license for the current context document if any.
     *
     * @return a license, or null if there is no current document, or the current document is not subject to licensing.
     */
    License getLicense();

    /**
     * Retrieve the currently applicable license for the given installed extension.
     *
     * @param extensionId name of an installed extension. This method automatically resolves the version of the
     *     extension which is installed
     * @return a license, or null if the given installed extension is not subject to licensing.
     * @since 1.34.0
     */
    default License getLicense(String extensionId)
    {
        return null;
    }

    /**
     * Retrieve the currently applicable license for the given installed extension.
     *
     * @param extensionId identifier of an installed extension
     * @return a license, or null if the given installed extension is not subject to licensing.
     */
    License getLicense(ExtensionId extensionId);

    /**
     * Get the license applicable to the given reference.
     * @param reference the reference to get the license from.
     * @return a license, or null if the given reference is not subject to licensing.
     */
    License getLicense(EntityReference reference);

    /**
     * @return true if the current document has a valid license or is not subject to licensing.
     */
    boolean hasLicensure();

    /**
     * Check if the given entity is covered by a valid license.
     *
     * @param reference the reference of the entity for which licensure should be checked.
     * @return true if the given reference has a valid license or is not subject to licensing.
     */
    boolean hasLicensure(EntityReference reference);

    /**
     * Check if the given extension is covered by a valid license.
     *
     * @param extensionId the identifier of the extension for which licensure should be checked.
     * @return true if the given extension has a valid license or is not subject to licensing.
     */
    boolean hasLicensure(ExtensionId extensionId);

    /**
     * Check if the given extension is covered by a valid license.
     *
     * @param extensionId the name of the extension for which licensure should be checked.
     * @return true if the given extension has a valid license or is not subject to licensing.
     * @since 1.34.0
     */
    default boolean hasLicensure(String extensionId)
    {
        return true;
    }

    /**
     * Count the active users of the instance, all wikis included. This is the count checked against the user limit of
     * the licenses.
     *
     * @return the number of active users
     * @throws Exception if the users of the instance cannot be counted
     * @since 1.34.0
     */
    @Unstable
    default long getUserCount() throws Exception
    {
        return 0;
    }

    /**
     * Check whether the given user is among the first {@code limit} active users of the instance, all wikis included,
     * ordered by creation date. For example, with a limit of {@code 10}, the 10 oldest active users are under the limit
     * and every newer one is over it. Users created in the same second are ordered by their serialized reference.
     *
     * @param user the user to check, e.g. {@code xwiki:XWiki.JohnDoe}
     * @param limit the maximum number of users allowed by the license; a negative value (e.g. {@code -1}) means
     *     unlimited
     * @return {@code true} if the limit is negative, whatever the user; otherwise {@code true} only if the user is an
     *     active user of the instance within the first {@code limit} ones, and {@code false} for a {@code null},
     *     inactive or nonexistent user (this includes the guest user and superadmin, which have no user profile)
     * @throws Exception if the users of the instance cannot be retrieved
     * @since 1.34.0
     */
    @Unstable
    default boolean isUserUnderLimit(DocumentReference user, long limit) throws Exception
    {
        return true;
    }

    /**
     * Check if the given extension is expiring in less than 10 days.
     *
     * @param extensionId the identifier of the extension for which licensure should be checked.
     * @return {@code true} if the given extension license is expiring in less than 10 days or {@code false} otherwise.
     * @since 1.30
     */
    @Unstable
    boolean isLicenseExpiring(ExtensionId extensionId);
}
