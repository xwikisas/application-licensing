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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xwiki.licensing.LicenseType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Policy}, {@link Product} and {@link Environment}.
 *
 * @version $Id$
 */
class PolicyTest
{
    private static final DocumentReference ACCOUNT = new DocumentReference("xwiki", "XWiki", "partner");

    @Test
    void defaults()
    {
        Policy policy = Policy.builder(ACCOUNT).enabled(null).maxBatchSize(null).licensee(null, null)
            .notifications(false, null).maxIssuancesPerDay(null).quota(null, null, null)
            .durations(Environment.PROD, List.of()).durations(Environment.TEST, List.of()).build();

        assertEquals(ACCOUNT, policy.getAccount());
        assertTrue(policy.isEnabled());
        assertTrue(policy.getProducts().isEmpty());
        assertTrue(policy.getAllowedEnvironments().isEmpty());
        assertEquals(List.of("1y"), policy.getDurations(Environment.PROD));
        assertEquals(List.of("6m"), policy.getDurations(Environment.TEST));
        assertNull(policy.getProdExpirationLimit());
        assertNull(policy.getMaxUserCount());
        assertNull(policy.getDefaultUserCount());
        assertFalse(policy.mayChooseInstanceId());
        assertEquals(10, policy.getMaxBatchSize());
        assertEquals(Policy.LicenseeMode.PARTNER, policy.getLicenseeMode());
        assertNull(policy.getDefaultSupport());
        assertFalse(policy.isSendExpirationReminders());
        assertEquals("none", policy.getSalesNotification());
        assertEquals(25, policy.getMaxIssuancesPerDay());
        assertEquals(Policy.QuotaMode.ALERT, policy.getQuotaMode());
        assertEquals("none", policy.getQuotaPeriod());
        assertTrue(policy.getAlertRecipients().isEmpty());
    }

    @Test
    void defaultSupport()
    {
        assertEquals("gold",
            Policy.builder(ACCOUNT).support(List.of("gold", "silver"), null).build().getDefaultSupport());
        assertEquals("silver",
            Policy.builder(ACCOUNT).support(List.of("gold", "silver"), "silver").build().getDefaultSupport());
    }

    @Test
    void allowedEnvironments()
    {
        Policy policy = Policy.builder(ACCOUNT).allowedEnvironments(Set.of(Environment.TEST)).build();

        assertEquals(Set.of(Environment.TEST), policy.getAllowedEnvironments());
    }

    @Test
    void productRenewsAllFeaturesByDefault()
    {
        Product product = new Product("bundle", List.of("a", "b"), Map.of("a", "1.0"), List.of());

        assertEquals("bundle", product.getId());
        assertEquals(List.of("a", "b"), product.getRenewedFeatureIds());
        assertEquals("1.0", product.getExtensionVersion("a"));
        assertNull(product.getExtensionVersion("b"));
        assertEquals(List.of("a"),
            new Product("bundle", List.of("a", "b"), Map.of(), List.of("a")).getRenewedFeatureIds());
    }

    @Test
    void environment()
    {
        assertEquals(LicenseType.PAID, Environment.PROD.getLicenseType());
        assertEquals(LicenseType.TRIAL, Environment.TEST.getLicenseType());
        assertEquals("test", Environment.TEST.getValue());
        assertEquals(Optional.of(Environment.PROD), Environment.fromValue("prod"));
        assertEquals(Optional.empty(), Environment.fromValue("PROD"));
        assertEquals(Optional.empty(), Environment.fromValue(null));
    }
}
