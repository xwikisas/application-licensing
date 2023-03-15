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

import org.xwiki.extension.DefaultExtensionDependency;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.version.VersionConstraint;
import org.xwiki.extension.version.internal.DefaultVersionConstraint;

/**
 * Identifier of a licensed extension.
 *
 * @version $Id$
 */
public class LicensedFeatureId
{
    private static final VersionConstraint NO_VERSION_CONSTRAINT = new DefaultVersionConstraint("(,)");

    private final String id;

    private final VersionConstraint versionConstraint;

    /**
     * Construct a feature identifier from a String without any version constraint.
     *
     * @param id the identifier as a string.
     */
    public LicensedFeatureId(String id)
    {
        this(id, null);
    }

    /**
     * Construct a feature identifier from a String with some version constraint.
     *
     * @param id the identifier as a string.
     * @param versionConstraint the version constraint to be applied.
     */
    public LicensedFeatureId(String id, String versionConstraint)
    {
        if (id == null) {
            throw new NullPointerException("LicensedExtensionId requires a non-null identifier");
        }
        this.id = id;
        if (versionConstraint != null) {
            this.versionConstraint = new DefaultVersionConstraint(versionConstraint);
        } else {
            // Version constraints should not be NULL to prevent NPE in the EM API.
            this.versionConstraint = NO_VERSION_CONSTRAINT;
        }
    }

    /**
     * @return the feature identifier as a string (without the version constraint).
     */
    public String getId()
    {
        return id;
    }

    /**
     * @return the version constraint applicable for this feature.
     */
    public String getVersionConstraint()
    {
        return (versionConstraint == NO_VERSION_CONSTRAINT) ? null : versionConstraint.getValue();
    }

    /**
     * @return an extension dependency object useful to find extensions matching this feature in the installed extension
     *     repository.
     */
    public ExtensionDependency getExtensionDependency()
    {
        return new DefaultExtensionDependency(id, versionConstraint);
    }

    /**
     * Check if a given extension implement a compatible feature for this identifier.
     *
     * @param extensionId the extension identifier.
     * @return true if the given extension implement the feature.
     */
    public boolean isCompatible(ExtensionId extensionId)
    {
        return id.equals(extensionId.getId()) && versionConstraint.isCompatible(extensionId.getVersion());
    }

    @Override
    public String toString()
    {
        return "<" + id + '-' + versionConstraint + '>';
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof LicensedFeatureId)) {
            return false;
        }

        LicensedFeatureId licensedFeatureId = (LicensedFeatureId) obj;

        return this.id.equals(licensedFeatureId.id)
            && ((this.versionConstraint == null && licensedFeatureId.versionConstraint == null)
            || (this.versionConstraint != null
            && this.versionConstraint.equals(licensedFeatureId.versionConstraint)));
    }

    @Override
    public int hashCode()
    {
        return super.hashCode();
    }
}
