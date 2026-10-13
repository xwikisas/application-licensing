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

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Benchmark for {@link UserCounter#isUserUnderLimit(DocumentReference, long)} on an instance with many users. It
 * doesn't assert on the measurements (they depend on the machine), it appends them to
 * {@code target/benchmark/UserCounter.csv} so that they can be compared between releases.
 *
 * @version $Id$
 * @since 1.33.0
 */
@ComponentTest
class UserCounterBenchmarkTest
{
    private static final int USER_COUNT = 10_000;

    private static final int USER_LIMIT = USER_COUNT - 100;

    private static final int RUNS = 30;

    private static final Path RESULTS = Paths.get("target", "benchmark", "UserCounter.csv");

    @InjectMockComponents
    private UserCounter userCounter;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private DocumentReferenceResolver<String> resolver;

    @MockComponent
    @Named("count")
    private QueryFilter countFilter;

    @MockComponent
    @Named("unique")
    private QueryFilter uniqueFilter;

    private final com.sun.management.ThreadMXBean threadMXBean =
        (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    @Test
    void isUserUnderLimitWithManyUsers() throws Exception
    {
        mockUsers();
        // The newest users are the worst case for the lookup, and the last one is over the limit.
        DocumentReference underLimit = new DocumentReference("xwiki", "XWiki", "User" + (USER_LIMIT - 1));
        DocumentReference overLimit = new DocumentReference("xwiki", "XWiki", "User" + (USER_COUNT - 1));

        // Cold: the user list is computed (queried, resolved and sorted) on every call, like after a user change.
        long[][] cold = measure(() -> {
            this.userCounter.flushCache();
            assertTrue(this.userCounter.isUserUnderLimit(underLimit, USER_LIMIT));
        });
        // Cached: the cost paid on every request once the user list is computed.
        long[][] cached = measure(() -> assertFalse(this.userCounter.isUserUnderLimit(overLimit, USER_LIMIT)));

        record(cold, cached);
    }

    private void mockUsers() throws Exception
    {
        when(this.wikiDescriptorManager.getAllIds()).thenReturn(List.of("xwiki"));

        // Creation dates have a precision of one second in the database, so several users share the same date.
        List<Object> users = new ArrayList<>();
        for (int i = 0; i < USER_COUNT; i++) {
            users.add(new Object[] { "XWiki.User" + i, new Date(i / 10 * 1000L) });
        }
        // The database doesn't return the users sorted.
        Collections.shuffle(users, new Random(42));

        // Stub-only mocks don't record their invocations, which would be counted in the allocated memory.
        Query query = mock(Query.class, withSettings().stubOnly());
        when(query.addFilter(any())).thenReturn(query);
        when(query.setWiki(any())).thenReturn(query);
        when(query.execute()).thenReturn(users);
        when(this.queryManager.createQuery(any(), any())).thenReturn(query);
        // Called once per user, so don't go through Mockito at all.
        DocumentReferenceResolver<String> stubResolver = (fullName, parameters) -> new DocumentReference(
            ((WikiReference) parameters[0]).getName(), "XWiki", fullName.substring("XWiki.".length()));
        ReflectionUtils.setFieldValue(this.userCounter, "documentReferenceResolver", stubResolver);
    }

    /**
     * @return the duration in nanoseconds and the allocated bytes of each run, sorted
     */
    private long[][] measure(Run run) throws Exception
    {
        // Let the JIT compile the code before measuring.
        for (int i = 0; i < RUNS; i++) {
            run.run();
        }
        long threadId = Thread.currentThread().getId();
        long[] durations = new long[RUNS];
        long[] allocations = new long[RUNS];
        for (int i = 0; i < RUNS; i++) {
            long allocatedBefore = this.threadMXBean.getThreadAllocatedBytes(threadId);
            long start = System.nanoTime();
            run.run();
            durations[i] = System.nanoTime() - start;
            allocations[i] = this.threadMXBean.getThreadAllocatedBytes(threadId) - allocatedBefore;
        }
        Arrays.sort(durations);
        Arrays.sort(allocations);
        return new long[][] { durations, allocations };
    }

    private void record(long[][] cold, long[][] cached) throws Exception
    {
        String header = "version,date,users,coldMedianMicros,coldMaxMicros,coldMedianAllocatedKB,"
            + "cachedMedianMicros,cachedMaxMicros,cachedMedianAllocatedKB\n";
        String line = String.format("%s,%s,%d,%d,%d,%d,%d,%d,%d%n", System.getProperty("project.version", "unknown"),
            LocalDate.now(), USER_COUNT, median(cold[0]) / 1000, max(cold[0]) / 1000, median(cold[1]) / 1024,
            median(cached[0]) / 1000, max(cached[0]) / 1000, median(cached[1]) / 1024);

        Files.createDirectories(RESULTS.getParent());
        if (!Files.exists(RESULTS)) {
            Files.write(RESULTS, header.getBytes(StandardCharsets.UTF_8));
        }
        Files.write(RESULTS, line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
    }

    private long median(long[] sorted)
    {
        return sorted[sorted.length / 2];
    }

    private long max(long[] sorted)
    {
        return sorted[sorted.length - 1];
    }

    @FunctionalInterface
    private interface Run
    {
        void run() throws Exception;
    }
}
