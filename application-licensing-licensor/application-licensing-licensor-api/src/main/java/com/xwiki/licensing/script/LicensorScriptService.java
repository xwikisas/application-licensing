package com.xwiki.licensing.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.avalon.framework.activity.Initializable;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseManager;
import com.xwiki.licensing.Licensor;
import com.xwiki.licensing.internal.enforcer.LicensingUtils;

/**
 * Script service for the licensor.
 *
 * @version $Id$
 */
@Component
@Named("licensor")
@Singleton
public class LicensorScriptService implements ScriptService, Initializable
{
    @Inject
    private Licensor licensor;

    @Inject
    private LicenseManager licenseManager;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Override
    public void initialize() throws InitializationException
    {
        if (!LicensingUtils.isPristineImpl(licensor)) {
            throw new InitializationException("Integrity check failed while loading the licensor.");
        }
    }

    /**
     * Retrieve the currently applicable license for the current context document if any.
     * Equivalent to licensor.getLicense() call.
     *
     * @return a license, or null if there is no current document, or the current document is not subject to licensing.
     */
    public License getLicense()
    {
        return licensor.getLicense();
    }

    /**
     * Retrieve the currently applicable license for the given installed extension.
     * Equivalent to licensor.getLicense(ExtensionId) call.
     *
     * @param extensionId identifier of an installed extension
     * @return a license, or null if the given installed extension is not subject to licensing.
     */
    public License getLicenseForExtension(ExtensionId extensionId)
    {
        return licensor.getLicense(extensionId);
    }

    /**
     * Get the license applicable to the given reference.
     * Equivalent to licensor.getLicense(EntityReference) call.
     *
     * @param reference the reference to get the license from.
     * @return a license, or null if the given reference is not subject to licensing.
     */
    public License getLicenseForEntity(EntityReference reference)
    {
        return licensor.getLicense(reference);
    }

    /**
     * Equivalent to licensor.hasLicensure() call.
     * @return true if the current document has a valid license or is not subject to licensing.
     */
    public boolean hasLicensure()
    {
        return licensor.hasLicensure();
    }

    /**
     * Check if the given extension is covered by a valid license.
     * Equivalent to licensor.hasLicensure(ExtensionId) call.
     *
     * @param extensionId the identifier of the extension for which licensure should be checked.
     * @return true if the given extension has a valid license or is not subject to licensing.
     */
    public boolean hasLicensureForExtension(ExtensionId extensionId)
    {
        return licensor.hasLicensure(extensionId);
    }

    /**
     * Check if the given entity is covered by a valid license.
     * Equivalent to licensor.hasLicensure(EntityReference) call.
     *
     * @param reference the reference of the entity for which licensure should be checked.
     * @return true if the given reference has a valid license or is not subject to licensing.
     */
    public boolean hasLicensureForEntity(EntityReference reference)
    {
        return licensor.hasLicensure(reference);
    }

    /**
     * @return the current licensor.
     */
    public Licensor getLicensor()
    {
        return licensor;
    }

    /**
     * @return the licence manager (programming rights is required).
     */
    public LicenseManager getLicenseManager() {
        if (contextualAuthorizationManager.hasAccess(Right.PROGRAM)) {
            return licenseManager;
        }
        return null;
    }

    /**
     * Check that a valid license is covering the current document and redirect to an information page if not.
     */
    public void checkLicense()
    {
        if (!licensor.hasLicensure()) {
            // TODO: redirect to a buy license stuff
        }
    }
}
