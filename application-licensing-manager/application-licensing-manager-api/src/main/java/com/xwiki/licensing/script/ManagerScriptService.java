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
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.stability.Unstable;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseId;
import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.internal.SignedLicenseGenerator;

/**
 * Script services related to Licensing Management API.
 *
 * @version $Id$
 * @since 1.1
 */
@Component
@Named("licensing.manager")
@Singleton
@Unstable
public class ManagerScriptService implements ScriptService
{
    @Inject
    private SignedLicenseGenerator signedLicenseGenerator;

    @Inject
    @Named("SHA256withRSAEncryption")
    private SignerFactory signerFactory;

    /**
     * Generate a signature for the passed license.
     *
     * @param license the license to sign (must contain a filled license type which will be used to find the right
     *        Certificate)
     * @param keyStore the store holding the keys
     * @param certificateStore the store holding the certificates
     * @param certificateSubject the DN for the certificate to use to sign the license. The certificate store will be
     *        searched for a matching DN (example of DN
     *        {@code CN=License Issuer 2016,OU=Licensing,O=XWiki SAS,L=Paris,C=FR})
     * @param keyPassword the password to be able to get the matching key from the keystore
     * @return a signed license
     * @throws Exception if an error occurred at any level during the license signing process
     */
    public License generate(License license, ScriptingKeyStore keyStore,
        ScriptingCertificateStore certificateStore, String certificateSubject, String keyPassword) throws Exception
    {
        return this.signedLicenseGenerator.generate(license,
            keyStore.retrieve(getCertificate(license.getType(), certificateStore, certificateSubject), keyPassword),
            this.signerFactory,
            certificateStore.getCertificateProvider());
    }

    /**
     * Retrieve a identified license from a multi-license store.
     *
     * @param licenseStore the store from which to find the license
     * @param licenseId the identifier of the license to be retrieved
     * @return the retrieved license or NULL if the license has not be found in the store
     * @throws IOException when an error occurs
     */
    public License retrieveGeneratedLicense(ScriptLicenseStore licenseStore, LicenseId licenseId) throws IOException
    {
        return licenseStore.retrieve(licenseId);
    }

    /**
     * Store a given license into a given store.
     *
     * @param licenseStore the store into which to save the license
     * @param license the license to be stored
     * @throws IOException when an error occurs
     */
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
