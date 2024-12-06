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

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.xwiki.crypto.internal.DefaultSecureRandomProvider;
import org.xwiki.crypto.internal.asymmetric.keyfactory.BcDSAKeyFactory;
import org.xwiki.crypto.internal.asymmetric.keyfactory.BcRSAKeyFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA256DigestFactory;
import org.xwiki.crypto.internal.digest.factory.DefaultDigestFactory;
import org.xwiki.crypto.internal.encoder.Base64BinaryStringEncoder;
import org.xwiki.crypto.pkix.internal.BcStoreX509CertificateProvider;
import org.xwiki.crypto.pkix.internal.BcX509CertificateChainBuilder;
import org.xwiki.crypto.pkix.internal.BcX509CertificateFactory;
import org.xwiki.crypto.pkix.internal.BcX509CertificateGeneratorFactory;
import org.xwiki.crypto.pkix.params.x509certificate.X509CertificateParameters;
import org.xwiki.crypto.signer.internal.DefaultBcContentVerifierProviderBuilder;
import org.xwiki.crypto.signer.internal.cms.DefaultCMSSignedDataVerifier;
import org.xwiki.crypto.signer.internal.factory.BcSHA256withRsaSignerFactory;
import org.xwiki.crypto.signer.internal.factory.DefaultSignerFactory;
import org.xwiki.test.annotation.ComponentList;

import com.xwiki.licensing.internal.LicenseConverter;
import com.xwiki.licensing.test.internal.DefaultSignedLicenseTestUtils;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Pack of default Component implementations that are needed for licensing test.
 *
 * @version $Id: 272ed420a3e84ef629043b77f0539b17b755c646 $
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE, METHOD, ANNOTATION_TYPE })
@ComponentList({
    Base64BinaryStringEncoder.class,
    DefaultSecureRandomProvider.class,
    BcDSAKeyFactory.class,
    BcRSAKeyFactory.class,
    DefaultDigestFactory.class,
    BcSHA256DigestFactory.class,
    BcSHA256withRsaSignerFactory.class,
    BcX509CertificateGeneratorFactory.class,
    X509CertificateParameters.class,
    DefaultSignerFactory.class,
    BcX509CertificateFactory.class,
    DefaultBcContentVerifierProviderBuilder.class,
    BcStoreX509CertificateProvider.class,
    BcX509CertificateChainBuilder.class,
    DefaultCMSSignedDataVerifier.class,
    DefaultSignedLicenseTestUtils.class,
    LicenseConverter.class})
@Inherited
public @interface LicensingComponentList
{
}
