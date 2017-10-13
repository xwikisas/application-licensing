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
package com.xwiki.licensing;

import java.util.Collections;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.crypto.pkix.params.x509certificate.X509CertifiedPublicKey;
import org.xwiki.crypto.signer.CMSSignedDataVerifier;
import org.xwiki.crypto.signer.internal.cms.DefaultCMSSignedDataVerifier;
import org.xwiki.crypto.signer.param.CMSSignedDataVerified;
import org.xwiki.crypto.signer.param.CMSSignerVerifiedInformation;
import org.xwiki.instance.InstanceId;
import org.xwiki.properties.converter.Converter;

import com.xwiki.licensing.internal.LicenseConverter;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link License}.
 * 
 * @version $Id$
 * @since 1.6
 */
public class LicenseTest
{
    private CMSSignedDataVerifier verifier = mock(DefaultCMSSignedDataVerifier.class);

    private Converter<License> converter = mock(LicenseConverter.class);

    private CMSSignedDataVerified signedDataVerified = mock(CMSSignedDataVerified.class);

    @Before
    public void configure() throws Exception
    {
        CMSSignerVerifiedInformation signatureInfo = mock(CMSSignerVerifiedInformation.class);
        X509CertifiedPublicKey rootCA = mock(X509CertifiedPublicKey.class);

        when(this.verifier.verify(any())).thenReturn(this.signedDataVerified);
        when(this.signedDataVerified.getSignatures()).thenReturn(Collections.singleton(signatureInfo));
        when(signatureInfo.isVerified()).thenReturn(true);
        when(signatureInfo.getCertificateChain()).thenReturn(Collections.singleton(rootCA));
        when(rootCA.isRootCA()).thenReturn(true);
        when(rootCA.isValidOn(any(Date.class))).thenReturn(true);
    }

    @Test
    public void isApplicableTo()
    {
        License license = new License();

        // We can't use new InstanceId() because we get a NullPointerException in InstanceId#getInstanceId()
        InstanceId nullInstanceId = mock(InstanceId.class);
        when(nullInstanceId.getInstanceId()).thenReturn(null);

        // A license that is not bound to an XWiki instance is not applicable to any instance.
        assertFalse(license.isApplicableTo(null));
        assertFalse(license.isApplicableTo(nullInstanceId));
        assertFalse(license.isApplicableTo(new InstanceId("1e597805-4a0c-4477-86f5-8e0c5086d2f6")));
        assertFalse(license.isApplicableTo(new InstanceId("1e597805-c0a4-4477-86f5-8e0c5086d2f6")));

        license.addInstanceId(new InstanceId("1e597805-c0a4-4477-86f5-8e0c5086d2f6"));

        // A license that is bound to an XWiki instance is applicable only to that specific instance.
        assertFalse(license.isApplicableTo(null));
        assertFalse(license.isApplicableTo(nullInstanceId));
        assertFalse(license.isApplicableTo(new InstanceId("1e597805-4a0c-4477-86f5-8e0c5086d2f6")));
        assertTrue(license.isApplicableTo(new InstanceId("1e597805-c0a4-4477-86f5-8e0c5086d2f6")));
    }

    @Test
    public void getOptimumLicense()
    {
        License license = new License();

        assertSame(license, License.getOptimumLicense(license, null));
        assertSame(license, License.getOptimumLicense(null, license));

        License alice = new License();
        alice.setId(new LicenseId("1e597805-4a0c-4477-86f5-8e0c5086d2f6"));

        License signedAlice = getSignedLicense(alice, "alice");
        assertSame(signedAlice, License.getOptimumLicense(alice, signedAlice));
        assertSame(signedAlice, License.getOptimumLicense(signedAlice, alice));

        License bob = new License();
        bob.setId(alice.getId());

        assertSame(alice, License.getOptimumLicense(alice, bob));
        assertSame(bob, License.getOptimumLicense(bob, alice));

        alice.setExpirationDate(6L);
        bob.setId(new LicenseId("1e597805-c0a4-4477-86f5-8e0c5086d2f6"));
        bob.setExpirationDate(5L);

        assertSame(alice, License.getOptimumLicense(alice, bob));
        assertSame(alice, License.getOptimumLicense(bob, alice));

        alice.setMaxUserCount(2L);
        bob.setExpirationDate(6L);
        bob.setMaxUserCount(3L);

        assertSame(bob, License.getOptimumLicense(alice, bob));
        assertSame(bob, License.getOptimumLicense(bob, alice));

        alice.setType(LicenseType.PAID);
        bob.setMaxUserCount(2L);
        bob.setType(LicenseType.TRIAL);

        assertSame(alice, License.getOptimumLicense(alice, bob));
        assertSame(alice, License.getOptimumLicense(bob, alice));

        bob.setType(LicenseType.PAID);
        bob.addFeatureId(new LicensedFeatureId("foo"));

        assertSame(bob, License.getOptimumLicense(alice, bob));
        assertSame(bob, License.getOptimumLicense(bob, alice));

        alice.addFeatureId(new LicensedFeatureId("foo"));
        alice.addInstanceId(new InstanceId("1e597805-c0a4-4477-86f5-8e0c5086d2f6"));

        assertSame(alice, License.getOptimumLicense(alice, bob));
        assertSame(alice, License.getOptimumLicense(bob, alice));

        bob.addInstanceId(new InstanceId("1e597805-c0a4-4477-86f5-8e0c5086d2f6"));

        assertSame(alice, License.getOptimumLicense(alice, bob));
        assertSame(bob, License.getOptimumLicense(bob, alice));
    }

    private License getSignedLicense(License license, String content)
    {
        when(this.signedDataVerified.getContent()).thenReturn(content.getBytes());
        when(this.converter.convert(License.class, content)).thenReturn(license);
        return new SignedLicense(content.getBytes(), this.verifier, this.converter);
    }
}
