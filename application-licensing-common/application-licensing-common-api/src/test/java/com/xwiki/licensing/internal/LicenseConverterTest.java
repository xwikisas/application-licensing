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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.instance.InstanceId;
import org.xwiki.properties.converter.Converter;
import org.xwiki.test.AllLogRule;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseId;
import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.LicensedFeatureId;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for {@link LicenseConverterTest}.
 *
 * @version $Id$
 */
public class LicenseConverterTest
{
    public static final boolean INSTANCE_ID_EQUALS_BROKEN = new InstanceId() != new InstanceId();

    @Rule
    public MockitoComponentMockingRule<Converter<License>> mockedConverter =
        new MockitoComponentMockingRule<>(LicenseConverter.class);

    @Rule
    public AllLogRule logRule = new AllLogRule();

    private Converter<License> converter;

    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

    @Before
    public void setUp() throws Exception
    {
        converter = mockedConverter.getComponentUnderTest();
    }

    @Test
    public void testMinimalLicenseConversion() throws Exception
    {
        License license = converter.convert(License.class, "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<license xmlns=\"http://www.xwiki.com/license\" id=\"00000000-0000-0000-0000-000000000000\">\n"
                + "    <modelVersion>1.0.0</modelVersion>\n"
                + "    <type>FREE</type>\n"
                + "    <licensed>\n"
                + "        <features>\n"
                + "            <feature>\n"
                + "                <id>test</id>\n"
                + "            </feature>\n"
                + "        </features>\n"
                + "    </licensed>\n"
                + "    <licencee>\n"
                + "        <name>user</name>\n"
                + "    </licencee>\n"
                + "</license>\n"
            );

        assertThat(license.getId(), equalTo(new LicenseId("00000000-0000-0000-0000-000000000000")));
        assertThat(license.getType(), equalTo(LicenseType.FREE));
        assertThat(license.getFeatureIds(), containsInAnyOrder(new LicensedFeatureId("test")));
        assertThat(license.getLicensee(), not(nullValue()));
        assertThat(license.getLicensee().size(), equalTo(1));
        assertThat(license.getLicensee().get("name"), equalTo("user"));
    }

    @Test
    public void testNoRestrictionLicenseConversion() throws Exception
    {
        License license = converter.convert(License.class, "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<license xmlns=\"http://www.xwiki.com/license\" id=\"00000000-0000-0000-0000-000000000000\">\n"
                + "    <modelVersion>1.0.0</modelVersion>\n"
                + "    <type>TRIAL</type>\n"
                + "    <licensed>\n"
                + "        <features>\n"
                + "            <feature>\n"
                + "                <id>test-ui</id>\n"
                + "                <version>1.0</version>\n"
                + "            </feature>\n"
                + "        </features>\n"
                + "    </licensed>\n"
                + "    <licencee>\n"
                + "        <name>user</name>\n"
                + "        <email>user@example.com</email>\n"
                + "    </licencee>\n"
                + "</license>\n"
            );

        assertThat(license.getId(), equalTo(new LicenseId("00000000-0000-0000-0000-000000000000")));
        assertThat(license.getType(), equalTo(LicenseType.TRIAL));
        assertThat(license.getFeatureIds(), containsInAnyOrder(new LicensedFeatureId("test-ui", "1.0")));
        assertThat(license.getLicensee(), not(nullValue()));
        assertThat(license.getLicensee().size(), equalTo(2));
        assertThat(license.getLicensee().get("name"), equalTo("user"));
        assertThat(license.getLicensee().get("email"), equalTo("user@example.com"));
    }

    @Test
    public void testInstanceRestrictionLicenseConversion() throws Exception
    {
        License license = converter.convert(License.class, "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<license xmlns=\"http://www.xwiki.com/license\" id=\"00000000-0000-0000-0000-000000000000\">\n"
                + "    <modelVersion>1.0.0</modelVersion>\n"
                + "    <type>TRIAL</type>\n"
                + "    <licensed>\n"
                + "        <features>\n"
                + "            <feature>\n"
                + "                <id>test-api</id>\n"
                + "                <version>1.0</version>\n"
                + "            </feature>\n"
                + "            <feature>\n"
                + "                <id>test-ui</id>\n"
                + "                <version>2.0</version>\n"
                + "            </feature>\n"
                + "        </features>\n"
                + "    </licensed>\n"
                + "    <restrictions>\n"
                + "        <instances>\n"
                + "            <instance>11111111-2222-3333-4444-555555555555</instance>\n"
                + "            <instance>66666666-7777-8888-9999-000000000000</instance>\n"
                + "        </instances>\n"
                + "    </restrictions>\n"
                + "    <licencee>\n"
                + "        <name>user</name>\n"
                + "    </licencee>\n"
                + "</license>\n"
            );

        assertThat(license.getId(), equalTo(new LicenseId("00000000-0000-0000-0000-000000000000")));
        assertThat(license.getType(), equalTo(LicenseType.TRIAL));
        assertThat(license.getFeatureIds(), containsInAnyOrder(new LicensedFeatureId("test-api", "1.0"), new LicensedFeatureId("test-ui", "2.0")));
        if (!INSTANCE_ID_EQUALS_BROKEN) {
            assertThat(license.getInstanceIds(), containsInAnyOrder(new InstanceId("11111111-2222-3333-4444-555555555555"), new InstanceId("66666666-7777-8888-9999-000000000000")));
        } else {
            assertThat(getInstanceIdsAsString(license.getInstanceIds()), containsInAnyOrder("11111111-2222-3333-4444-555555555555","66666666-7777-8888-9999-000000000000"));
        }
        assertThat(license.getLicensee(), not(nullValue()));
        assertThat(license.getLicensee().size(), equalTo(1));
        assertThat(license.getLicensee().get("name"), equalTo("user"));
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
    public void testDateRestrictionLicenseConversion() throws Exception
    {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(2017,Calendar.JUNE,2);

        License license = converter.convert(License.class, "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<license xmlns=\"http://www.xwiki.com/license\" id=\"00000000-0000-0000-0000-000000000000\">\n"
                + "    <modelVersion>1.0.0</modelVersion>\n"
                + "    <type>TRIAL</type>\n"
                + "    <licensed>\n"
                + "        <features>\n"
                + "            <feature>\n"
                + "                <id>test</id>\n"
                + "                <version>1.0</version>\n"
                + "            </feature>\n"
                + "        </features>\n"
                + "    </licensed>\n"
                + "    <restrictions>\n"
                + "        <expire>" + dateFormatter.format(calendar.getTimeInMillis()) + "</expire>\n"
                + "    </restrictions>\n"
                + "    <licencee>\n"
                + "        <name>user</name>\n"
                + "    </licencee>\n"
                + "</license>\n"
            );

        assertThat(license.getId(), equalTo(new LicenseId("00000000-0000-0000-0000-000000000000")));
        assertThat(license.getType(), equalTo(LicenseType.TRIAL));
        assertThat(license.getFeatureIds(), containsInAnyOrder(new LicensedFeatureId("test", "1.0")));
        assertThat(license.getExpirationDate(), equalTo(calendar.getTimeInMillis()));
        assertThat(license.getLicensee(), not(nullValue()));
        assertThat(license.getLicensee().size(), equalTo(1));
        assertThat(license.getLicensee().get("name"), equalTo("user"));
    }

    @Test
    public void testUserRestrictionLicenseConversion() throws Exception
    {
        License license = converter.convert(License.class, "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<license xmlns=\"http://www.xwiki.com/license\" id=\"00000000-0000-0000-0000-000000000000\">\n"
                + "    <modelVersion>1.0.0</modelVersion>\n"
                + "    <type>TRIAL</type>\n"
                + "    <licensed>\n"
                + "        <features>\n"
                + "            <feature>\n"
                + "                <id>test</id>\n"
                + "                <version>1.0</version>\n"
                + "            </feature>\n"
                + "        </features>\n"
                + "    </licensed>\n"
                + "    <restrictions>\n"
                + "        <users>1000</users>\n"
                + "    </restrictions>\n"
                + "    <licencee>\n"
                + "        <name>user</name>\n"
                + "    </licencee>\n"
                + "</license>\n"
            );

        assertThat(license.getId(), equalTo(new LicenseId("00000000-0000-0000-0000-000000000000")));
        assertThat(license.getType(), equalTo(LicenseType.TRIAL));
        assertThat(license.getFeatureIds(), containsInAnyOrder(new LicensedFeatureId("test", "1.0")));
        assertThat(license.getMaxUserCount(), equalTo(1000L));
        assertThat(license.getLicensee(), not(nullValue()));
        assertThat(license.getLicensee().size(), equalTo(1));
        assertThat(license.getLicensee().get("name"), equalTo("user"));
    }

    @Test
    public void testRestrictedLicenseConversion() throws Exception
    {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.set(2017,Calendar.JUNE,2);

        License license = converter.convert(License.class, "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<license xmlns=\"http://www.xwiki.com/license\" id=\"00000000-0000-0000-0000-000000000000\">\n"
                + "    <modelVersion>1.0.0</modelVersion>\n"
                + "    <type>TRIAL</type>\n"
                + "    <licensed>\n"
                + "        <features>\n"
                + "            <feature>\n"
                + "                <id>test-ui</id>\n"
                + "                <version>1.0</version>\n"
                + "            </feature>\n"
                + "            <feature>\n"
                + "                <id>test-api</id>\n"
                + "                <version>2.0</version>\n"
                + "            </feature>\n"
                + "        </features>\n"
                + "    </licensed>\n"
                + "    <restrictions>\n"
                + "        <instances>\n"
                + "            <instance>11111111-2222-3333-4444-555555555555</instance>\n"
                + "            <instance>66666666-7777-8888-9999-000000000000</instance>\n"
                + "        </instances>\n"
                + "        <expire>" + dateFormatter.format(calendar.getTimeInMillis()) + "</expire>\n"
                + "        <users>100</users>\n"
                + "    </restrictions>\n"
                + "    <licencee>\n"
                + "        <name>user</name>\n"
                + "        <email>user@example.com</email>\n"
                + "    </licencee>\n"
                + "</license>\n"
            );

        assertThat(license.getId(), equalTo(new LicenseId("00000000-0000-0000-0000-000000000000")));
        assertThat(license.getType(), equalTo(LicenseType.TRIAL));
        assertThat(license.getFeatureIds(), containsInAnyOrder(new LicensedFeatureId("test-ui", "1.0"), new LicensedFeatureId("test-api", "2.0")));
        if (!INSTANCE_ID_EQUALS_BROKEN) {
            assertThat(license.getInstanceIds(), containsInAnyOrder(new InstanceId("11111111-2222-3333-4444-555555555555"), new InstanceId("66666666-7777-8888-9999-000000000000")));
        } else {
            assertThat(getInstanceIdsAsString(license.getInstanceIds()), containsInAnyOrder("11111111-2222-3333-4444-555555555555","66666666-7777-8888-9999-000000000000"));
        }
        assertThat(license.getExpirationDate(), equalTo(calendar.getTimeInMillis()));
        assertThat(license.getMaxUserCount(), equalTo(100L));
        assertThat(license.getLicensee(), not(nullValue()));
        assertThat(license.getLicensee().size(), equalTo(2));
        assertThat(license.getLicensee().get("name"), equalTo("user"));
        assertThat(license.getLicensee().get("email"), equalTo("user@example.com"));
    }
}
