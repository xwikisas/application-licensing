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

import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xwiki.licensing.manager.rest.model.PolicyView;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PolicyViews}.
 *
 * @version $Id$
 */
class PolicyViewsTest
{
    private static final DocumentReference ACCOUNT = new DocumentReference("xwiki", "XWiki", "partner");

    @Test
    void toPolicyView()
    {
        Policy policy = Policy.builder(ACCOUNT).products(List.of("bundle"))
            .allowedEnvironments(EnumSet.of(Environment.PROD, Environment.TEST))
            .durations(Environment.TEST, List.of("6m", "1y"))
            .prodExpirationLimit(Date.from(Instant.parse("2030-01-01T00:00:00Z"))).userCounts(5000L, 100L)
            .mayChooseInstanceId(true).maxBatchSize(20)
            .licensee(Set.of(Policy.LicenseeMode.CUSTOMER, Policy.LicenseeMode.NONE), Policy.LicenseeMode.NONE,
                new Policy.Licensee("Partner", "Inc", "partner@example.com"))
            .support(List.of("gold", "silver"), "silver").maxIssuancesPerDay(30).volumeLimits(10, 20, 50000L)
            .quota(Policy.QuotaMode.REJECT, null, null).build();

        PolicyView view = PolicyViews.toPolicyView(policy, 7);

        assertEquals(List.of("bundle"), view.getProducts());
        assertEquals(List.of("prod", "test"), view.getEnvironments());
        assertEquals(List.of("1y"), view.getProdDurations());
        assertEquals(List.of("6m", "1y"), view.getTestDurations());
        assertEquals("2030-01-01T00:00:00Z", view.getProdExpirationLimit());
        assertEquals(5000L, view.getMaxUserCount());
        assertEquals(100L, view.getDefaultUserCount());
        assertTrue(view.getMayChooseInstanceId());
        assertEquals(20, view.getMaxBatchSize());
        assertEquals(List.of("customer", "none"), view.getLicenseeModes());
        assertEquals("none", view.getDefaultLicenseeMode());
        assertEquals(List.of("gold", "silver"), view.getSupportLevels());
        assertEquals("silver", view.getDefaultSupport());
        assertEquals(30, view.getMaxIssuancesPerDay());
        assertEquals(10, view.getMaxActiveProdInstances());
        assertEquals(20, view.getMaxActiveTestInstances());
        assertEquals(50000L, view.getMaxTotalUsers());
        assertEquals("reject", view.getQuotaMode());
        assertEquals(7, view.getUsage().getIssuancesLast24Hours());
    }

    @Test
    void toPolicyViewWithDefaults()
    {
        PolicyView view = PolicyViews.toPolicyView(Policy.builder(ACCOUNT).build(), 0);

        assertEquals(List.of(), view.getEnvironments());
        assertNull(view.getProdExpirationLimit());
        assertNull(view.getMaxUserCount());
        assertEquals(List.of("partner"), view.getLicenseeModes());
        assertEquals("alert", view.getQuotaMode());
        assertEquals(25, view.getMaxIssuancesPerDay());
    }
}
