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
import java.util.Calendar;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.x509certificate.DistinguishedName;
import org.xwiki.crypto.script.ScriptingCertificateStore;
import org.xwiki.crypto.script.ScriptingKeyStore;
import org.xwiki.crypto.signer.SignerFactory;
import org.xwiki.crypto.store.CertificateStoreException;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.stability.Unstable;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseId;
import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.SignedLicense;
import com.xwiki.licensing.internal.SignedLicenseGenerator;

/**
 * @version $Id$
 * @since 1.1
 */
@Component
@Named("licensing.manager")
@Singleton
@Unstable
public class ManagerScriptService extends AbstractLicenseScriptService
{
    private static final String GENERATED_STORE_NAME = "generated-licenses";

    @Inject
    private SignedLicenseGenerator signedLicenseGenerator;

    @Inject
    @Named("SHA1withRSAEncryption")
    private SignerFactory signerFactory;

    public SignedLicense generate(License license, ScriptingKeyStore keyStore,
        ScriptingCertificateStore certificateStore, String keyPassword) throws Exception
    {
        return this.signedLicenseGenerator.generate(license,
            keyStore.retrieve(getCertificate(license.getType(), certificateStore), keyPassword),
            this.signerFactory,
            certificateStore.getCertificateProvider());
    }

    public SignedLicense retrieveGeneratedLicense(LicenseId licenseId) throws IOException
    {
        ScriptLicenseStore licenseStore = new ScriptLicenseStore(this.filesystemLicenseStore,
            getFileLicenseStoreReference(GENERATED_STORE_NAME, true));
        License license = licenseStore.retrieve(licenseId);
        if (license == null) {
            throw new IOException(String.format("License not found in store for ID [%s]", licenseId.toString()));
        }
        if (!(license instanceof SignedLicense)) {
            throw new IOException(String.format("Stored license for ID [%s] is not signed when it should!",
                licenseId.toString()));
        }
        return (SignedLicense) license;
    }

    public void storeGeneratedLicense(License license) throws IOException
    {
        ScriptLicenseStore licenseStore = new ScriptLicenseStore(this.filesystemLicenseStore,
            getFileLicenseStoreReference(GENERATED_STORE_NAME, true));
        licenseStore.store(license);
    }

    private CertifiedPublicKey getCertificate(LicenseType licenseType, ScriptingCertificateStore certificateStore)
        throws CertificateStoreException, AccessDeniedException
    {
        DistinguishedName subject = getLicenseSubject(licenseType);
        for (CertifiedPublicKey certificate : certificateStore.getAllCertificates()) {
            if (certificate.getSubject().equals(subject)) {
                return certificate;
            }
        }

        throw new CertificateStoreException(
            String.format("Cannot find certificate in certificate store for license type [%s]", licenseType));
    }

    private DistinguishedName getLicenseSubject(LicenseType licenseType)
    {
        return new DistinguishedName(String.format("CN={} License Issuer {},OU=Licensing,O=XWiki SAS,L=Paris,C=FR",
            StringUtils.capitalize(StringUtils.lowerCase(licenseType.toString())),
            Calendar.getInstance().get(Calendar.YEAR)));
    }
}
