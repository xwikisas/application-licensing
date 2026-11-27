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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link LicenseDuration}.
 *
 * @version $Id$
 */
class LicenseDurationTest
{
    private static final ZonedDateTime START = ZonedDateTime.of(2024, 2, 29, 10, 0, 0, 0, ZoneOffset.UTC);

    @ParameterizedTest
    @CsvSource({ "10d, 2024-03-10", "2w, 2024-03-14", "6m, 2024-08-29", "1y, 2025-02-28", "3y, 2027-02-28" })
    void addTo(String duration, String expected)
    {
        assertEquals(expected, LicenseDuration.parse(duration).get().addTo(START).toLocalDate().toString());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "1", "y", "0y", "-1y", "1Y", "1 y", "1h", "12345d", "1y2m" })
    void parseInvalid(String duration)
    {
        assertTrue(LicenseDuration.parse(duration).isEmpty());
    }
}
