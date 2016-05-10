package com.xwiki.licensing.test;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.xwiki.crypto.internal.asymmetric.keyfactory.BcDSAKeyFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA1DigestFactory;
import org.xwiki.crypto.internal.digest.factory.DefaultDigestFactory;
import org.xwiki.crypto.internal.encoder.Base64BinaryStringEncoder;
import org.xwiki.crypto.pkix.internal.BcStoreX509CertificateProvider;
import org.xwiki.crypto.pkix.internal.BcX509CertificateChainBuilder;
import org.xwiki.crypto.pkix.internal.BcX509CertificateFactory;
import org.xwiki.crypto.signer.internal.DefaultBcContentVerifierProviderBuilder;
import org.xwiki.crypto.signer.internal.cms.DefaultCMSSignedDataVerifier;
import org.xwiki.crypto.signer.internal.factory.BcDSAwithSHA1SignerFactory;
import org.xwiki.crypto.signer.internal.factory.BcSHA1withRsaSignerFactory;
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
    BcDSAKeyFactory.class,
    DefaultDigestFactory.class,
    BcSHA1DigestFactory.class,
    BcSHA1withRsaSignerFactory.class,
    BcDSAwithSHA1SignerFactory.class,
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
