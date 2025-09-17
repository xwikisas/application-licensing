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
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.licensing.LicensedExtensionManager;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.extension.ExtensionId;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.properties.converter.Converter;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.sheet.SheetManager;
import org.xwiki.stability.Unstable;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseManager;
import com.xwiki.licensing.Licensor;
import com.xwiki.licensing.internal.UserCounter;
import com.xwiki.licensing.internal.enforcer.LicensingUtils;

/**
 * Script service for the licensor.
 *
 * @version $Id$
 */
@Component
@Named("licensing.licensor")
@Singleton
public class LicensorScriptService implements ScriptService, Initializable
{
    @Inject
    private Logger logger;

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

    @Inject
    private UserCounter userCounter;

    @Inject
    private SheetManager sheetManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private LicensedExtensionManager licensedExtensionManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public void initialize() throws InitializationException
    {
        if (!LicensingUtils.isPristineImpl(licensor)) {
            throw new InitializationException("Integrity check failed while loading the licensor.");
        }
    }

    /**
     * Retrieve the currently applicable license for the current context document if any. Equivalent to
     * licensor.getLicense() call.
     *
     * @return a license, or null if there is no current document, or the current document is not subject to licensing.
     */
    public License getLicense()
    {
        return licensor.getLicense();
    }

    /**
     * Retrieve the currently applicable license for the given installed extension. Equivalent to
     * licensor.getLicense(ExtensionId) call.
     *
     * @param extensionId identifier of an installed extension
     * @return a license, or null if the given installed extension is not subject to licensing.
     */
    public License getLicenseForExtension(ExtensionId extensionId)
    {
        return licensor.getLicense(extensionId);
    }

    /**
     * Get the license applicable to the given reference. Equivalent to licensor.getLicense(EntityReference) call.
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
     *
     * @return true if the current document has a valid license or is not subject to licensing.
     */
    public boolean hasLicensure()
    {
        return licensor.hasLicensure();
    }

    /**
     * Check if the given extension is covered by a valid license. Equivalent to licensor.hasLicensure(ExtensionId)
     * call.
     *
     * @param extensionId the identifier of the extension for which licensure should be checked.
     * @return true if the given extension has a valid license or is not subject to licensing.
     */
    public boolean hasLicensureForExtension(ExtensionId extensionId)
    {
        return licensor.hasLicensure(extensionId, xcontextProvider.get().getUserReference());
    }

    /**
     * Check if the given entity is covered by a valid license. Equivalent to licensor.hasLicensure(EntityReference)
     * call.
     *
     * @param reference the reference of the entity for which licensure should be checked.
     * @return true if the given reference has a valid license or is not subject to licensing.
     */
    public boolean hasLicensureForEntity(EntityReference reference)
    {
        return licensor.hasLicensure(reference, xcontextProvider.get().getUserReference());
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
    public LicenseManager getLicenseManager()
    {
        if (contextualAuthorizationManager.hasAccess(Right.PROGRAM)) {
            return licenseManager;
        }
        return null;
    }

    /**
     * @return the {@link LicensedExtensionManager} (programming rights is required).
     * @since 1.29
     */
    public LicensedExtensionManager getLicensedExtensionManager()
    {
        if (contextualAuthorizationManager.hasAccess(Right.PROGRAM)) {
            return licensedExtensionManager;
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
    public boolean addLicense(String license) throws AccessDeniedException, IOException
    {
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
    public boolean addLicense(byte[] license) throws AccessDeniedException, IOException
    {
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

    /**
     * @return the user count
     * @since 1.6
     */
    @Unstable
    public Long getUserCount()
    {
        try {
            return this.userCounter.getUserCount();
        } catch (Exception e) {
            this.logger.warn("Failed to count the users. Root cause is: [{}].", ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    /**
     * @param documentReference the document for which to search for associated sheets with missing license
     * @param action represents the action the sheets have to match
     * @return the list of sheets associated with the specified document and action and that are missing a license
     */
    public List<DocumentReference> getUnlicensedSheets(DocumentReference documentReference, String action)
    {
        try {
            DocumentModelBridge documentModelBridge = documentAccessBridge.getDocument(documentReference);
            List<DocumentReference> sheetsUsedByDocument = sheetManager.getSheets(documentModelBridge, action);
            return sheetsUsedByDocument.stream()
                .filter(sheet -> !this.hasLicensureForEntity(sheet))
                .collect(Collectors.toList());
        } catch (Exception e) {
            this.logger.error("Failed to get the list of associated sheets. Root cause is: [{}].",
                ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }
}
