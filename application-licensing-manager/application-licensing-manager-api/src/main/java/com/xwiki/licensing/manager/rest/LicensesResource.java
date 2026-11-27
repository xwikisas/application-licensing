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
package com.xwiki.licensing.manager.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.xwiki.rest.XWikiRestComponent;
import org.xwiki.stability.Unstable;

/**
 * Issues, renews and lists the licenses of the calling account (License Issuing API). Errors are returned as
 * {@link com.xwiki.licensing.manager.rest.model.ErrorBody}.
 *
 * @version $Id$
 * @since 1.34
 */
@Unstable
@Path("/licensing/licenses")
public interface LicensesResource extends XWikiRestComponent
{
    /**
     * Issues licenses for new instances, or renews the licenses of an instance owned by the account.
     *
     * @param body the JSON {@link com.xwiki.licensing.manager.rest.model.IssueRequest}; only JSON is accepted, so that
     *     a form posted from another site can't issue licenses
     * @return 201 with the {@link com.xwiki.licensing.manager.rest.model.IssueResponse}, or an error (400, 401, 403,
     *     409, 429 with {@code Retry-After}, 500)
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response issue(String body);

    /**
     * Lists the licenses of the account's instances.
     *
     * @param instanceId only the licenses of this instance
     * @param environment only the instances of this environment ({@code prod} or {@code test})
     * @param product only the instances of this product
     * @param activeOnly whether to leave out the expired licenses
     * @return 200 with the {@link com.xwiki.licensing.manager.rest.model.IssueResponse}, or an error
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Response list(@QueryParam("instanceId") String instanceId, @QueryParam("environment") String environment,
        @QueryParam("product") String product, @QueryParam("activeOnly") @DefaultValue("false") boolean activeOnly);

    /**
     * Returns one license of the account's instances.
     *
     * @param licenseId the license id
     * @return 200 with the {@link com.xwiki.licensing.manager.rest.model.LicenseKey}, 404 if the license doesn't belong
     *     to one of the account's instances, or another error
     */
    @GET
    @Path("/{licenseId}")
    @Produces(MediaType.APPLICATION_JSON)
    Response get(@PathParam("licenseId") String licenseId);
}
