package com.xwiki.licensing.internal.enforcer;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.avalon.framework.activity.Initializable;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseManager;
import com.xwiki.licensing.LicenseValidator;
import com.xwiki.licensing.Licensor;

/**
 * Default implementation for {@link Licensor}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultLicensor implements Licensor, Initializable
{
    @Inject
    private LicenseManager licenseManager;

    @Inject
    private LicenseValidator licenseValidator;

    @Inject
    private EntityLicenseManager entityLicenseManager;

    @Inject
    private Provider<XWikiContext> context;

    @Override
    public void initialize() throws InitializationException
    {
        LicensingUtils.checkIntegrity(licenseManager, licenseValidator, entityLicenseManager);
    }

    @Override
    public License getLicense()
    {
        try {
            return getLicense(context.get().getDoc().getDocumentReference());
        } catch (Throwable e) {
            // Ignored
        }
        return null;
    }

    @Override
    public License getLicense(EntityReference reference)
    {
        try {
            return entityLicenseManager.get(reference);
        } catch (Throwable e) {
            // Ignored
        }
        return null;
    }

    @Override
    public License getLicense(ExtensionId extensionId)
    {
        try {
            return licenseManager.get(extensionId);
        } catch (Throwable e) {
            // Ignored
        }
        return null;
    }

    @Override
    public boolean hasLicensure()
    {
        License license = getLicense();
        return license == null || licenseValidator.isValid(license);
    }

    @Override
    public boolean hasLicensure(EntityReference reference)
    {
        License license = getLicense(reference);
        return license == null || licenseValidator.isValid(license);
    }

    @Override
    public boolean hasLicensure(ExtensionId extensionId)
    {
        License license = getLicense(extensionId);
        return license == null || licenseValidator.isValid(license);
    }
}
