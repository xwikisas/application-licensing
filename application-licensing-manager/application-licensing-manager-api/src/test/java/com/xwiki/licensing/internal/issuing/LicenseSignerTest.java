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
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.x509certificate.DistinguishedName;
import org.xwiki.crypto.signer.internal.cms.DefaultCMSSignedDataGenerator;
import org.xwiki.crypto.store.CertificateStore;
import org.xwiki.crypto.store.FileStoreReference;
import org.xwiki.crypto.store.KeyStore;
import org.xwiki.crypto.store.KeyStoreException;
import org.xwiki.crypto.store.WikiStoreReference;
import org.xwiki.instance.InstanceId;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseId;
import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.LicensedFeatureId;
import com.xwiki.licensing.SignedLicense;
import com.xwiki.licensing.internal.DefaultSignedLicenseGenerator;
import com.xwiki.licensing.internal.XmlStringLicenseSerializer;
import com.xwiki.licensing.test.LicensingComponentList;
import com.xwiki.licensing.test.SignedLicenseTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LicenseSigner}. The license is really signed, with the test keys of the common module.
 *
 * @version $Id$
 */
@ComponentTest
@LicensingComponentList
@ComponentList({ DefaultSignedLicenseGenerator.class, DefaultCMSSignedDataGenerator.class,
    XmlStringLicenseSerializer.class })
class LicenseSignerTest
{
    private static final int YEAR = 2026;

    private static final WikiReference MAIN_WIKI = new WikiReference("xwiki");

    private static final byte[] PASSWORD = "secret".getBytes(StandardCharsets.UTF_8);

    @InjectMockComponents
    private LicenseSigner signer;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    @Named("X509file")
    private KeyStore keyStore;

    @MockComponent
    @Named("X509wiki")
    private CertificateStore certificateStore;

    @MockComponent
    private org.xwiki.environment.Environment environment;

    @MockComponent
    private ApiConfiguration configuration;

    @TempDir
    private File permanentDirectory;

    private SignedLicenseTestUtils utils;

    private CertifiedPublicKey paidCertificate;

    @BeforeEach
    void setUp() throws Exception
    {
        this.utils = this.componentManager.getInstance(SignedLicenseTestUtils.class);
        when(this.environment.getPermanentDirectory()).thenReturn(this.permanentDirectory);
        when(this.configuration.getMainWiki()).thenReturn(MAIN_WIKI);
        when(this.configuration.getSigningPassword(LicenseType.PAID)).thenReturn("secret");

        // The certificate store holds the issuers of every type and year; only the subject is used to choose.
        this.paidCertificate = mockCertificate(LicenseSigner.getIssuer(LicenseType.PAID, YEAR));
        List<CertifiedPublicKey> certificates =
            List.of(mockCertificate(LicenseSigner.getIssuer(LicenseType.TRIAL, YEAR)),
                mockCertificate(LicenseSigner.getIssuer(LicenseType.PAID, YEAR - 1)), this.paidCertificate);
        when(this.certificateStore.getAllCertificates(isCertificatesSpace())).thenReturn(certificates);
        when(this.certificateStore.getCertificateProvider(isCertificatesSpace()))
            .thenReturn(this.utils.getCertificateProvider());
    }

    @Test
    void issuerIsTheOneOfTheGenerateLicenseMacro()
    {
        assertEquals("CN=Paid License Issuer 2026,OU=Licensing,O=XWiki SAS,L=Paris,C=FR",
            LicenseSigner.getIssuer(LicenseType.PAID, 2026));
        assertEquals("CN=Trial License Issuer 2027,OU=Licensing,O=XWiki SAS,L=Paris,C=FR",
            LicenseSigner.getIssuer(LicenseType.TRIAL, 2027));
        assertEquals("CN=Free License Issuer 2026,OU=Licensing,O=XWiki SAS,L=Paris,C=FR",
            LicenseSigner.getIssuer(LicenseType.FREE, 2026));
    }

    @Test
    void signWithTheKeyOfTheCurrentIssuer() throws Exception
    {
        File keyStoreFile = new File(this.permanentDirectory, "license-keystore");
        when(this.keyStore.retrieve(argThat(reference -> reference instanceof FileStoreReference
            && ((FileStoreReference) reference).getFile().equals(keyStoreFile)
            && ((FileStoreReference) reference).isMulti()), eq(this.paidCertificate), eq(PASSWORD)))
            .thenReturn(this.utils.getSigningKeyPair());

        License license = createLicense();
        SignedLicense signedLicense = this.signer.sign(license, YEAR);

        assertEquals(license.getId(), signedLicense.getId());
        assertEquals(LicenseType.PAID, signedLicense.getType());
        assertEquals(List.copyOf(license.getFeatureIds()), List.copyOf(signedLicense.getFeatureIds()));
        assertEquals(license.getLicensee(), signedLicense.getLicensee());
        assertFalse(signedLicense.getCertificates().isEmpty());

        // Customer wikis can decode the signed license back.
        License decoded = this.utils.getLicenseConverter().convert(License.class, signedLicense.getEncoded());
        assertEquals(license.getId(), decoded.getId());
        assertEquals(license.getExpirationDate(), decoded.getExpirationDate());
    }

    @Test
    void signFailsWithoutCertificateForTheType() throws Exception
    {
        License license = createLicense();
        license.setType(LicenseType.FREE);

        LicenseSigningException exception = assertThrows(LicenseSigningException.class,
            () -> this.signer.sign(license, YEAR));

        assertEquals(String.format("Cannot find the certificate [%s] in [License, Certificates].",
            LicenseSigner.getIssuer(LicenseType.FREE, YEAR)), exception.getMessage());
    }

    @Test
    void signFailsWithoutPassword()
    {
        License license = createLicense();
        license.setType(LicenseType.TRIAL);

        LicenseSigningException exception = assertThrows(LicenseSigningException.class,
            () -> this.signer.sign(license, YEAR));

        assertEquals("No key password is configured for [TRIAL] licenses.", exception.getMessage());
    }

    @Test
    void signFailsWhenTheKeyCantBeRead() throws Exception
    {
        KeyStoreException cause = new KeyStoreException("wrong password");
        when(this.keyStore.retrieve(any(), any(CertifiedPublicKey.class), any())).thenThrow(cause);
        License license = createLicense();

        LicenseSigningException exception = assertThrows(LicenseSigningException.class,
            () -> this.signer.sign(license, YEAR));

        assertEquals(String.format("Failed to sign the license [%s] with [%s].", license.getId(),
            LicenseSigner.getIssuer(LicenseType.PAID, YEAR)), exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    private static WikiStoreReference isCertificatesSpace()
    {
        return argThat(reference -> reference != null && reference.getReference()
            .equals(new SpaceReference("xwiki", List.of("License", "Certificates"))));
    }

    private static CertifiedPublicKey mockCertificate(String subject)
    {
        CertifiedPublicKey certificate = mock(CertifiedPublicKey.class);
        when(certificate.getSubject()).thenReturn(new DistinguishedName(subject));
        return certificate;
    }

    private static License createLicense()
    {
        License license = new License();
        license.setId(new LicenseId());
        license.setType(LicenseType.PAID);
        license.addFeatureId(new LicensedFeatureId("com.xwiki.pro:xwiki-pro-ui"));
        license.addInstanceId(new InstanceId("11111111-2222-3333-4444-555555555555"));
        license.setExpirationDate(4102444800000L);
        license.setMaxUserCount(-1);
        license.addLicenseeInfo("email", "partner@example.com");
        return license;
    }
}
