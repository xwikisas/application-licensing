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
package com.xwiki.licensing.script;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.avalon.framework.activity.Initializable;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.extension.ExtensionId;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.properties.converter.Converter;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
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

    @Inject
    private EntityReferenceProvider entityReferenceProvider;

    @Inject
    @Named("Base64")
    private BinaryStringEncoder base64decoder;

    @Inject
    private Converter<License> converter;

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
     * Add a new signed license to the current set of active license. The added license is checked to be applicable to
     * the current wiki instance, else it will not be added. The license is also checked to be more useful than the
     * currently installed licenses. If the license does not provides any improvement of the licensing state of this
     * wiki, it will not be added. These evaluations are done for each licensed extension independently, whether the
     * extension are currently installed or not.
     *
     * @param license a base 64 representation of the license to add.
     * @return true if the license has been successfully added, false if it was useless.
     * @throws AccessDeniedException if the user does not have admin rights on the main wiki.
     * @throws IOException if the user does not have admin rights on the main wiki.
     */
    public boolean addLicense(String license) throws AccessDeniedException, IOException {
        contextualAuthorizationManager.checkAccess(Right.ADMIN,
            entityReferenceProvider.getDefaultReference(EntityType.WIKI));
        return licenseManager.add(converter.convert(License.class, base64decoder.decode(license)));
    }

    /**
     * Add a new signed license to the current set of active license. The added license is checked to be applicable to
     * the current wiki instance, else it will not be added. The license is also checked to be more useful than the
     * currently installed licenses. If the license does not provides any improvement of the licensing state of this
     * wiki, it will not be added. These evaluations are done for each licensed extension independently, whether the
     * extension are currently installed or not.
     *
     * @param license a base 64 representation of the license to add.
     * @return true if the license has been successfully added, false if it was useless.
     * @throws AccessDeniedException if the user does not have admin rights on the main wiki.
     * @throws IOException if the user does not have admin rights on the main wiki.
     */
    public boolean addLicense(byte[] license) throws AccessDeniedException, IOException {
        contextualAuthorizationManager.checkAccess(Right.ADMIN,
            entityReferenceProvider.getDefaultReference(EntityType.WIKI));
        return licenseManager.add(converter.convert(License.class, license));
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
