package com.xwiki.licensing.test;

import org.xwiki.component.annotation.Role;
import org.xwiki.crypto.pkix.CertificateProvider;
import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.signer.SignerFactory;
import org.xwiki.properties.converter.Converter;

import com.xwiki.licensing.License;
import com.xwiki.licensing.SignedLicense;

/**
 * Provide signed test license for testing the licensing application.
 *
 * @version $Id$
 */
@Role
public interface SignedLicenseTestUtils
{
    CertifiedKeyPair getSigningKeyPair();

    SignerFactory getSignerFactory();

    CertificateProvider getCertificateProvider();

    CertifiedPublicKey getRootCertificate();

    CertifiedPublicKey getIntermediateCertificate();

    CertifiedPublicKey getSigningCertificate();

    SignedLicense getSignedLicense();

    Converter<License> getLicenseConverter();
}
