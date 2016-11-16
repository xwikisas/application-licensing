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

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.BinaryStringEncoder;

import com.xwiki.licensing.LicenseSerializer;
import com.xwiki.licensing.LicensedFeatureId;
import com.xwiki.licensing.model.jaxb.FeatureId;
import com.xwiki.licensing.model.jaxb.FeatureIdCollection;
import com.xwiki.licensing.model.jaxb.InstanceIdCollection;
import com.xwiki.licensing.model.jaxb.License;
import com.xwiki.licensing.model.jaxb.LicenseType;
import com.xwiki.licensing.model.jaxb.ObjectFactory;
import com.xwiki.licensing.model.jaxb.Restrictions;

/**
 * Serialize licence to XML.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named("xml")
public class XmlStringLicenseSerializer implements LicenseSerializer<String>
{
    @Inject
    private Logger logger;

    @Inject
    @Named("Base64")
    private BinaryStringEncoder base64Encoder;

    private ObjectFactory objectFactory = new ObjectFactory();

    @Override
    @SuppressWarnings("unchecked")
    public <G extends String> G serialize(com.xwiki.licensing.License license)
    {
        return (G) serializeToString(license);
    }

    private String serializeToString(com.xwiki.licensing.License license)
    {
        if (license.getFeatureIds().isEmpty() || license.getLicensee().isEmpty()) {
            throw new IllegalArgumentException("License could not be serialized without licensed items and licensee.");
        }

        License xmlLicense = objectFactory.createLicense()
            .withId(license.getId().toString())
            .withModelVersion("1.0.0")
            .withType(LicenseType.fromValue(license.getType().toString()))
            .withLicensed(objectFactory.createLicensedItems().withFeatures(getLicensedExtensionIdCollection(license)));

        Restrictions restrictions = getRestrictions(license);
        if (restrictions != null) {
            xmlLicense.setRestrictions(restrictions);
        }
        xmlLicense.setLicencee(objectFactory.createLicensee()
            .withName(license.getLicensee().get("name"))
            .withEmail(license.getLicensee().get("email")));

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JAXBContext jaxbContext = JAXBContext.newInstance(License.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(xmlLicense, baos);
            return baos.toString("UTF-8");
        } catch (Exception e) {
            logger.error("Error during license marshalling", e);
        }

        return null;
    }

    private FeatureIdCollection getLicensedExtensionIdCollection(com.xwiki.licensing.License license)
    {
        FeatureIdCollection extIdsColl = objectFactory.createFeatureIdCollection();
        List<FeatureId> extIds = extIdsColl.getFeatures();

        for (LicensedFeatureId id : license.getFeatureIds()) {
            extIds.add(objectFactory.createFeatureId().withId(id.getId()).withVersion(id.getVersionConstraint()));
        }
        return extIdsColl;
    }

    private Restrictions getRestrictions(com.xwiki.licensing.License license)
    {
        if (license.getInstanceIds().isEmpty()
            && license.getExpirationDate() == Long.MAX_VALUE
            && license.getMaxUserCount() == Long.MAX_VALUE) {
            return null;
        }

        Restrictions restrictions = objectFactory.createRestrictions();
        if (!license.getInstanceIds().isEmpty()) {
            restrictions.setInstances(getInstanceIdCollection(license));
        }
        if (license.getExpirationDate() != Long.MAX_VALUE) {
            Calendar expire = Calendar.getInstance();
            expire.setTimeInMillis(license.getExpirationDate());
            restrictions.setExpire(expire);
        }
        if (license.getMaxUserCount() != Long.MAX_VALUE) {
            restrictions.setUsers(BigInteger.valueOf(license.getMaxUserCount()));
        }
        return restrictions;
    }

    private InstanceIdCollection getInstanceIdCollection(com.xwiki.licensing.License license)
    {
        InstanceIdCollection instIdsColl = objectFactory.createInstanceIdCollection();
        List<String> instIds = instIdsColl.getInstances();

        for (org.xwiki.instance.InstanceId id : license.getInstanceIds()) {
            instIds.add(id.getInstanceId());
        }
        return instIdsColl;
    }
}
