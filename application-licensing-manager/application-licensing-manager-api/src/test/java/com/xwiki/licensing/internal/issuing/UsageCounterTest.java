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
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.licensing.manager.IssuingException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UsageCounter}.
 *
 * @version $Id$
 */
@ComponentTest
class UsageCounterTest
{
    private static final DocumentReference ACCOUNT = new DocumentReference("xwiki", "XWiki", "partner");

    private static final long HOUR = TimeUnit.HOURS.toMillis(1);

    private static final Date NOW = new Date(1_800_000_000_000L);

    private static final List<String> ACCOUNT_VALUES = List.of("XWiki.partner", "xwiki:XWiki.partner");

    @InjectMockComponents
    private UsageCounter counter;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private InstanceRecordStore instanceRecordStore;

    @MockComponent
    private ApiConfiguration configuration;

    @MockComponent
    private IssuingAlerts alerts;

    private final Query query = mock(Query.class);

    private final Policy policy = Policy.builder(ACCOUNT).maxIssuancesPerDay(3).build();

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.configuration.getMainWiki()).thenReturn(new WikiReference("xwiki"));
        when(this.instanceRecordStore.getAccountValues(ACCOUNT)).thenReturn(ACCOUNT_VALUES);
        when(this.queryManager.createQuery(contains("instance.account in (:accounts)"), eq(Query.XWQL)))
            .thenReturn(this.query);
        when(this.query.setWiki("xwiki")).thenReturn(this.query);
        when(this.query.bindValue("accounts", ACCOUNT_VALUES)).thenReturn(this.query);
        // The window starts 24 hours before the issue date.
        when(this.query.bindValue("since", new Date(NOW.getTime() - 24 * HOUR))).thenReturn(this.query);
        when(this.query.execute()).thenReturn(List.of(ago(23), ago(22), ago(1)));
    }

    @Test
    void underTheLimit() throws Exception
    {
        when(this.query.execute()).thenReturn(List.of(ago(23)));

        this.counter.checkRateLimit(this.policy, 2, NOW);

        verify(this.alerts, never()).rateLimitReached(this.policy);
    }

    @Test
    void rateLimited()
    {
        IssuingException exception =
            assertThrows(IssuingException.class, () -> this.counter.checkRateLimit(this.policy, 1, NOW));

        assertEquals(429, exception.getStatus());
        assertEquals("rateLimited", exception.getCode());
        // The oldest license leaves the window in one hour.
        assertEquals(3600L, exception.getRetryAfter());
        verify(this.alerts).rateLimitReached(this.policy);

        // Two licenses need the two oldest ones to leave the window.
        exception = assertThrows(IssuingException.class, () -> this.counter.checkRateLimit(this.policy, 2, NOW));
        assertEquals(7200L, exception.getRetryAfter());
    }

    @Test
    void retryAfterIsAtLeastOneSecond() throws Exception
    {
        when(this.query.execute()).thenReturn(List.of(new Date(NOW.getTime() - 24 * HOUR + 1), ago(2), ago(1)));

        assertEquals(1L,
            assertThrows(IssuingException.class, () -> this.counter.checkRateLimit(this.policy, 1, NOW))
                .getRetryAfter());
    }

    @Test
    void requestOverTheDailyLimit() throws Exception
    {
        IssuingException exception =
            assertThrows(IssuingException.class, () -> this.counter.checkRateLimit(this.policy, 4, NOW));

        assertEquals(403, exception.getStatus());
        assertEquals("limitExceeded", exception.getCode());
        verify(this.queryManager, never()).createQuery(anyString(), anyString());
    }

    @Test
    void countIssuances() throws Exception
    {
        assertEquals(3, this.counter.countIssuances(ACCOUNT, NOW));
    }

    @Test
    void queryFailure() throws Exception
    {
        when(this.query.execute()).thenThrow(new QueryException("failed", null, null));

        assertEquals(500, assertThrows(IssuingException.class, () -> this.counter.countIssuances(ACCOUNT, NOW))
            .getStatus());
    }

    private static Date ago(long hours)
    {
        return new Date(NOW.getTime() - hours * HOUR);
    }
}
