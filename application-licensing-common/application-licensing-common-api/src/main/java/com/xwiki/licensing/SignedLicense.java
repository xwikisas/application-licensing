package com.xwiki.licensing;

import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.x509certificate.X509CertifiedPublicKey;
import org.xwiki.crypto.signer.CMSSignedDataVerifier;
import org.xwiki.crypto.signer.internal.cms.DefaultCMSSignedDataVerifier;
import org.xwiki.crypto.signer.param.CMSSignedDataVerified;
import org.xwiki.crypto.signer.param.CMSSignerVerifiedInformation;
import org.xwiki.instance.InstanceId;
import org.xwiki.properties.converter.Converter;

import com.xwiki.licensing.internal.LicenseConverter;

/**
 * A software license instance with a valid signature.
 *
 * @version $Id$
 */
public final class SignedLicense extends License
{
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final String UNSUPPORTED_METHOD_ERROR = "Signed license could not be tampered.";

    private final byte[] signedLicense;

    /**
     * Construct a Signed license, not for public usage, see the LicenseConverter to convert a encoded license
     * into a SignedLicense.
     *
     * @param signedLicense the encoded signed license.
     * @param verifier the signature verifier, should be an instance of DefaultCMSSignedDataVerifier.
     * @param converter the license converter, should be an instance of LicenseConverter.
     */
    public SignedLicense(byte[] signedLicense, CMSSignedDataVerifier verifier, Converter<License> converter)
    {
        if (!(verifier instanceof DefaultCMSSignedDataVerifier) || !(converter instanceof LicenseConverter)) {
            throw new IllegalArgumentException("Untrusted signature verifier or license converter received.");
        }

        this.signedLicense = signedLicense;

        String xmlLicense = getTrustedContent(verifier);
        if (xmlLicense == null) {
            throw new IllegalArgumentException("Invalid signed license data received. Signature not trusted.");
        }

        License license = converter.convert(License.class, xmlLicense);
        initialize(license);
    }

    /**
     * Copy constructor.
     * @param license the license to copy.
     */
    public SignedLicense(SignedLicense license)
    {
        initialize(license);
        this.signedLicense = license.signedLicense;
    }

    /**
     * @return the encoded license representing this signed license.
     */
    public byte[] getEncoded()
    {
        return signedLicense;
    }

    private void initialize(License license)
    {
        super.setId(license.getId());
        super.setType(license.getType());
        super.setFeatureIds(license.getFeatureIds());
        super.setInstanceIds(license.getInstanceIds());
        super.setExpirationDate(license.getExpirationDate());
        super.setMaxUserCount(license.getMaxUserCount());
        super.setLicensee(license.getLicensee());
    }

    private String getTrustedContent(CMSSignedDataVerifier verifier)
    {
        CMSSignedDataVerified signedDataVerified;
        try {
            signedDataVerified = verifier.verify(signedLicense);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Invalid signed license data received", e);
        }

        for (CMSSignerVerifiedInformation signatureInfo : signedDataVerified.getSignatures()) {
            if (isSignatureTrusted(signatureInfo)) {
                return StringUtils.newStringUtf8(signedDataVerified.getContent());
            }
        }

        return null;
    }

    private boolean isSignatureTrusted(CMSSignerVerifiedInformation signature)
    {
        if (!signature.isVerified()) {
            return false;
        }

        Collection<CertifiedPublicKey> chain = signature.getCertificateChain();

        if (chain == null || chain.isEmpty()) {
            return false;
        }

        CertifiedPublicKey expectedRootCA = chain.iterator().next();

        if (!(expectedRootCA instanceof X509CertifiedPublicKey)) {
            return false;
        }

        X509CertifiedPublicKey rootCA = (X509CertifiedPublicKey) expectedRootCA;

        return !(!rootCA.isRootCA() || !rootCA.isValidOn(new Date())) && checkChainValidity(chain);
    }

    private boolean checkChainValidity(Collection<CertifiedPublicKey> chain) {
        for (CertifiedPublicKey cert : chain) {
            if (!(cert instanceof X509CertifiedPublicKey)) {
                return false;
            }
            if (!((X509CertifiedPublicKey) cert).isValidOn(new Date())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setExpirationDate(Long date)
    {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD_ERROR);
    }

    @Override
    public void setFeatureIds(Collection<LicensedFeatureId> ids)
    {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD_ERROR);
    }

    @Override
    public void addFeatureId(LicensedFeatureId id)
    {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD_ERROR);
    }

    @Override
    public void setId(LicenseId id)
    {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD_ERROR);
    }

    @Override
    public void setInstanceIds(Collection<InstanceId> ids)
    {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD_ERROR);
    }

    @Override
    public void addInstanceId(InstanceId id)
    {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD_ERROR);
    }

    @Override
    public void setLicensee(Map<String, String> licensee)
    {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD_ERROR);
    }

    @Override
    public void addLicenseeInfo(String key, String value)
    {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD_ERROR);
    }

    @Override
    public void setMaxUserCount(long userCount)
    {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD_ERROR);
    }

    @Override
    public void setType(LicenseType type)
    {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD_ERROR);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o.getClass() != getClass()) {
            return false;
        }

        SignedLicense license = (SignedLicense) o;

        return new EqualsBuilder()
            .appendSuper(super.equals(license))
            .append(getEncoded(), license.getEncoded())
            .isEquals();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
