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

import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.crypto.pkix.CertificateProvider;
import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.x509certificate.DistinguishedName;
import org.xwiki.crypto.script.ScriptingCertificateStore;
import org.xwiki.crypto.script.ScriptingKeyStore;
import org.xwiki.crypto.signer.SignerFactory;
import org.xwiki.crypto.store.CertificateStoreException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseId;
import com.xwiki.licensing.LicenseStore;
import com.xwiki.licensing.LicenseStoreReference;
import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.SignedLicense;
import com.xwiki.licensing.internal.SignedLicenseGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ManagerScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
class ManagerScriptServiceTest
{
    private static final String PAID_ISSUER = "CN=Paid License Issuer 2026,OU=Licensing,O=XWiki SAS,L=Paris,C=FR";

    private static final String TRIAL_ISSUER = "CN=Trial License Issuer 2026,OU=Licensing,O=XWiki SAS,L=Paris,C=FR";

    @InjectMockComponents
    private ManagerScriptService scriptService;

    @MockComponent
    private SignedLicenseGenerator signedLicenseGenerator;

    @MockComponent
    @Named("SHA256withRSAEncryption")
    private SignerFactory signerFactory;

    @Test
    void generateSignsWithTheKeyOfTheMatchingCertificate() throws Exception
    {
        License license = new License();
        license.setType(LicenseType.PAID);

        CertifiedPublicKey trialCertificate = mockCertificate(TRIAL_ISSUER);
        CertifiedPublicKey paidCertificate = mockCertificate(PAID_ISSUER);
        ScriptingCertificateStore certificateStore = mock(ScriptingCertificateStore.class);
        when(certificateStore.getAllCertificates()).thenReturn(List.of(trialCertificate, paidCertificate));
        CertificateProvider certificateProvider = mock(CertificateProvider.class);
        when(certificateStore.getCertificateProvider()).thenReturn(certificateProvider);

        CertifiedKeyPair keyPair = mock(CertifiedKeyPair.class);
        ScriptingKeyStore keyStore = mock(ScriptingKeyStore.class);
        when(keyStore.retrieve(paidCertificate, "secret")).thenReturn(keyPair);

        SignedLicense signedLicense = mock(SignedLicense.class);
        when(this.signedLicenseGenerator.generate(license, keyPair, this.signerFactory, certificateProvider))
            .thenReturn(signedLicense);

        assertSame(signedLicense,
            this.scriptService.generate(license, keyStore, certificateStore, PAID_ISSUER, "secret"));
    }

    @Test
    void generateFailsWhenNoCertificateMatchesTheSubject() throws Exception
    {
        License license = new License();
        license.setType(LicenseType.TRIAL);

        CertifiedPublicKey paidCertificate = mockCertificate(PAID_ISSUER);
        ScriptingCertificateStore certificateStore = mock(ScriptingCertificateStore.class);
        when(certificateStore.getAllCertificates()).thenReturn(List.of(paidCertificate));
        ScriptingKeyStore keyStore = mock(ScriptingKeyStore.class);

        CertificateStoreException exception = assertThrows(CertificateStoreException.class,
            () -> this.scriptService.generate(license, keyStore, certificateStore, TRIAL_ISSUER, "secret"));

        assertEquals("Cannot find certificate in certificate store for license type [TRIAL]",
            exception.getMessage());
        verify(this.signedLicenseGenerator, never()).generate(any(), any(), any(), any());
    }

    @Test
    void retrieveGeneratedLicense() throws Exception
    {
        LicenseStore licenseStore = mock(LicenseStore.class);
        LicenseStoreReference storeReference = mock(LicenseStoreReference.class);
        LicenseId licenseId = new LicenseId();
        License license = new License();
        when(licenseStore.retrieve(storeReference, licenseId)).thenReturn(license);
        ScriptLicenseStore scriptLicenseStore =
            new ScriptLicenseStore(licenseStore, storeReference, mock(ContextualAuthorizationManager.class));

        assertSame(license, this.scriptService.retrieveGeneratedLicense(scriptLicenseStore, licenseId));
    }

    @Test
    void storeGeneratedLicense() throws Exception
    {
        LicenseStore licenseStore = mock(LicenseStore.class);
        LicenseStoreReference storeReference = mock(LicenseStoreReference.class);
        License license = new License();
        ScriptLicenseStore scriptLicenseStore =
            new ScriptLicenseStore(licenseStore, storeReference, mock(ContextualAuthorizationManager.class));

        this.scriptService.storeGeneratedLicense(scriptLicenseStore, license);

        // Storing has no return value: the outcome is the license handed to the underlying store.
        verify(licenseStore).store(storeReference, license);
    }

    private CertifiedPublicKey mockCertificate(String subject)
    {
        CertifiedPublicKey certificate = mock(CertifiedPublicKey.class);
        when(certificate.getSubject()).thenReturn(new DistinguishedName(subject));
        return certificate;
    }
}
