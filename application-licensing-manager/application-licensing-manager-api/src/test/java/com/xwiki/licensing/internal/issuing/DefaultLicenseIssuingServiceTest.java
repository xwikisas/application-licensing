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

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.SignedLicense;
import com.xwiki.licensing.manager.IssuingException;
import com.xwiki.licensing.manager.rest.model.InstanceLicenses;
import com.xwiki.licensing.manager.rest.model.IssueRequest;
import com.xwiki.licensing.manager.rest.model.IssueResponse;
import com.xwiki.licensing.manager.rest.model.LicenseKey;
import com.xwiki.licensing.manager.rest.model.PolicyView;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultLicenseIssuingService}. The rules are tested with the components that hold them
 * ({@link PolicyValidatorTest}, {@link LicenseFactoryTest}, {@link IssuanceWriterTest}...); this checks how they are
 * put together.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultLicenseIssuingServiceTest
{
    private static final DocumentReference ACCOUNT = new DocumentReference("xwiki", "XWiki", "partner");

    private static final String INSTANCE_ID = "9f1c2e4a-7b3d-4c8e-a1f0-2d5e6b7c8a90";

    private static final String PRO = "com.xwiki.pro:xwiki-pro-ui";

    private static final String INJECTOR = "com.xwiki:injector";

    @InjectMockComponents
    private DefaultLicenseIssuingService service;

    @MockComponent
    private PolicyValidator validator;

    @MockComponent
    private UsageCounter usageCounter;

    @MockComponent
    private LicenseFactory licenseFactory;

    @MockComponent
    private IssuanceWriter writer;

    @MockComponent
    private LicenseViews views;

    private final IssueRequest request = new IssueRequest();

    private final Policy policy = Policy.builder(ACCOUNT).build();

    private IssuingPlan plan;

    @BeforeEach
    void setUp() throws Exception
    {
        this.plan = new IssuingPlan();
        this.plan.setPolicy(this.policy);
        this.plan.setProduct(new Product("bundle", List.of(PRO, INJECTOR), Map.of(), List.of(PRO)));
        this.plan.setEnvironment(Environment.PROD);
        this.plan.setCount(1);
        this.plan.setDomain("wiki.example.com");
        when(this.validator.validate(ACCOUNT, this.request)).thenReturn(this.plan);
        when(this.licenseFactory.create(eq(this.plan), anyString(), anyString(), any(Date.class)))
            .thenAnswer(invocation -> license(invocation.getArgument(1), invocation.getArgument(2),
                invocation.getArgument(3)));
    }

    @Test
    void issueNewInstances() throws Exception
    {
        this.plan.setCount(2);

        IssueResponse response = this.service.issue(ACCOUNT, this.request);

        assertEquals(2, response.getInstances().size());
        InstanceLicenses first = response.getInstances().get(0);
        InstanceLicenses second = response.getInstances().get(1);
        assertNotEquals(first.getInstanceId(), second.getInstanceId());
        assertEquals("prod", first.getEnvironment());
        assertEquals("wiki.example.com", first.getDomain());
        assertEquals("bundle", first.getProduct());
        assertEquals(List.of(PRO, INJECTOR), List.of(first.getLicenses().get(0).getFeatureId(),
            first.getLicenses().get(1).getFeatureId()));
        assertEquals("KEY-" + PRO + ",KEY-" + INJECTOR, first.getKeys());

        verify(this.validator).checkRecordAuthor();
        verify(this.validator).checkInstance(this.plan, ACCOUNT);
        verify(this.usageCounter).checkRateLimit(eq(this.policy), eq(4), any(Date.class));
        // The new instance records and the 4 licenses are written at once, and all licenses share the issue date.
        verify(this.writer).write(argThat(instances -> instances.size() == 2
            && instances.get(0).getInstanceId().equals(first.getInstanceId())
            && instances.get(0).getAccount().equals(ACCOUNT)
            && instances.get(0).getOrigin() == InstanceRecord.Origin.API
            && instances.get(0).getCreatedDate().equals(instances.get(1).getCreatedDate())),
            argThat(licenses -> licenses.size() == 4
                && licenses.stream().map(license -> license.getRecord().getIssueDate()).distinct().count() == 1),
            isNull(), eq("wiki.example.com"));
    }

    @Test
    void issueForAChosenInstanceId() throws Exception
    {
        this.plan.setInstanceId(INSTANCE_ID);

        IssueResponse response = this.service.issue(ACCOUNT, this.request);

        assertEquals(INSTANCE_ID, response.getInstances().get(0).getInstanceId());
    }

    @Test
    void renewal() throws Exception
    {
        this.plan.setInstanceId(INSTANCE_ID);
        this.plan.setExistingInstance(new InstanceRecord(INSTANCE_ID, ACCOUNT, Environment.PROD, "old.example.com",
            "bundle", InstanceRecord.Origin.IMPORT, new Date(0)));

        IssueResponse response = this.service.issue(ACCOUNT, this.request);

        InstanceLicenses instance = response.getInstances().get(0);
        assertEquals(INSTANCE_ID, instance.getInstanceId());
        // Only the renewed features.
        assertEquals(1, instance.getLicenses().size());
        assertEquals(PRO, instance.getLicenses().get(0).getFeatureId());
        // The new domain is returned and saved.
        assertEquals("wiki.example.com", instance.getDomain());
        verify(this.writer).write(eq(List.of()), argThat(licenses -> licenses.size() == 1), eq(INSTANCE_ID),
            eq("wiki.example.com"));
    }

    @Test
    void renewalWithoutANewDomain() throws Exception
    {
        this.plan.setExistingInstance(new InstanceRecord(INSTANCE_ID, ACCOUNT, Environment.PROD, "old.example.com",
            "bundle", InstanceRecord.Origin.API, new Date(0)));
        this.plan.setDomain(null);

        assertEquals("old.example.com", this.service.issue(ACCOUNT, this.request).getInstances().get(0).getDomain());

        this.plan.setDomain("old.example.com");
        this.service.issue(ACCOUNT, this.request);

        verify(this.writer, never()).write(any(), any(), eq(INSTANCE_ID), any());
    }

    @Test
    void issueStopsAtTheFirstError() throws Exception
    {
        doThrow(IssuingErrors.rateLimited("limit", 60)).when(this.usageCounter).checkRateLimit(eq(this.policy),
            eq(2), any(Date.class));

        IssuingException exception =
            assertThrows(IssuingException.class, () -> this.service.issue(ACCOUNT, this.request));

        assertEquals(60L, exception.getRetryAfter());
        verify(this.licenseFactory, never()).create(any(), any(), any(), any());
        verify(this.writer, never()).write(any(), any(), any(), any());
    }

    @Test
    void list() throws Exception
    {
        List<InstanceLicenses> instances = List.of(new InstanceLicenses());
        when(this.views.list(ACCOUNT, INSTANCE_ID, "prod", "bundle", true)).thenReturn(instances);

        assertSame(instances, this.service.list(ACCOUNT, INSTANCE_ID, "prod", "bundle", true).getInstances());

        verify(this.validator).getPolicy(ACCOUNT);
    }

    @Test
    void get() throws Exception
    {
        LicenseKey license = new LicenseKey();
        when(this.views.get(ACCOUNT, "id")).thenReturn(license);

        assertSame(license, this.service.get(ACCOUNT, "id"));

        verify(this.validator).getPolicy(ACCOUNT);
    }

    @Test
    void getPolicy() throws Exception
    {
        when(this.validator.getPolicy(ACCOUNT)).thenReturn(this.policy);
        when(this.usageCounter.countIssuances(eq(ACCOUNT), any(Date.class))).thenReturn(3);

        PolicyView view = this.service.getPolicy(ACCOUNT);

        assertEquals(3, view.getUsage().getIssuancesLast24Hours());
        assertNull(view.getMaxUserCount());
    }

    private static PendingLicense license(String instanceId, String featureId, Date issueDate)
    {
        LicenseRecord licenseRecord = new LicenseRecord();
        licenseRecord.setLicenseId(instanceId + featureId);
        licenseRecord.setInstanceId(instanceId);
        licenseRecord.setFeatureId(featureId);
        licenseRecord.setType(LicenseType.PAID);
        licenseRecord.setIssueDate(issueDate);
        licenseRecord.setExpirationDate(new Date(issueDate.getTime() + 1000));
        return new PendingLicense(licenseRecord, mock(SignedLicense.class), "KEY-" + featureId);
    }
}
