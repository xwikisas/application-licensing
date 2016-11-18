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
    @Inject
    private SignedLicenseGenerator signedLicenseGenerator;

    @Inject
    @Named("SHA1withRSAEncryption")
    private SignerFactory signerFactory;

    public License generate(License license, ScriptingKeyStore keyStore,
        ScriptingCertificateStore certificateStore, String certificateSubject, String keyPassword) throws Exception
    {
        return this.signedLicenseGenerator.generate(license,
            keyStore.retrieve(getCertificate(license.getType(), certificateStore, certificateSubject), keyPassword),
            this.signerFactory,
            certificateStore.getCertificateProvider());
    }

    public License retrieveGeneratedLicense(ScriptLicenseStore licenseStore, LicenseId licenseId) throws IOException
    {
        return licenseStore.retrieve(licenseId);
    }

    public void storeGeneratedLicense(ScriptLicenseStore licenseStore, License license) throws IOException
    {
        licenseStore.store(license);
    }

    private CertifiedPublicKey getCertificate(LicenseType licenseType, ScriptingCertificateStore certificateStore,
        String certificateSubject) throws CertificateStoreException, AccessDeniedException
    {
        DistinguishedName dn = new DistinguishedName(certificateSubject);
        for (CertifiedPublicKey certificate : certificateStore.getAllCertificates()) {
            if (certificate.getSubject().equals(dn)) {
                return certificate;
            }
        }

        throw new CertificateStoreException(
            String.format("Cannot find certificate in certificate store for license type [%s]", licenseType));
    }
}
