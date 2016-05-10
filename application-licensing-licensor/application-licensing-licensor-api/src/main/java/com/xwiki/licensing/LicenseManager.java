package com.xwiki.licensing;

import java.util.Collection;

import org.xwiki.extension.ExtensionId;

/**
 * Licensing manager allowing to retrieve active licenses, to add new ones, and drop existing ones.
 *
 * @version $Id$
 */
public interface LicenseManager
{
    /**
     * Retrieve the currently applicable license for the given installed extension.
     * @param extensionId identifier of an installed extension
     * @return a license.
     */
    License get(ExtensionId extensionId);

    /**
     * Add a new license to the current set of active license. The added license is checked to be applicable to the
     * current wiki instance, else it will not be added. The license is also checked to be more interesting than the
     * currently installed licenses. If the license does not provides any improvement of the licensing state of this
     * wiki, it will not be added. This check does not reject unsigned license, but give immediate priority to signed
     * ones over unsigned ones, even if the signed license has narrower constraints. These evaluations are done for
     * each licensed extension independently, whether these extension are currently installed or not.
     *
     * @param license a license to be added (could be signed or not, unsigned will be stored but not really applied)
     * @return true if the license has been actually added (see above).
     */
    boolean add(License license);

    /**
     * Try to delete the given license from the persistence store. The license is not removed from the active set until
     * the next restart.
     * @param licenseId the id of the license to be removed.
     */
    void delete(LicenseId licenseId);

    /**
     * @return an unmodifiable collection of licenses currently active. Active just means that they could have a
     * usage in the set, not that they are currently applied to an installed extension.
     */
    Collection<License> getActiveLicenses();

    /**
     * @return an unmodifiable collection of license currently persisted.
     */
    Collection<LicenseId> getPersistedLicenses();

    /**
     * @return an modifiable collection of license currently persisted but that are not active.
     */
    Collection<LicenseId> getUnusedPersistedLicenses();

    /**
     * @return
     */
    Collection<License> getUsedLicenses();
}
