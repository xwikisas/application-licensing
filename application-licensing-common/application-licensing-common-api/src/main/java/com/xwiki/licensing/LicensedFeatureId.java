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
    private final String id;
    private final VersionConstraint versionConstraint;

    public LicensedFeatureId(String id)
    {
        this(id, null);
    }

    public LicensedFeatureId(String id, String versionConstraint)
    {
        if (id == null) {
            throw new NullPointerException("LicensedExtensionId requires a non-null identifier");
        }
        this.id = id;
        if (versionConstraint != null) {
            this.versionConstraint = new DefaultVersionConstraint(versionConstraint);
        } else {
            this.versionConstraint = null;
        }
    }

    public String getId()
    {
        return id;
    }

    public String getVersionConstraint()
    {
        return (versionConstraint != null) ? versionConstraint.getValue() : null;
    }

    public ExtensionDependency getExtensionDependency()
    {
        return new DefaultExtensionDependency(id, versionConstraint);
    }

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
}
