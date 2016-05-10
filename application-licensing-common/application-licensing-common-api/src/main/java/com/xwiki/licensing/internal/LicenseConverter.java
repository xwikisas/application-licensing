package com.xwiki.licensing.internal;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.signer.CMSSignedDataVerifier;
import org.xwiki.instance.InstanceId;
import org.xwiki.properties.converter.AbstractConverter;
import org.xwiki.properties.converter.ConversionException;

import com.xwiki.licensing.LicenseId;
import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.LicensedFeatureId;
import com.xwiki.licensing.SignedLicense;
import com.xwiki.licensing.model.jaxb.FeatureId;
import com.xwiki.licensing.model.jaxb.License;

/**
 * Create an {@link License} from a string.
 *
 * @version $Id$
 */
@Component
@Singleton
public class LicenseConverter extends AbstractConverter<com.xwiki.licensing.License>
{
    @Inject
    private CMSSignedDataVerifier verifier;

    @Override
    @SuppressWarnings("unchecked")
    protected <G extends com.xwiki.licensing.License> G convertToType(Type targetType, Object value)
    {
        if (value instanceof String) {
            return (G) convertToLicense((String) value);
        } else if (value instanceof byte[]) {
            return (G) convertToLicense((byte[]) value);
        }

        throw new ConversionException(String.format("Unsupported target type [%s]", targetType));
    }

    private com.xwiki.licensing.SignedLicense convertToLicense(byte[] signedLicense)
    {
        return new SignedLicense(signedLicense, verifier, this);
    }

    private com.xwiki.licensing.License convertToLicense(String serializedLicense)
    {
        License xmlLicense;

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(License.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            xmlLicense = (License) jaxbUnmarshaller.unmarshal(
                new ByteArrayInputStream(serializedLicense.getBytes(Charset.forName("UTF-8"))));
        } catch (JAXBException e) {
            throw new ConversionException(String.format("Failed to convert the following string to a license: %s",
                serializedLicense), e);
        }

        com.xwiki.licensing.License license = new com.xwiki.licensing.License();
        license.setId(new LicenseId(xmlLicense.getId()));
        if (xmlLicense.getType() != null) {
            license.setType(LicenseType.valueOf(xmlLicense.getType().toString()));
        }
        license.setFeatureIds(getLicensedExtensionIds(xmlLicense));
        license.setInstanceIds(getInstanceId(xmlLicense));
        if (xmlLicense.getRestrictions() != null) {
            if (xmlLicense.getRestrictions().getExpire() != null) {
                license.setExpirationDate(xmlLicense.getRestrictions().getExpire().getTimeInMillis());
            }
            if (xmlLicense.getRestrictions().getUsers() != null) {
                license.setMaxUserCount(xmlLicense.getRestrictions().getUsers().longValue());
            }
        }
        license.setLicensee(getLicensee(xmlLicense));
        return license;
    }

    private Collection<LicensedFeatureId> getLicensedExtensionIds(License xmlLicense)
    {
        ArrayList<LicensedFeatureId> extIds = new ArrayList<>();
        if (xmlLicense.getLicensed() != null && xmlLicense.getLicensed().getFeatures() != null) {
            for (FeatureId id : xmlLicense.getLicensed().getFeatures().getFeatures()) {
                extIds.add(new LicensedFeatureId(id.getId(), id.getVersion()));
            }
        }
        return extIds.isEmpty() ? null : extIds;
    }

    private Collection<InstanceId> getInstanceId(License xmlLicense)
    {
        ArrayList<InstanceId> instIds = new ArrayList<>();
        if (xmlLicense.getRestrictions() != null && xmlLicense.getRestrictions().getInstances() != null) {
            for (String id : xmlLicense.getRestrictions().getInstances().getInstances()) {
                instIds.add(new InstanceId(id));
            }
        }
        return instIds.isEmpty() ? null : instIds;
    }

    private Map<String, String> getLicensee(License xmlLicense)
    {
        Map<String, String> map = new HashMap<>();

        if (xmlLicense.getLicencee() != null) {
            if (xmlLicense.getLicencee().getName() != null) {
                map.put("name", xmlLicense.getLicencee().getName());
            }
            if (xmlLicense.getLicencee().getEmail() != null) {
                map.put("email", xmlLicense.getLicencee().getEmail());
            }
        }

        return map.isEmpty() ? null : map;
    }
}
