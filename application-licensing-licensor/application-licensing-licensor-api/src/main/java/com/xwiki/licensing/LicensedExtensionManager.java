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

import java.util.Collection;
import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.stability.Unstable;

/**
 * Component used to manage the extensions that require a license.
 * 
 * @version $Id$
 * @since 1.13.6
 */
@Role
@Unstable
public interface LicensedExtensionManager
{
    /**
     * @return all the installed extensions (from all the namespaces) that require a license (even if there's no license
     *         available for them yet)
     */
    Collection<ExtensionId> getLicensedExtensions();

    /**
     * @param namespace the namespace where to look for licensed extensions
     * @return all the extensions installed on the specified namespaces that require a license (even if there's no
     *         license available for them yet)
     */
    Collection<ExtensionId> getLicensedExtensions(String namespace);

    /**
     * Use this to determine which installed extensions (from any namespace) are covered by a given license.
     * 
     * @param licensedFeatureId a licensed feature id
     * @return the installed extensions (from all the namespaces) that are covered by the specified licensed feature
     */
    Collection<ExtensionId> getLicensedExtensions(LicensedFeatureId licensedFeatureId);

    /**
     * Get the list of installed licensed extensions for which a license is mandatory because they are not covered by
     * the license of other installed extensions (there aren't any other licensed extensions that depend on them).
     *
     * @return licensed extensions that are not dependencies of other licensed extensions
     * @since 1.17
     */
    Collection<ExtensionId> getMandatoryLicensedExtensions();

    /**
     * Invalidate the cached list of mandatory licensed extensions.
     * @since 1.21.1
     */
    void invalidateMandatoryLicensedExtensionsCache();

    /**
     * Get the list of licensed dependencies of an extension from a specific namespace.
     *
     * @param installedExtension the installed extension for which to look for licensed dependencies
     * @param namespace the namespace where to look for dependencies
     * @return a set of installed licensed dependencies
     * @since 1.27
     */
    Set<ExtensionId> getLicensedDependencies(InstalledExtension installedExtension, String namespace);
}
