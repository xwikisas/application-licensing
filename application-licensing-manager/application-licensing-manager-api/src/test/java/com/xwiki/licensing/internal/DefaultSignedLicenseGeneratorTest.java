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

import java.util.Calendar;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.xwiki.crypto.signer.internal.cms.DefaultCMSSignedDataGenerator;
import org.xwiki.instance.InstanceId;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseId;
import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.LicensedFeatureId;
import com.xwiki.licensing.SignedLicense;
import com.xwiki.licensing.test.LicensingComponentList;
import com.xwiki.licensing.test.SignedLicenseTestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit test for {@link DefaultSignedLicenseGenerator}
 *
 * @version $Id$
 */
@ComponentTest
@LicensingComponentList
@ComponentList({ DefaultCMSSignedDataGenerator.class, XmlStringLicenseSerializer.class })
class DefaultSignedLicenseGeneratorTest
{
    @InjectMockComponents
    private DefaultSignedLicenseGenerator generator;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @Test
    void signedLicenseGeneration() throws Exception
    {
        SignedLicenseTestUtils utils = this.componentManager.getInstance(SignedLicenseTestUtils.class);

        License license = new License();
        license.setId(new LicenseId("00000000-0000-0000-0000-000000000000"));
        license.setType(LicenseType.PAID);
        license.addFeatureId(new LicensedFeatureId("test-api", "2.0"));
        license.addFeatureId(new LicensedFeatureId("test-ui", "1.0"));
        license.addInstanceId(new InstanceId("11111111-2222-3333-4444-555555555555"));
        license.addInstanceId(new InstanceId("66666666-7777-8888-9999-000000000000"));
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.YEAR, 1);
        license.setExpirationDate(calendar.getTimeInMillis());
        license.setMaxUserCount(100L);
        license.addLicenseeInfo("firstName", "John");
        license.addLicenseeInfo("lastName", "Doe");
        license.addLicenseeInfo("email", "user@example.com");
        license.addLicenseeInfo("support", "bronze");

        SignedLicense signedLicence = this.generator.generate(license, utils.getSigningKeyPair(),
            utils.getSignerFactory(), utils.getCertificateProvider());

        assertEquals(new LicenseId("00000000-0000-0000-0000-000000000000"), signedLicence.getId());
        assertEquals(LicenseType.PAID, signedLicence.getType());
        assertThat(signedLicence.getFeatureIds(),
            containsInAnyOrder(new LicensedFeatureId("test-ui", "1.0"), new LicensedFeatureId("test-api", "2.0")));
        // InstanceId doesn't implement equals, so compare the string values.
        assertThat(signedLicence.getInstanceIds().stream().map(InstanceId::getInstanceId).collect(Collectors.toList()),
            containsInAnyOrder("11111111-2222-3333-4444-555555555555", "66666666-7777-8888-9999-000000000000"));
        assertEquals(calendar.getTimeInMillis(), signedLicence.getExpirationDate());
        assertEquals(100L, signedLicence.getMaxUserCount());
        assertNotNull(signedLicence.getLicensee());
        assertEquals(4, signedLicence.getLicensee().size());
        assertEquals("John", signedLicence.getLicensee().get("firstName"));
        assertEquals("Doe", signedLicence.getLicensee().get("lastName"));
        assertEquals("user@example.com", signedLicence.getLicensee().get("email"));
        assertEquals("bronze", signedLicence.getLicensee().get("support"));
    }
}
