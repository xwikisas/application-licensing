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
package com.xwiki.licensing.internal;

import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.licensing.internal.enforcer.EntityLicenseManager;

/**
 * Interface for extensions which manage users, so the license user limit can only take into account the users managed
 * by those extensions for computing the user limit.
 * <p>
 * Components implementing this interface should be named as extension id, so the user manager is discoverable.
 * FIXME: Maybe implement a {@link EntityLicenseManager} instead?
 *
 * @version $Id$
 * @since 1.31
 */
@Role
public interface AuthExtensionUserManager
{
    /**
     * True if this manager manages the given user. Should return false for invalid licenses.
     *
     * @param user the user to check
     * @return true if this manager manages the given user.
     */
    boolean managesUser(DocumentReference user);

    /**
     * True if this manager manages the given user. Should return false for invalid licenses.
     *
     * @param user the user to check
     * @return true if this manager manages the given user.
     */
    boolean managesUser(XWikiDocument user);

//    /**
//     * True if this manager manages the given user. Should return false for invalid licenses.
//     *
//     * @param userReference the user to check
//     * @return true if this manager manages the given user.
//     */
//    boolean managesUser(UserReference userReference);

    /**
     * Get a list of all users managed by this manager.
     *
     * @return a list of all users managed by this manager
     */
    Set<XWikiDocument> getManagedUsers();

    /**
     * If the given user has access to the given license.
     *
     * @param user user to check
     * @return whether the given user has access to the given license
     * @since 1.30.0
     */
    @Unstable
    boolean shouldBeActive(DocumentReference user);
}
