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
package com.xwiki.licensing.internal.issuing;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.x509certificate.DistinguishedName;
import org.xwiki.crypto.signer.SignerFactory;
import org.xwiki.crypto.store.CertificateStore;
import org.xwiki.crypto.store.CertificateStoreException;
import org.xwiki.crypto.store.FileStoreReference;
import org.xwiki.crypto.store.KeyStore;
import org.xwiki.crypto.store.KeyStoreException;
import org.xwiki.crypto.store.WikiStoreReference;
import org.xwiki.model.reference.SpaceReference;

import com.xpn.xwiki.XWikiException;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.SignedLicense;
import com.xwiki.licensing.internal.SignedLicenseGenerator;

/**
 * Signs licenses with the same keys and certificates as the {@code #generateLicense} macro of
 * {@code License.Code.LicenseDetailsMacros}, so that the licenses issued through the API are identical to the ones
 * Sales generates.
 *
 * @version $Id$
 * @since 1.34
 */
@Component(roles = LicenseSigner.class)
@Singleton
public class LicenseSigner
{
    /**
     * The key store, in the permanent directory.
     */
    static final String KEY_STORE = "license-keystore";

    /**
     * The space holding the issuer certificates.
     */
    static final List<String> CERTIFICATES_SPACE = List.of("License", "Certificates");

    private static final String ISSUER_DN = "CN=%s License Issuer %d,OU=Licensing,O=XWiki SAS,L=Paris,C=FR";

    @Inject
    private SignedLicenseGenerator signedLicenseGenerator;

    @Inject
    @Named("SHA256withRSAEncryption")
    private SignerFactory signerFactory;

    @Inject
    @Named("X509file")
    private KeyStore keyStore;

    @Inject
    @Named("X509wiki")
    private CertificateStore certificateStore;

    @Inject
    private org.xwiki.environment.Environment environment;

    @Inject
    private ApiConfiguration configuration;

    /**
     * Signs a license with the issuer certificate of its type for the given year.
     *
     * @param license the license to sign; its type selects the certificate and the key
     * @param year the year the license is issued, which selects the issuer certificate (the {@code #generateLicense}
     *     macro uses the current year)
     * @return the signed license
     * @throws LicenseSigningException if the certificate, the key or the key password can't be found, or if the
     *     signature fails
     */
    public SignedLicense sign(License license, int year) throws LicenseSigningException
    {
        LicenseType type = license.getType();
        String issuer = getIssuer(type, year);
        try {
            WikiStoreReference certificates = new WikiStoreReference(
                new SpaceReference(this.configuration.getMainWiki().getName(), CERTIFICATES_SPACE));
            CertifiedPublicKey certificate = findCertificate(certificates, issuer);
            return this.signedLicenseGenerator.generate(license,
                this.keyStore.retrieve(getKeyStoreReference(), certificate,
                    getPassword(type).getBytes(StandardCharsets.UTF_8)),
                this.signerFactory, this.certificateStore.getCertificateProvider(certificates));
        } catch (KeyStoreException | CertificateStoreException | XWikiException | GeneralSecurityException
            | IOException e) {
            throw new LicenseSigningException(
                String.format("Failed to sign the license [%s] with [%s].", license.getId(), issuer), e);
        }
    }

    /**
     * @param type a license type
     * @param year the year the license is signed
     * @return the distinguished name of the certificate that signs the licenses of that type during that year
     */
    static String getIssuer(LicenseType type, int year)
    {
        String name;
        if (type == LicenseType.PAID) {
            name = "Paid";
        } else if (type == LicenseType.FREE) {
            name = "Free";
        } else {
            // Same fallback as the macro.
            name = "Trial";
        }
        return String.format(ISSUER_DN, name, year);
    }

    private CertifiedPublicKey findCertificate(WikiStoreReference certificates, String issuer)
        throws CertificateStoreException, LicenseSigningException
    {
        DistinguishedName dn = new DistinguishedName(issuer);
        for (CertifiedPublicKey certificate : this.certificateStore.getAllCertificates(certificates)) {
            if (dn.equals(certificate.getSubject())) {
                return certificate;
            }
        }
        throw new LicenseSigningException(String.format("Cannot find the certificate [%s] in %s.", issuer,
            CERTIFICATES_SPACE));
    }

    private String getPassword(LicenseType type) throws XWikiException, LicenseSigningException
    {
        String password = this.configuration.getSigningPassword(type);
        if (password == null || password.isEmpty()) {
            throw new LicenseSigningException(
                String.format("No key password is configured for [%s] licenses.", type));
        }
        return password;
    }

    private FileStoreReference getKeyStoreReference()
    {
        return new FileStoreReference(new File(this.environment.getPermanentDirectory(), KEY_STORE), true);
    }
}
