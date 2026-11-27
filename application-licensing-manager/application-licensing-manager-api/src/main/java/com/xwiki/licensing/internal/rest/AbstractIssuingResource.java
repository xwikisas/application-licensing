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

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.XWikiResource;

import com.xwiki.licensing.internal.issuing.IssuingErrors;
import com.xwiki.licensing.manager.IssuingException;
import com.xwiki.licensing.manager.LicenseIssuingService;
import com.xwiki.licensing.manager.rest.model.ErrorBody;

/**
 * Common code of the License Issuing API resources: the calling account and the error mapping (spec 4.5).
 *
 * @version $Id$
 * @since 1.34
 */
public abstract class AbstractIssuingResource extends XWikiResource
{
    private static final int UNAUTHORIZED = 401;

    @Inject
    protected LicenseIssuingService issuingService;

    /**
     * A call of the issuing service for the calling account.
     */
    @FunctionalInterface
    protected interface IssuingCall
    {
        /**
         * @param account the calling account
         * @return the response entity
         * @throws IssuingException if the call fails
         */
        Object call(DocumentReference account) throws IssuingException;
    }

    /**
     * @param status the status of a successful call
     * @param call the call
     * @return the response: the entity of the call, or the error
     */
    protected Response handle(Response.Status status, IssuingCall call)
    {
        DocumentReference account = getXWikiContext().getUserReference();
        if (account == null) {
            return error(new IssuingException(UNAUTHORIZED, IssuingErrors.UNAUTHENTICATED, "Authentication required."));
        }
        try {
            return Response.status(status).type(MediaType.APPLICATION_JSON_TYPE).entity(call.call(account)).build();
        } catch (IssuingException e) {
            if (e.getCause() != null) {
                getLogger().error("License Issuing API request of [{}] failed: {}", account, e.getMessage(),
                    e.getCause());
            }
            return error(e);
        }
    }

    private static Response error(IssuingException exception)
    {
        ErrorBody body = new ErrorBody();
        body.setCode(exception.getCode());
        body.setMessage(exception.getMessage());
        Response.ResponseBuilder response =
            Response.status(exception.getStatus()).type(MediaType.APPLICATION_JSON_TYPE).entity(body);
        if (exception.getRetryAfter() != null) {
            response.header("Retry-After", exception.getRetryAfter());
        }
        return response.build();
    }
}
