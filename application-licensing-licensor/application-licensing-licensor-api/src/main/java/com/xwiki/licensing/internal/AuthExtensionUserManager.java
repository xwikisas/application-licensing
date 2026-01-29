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

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Interface for extensions which manage users, so the license user limit can only take into account the users managed
 * by those extensions for computing the user limit.
 * <p>
 * Components implementing this interface should be named as extension id, so the user manager is discoverable.
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

    /**
     * Get a list of all users managed by this authentication system.
     *
     * @return a list of all users managed by this manager
     */
    List<XWikiDocument> getManagedUsers();

    /**
     * Get a list of all active users managed by this manager.
     *
     * @return a list of all active users managed by this auth extension
     */
    List<XWikiDocument> getActiveManagedUsers();

    /**
     * Resolve a username (the string used by a user to log in) to the XWiki user page. This method should resolve all
     * valid usernames that a user can use to log into their account.
     *
     * @param username the username used by a user to login
     * @param context XWiki context, to help in querying pages
     * @return a reference to the user page with the XWiki.XWikiUsers object, or null if not existent
     */
    DocumentReference getUserDocFromUsername(String username, XWikiContext context);
}
