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
package com.xwiki.licensing.internal.enforcer;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;

import com.xwiki.licensing.License;

/**
 * Manage licenses.
 *
 * @version $Id$
 */
@Role
public interface EntityLicenseManager
{
    /**
     * Retrieve the license applicable for a given entity.
     *
     * @param reference the reference of the entity.
     * @return the best applicable license or NULL if no license need to be applied. If no license is available but one
     * should be applied, this method will return a License.UNLICENSED license.
     */
    License get(EntityReference reference);
}
