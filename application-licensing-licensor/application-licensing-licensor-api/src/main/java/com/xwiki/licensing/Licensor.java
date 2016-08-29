package com.xwiki.licensing;

import org.xwiki.component.annotation.Role;
import org.xwiki.extension.ExtensionId;
import org.xwiki.model.reference.EntityReference;

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
}
