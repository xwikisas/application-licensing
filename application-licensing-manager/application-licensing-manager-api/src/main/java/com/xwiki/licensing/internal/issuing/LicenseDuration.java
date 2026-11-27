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

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A license duration as written in the policies and the requests: a number followed by a unit, {@code d} (days),
 * {@code w} (weeks), {@code m} (months) or {@code y} (years), e.g. {@code 1y} or {@code 6m}.
 *
 * @version $Id$
 * @since 1.34
 */
public final class LicenseDuration
{
    private static final Pattern FORMAT = Pattern.compile("([1-9]\\d{0,3})([dwmy])");

    private final int amount;

    private final char unit;

    private LicenseDuration(int amount, char unit)
    {
        this.amount = amount;
        this.unit = unit;
    }

    /**
     * @param value a duration, e.g. {@code 1y}
     * @return the duration, or empty if the value isn't a valid duration
     */
    public static Optional<LicenseDuration> parse(String value)
    {
        Matcher matcher = FORMAT.matcher(value);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(new LicenseDuration(Integer.parseInt(matcher.group(1)), matcher.group(2).charAt(0)));
    }

    /**
     * @param start a date
     * @return the date plus this duration (calendar units, so {@code 1y} after 29 February is 28 February)
     */
    public ZonedDateTime addTo(ZonedDateTime start)
    {
        switch (this.unit) {
            case 'd':
                return start.plusDays(this.amount);
            case 'w':
                return start.plusWeeks(this.amount);
            case 'm':
                return start.plusMonths(this.amount);
            default:
                return start.plusYears(this.amount);
        }
    }
}
