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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.eventstream.RecordableEventConverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicensedFeatureId;

/**
 * Converter for the event {@link LicenseUserLimitWarningEvent}.
 *
 * @version $Id$
 * @since 1.34
 */
@Component
@Singleton
@Named(LicenseUserLimitWarningEvent.EVENT_TYPE)
public class LicenseUserLimitWarningEventConverter implements RecordableEventConverter
{
    @Inject
    private RecordableEventConverter defaultConverter;

    @Inject
    private Logger logger;

    @Override
    public Event convert(RecordableEvent recordableEvent, String source, Object data) throws Exception
    {
        LicenseUserLimitWarningEvent event = (LicenseUserLimitWarningEvent) recordableEvent;

        Event convertedEvent = this.defaultConverter.convert(event, source, data);

        convertedEvent.setBody(serializeParams(
            Map.of("userCount", event.getUserCount(), "licenses", getLicensesInfo(event.getLicenses()))));
        return convertedEvent;
    }

    private Map<String, Map<String, Object>> getLicensesInfo(Collection<License> licenses)
    {
        // The same license can be present more than once, as different License instances (License#hashCode() is not
        // consistent with License#equals()), so keep only the first one for each license id.
        return licenses.stream().collect(
            Collectors.toMap(license -> license.getId().getId(), this::getLicenseInfo, (first, duplicate) -> first));
    }

    private Map<String, Object> getLicenseInfo(License license)
    {
        List<String> affectedExtensions =
            license.getFeatureIds().stream().map(LicensedFeatureId::getId).collect(Collectors.toList());
        return Map.of("affectedExtensions", affectedExtensions, "maxUserCount", license.getMaxUserCount());
    }

    @Override
    public List<RecordableEvent> getSupportedEvents()
    {
        return List.of(new LicenseUserLimitWarningEvent());
    }

    /**
     * Utility method to convert an object to JSON.
     *
     * @param params the event parameters to serialize
     * @return the JSON string, or {@code null} if the serialization failed
     */
    private String serializeParams(Map<String, Object> params)
    {
        String json = null;
        try {
            ObjectWriter ow = new ObjectMapper().writer();
            json = ow.writeValueAsString(params);
        } catch (Exception e) {
            logger.warn("Error while serializing parameters of LicenseUserLimitWarningEvent. Cause: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
        }
        return json;
    }
}
