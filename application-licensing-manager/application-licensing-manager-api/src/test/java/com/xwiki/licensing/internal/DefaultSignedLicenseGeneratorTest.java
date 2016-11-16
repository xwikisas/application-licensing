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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.crypto.signer.internal.cms.DefaultCMSSignedDataGenerator;
import org.xwiki.instance.InstanceId;
import org.xwiki.test.AllLogRule;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseId;
import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.LicensedFeatureId;
import com.xwiki.licensing.SignedLicense;
import com.xwiki.licensing.test.LicensingComponentList;
import com.xwiki.licensing.test.SignedLicenseTestUtils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

/**
 * Unit test for {@link DefaultSignedLicenseGenerator}
 *
 * @version $Id$
 */
@LicensingComponentList
@ComponentList({DefaultCMSSignedDataGenerator.class, XmlStringLicenseSerializer.class})
public class DefaultSignedLicenseGeneratorTest
{
    public static final boolean INSTANCE_ID_EQUALS_BROKEN = new InstanceId() != new InstanceId();

    @Rule
    public MockitoComponentMockingRule<SignedLicenseGenerator> mocker =
        new MockitoComponentMockingRule<>(DefaultSignedLicenseGenerator.class);

    @Rule
    public AllLogRule logRule = new AllLogRule();

    private SignedLicenseGenerator generator;

    private SignedLicenseTestUtils utils;

    @Before
    public void setUp() throws Exception
    {
        generator = mocker.getComponentUnderTest();
        utils = mocker.getInstance(SignedLicenseTestUtils.class);
    }

    private ArrayList<String> getInstanceIdsAsString(Collection<InstanceId> instanceIds)
    {
        ArrayList<String> instanceIdsAsString = new ArrayList<>();
        for(InstanceId instanceId : instanceIds) {
            instanceIdsAsString.add(instanceId.getInstanceId());
        }
        return instanceIdsAsString;
    }

    @Test
    public void testSignedLicenseGeneration() throws Exception
    {
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
        license.addLicenseeInfo("name", "user");
        license.addLicenseeInfo("email", "user@example.com");

        SignedLicense signedLicence =
            generator.generate(license, utils.getSigningKeyPair(), utils.getSignerFactory(), utils.getCertificateProvider());

        assertThat(signedLicence.getId(), equalTo(new LicenseId("00000000-0000-0000-0000-000000000000")));
        assertThat(signedLicence.getType(), equalTo(LicenseType.PAID));
        assertThat(signedLicence.getFeatureIds(),
            containsInAnyOrder(new LicensedFeatureId("test-ui", "1.0"), new LicensedFeatureId("test-api", "2.0")));
        if (!INSTANCE_ID_EQUALS_BROKEN) {
            assertThat(signedLicence.getInstanceIds(),
                containsInAnyOrder(new InstanceId("11111111-2222-3333-4444-555555555555"),
                    new InstanceId("66666666-7777-8888-9999-000000000000")));
        } else {
            assertThat(getInstanceIdsAsString(signedLicence.getInstanceIds()),
                containsInAnyOrder("11111111-2222-3333-4444-555555555555", "66666666-7777-8888-9999-000000000000"));
        }
        assertThat(signedLicence.getExpirationDate(), equalTo(calendar.getTimeInMillis()));
        assertThat(signedLicence.getMaxUserCount(), equalTo(100L));
        assertThat(signedLicence.getLicensee(), not(nullValue()));
        assertThat(signedLicence.getLicensee().size(), equalTo(2));
        assertThat(signedLicence.getLicensee().get("name"), equalTo("user"));
        assertThat(signedLicence.getLicensee().get("email"), equalTo("user@example.com"));

        //BinaryStringEncoder encoder = mocker.getInstance(BinaryStringEncoder.class, "Base64");
        //assertThat(encoder.encode(signedLicence.getEncoded()), equalTo(""));
    }
}
