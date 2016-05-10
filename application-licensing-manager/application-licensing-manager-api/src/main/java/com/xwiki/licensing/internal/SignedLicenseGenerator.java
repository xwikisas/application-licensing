package com.xwiki.licensing.internal;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.xwiki.component.annotation.Role;
import org.xwiki.crypto.pkix.CertificateProvider;
import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.signer.SignerFactory;

import com.xwiki.licensing.License;
import com.xwiki.licensing.SignedLicense;

/**
 * Generate a signed license.
 *
 * @version $Id$
 */
@Role
public interface SignedLicenseGenerator
{
    /**
     * Generate a signature.
     *
     * @param license a license to sign.
     * @param keyPair a certified keypair to sign the license.
     * @param signerFactory a signer factory compatible with the keyPair provided
     * @param certificateProvider a certificate provider providing the full certificate chain for the signing key.
     * @return a verified signed license.
     * @throws GeneralSecurityException on signature operation error.
     * @throws IOException on encoding error.
     */
    SignedLicense generate(License license,
        CertifiedKeyPair keyPair, SignerFactory signerFactory, CertificateProvider certificateProvider)
        throws GeneralSecurityException, IOException;
}
