package com.xwiki.licensing.internal;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.pkix.CertificateChainBuilder;
import org.xwiki.crypto.pkix.CertificateProvider;
import org.xwiki.crypto.pkix.CertifyingSigner;
import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.signer.CMSSignedDataGenerator;
import org.xwiki.crypto.signer.SignerFactory;
import org.xwiki.crypto.signer.param.CMSSignedDataGeneratorParameters;
import org.xwiki.properties.converter.Converter;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseSerializer;
import com.xwiki.licensing.SignedLicense;

/**
 * Default implementation of {@link SignedLicenseGenerator}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultSignedLicenseGenerator implements SignedLicenseGenerator
{
    private static final Charset UTF8 = Charset.forName("UTF-8");

    @Inject
    @Named("xml")
    private LicenseSerializer<String> serializer;

    @Inject
    private CMSSignedDataGenerator generator;

    @Inject
    private Converter<License> converter;

    @Inject
    @Named("X509")
    private CertificateChainBuilder chainBuilder;

    @Override
    public SignedLicense generate(License license,
        CertifiedKeyPair keyPair, SignerFactory signerFactory, CertificateProvider certificateProvider)
        throws GeneralSecurityException, IOException
    {
        License licenseToSign = new License(license);
        CMSSignedDataGeneratorParameters parameters = new CMSSignedDataGeneratorParameters()
            .addSigner(CertifyingSigner.getInstance(true, keyPair, signerFactory));

        for (CertifiedPublicKey cert : chainBuilder.build(keyPair.getCertificate(), certificateProvider)) {
            parameters.addCertificate(cert);
        }

        return converter.convert(License.class,
            generator.generate(serializer.serialize(licenseToSign).getBytes(UTF8), parameters, true));
    }
}
