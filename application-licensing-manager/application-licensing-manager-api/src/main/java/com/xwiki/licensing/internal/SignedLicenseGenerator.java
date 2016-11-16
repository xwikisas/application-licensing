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
