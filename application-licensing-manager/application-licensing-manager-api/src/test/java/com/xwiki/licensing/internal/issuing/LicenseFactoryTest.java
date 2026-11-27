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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.instance.InstanceId;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.LicensedFeatureId;
import com.xwiki.licensing.SignedLicense;
import com.xwiki.licensing.manager.IssuingException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LicenseFactory}.
 *
 * @version $Id$
 */
@ComponentTest
class LicenseFactoryTest
{
    private static final DocumentReference ACCOUNT = new DocumentReference("xwiki", "XWiki", "partner");

    private static final String INSTANCE_ID = "9f1c2e4a-7b3d-4c8e-a1f0-2d5e6b7c8a90";

    private static final String FEATURE_ID = "com.xwiki.pro:xwiki-pro-ui";

    private static final ZoneId ZONE = ZoneId.systemDefault();

    private static final ZonedDateTime ISSUE = ZonedDateTime.of(2026, 11, 27, 10, 0, 0, 0, ZONE);

    private static final Date ISSUE_DATE = Date.from(ISSUE.toInstant());

    @InjectMockComponents
    private LicenseFactory factory;

    @MockComponent
    private LicensedDependencyResolver dependencyResolver;

    @MockComponent
    private LicenseRecordStore licenseRecordStore;

    @MockComponent
    private LicenseSigner signer;

    @MockComponent
    private GeneratedLicenseStore generatedLicenseStore;

    private final SignedLicense signedLicense = mock(SignedLicense.class);

    @BeforeEach
    void setUp() throws Exception
    {
        Set<String> dependencies = new LinkedHashSet<>(List.of("com.xwiki.pro:pro-api", "com.xwiki.date:date-api"));
        when(this.dependencyResolver.resolve(FEATURE_ID, "1.2"))
            .thenReturn(Optional.of(new LicensedDependencyResolver.LicensedDependencies("1.2", dependencies)));
        when(this.signer.sign(any(License.class), eq(2026))).thenReturn(this.signedLicense);
        when(this.generatedLicenseStore.encode(this.signedLicense)).thenReturn("KEY");
    }

    @Test
    void createForANewInstance() throws Exception
    {
        IssuingPlan plan = plan(Environment.PROD, null);

        PendingLicense license = this.factory.create(plan, INSTANCE_ID, FEATURE_ID, ISSUE_DATE);

        assertSame(this.signedLicense, license.getSignedLicense());
        assertEquals("KEY", license.getKey());
        LicenseRecord licenseRecord = license.getRecord();
        assertEquals(36, licenseRecord.getLicenseId().length());
        assertEquals(LicenseType.PAID, licenseRecord.getType());
        assertEquals("Partner", licenseRecord.getFirstName());
        assertEquals("Inc", licenseRecord.getLastName());
        assertEquals("partner@example.com", licenseRecord.getEmail());
        assertEquals(INSTANCE_ID, licenseRecord.getInstanceId());
        assertEquals(FEATURE_ID, licenseRecord.getFeatureId());
        assertEquals(List.of("com.xwiki.pro:pro-api", "com.xwiki.date:date-api"),
            licenseRecord.getDependentFeatureIds());
        assertEquals("1.2", licenseRecord.getExtensionVersion());
        assertEquals("gold", licenseRecord.getSupport());
        assertEquals(500L, licenseRecord.getMaxUserCount());
        assertEquals(ISSUE_DATE, licenseRecord.getIssueDate());
        assertEquals(Date.from(ISSUE.plusYears(1).toInstant()), licenseRecord.getExpirationDate());
        assertTrue(licenseRecord.hasLicense());
        assertTrue(licenseRecord.isSendEmails());
    }

    @Test
    void createForATestInstance() throws Exception
    {
        // The production expiration limit doesn't apply to test licenses.
        IssuingPlan plan = plan(Environment.TEST, new Date(ISSUE_DATE.getTime() + 1000));

        LicenseRecord licenseRecord = this.factory.create(plan, INSTANCE_ID, FEATURE_ID, ISSUE_DATE).getRecord();

        assertEquals(LicenseType.TRIAL, licenseRecord.getType());
        assertEquals(Date.from(ISSUE.plusYears(1).toInstant()), licenseRecord.getExpirationDate());
    }

    @Test
    void createCappedByTheProdExpirationLimit() throws Exception
    {
        Date limit = Date.from(ISSUE.plusMonths(3).toInstant());

        LicenseRecord licenseRecord =
            this.factory.create(plan(Environment.PROD, limit), INSTANCE_ID, FEATURE_ID, ISSUE_DATE).getRecord();

        assertEquals(limit, licenseRecord.getExpirationDate());

        // A later limit doesn't change anything.
        Date later = Date.from(ISSUE.plusYears(2).toInstant());
        licenseRecord =
            this.factory.create(plan(Environment.PROD, later), INSTANCE_ID, FEATURE_ID, ISSUE_DATE).getRecord();
        assertEquals(Date.from(ISSUE.plusYears(1).toInstant()), licenseRecord.getExpirationDate());
    }

    @Test
    void renewalExtendsTheCurrentExpirationDate() throws Exception
    {
        IssuingPlan plan = renewal();
        ZonedDateTime current = ISSUE.plusMonths(2);
        when(this.licenseRecordStore.getLatest(INSTANCE_ID, FEATURE_ID))
            .thenReturn(Optional.of(record(Date.from(current.toInstant()))));

        LicenseRecord licenseRecord = this.factory.create(plan, INSTANCE_ID, FEATURE_ID, ISSUE_DATE).getRecord();

        assertEquals(Date.from(current.plusYears(1).toInstant()), licenseRecord.getExpirationDate());
    }

    @Test
    void renewalOfAnExpiredLicenseStartsAtTheIssueDate() throws Exception
    {
        IssuingPlan plan = renewal();
        Date expectedExpiration = Date.from(ISSUE.plusYears(1).toInstant());

        when(this.licenseRecordStore.getLatest(INSTANCE_ID, FEATURE_ID))
            .thenReturn(Optional.of(record(Date.from(ISSUE.minusDays(1).toInstant()))));
        assertEquals(expectedExpiration,
            this.factory.create(plan, INSTANCE_ID, FEATURE_ID, ISSUE_DATE).getRecord().getExpirationDate());

        when(this.licenseRecordStore.getLatest(INSTANCE_ID, FEATURE_ID)).thenReturn(Optional.of(record(null)));
        assertEquals(expectedExpiration,
            this.factory.create(plan, INSTANCE_ID, FEATURE_ID, ISSUE_DATE).getRecord().getExpirationDate());

        when(this.licenseRecordStore.getLatest(INSTANCE_ID, FEATURE_ID)).thenReturn(Optional.empty());
        assertEquals(expectedExpiration,
            this.factory.create(plan, INSTANCE_ID, FEATURE_ID, ISSUE_DATE).getRecord().getExpirationDate());
    }

    @Test
    void failures() throws Exception
    {
        IssuingPlan plan = plan(Environment.PROD, null);

        when(this.dependencyResolver.resolve("unknown", null)).thenReturn(Optional.empty());
        assertInternalError(() -> this.factory.create(plan, INSTANCE_ID, "unknown", ISSUE_DATE));

        when(this.generatedLicenseStore.encode(this.signedLicense)).thenThrow(new IOException());
        assertInternalError(() -> this.factory.create(plan, INSTANCE_ID, FEATURE_ID, ISSUE_DATE));

        when(this.signer.sign(any(License.class), eq(2026))).thenThrow(new LicenseSigningException("failed"));
        assertInternalError(() -> this.factory.create(plan, INSTANCE_ID, FEATURE_ID, ISSUE_DATE));

        IssuingPlan renewal = renewal();
        when(this.licenseRecordStore.getLatest(INSTANCE_ID, FEATURE_ID))
            .thenThrow(new QueryException("failed", null, null));
        assertInternalError(() -> this.factory.create(renewal, INSTANCE_ID, FEATURE_ID, ISSUE_DATE));
    }

    @Test
    void toLicense()
    {
        LicenseRecord licenseRecord = record(Date.from(ISSUE.plusYears(1).toInstant()));
        licenseRecord.setLicenseId("c0ffee00-0000-4000-8000-000000000001");
        licenseRecord.setType(LicenseType.PAID);
        licenseRecord.setInstanceId(INSTANCE_ID);
        licenseRecord.setFeatureId(FEATURE_ID);
        licenseRecord.setDependentFeatureIds(List.of("com.xwiki.pro:pro-api"));
        licenseRecord.setMaxUserCount(-1L);
        licenseRecord.setIssueDate(ISSUE_DATE);
        licenseRecord.setFirstName("Jane");

        License license = LicenseFactory.toLicense(licenseRecord);

        assertEquals("c0ffee00-0000-4000-8000-000000000001", license.getId().getId());
        assertEquals(LicenseType.PAID, license.getType());
        assertTrue(license.isApplicableTo(new InstanceId(INSTANCE_ID)));
        assertEquals(licenseRecord.getExpirationDate().getTime(), license.getExpirationDate());
        assertEquals(-1L, license.getMaxUserCount());
        // The same licensee information as the #generateLicense macro, the missing values being empty.
        assertEquals(Map.of("issueDate", String.valueOf(ISSUE_DATE.getTime()), "firstName", "Jane", "lastName", "",
            "email", "", "support", ""), license.getLicensee());
        assertEquals(Set.of(new LicensedFeatureId(FEATURE_ID), new LicensedFeatureId("com.xwiki.pro:pro-api")),
            Set.copyOf(license.getFeatureIds()));
        assertFalse(license.isApplicableTo(new InstanceId("11111111-2222-3333-4444-555555555555")));
    }

    private IssuingPlan renewal()
    {
        IssuingPlan plan = plan(Environment.PROD, null);
        plan.setExistingInstance(new InstanceRecord(INSTANCE_ID, ACCOUNT, Environment.PROD, null, "bundle",
            InstanceRecord.Origin.IMPORT, ISSUE_DATE));
        return plan;
    }

    private static IssuingPlan plan(Environment environment, Date prodExpirationLimit)
    {
        IssuingPlan plan = new IssuingPlan();
        plan.setPolicy(Policy.builder(ACCOUNT).allowedEnvironments(EnumSet.allOf(Environment.class))
            .prodExpirationLimit(prodExpirationLimit).notifications(true, null).build());
        plan.setProduct(new Product("bundle", List.of(FEATURE_ID), Map.of(FEATURE_ID, "1.2"), List.of()));
        plan.setEnvironment(environment);
        plan.setDuration(LicenseDuration.parse("1y").get());
        plan.setSupport("gold");
        plan.setLicensee(new Policy.Licensee("Partner", "Inc", "partner@example.com"));
        plan.setMaxUserCount(500L);
        plan.setCount(1);
        return plan;
    }

    private static LicenseRecord record(Date expirationDate)
    {
        LicenseRecord licenseRecord = new LicenseRecord();
        licenseRecord.setExpirationDate(expirationDate);
        return licenseRecord;
    }

    private static void assertInternalError(org.junit.jupiter.api.function.Executable executable)
    {
        IssuingException exception = assertThrows(IssuingException.class, executable);
        assertEquals(500, exception.getStatus());
        assertEquals("internalError", exception.getCode());
    }
}
