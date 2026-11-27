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
package com.xwiki.licensing.internal.rest;

import javax.inject.Named;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.xwiki.licensing.internal.issuing.IssuingErrors;
import com.xwiki.licensing.manager.IssuingException;
import com.xwiki.licensing.manager.rest.LicensesResource;
import com.xwiki.licensing.manager.rest.model.IssueRequest;

/**
 * Default {@link LicensesResource}.
 *
 * @version $Id$
 * @since 1.34
 */
@Component
@Named("com.xwiki.licensing.internal.rest.DefaultLicensesResource")
public class DefaultLicensesResource extends AbstractIssuingResource implements LicensesResource
{
    /**
     * Unknown fields are refused, so that a misspelled optional field isn't silently ignored.
     */
    private static final ObjectReader REQUEST_READER = new ObjectMapper()
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readerFor(IssueRequest.class);

    @Override
    public Response issue(String body)
    {
        return handle(Response.Status.CREATED,
            account -> this.issuingService.issue(account, parse(body)));
    }

    @Override
    public Response list(String instanceId, String environment, String product, boolean activeOnly)
    {
        return handle(Response.Status.OK,
            account -> this.issuingService.list(account, instanceId, environment, product, activeOnly));
    }

    @Override
    public Response get(String licenseId)
    {
        return handle(Response.Status.OK, account -> this.issuingService.get(account, licenseId));
    }

    private static IssueRequest parse(String body) throws IssuingException
    {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return REQUEST_READER.readValue(body);
        } catch (JsonProcessingException e) {
            throw IssuingErrors.badRequest(IssuingErrors.INVALID_REQUEST,
                String.format("Invalid JSON request: %s", e.getOriginalMessage()));
        }
    }
}
