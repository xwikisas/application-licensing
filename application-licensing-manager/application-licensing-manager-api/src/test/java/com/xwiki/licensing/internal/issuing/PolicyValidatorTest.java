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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiException;
import com.xwiki.licensing.manager.IssuingException;
import com.xwiki.licensing.manager.rest.model.IssueRequest;
import com.xwiki.licensing.manager.rest.model.LicenseeDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PolicyValidator} and {@link LicenseeValidator}: every 400/403/409 rule of the spec.
 *
 * @version $Id$
 */
@ComponentTest
class PolicyValidatorTest
{
    private static final DocumentReference ACCOUNT = new DocumentReference("xwiki", "XWiki", "partner");

    private static final DocumentReference OTHER_ACCOUNT = new DocumentReference("xwiki", "XWiki", "other");

    private static final String PRODUCT_ID = "bundle";

    private static final String INSTANCE_ID = "9f1c2e4a-7b3d-4c8e-a1f0-2d5e6b7c8a90";

    private static final Product PRODUCT =
        new Product(PRODUCT_ID, List.of("com.xwiki.pro:xwiki-pro-ui", "com.xwiki:injector"), Map.of(),
            List.of("com.xwiki.pro:xwiki-pro-ui"));

    @InjectMockComponents
    private PolicyValidator validator;

    @MockComponent
    private ApiConfiguration configuration;

    @MockComponent
    private LicenseRecordStore licenseRecordStore;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.configuration.getProduct(PRODUCT_ID)).thenReturn(Optional.of(PRODUCT));
        when(this.configuration.getProduct("unknown")).thenReturn(Optional.empty());
        when(this.configuration.getProduct("other")).thenReturn(Optional.of(new Product("other", List.of("f"),
            Map.of(), List.of())));
        setPolicy(builder -> {
        });
    }

    @Test
    void validateWithDefaults() throws Exception
    {
        IssuingPlan plan = this.validator.validate(ACCOUNT, request());

        assertSame(PRODUCT, plan.getProduct());
        assertEquals(ACCOUNT, plan.getPolicy().getAccount());
        assertEquals(Environment.PROD, plan.getEnvironment());
        assertEquals(1, plan.getCount());
        assertNull(plan.getInstanceId());
        assertNull(plan.getDomain());
        assertEquals("gold", plan.getSupport());
        assertEquals(IssuingPlan.UNLIMITED, plan.getMaxUserCount());
        assertEquals("Partner", plan.getLicensee().getFirstName());
        assertEquals("partner@example.com", plan.getLicensee().getEmail());
        // The first policy duration: 1y.
        assertEquals("2025-01-01", plan.getDuration()
            .addTo(java.time.ZonedDateTime.parse("2024-01-01T00:00:00Z")).toLocalDate().toString());
        assertFalse(plan.isRenewal());
        assertEquals(PRODUCT.getFeatureIds(), plan.getFeatureIds());
        assertEquals(2, plan.getLicenseCount());
    }

    @Test
    void validateWithRequestValues() throws Exception
    {
        setPolicy(builder -> builder.userCounts(1000L, 100L).support(List.of("gold", "silver"), null)
            .allowedEnvironments(EnumSet.allOf(Environment.class))
            .licensee(EnumSet.allOf(Policy.LicenseeMode.class), Policy.LicenseeMode.PARTNER, partner()));
        IssueRequest request = request();
        request.setEnvironment("test");
        request.setCount(3);
        request.setDomain("  wiki.example.com ");
        request.setMaxUserCount(500L);
        request.setDuration(" 1Y ");
        request.setSupport("silver");
        request.setLicenseeMode("customer");
        request.setLicensee(licensee(" John ", "Smith", "john@example.com"));

        IssuingPlan plan = this.validator.validate(ACCOUNT, request);

        assertEquals(Environment.TEST, plan.getEnvironment());
        assertEquals(3, plan.getCount());
        assertEquals(6, plan.getLicenseCount());
        assertEquals("wiki.example.com", plan.getDomain());
        assertEquals(500L, plan.getMaxUserCount());
        assertEquals("2025-01-01", plan.getDuration()
            .addTo(java.time.ZonedDateTime.parse("2024-01-01T00:00:00Z")).toLocalDate().toString());
        assertEquals("silver", plan.getSupport());
        assertEquals("John", plan.getLicensee().getFirstName());
        assertEquals("Smith", plan.getLicensee().getLastName());
        assertEquals("john@example.com", plan.getLicensee().getEmail());
    }

    @Test
    void validateUserCountDefaults() throws Exception
    {
        // No default: the policy maximum.
        setPolicy(builder -> builder.userCounts(1000L, null));
        assertEquals(1000L, this.validator.validate(ACCOUNT, request()).getMaxUserCount());

        // A negative maximum means unlimited.
        setPolicy(builder -> builder.userCounts(-1L, 50L));
        assertEquals(50L, this.validator.validate(ACCOUNT, request()).getMaxUserCount());
        IssueRequest request = request();
        request.setMaxUserCount(-1L);
        assertEquals(IssuingPlan.UNLIMITED, this.validator.validate(ACCOUNT, request).getMaxUserCount());
    }

    @Test
    void validateLicenseeNone() throws Exception
    {
        setPolicy(builder -> builder.licensee(Set.of(Policy.LicenseeMode.NONE), null, partner()));

        Policy.Licensee licensee = this.validator.validate(ACCOUNT, request()).getLicensee();

        assertNull(licensee.getFirstName());
        assertNull(licensee.getLastName());
        assertNull(licensee.getEmail());
    }

    @Test
    void policyErrors() throws Exception
    {
        when(this.configuration.getPolicy(ACCOUNT)).thenReturn(Optional.empty());
        assertError(403, "noPolicy", request());

        setPolicy(builder -> builder.enabled(false));
        assertError(403, "policyDisabled", request());

        when(this.configuration.getPolicy(ACCOUNT)).thenThrow(new XWikiException());
        assertError(500, "internalError", request());
    }

    @Test
    void invalidRequests()
    {
        IssuingException exception = assertThrows(IssuingException.class, () -> this.validator.validate(ACCOUNT, null));
        assertEquals("invalidRequest", exception.getCode());

        assertError(400, "invalidRequest", request(r -> r.setProduct(" ")));
        assertError(400, "unknownProduct", request(r -> r.setProduct("unknown")));
        assertError(400, "invalidRequest", request(r -> r.setEnvironment(null)));
        assertError(400, "invalidRequest", request(r -> r.setEnvironment("PROD")));
        assertError(400, "invalidRequest", request(r -> {
            r.setInstanceId(INSTANCE_ID);
            r.setCount(2);
        }));
        assertError(400, "invalidRequest", request(r -> r.setInstanceId("../other")));
        assertError(400, "invalidRequest", request(r -> r.setInstanceId("instance-1")));
        assertError(400, "invalidRequest", request(r -> r.setCount(0)));
        assertError(400, "invalidRequest", request(r -> r.setMaxUserCount(0L)));
        assertError(400, "invalidRequest", request(r -> r.setMaxUserCount(-2L)));
        assertError(400, "invalidRequest", request(r -> r.setDomain(StringUtils.repeat('a', 256))));
        assertError(400, "invalidRequest", request(r -> r.setLicenseeMode("Partner")));
    }

    @Test
    void notAllowed()
    {
        assertError(403, "productNotAllowed", request(r -> r.setProduct("other")));
        assertError(403, "environmentNotAllowed", request(r -> r.setEnvironment("test")));
        assertError(403, "limitExceeded", request(r -> r.setCount(11)));
        assertError(403, "durationNotAllowed", request(r -> r.setDuration("2y")));
        assertError(403, "supportNotAllowed", request(r -> r.setSupport("silver")));
        assertError(403, "licenseeNotAllowed", request(r -> r.setLicenseeMode("customer")));
        assertError(403, "licenseeNotAllowed", request(r -> r.setLicensee(licensee("a", "b", "c"))));
    }

    @Test
    void userCountOverTheMaximum() throws Exception
    {
        setPolicy(builder -> builder.userCounts(1000L, null));

        assertError(403, "limitExceeded", request(r -> r.setMaxUserCount(1001L)));
        assertError(403, "limitExceeded", request(r -> r.setMaxUserCount(-1L)));
    }

    @Test
    void customerLicenseeRequired() throws Exception
    {
        setPolicy(builder -> builder.licensee(Set.of(Policy.LicenseeMode.CUSTOMER), null, partner()));

        assertError(400, "licenseeRequired", request());
        assertError(400, "licenseeRequired", request(r -> r.setLicensee(licensee("John", " ", "john@example.com"))));
        assertError(400, "licenseeRequired", request(r -> r.setLicensee(licensee(null, "Smith", "john@example.com"))));
        assertError(400, "licenseeRequired", request(r -> r.setLicensee(licensee("John", "Smith", null))));
        assertError(400, "invalidRequest",
            request(r -> r.setLicensee(licensee("John", "Smith", StringUtils.repeat('a', 256)))));
    }

    @Test
    void configurationErrors() throws Exception
    {
        // The partner mode without partner details.
        setPolicy(builder -> builder.licensee(null, null, new Policy.Licensee("Partner", null, "p@example.com")));
        assertError(500, "internalError", request());

        // An invalid policy duration.
        setPolicy(builder -> builder.durations(Environment.PROD, List.of("1year")));
        assertError(500, "internalError", request());

        when(this.configuration.getProduct(PRODUCT_ID)).thenThrow(new XWikiException());
        assertError(500, "internalError", request());
    }

    @Test
    void prodExpirationLimitReached() throws Exception
    {
        setPolicy(builder -> builder.prodExpirationLimit(new Date(System.currentTimeMillis() - 1000)));
        assertError(403, "limitExceeded", request());

        // It only applies to production licenses.
        setPolicy(builder -> builder.prodExpirationLimit(new Date(System.currentTimeMillis() - 1000))
            .allowedEnvironments(EnumSet.allOf(Environment.class)));
        assertEquals(Environment.TEST, this.validator.validate(ACCOUNT, request(r -> r.setEnvironment("test")))
            .getEnvironment());

        setPolicy(builder -> builder.prodExpirationLimit(new Date(System.currentTimeMillis() + 100000)));
        assertEquals(Environment.PROD, this.validator.validate(ACCOUNT, request()).getEnvironment());
    }

    @Test
    void checkInstanceWithoutInstanceId() throws Exception
    {
        IssuingPlan plan = this.validator.validate(ACCOUNT, request());

        this.validator.checkInstance(plan, ACCOUNT);

        assertFalse(plan.isRenewal());
    }

    @Test
    void checkInstanceRenewal() throws Exception
    {
        InstanceRecord instance = instance(ACCOUNT, Environment.PROD, PRODUCT_ID);
        when(this.configuration.getInstance(INSTANCE_ID)).thenReturn(Optional.of(instance));
        IssuingPlan plan = this.validator.validate(ACCOUNT, request(r -> r.setInstanceId(INSTANCE_ID)));

        this.validator.checkInstance(plan, ACCOUNT);

        assertTrue(plan.isRenewal());
        assertSame(instance, plan.getExistingInstance());
        assertEquals(INSTANCE_ID, plan.getInstanceId());
        // A renewal only licenses the renewed features.
        assertEquals(List.of("com.xwiki.pro:xwiki-pro-ui"), plan.getFeatureIds());
        assertEquals(1, plan.getLicenseCount());
    }

    @Test
    void checkInstanceConflicts() throws Exception
    {
        assertInstanceError(instance(OTHER_ACCOUNT, Environment.PROD, PRODUCT_ID), 409, "instanceIdTaken");
        assertInstanceError(instance(ACCOUNT, Environment.TEST, PRODUCT_ID), 409, "environmentConflict");
        assertInstanceError(instance(ACCOUNT, null, PRODUCT_ID), 409, "environmentConflict");
        assertInstanceError(instance(ACCOUNT, Environment.PROD, "other"), 409, "productConflict");
    }

    @Test
    void checkNewInstance() throws Exception
    {
        when(this.configuration.getInstance(INSTANCE_ID)).thenReturn(Optional.empty());
        IssuingPlan plan = this.validator.validate(ACCOUNT, request(r -> r.setInstanceId(INSTANCE_ID)));

        IssuingException exception =
            assertThrows(IssuingException.class, () -> this.validator.checkInstance(plan, ACCOUNT));
        assertEquals(403, exception.getStatus());
        assertEquals("instanceIdNotAllowed", exception.getCode());

        setPolicy(builder -> builder.mayChooseInstanceId(true));
        IssuingPlan allowed = this.validator.validate(ACCOUNT, request(r -> r.setInstanceId(INSTANCE_ID)));
        this.validator.checkInstance(allowed, ACCOUNT);
        assertFalse(allowed.isRenewal());
        assertEquals(INSTANCE_ID, allowed.getInstanceId());

        // Licensed by hand before the API.
        when(this.licenseRecordStore.hasRecords(INSTANCE_ID)).thenReturn(true);
        exception = assertThrows(IssuingException.class, () -> this.validator.checkInstance(allowed, ACCOUNT));
        assertEquals(409, exception.getStatus());
        assertEquals("instanceNotImported", exception.getCode());

        when(this.licenseRecordStore.hasRecords(INSTANCE_ID)).thenThrow(new QueryException("failed", null, null));
        exception = assertThrows(IssuingException.class, () -> this.validator.checkInstance(allowed, ACCOUNT));
        assertEquals(500, exception.getStatus());
    }

    @Test
    void checkRecordAuthor() throws Exception
    {
        when(this.configuration.getRecordAuthor()).thenReturn(mock(UserReference.class));
        this.validator.checkRecordAuthor();

        when(this.configuration.getRecordAuthor()).thenReturn(null);
        assertEquals(500, assertThrows(IssuingException.class, () -> this.validator.checkRecordAuthor()).getStatus());

        when(this.configuration.getRecordAuthor()).thenThrow(new XWikiException());
        assertEquals(500, assertThrows(IssuingException.class, () -> this.validator.checkRecordAuthor()).getStatus());
    }

    private void assertInstanceError(InstanceRecord instance, int status, String code) throws Exception
    {
        when(this.configuration.getInstance(INSTANCE_ID)).thenReturn(Optional.of(instance));
        IssuingPlan plan = this.validator.validate(ACCOUNT, request(r -> r.setInstanceId(INSTANCE_ID)));

        IssuingException exception =
            assertThrows(IssuingException.class, () -> this.validator.checkInstance(plan, ACCOUNT));

        assertEquals(status, exception.getStatus());
        assertEquals(code, exception.getCode());
    }

    private void assertError(int status, String code, IssueRequest request)
    {
        IssuingException exception =
            assertThrows(IssuingException.class, () -> this.validator.validate(ACCOUNT, request));
        assertEquals(status, exception.getStatus(), exception.getMessage());
        assertEquals(code, exception.getCode(), exception.getMessage());
    }

    private void setPolicy(Consumer<Policy.Builder> customizer) throws Exception
    {
        Policy.Builder builder = Policy.builder(ACCOUNT).products(List.of(PRODUCT_ID))
            .allowedEnvironments(EnumSet.of(Environment.PROD)).durations(Environment.TEST, List.of("6m", "1y"))
            .licensee(null, null, partner()).support(List.of("gold"), null);
        customizer.accept(builder);
        when(this.configuration.getPolicy(ACCOUNT)).thenReturn(Optional.of(builder.build()));
    }

    private static Policy.Licensee partner()
    {
        return new Policy.Licensee("Partner", "Inc", "partner@example.com");
    }

    private static InstanceRecord instance(DocumentReference account, Environment environment, String product)
    {
        return new InstanceRecord(INSTANCE_ID, account, environment, null, product, InstanceRecord.Origin.API,
            new Date());
    }

    private static IssueRequest request()
    {
        IssueRequest request = new IssueRequest();
        request.setProduct(PRODUCT_ID);
        request.setEnvironment("prod");
        return request;
    }

    private static IssueRequest request(Consumer<IssueRequest> customizer)
    {
        IssueRequest request = request();
        customizer.accept(request);
        return request;
    }

    private static LicenseeDetails licensee(String firstName, String lastName, String email)
    {
        LicenseeDetails licensee = new LicenseeDetails();
        licensee.setFirstName(firstName);
        licensee.setLastName(lastName);
        licensee.setEmail(email);
        return licensee;
    }
}
