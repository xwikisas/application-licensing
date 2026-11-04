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
package com.xwiki.licensing.internal.limitwarnings.userlimit;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEventConverter;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseId;
import com.xwiki.licensing.LicensedFeatureId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LicenseUserLimitWarningEventConverter}.
 *
 * @version $Id$
 */
@ComponentTest
class LicenseUserLimitWarningEventConverterTest
{
    private static final String LICENSE_ID = "00000000-0000-0000-0000-000000000000";

    @InjectMockComponents
    private LicenseUserLimitWarningEventConverter converter;

    @MockComponent
    private RecordableEventConverter defaultConverter;

    @Test
    void convertWithDuplicateLicense() throws Exception
    {
        Event convertedEvent = mock(Event.class);
        when(this.defaultConverter.convert(any(), any(), any())).thenReturn(convertedEvent);

        // Two instances of the same license, as returned by LicenseManager#getUsedLicenses().
        LicenseUserLimitWarningEvent event =
            new LicenseUserLimitWarningEvent(List.of(createLicense(), createLicense()), 4, Set.of());

        this.converter.convert(event, "source", null);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(convertedEvent).setBody(body.capture());
        Map<String, Object> licenses = (Map<String, Object>) new ObjectMapper().readValue(body.getValue(), Map.class)
            .get("licenses");
        assertEquals(Map.of(LICENSE_ID, Map.of("affectedExtensions", List.of("foo"), "maxUserCount", 5)), licenses);
    }

    private License createLicense()
    {
        License license = new License();
        license.setId(new LicenseId(LICENSE_ID));
        license.addFeatureId(new LicensedFeatureId("foo"));
        license.setMaxUserCount(5);
        return license;
    }
}
