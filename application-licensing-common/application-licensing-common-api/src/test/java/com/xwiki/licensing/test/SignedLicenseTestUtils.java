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
