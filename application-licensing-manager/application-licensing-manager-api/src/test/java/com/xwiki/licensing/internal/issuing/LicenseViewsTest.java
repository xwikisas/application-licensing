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
package com.xwiki.licensing.internal.issuing;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryException;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiException;
import com.xwiki.licensing.LicenseId;
import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.SignedLicense;
import com.xwiki.licensing.manager.IssuingException;
import com.xwiki.licensing.manager.rest.model.InstanceLicenses;
import com.xwiki.licensing.manager.rest.model.LicenseKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LicenseViews}.
 *
 * @version $Id$
 */
@ComponentTest
class LicenseViewsTest
{
    private static final DocumentReference ACCOUNT = new DocumentReference("xwiki", "XWiki", "partner");

    private static final DocumentReference OTHER_ACCOUNT = new DocumentReference("xwiki", "XWiki", "other");

    private static final String PROD_ID = "11111111-0000-4000-8000-000000000001";

    private static final String TEST_ID = "11111111-0000-4000-8000-000000000002";

    private static final String PRO = "com.xwiki.pro:xwiki-pro-ui";

    private static final String INJECTOR = "com.xwiki:injector";

    private static final long DAY = 24 * 3600 * 1000L;

    private static final long NOW = System.currentTimeMillis();

    @InjectMockComponents
    private LicenseViews views;

    @MockComponent
    private ApiConfiguration configuration;

    @MockComponent
    private InstanceRecordStore instanceRecordStore;

    @MockComponent
    private LicenseRecordStore licenseRecordStore;

    @MockComponent
    private GeneratedLicenseStore generatedLicenseStore;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private final InstanceRecord prod = new InstanceRecord(PROD_ID, ACCOUNT, Environment.PROD, "wiki.example.com",
        "bundle", InstanceRecord.Origin.IMPORT, new Date());

    private final InstanceRecord test =
        new InstanceRecord(TEST_ID, ACCOUNT, null, null, "bundle", InstanceRecord.Origin.API, new Date());

    // The latest issued first, as LicenseRecordStore#getGenerated returns them.
    private final LicenseRecord renewedPro = record("a0000000-0000-4000-8000-000000000003", PROD_ID, PRO, 3, 400);

    private final LicenseRecord pro = record("a0000000-0000-4000-8000-000000000002", PROD_ID, PRO, 2, 30);

    private final LicenseRecord injector =
        record("a0000000-0000-4000-8000-000000000001", PROD_ID, INJECTOR, 1, -1);

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.instanceRecordStore.getInstances(ACCOUNT, null, null)).thenReturn(List.of(this.prod, this.test));
        when(this.licenseRecordStore.getGenerated(PROD_ID))
            .thenReturn(List.of(this.renewedPro, this.pro, this.injector));
        when(this.licenseRecordStore.getGenerated(TEST_ID)).thenReturn(List.of());
        mockKey(this.renewedPro, "KEY3");
        mockKey(this.pro, "KEY2");
        mockKey(this.injector, "KEY1");
    }

    @Test
    void list() throws Exception
    {
        List<InstanceLicenses> instances = this.views.list(ACCOUNT, null, null, null, false);

        assertEquals(2, instances.size());
        InstanceLicenses instance = instances.get(0);
        assertEquals(PROD_ID, instance.getInstanceId());
        assertEquals("prod", instance.getEnvironment());
        assertEquals("wiki.example.com", instance.getDomain());
        assertEquals("bundle", instance.getProduct());
        assertEquals(3, instance.getLicenses().size());
        // The latest license of each feature.
        assertEquals("KEY3,KEY1", instance.getKeys());
        // The injector license expired yesterday: the instance needs a renewal since then.
        assertEquals(LicenseViews.format(this.injector.getExpirationDate()), instance.getExpirationDate());

        LicenseKey license = instance.getLicenses().get(0);
        assertEquals(this.renewedPro.getLicenseId(), license.getLicenseId());
        assertEquals(PROD_ID, license.getInstanceId());
        assertEquals(PRO, license.getFeatureId());
        assertEquals("PAID", license.getType());
        assertEquals(this.renewedPro.getIssueDate().toInstant().toString(), license.getIssueDate());
        assertEquals(this.renewedPro.getExpirationDate().toInstant().toString(), license.getExpirationDate());
        assertEquals(100L, license.getMaxUserCount());
        assertEquals("KEY3", license.getKey());

        InstanceLicenses empty = instances.get(1);
        assertNull(empty.getEnvironment());
        assertEquals(List.of(), empty.getLicenses());
        assertEquals("", empty.getKeys());
        assertNull(empty.getExpirationDate());
    }

    @Test
    void listActiveOnlyForOneInstance() throws Exception
    {
        when(this.instanceRecordStore.getInstances(ACCOUNT, Environment.PROD, "bundle"))
            .thenReturn(List.of(this.prod, this.test));

        List<InstanceLicenses> instances = this.views.list(ACCOUNT, PROD_ID, "prod", "bundle", true);

        assertEquals(1, instances.size());
        assertEquals(List.of(this.renewedPro.getLicenseId(), this.pro.getLicenseId()),
            instances.get(0).getLicenses().stream().map(LicenseKey::getLicenseId)
                .collect(Collectors.toList()));
        assertEquals("KEY3", instances.get(0).getKeys());
        // The earliest expiration of the keys.
        assertEquals(LicenseViews.format(this.renewedPro.getExpirationDate()), instances.get(0).getExpirationDate());
    }

    @Test
    void listWithMissingKeys() throws Exception
    {
        when(this.generatedLicenseStore.get(new LicenseId(this.renewedPro.getLicenseId()))).thenReturn(null);
        when(this.generatedLicenseStore.get(new LicenseId(this.injector.getLicenseId())))
            .thenThrow(new IOException("unreadable"));

        InstanceLicenses instance = this.views.list(ACCOUNT, PROD_ID, null, null, false).get(0);

        assertNull(instance.getLicenses().get(0).getKey());
        // Without a key, the previous license of the feature is used.
        assertEquals("KEY2", instance.getKeys());
        assertEquals("The signed license [a0000000-0000-4000-8000-000000000003] is missing from the store.",
            this.logCapture.getMessage(0));
        assertEquals("Failed to read the signed license [a0000000-0000-4000-8000-000000000001].",
            this.logCapture.getMessage(1));
    }

    @Test
    void listErrors() throws Exception
    {
        IssuingException exception =
            assertThrows(IssuingException.class, () -> this.views.list(ACCOUNT, null, "staging", null, false));
        assertEquals(400, exception.getStatus());
        assertEquals("invalidRequest", exception.getCode());

        when(this.licenseRecordStore.getGenerated(PROD_ID)).thenThrow(new QueryException("failed", null, null));
        assertEquals(500,
            assertThrows(IssuingException.class, () -> this.views.list(ACCOUNT, null, null, null, false)).getStatus());
    }

    @Test
    void get() throws Exception
    {
        when(this.licenseRecordStore.get(this.pro.getLicenseId())).thenReturn(Optional.of(this.pro));
        when(this.configuration.getInstance(PROD_ID)).thenReturn(Optional.of(this.prod));

        LicenseKey license = this.views.get(ACCOUNT, this.pro.getLicenseId());

        assertEquals(this.pro.getLicenseId(), license.getLicenseId());
        assertEquals("KEY2", license.getKey());

        // Another account gets a 404, as if the license didn't exist.
        assertNotFound(OTHER_ACCOUNT, this.pro.getLicenseId());
    }

    @Test
    void getNotFound() throws Exception
    {
        assertNotFound(ACCOUNT, "../../License/Data/x");

        String missing = "b0000000-0000-4000-8000-000000000000";
        when(this.licenseRecordStore.get(missing)).thenReturn(Optional.empty());
        assertNotFound(ACCOUNT, missing);

        // Requested from Sales but not generated.
        LicenseRecord requested = record("b0000000-0000-4000-8000-000000000001", PROD_ID, PRO, 1, 1);
        requested.setHasLicense(false);
        when(this.licenseRecordStore.get(requested.getLicenseId())).thenReturn(Optional.of(requested));
        assertNotFound(ACCOUNT, requested.getLicenseId());

        // Licensed by hand, never imported.
        LicenseRecord manual = record("b0000000-0000-4000-8000-000000000002", "manual", PRO, 1, 1);
        when(this.licenseRecordStore.get(manual.getLicenseId())).thenReturn(Optional.of(manual));
        when(this.configuration.getInstance("manual")).thenReturn(Optional.empty());
        assertNotFound(ACCOUNT, manual.getLicenseId());

        when(this.licenseRecordStore.get(missing)).thenThrow(new XWikiException());
        assertEquals(500, assertThrows(IssuingException.class, () -> this.views.get(ACCOUNT, missing)).getStatus());
    }

    @Test
    void format()
    {
        assertNull(LicenseViews.format(null));
        assertEquals("2027-01-15T08:00:00Z", LicenseViews.format(Date.from(Instant.parse("2027-01-15T08:00:00Z"))));
        LicenseRecord licenseRecord = new LicenseRecord();
        assertNull(LicenseViews.toLicenseKey(licenseRecord, null).getType());
    }

    private void assertNotFound(DocumentReference account, String licenseId)
    {
        IssuingException exception =
            assertThrows(IssuingException.class, () -> this.views.get(account, licenseId));
        assertEquals(404, exception.getStatus());
        assertEquals("notFound", exception.getCode());
    }

    private void mockKey(LicenseRecord licenseRecord, String key) throws Exception
    {
        SignedLicense signedLicense = mock(SignedLicense.class);
        when(this.generatedLicenseStore.get(new LicenseId(licenseRecord.getLicenseId()))).thenReturn(signedLicense);
        when(this.generatedLicenseStore.encode(signedLicense)).thenReturn(key);
    }

    private static LicenseRecord record(String licenseId, String instanceId, String featureId, int issuedDaysAgo,
        int expiresInDays)
    {
        LicenseRecord licenseRecord = new LicenseRecord();
        licenseRecord.setLicenseId(licenseId);
        licenseRecord.setInstanceId(instanceId);
        licenseRecord.setFeatureId(featureId);
        licenseRecord.setType(LicenseType.PAID);
        licenseRecord.setMaxUserCount(100L);
        licenseRecord.setIssueDate(new Date(NOW - issuedDaysAgo * DAY));
        licenseRecord.setExpirationDate(new Date(NOW + expiresInDays * DAY));
        licenseRecord.setHasLicense(true);
        return licenseRecord;
    }
}
