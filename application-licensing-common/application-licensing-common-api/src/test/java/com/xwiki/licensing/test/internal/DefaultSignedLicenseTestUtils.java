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
package com.xwiki.licensing.test.internal;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.crypto.AsymmetricKeyFactory;
import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.crypto.params.cipher.asymmetric.PrivateKeyParameters;
import org.xwiki.crypto.params.cipher.asymmetric.PublicKeyParameters;
import org.xwiki.crypto.pkix.CertificateFactory;
import org.xwiki.crypto.pkix.CertificateGeneratorFactory;
import org.xwiki.crypto.pkix.CertificateProvider;
import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.PrincipalIndentifier;
import org.xwiki.crypto.pkix.params.x509certificate.DistinguishedName;
import org.xwiki.crypto.pkix.params.x509certificate.X509CertificateGenerationParameters;
import org.xwiki.crypto.pkix.params.x509certificate.X509CertificateParameters;
import org.xwiki.crypto.pkix.params.x509certificate.X509CertifiedPublicKey;
import org.xwiki.crypto.signer.SignerFactory;
import org.xwiki.properties.converter.Converter;

import com.xwiki.licensing.License;
import com.xwiki.licensing.SignedLicense;
import com.xwiki.licensing.test.SignedLicenseTestUtils;

/**
 * Default implementation of {@link SignedLicenseTestUtils}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultSignedLicenseTestUtils implements SignedLicenseTestUtils, Initializable
{
    private static final String RSA_PRIVATE_KEY =
        // Link to decoded ASN.1: https://goo.gl/kgV0IB
        "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDCmjim/3likJ4"
            + "VF564UyygqPjIX/z090AImLl0fDLUkIyCVTSd18wJ3axr1qjLtSgNPWet0puSxO"
            + "FH0AzFKRCJOjUkQRU8iAkz64MLAf9xrx4nBECciqeB941s01kLtG8C/UqC3O9Sw"
            + "HSdhtUpUU8V/91SiD09yNJsnODi3WqM3oLg1QYzKhoaD2mVo2xJLQ/QXqr2XIc5"
            + "i2Mlpfq6S5JNbFD/I+UFhBUlBNuDOEV7ttIt2eFMEUsfkCestGo0YoQYOpTLPcP"
            + "GRS7MnSY1CLWGUYqaMSnes0nS8ke2PPD4Q0suAZz4msnhNufanscstM8tcNtsZF"
            + "6hj0JvbZok89szAgMBAAECggEBAKWJ1SlR5ysORDtDBXRc5HiiZEbnSGIFtYXaj"
            + "N/nCsJBWBVCb+jZeirmU9bEGoB20OQ6WOjHYCnAqraQ51wMK5HgXvZBGtSMD/AH"
            + "pkiF4YsOYULlXiUL2aQ4NijdvEC1sz1Cw9CAKmElb83UtZ1ZGkJnjhi35giZvU5"
            + "BQRgbK5k57DFY66yv9VDg8tuD/enI9sRsCUZfCImuShGv4nLqhPMPg+1UxDPGet"
            + "Vs8uEaJQ017E14wLKLA0DlED13icelU1A7ufkEdeBSv/yZ7ENjervzPwa9nITK/"
            + "19uzqaHOcYZxmDQn6UHTnaLpIEaUvpp/pbed5S97ETSsqUBC8fqEUECgYEA/Sba"
            + "o6efydhlXDHbXtyvaJWao19sbI9OfxGC6dR2fZiBx8Do9kVDDbMtb1PYEfLhYbi"
            + "urmKGbUtcLSFgxNbZifUmG54M92nBsnsetMCqvMVNzYl2Je83V+NrIsLJjFIZ2C"
            + "BvZa/FKOLDTwSe35fNqaS0ExdwcGNMIT//bDQCmyECgYEAxMq6rN+HpBRuhvvst"
            + "V99zV+lI/1DzZuXExd+c3PSchiqkJrTLaQDvcaHQir9hK7RqF9vO7tvdluJjgX+"
            + "f/CMPNQuC5k6vY/0fS4V2NQWtln9BBSzHtocTnZzFNq8tAZqyEhZUHIbkncroXv"
            + "eUXqtlfOnKB2aYI/+3gPEMYJlH9MCgYA4exjA9r9B65QB0+Xb7mT8cpSD6uBoAD"
            + "lFRITu4sZlE0exZ6sSdzWUsutqMUy+BHCguvHOWpEfhXbVYuMSR9VVYGrWMpc2B"
            + "FSBG9MoBOyTHXpUZ10C7bJtW4IlyUvqkM7PV71C9MqKar2kvaUswdPTC7pZoBso"
            + "GB9+M6crXxdNwQKBgDUVMlGbYi1CTaYfonQyM+8IE7WnhXiatZ+ywKtH3MZmHOw"
            + "wtzIigdfZC3cvvX7i4S73vztvjdtxSaODvmiobEukOF9sj8m+YQa7Pa1lWFML5x"
            + "IIu2BhGS2ZCeXgMvKkoH0x9tWaUhGqD5zZmtiDrPs75CUQBypw7SDaBzwLnld9A"
            + "oGBAPgUh90PvUzbVVkzpVCPI82cmOIVMI1rDE6uCeNzIlN6Xu80RimCSaaDsESi"
            + "tBtoVWLRWWmuCINyqr6e9AdyvbvT6mQCjbn9+y7t6ZAhLaya5ZMUVEBLyLLqMzr"
            + "y oi/huj7m4nV4kPZz9LKxDRu3r6o0Pah+daDsTxEYObtsKa7e";

    private static final String RSA_PUBLIC_KEY =
        // Link to decoded ASN.1: https://goo.gl/2YsSco
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwpo4pv95YpCeFReeuFM"
            + "soKj4yF/89PdACJi5dHwy1JCMglU0ndfMCd2sa9aoy7UoDT1nrdKbksThR9AMxS"
            + "kQiTo1JEEVPIgJM+uDCwH/ca8eJwRAnIqngfeNbNNZC7RvAv1KgtzvUsB0nYbVK"
            + "VFPFf/dUog9PcjSbJzg4t1qjN6C4NUGMyoaGg9plaNsSS0P0F6q9lyHOYtjJaX6"
            + "ukuSTWxQ/yPlBYQVJQTbgzhFe7bSLdnhTBFLH5AnrLRqNGKEGDqUyz3DxkUuzJ0"
            + "mNQi1hlGKmjEp3rNJ0vJHtjzw+ENLLgGc+JrJ4Tbn2p7HLLTPLXDbbGReoY9Cb2"
            + "2aJPPbMwIDAQAB";

    private static final String SIGNED_LICENSE = "MIAGCSqGSIb3DQEHAqCAMIACAQExDzANBglghkgBZQMEAgEFADCABgkqhkiG9w0B"
        + "BwGggASCAxo8P3htbCB2ZXJzaW9uPSIxLjAiIGVuY29kaW5nPSJVVEYtOCIgc3Rh"
        + "bmRhbG9uZT0ieWVzIj8+CjxsaWNlbnNlIHhtbG5zPSJodHRwOi8vd3d3Lnh3aWtp"
        + "LmNvbS9saWNlbnNlIiBpZD0iNjQ4NThiZTgtOGEyNS00YWM5LTg1ODAtNWUxYTUx"
        + "MWIzMTE3Ij4KICAgIDxtb2RlbFZlcnNpb24+Mi4wLjA8L21vZGVsVmVyc2lvbj4K"
        + "ICAgIDx0eXBlPkZSRUU8L3R5cGU+CiAgICA8bGljZW5zZWQ+CiAgICAgICAgPGZl"
        + "YXR1cmVzPgogICAgICAgICAgICA8ZmVhdHVyZT4KICAgICAgICAgICAgICAgIDxp"
        + "ZD5jb20ueHdpa2kubGljZW5zaW5nOmFwcGxpY2F0aW9uLWxpY2Vuc2luZy10ZXN0"
        + "LWV4YW1wbGU8L2lkPgogICAgICAgICAgICA8L2ZlYXR1cmU+CiAgICAgICAgPC9m"
        + "ZWF0dXJlcz4KICAgIDwvbGljZW5zZWQ+CiAgICA8cmVzdHJpY3Rpb25zPgogICAg"
        + "ICAgIDxpbnN0YW5jZXM+CiAgICAgICAgICAgIDxpbnN0YW5jZT5kYTI5MGI5MC1k"
        + "MzNkLTQ0YWUtOGY4ZC02NmQ5ZWUwZTQwZTY8L2luc3RhbmNlPgogICAgICAgIDwv"
        + "aW5zdGFuY2VzPgogICAgICAgIDxleHBpcmU+MjAyNS0wNC0wM1QxMTo0ODowMC44"
        + "MDIrMDM6MDA8L2V4cGlyZT4KICAgIDwvcmVzdHJpY3Rpb25zPgogICAgPGxpY2Vu"
        + "Y2VlPgogICAgICAgIDxmaXJzdE5hbWU+SmFuZTwvZmlyc3ROYW1lPgogICAgICAg"
        + "IDxsYXN0TmFtZT5Eb2V5PC9sYXN0TmFtZT4KICAgICAgICA8ZW1haWw+ZG93QG1h"
        + "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";

    private class ArrayBasedCertificateProvider implements CertificateProvider
    {
        Collection<X509CertifiedPublicKey> certs = new ArrayList<>();

        public ArrayBasedCertificateProvider(Collection<CertifiedPublicKey> certs)
        {
            for (CertifiedPublicKey cert : certs) {
                this.certs.add((X509CertifiedPublicKey) cert);
            }
        }

        @Override
        public CertifiedPublicKey getCertificate(byte[] keyIdentifier)
        {
            for (X509CertifiedPublicKey cert : certs) {
                if (Arrays.equals(cert.getSubjectKeyIdentifier(), keyIdentifier)) {
                    return cert;
                }
            }
            return null;
        }

        @Override
        public CertifiedPublicKey getCertificate(PrincipalIndentifier issuer, BigInteger serial)
        {
            for (X509CertifiedPublicKey cert : certs) {
                if (cert.getIssuer().equals(issuer) && cert.getSerialNumber().equals(serial)) {
                    return cert;
                }
            }
            return null;
        }

        @Override
        public CertifiedPublicKey getCertificate(PrincipalIndentifier issuer, BigInteger serial, byte[] keyIdentifier)
        {
            for (X509CertifiedPublicKey cert : certs) {
                if (cert.getIssuer().equals(issuer) && cert.getSerialNumber().equals(serial) && Arrays.equals(cert.getSubjectKeyIdentifier(), keyIdentifier)){
                    return cert;
                }
            }
            return null;
        }

        @Override
        public Collection<CertifiedPublicKey> getCertificate(PrincipalIndentifier subject)
        {
            List<CertifiedPublicKey> result = new ArrayList<>();
            for (X509CertifiedPublicKey cert : certs) {
                if (cert.getSubject().equals(subject)) {
                    result.add(cert);
                }
            }
            return result;
        }
    }

    @Inject
    @Named("Base64")
    private BinaryStringEncoder base64encoder;

    @Inject
    @Named("RSA")
    private AsymmetricKeyFactory rsaKeyFactory;

    @Inject
    @Named("X509")
    private CertificateFactory certFactory;

    @Inject
    private Converter<License> converter;

    @Inject
    @Named("SHA256withRSAEncryption")
    private SignerFactory rsaSignerFactory;

    @Inject
    @Named("X509")
    private CertificateGeneratorFactory certificateFactory;

    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    private CertifiedKeyPair keyPair;
    private CertificateProvider certProvider;
    private SignedLicense signedLicense;
    private X509CertifiedPublicKey caKey;


    @Override
    public void initialize() throws InitializationException
    {
        try {
            PrivateKeyParameters rsaPrivateKey = rsaKeyFactory.fromPKCS8(base64encoder.decode(RSA_PRIVATE_KEY));
            PublicKeyParameters rsaPublicKey = rsaKeyFactory.fromX509(base64encoder.decode(RSA_PUBLIC_KEY));

            CertifiedPublicKey v3Certificate =
                certificateFactory.getInstance(rsaSignerFactory.getInstance(true, rsaPrivateKey),
                        new X509CertificateGenerationParameters(null))
                    .generate(new DistinguishedName("CN=Test"), rsaPublicKey,
                        new X509CertificateParameters());
            caKey = (X509CertifiedPublicKey) v3Certificate;

            keyPair = new CertifiedKeyPair(rsaPrivateKey, caKey);
            certProvider = new ArrayBasedCertificateProvider(Collections.singletonList(caKey));
            signedLicense = converter.convert(License.class, base64encoder.decode(SIGNED_LICENSE));
        } catch(Exception e) {
            throw new InitializationException("Unable to initialized Signed License tests", e);
        }
    }

    @Override
    public CertifiedKeyPair getSigningKeyPair()
    {
        return keyPair;
    }

    @Override
    public SignerFactory getSignerFactory()
    {
        return rsaSignerFactory;
    }

    @Override
    public CertificateProvider getCertificateProvider()
    {
        return certProvider;
    }

    @Override
    public CertifiedPublicKey getRootCertificate()
    {
        return caKey;
    }

    @Override
    public CertifiedPublicKey getIntermediateCertificate()
    {
        return caKey;
    }

    @Override
    public CertifiedPublicKey getSigningCertificate()
    {
        return caKey;
    }

    @Override
    public SignedLicense getSignedLicense()
    {
        return signedLicense;
    }

    @Override
    public Converter<License> getLicenseConverter()
    {
        return converter;
    }
}
