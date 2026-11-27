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

import javax.inject.Provider;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.licensing.internal.issuing.IssuingErrors;
import com.xwiki.licensing.manager.IssuingException;
import com.xwiki.licensing.manager.LicenseIssuingService;
import com.xwiki.licensing.manager.rest.model.ErrorBody;
import com.xwiki.licensing.manager.rest.model.IssueRequest;
import com.xwiki.licensing.manager.rest.model.IssueResponse;
import com.xwiki.licensing.manager.rest.model.LicenseKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultLicensesResource}: status, body and error mapping.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultLicensesResourceTest
{
    private static final DocumentReference ACCOUNT = new DocumentReference("xwiki", "XWiki", "partner");

    @InjectMockComponents
    private DefaultLicensesResource resource;

    @MockComponent
    private LicenseIssuingService issuingService;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private final XWikiContext xcontext = mock(XWikiContext.class);

    @BeforeComponent
    void beforeComponent()
    {
        when(this.contextProvider.get()).thenReturn(this.xcontext);
    }

    @BeforeEach
    void setUp()
    {
        when(this.xcontext.getUserReference()).thenReturn(ACCOUNT);
    }

    @Test
    void issue() throws Exception
    {
        IssueResponse response = new IssueResponse();
        when(this.issuingService.issue(eq(ACCOUNT), argThat(request -> "bundle".equals(request.getProduct())
            && "prod".equals(request.getEnvironment()) && request.getCount() == 2
            && Long.valueOf(500).equals(request.getMaxUserCount())
            && "customer".equals(request.getLicenseeMode())
            && "Jane".equals(request.getLicensee().getFirstName())))).thenReturn(response);

        Response result = this.resource.issue("{\"product\": \"bundle\", \"environment\": \"prod\", \"count\": 2, "
            + "\"maxUserCount\": 500, \"licenseeMode\": \"customer\", \"licensee\": {\"firstName\": \"Jane\"}}");

        assertEquals(201, result.getStatus());
        assertSame(response, result.getEntity());
        assertEquals("application/json", result.getMetadata().getFirst("Content-Type").toString());
    }

    @Test
    void issueWithInvalidJson() throws Exception
    {
        assertError(this.resource.issue("{\"product\": "), 400, "invalidRequest");

        // A misspelled field isn't ignored.
        Response result = this.resource.issue("{\"product\": \"bundle\", \"enviroment\": \"prod\"}");
        assertError(result, 400, "invalidRequest");
        assertTrue(((ErrorBody) result.getEntity()).getMessage().contains("enviroment"));

        verify(this.issuingService, never()).issue(any(), any());
    }

    @Test
    void issueWithoutBody() throws Exception
    {
        when(this.issuingService.issue(eq(ACCOUNT), isNull()))
            .thenThrow(IssuingErrors.badRequest("invalidRequest", "The request has no body."));

        assertError(this.resource.issue(" "), 400, "invalidRequest");
        assertError(this.resource.issue(null), 400, "invalidRequest");
    }

    @Test
    void issueRateLimited() throws Exception
    {
        when(this.issuingService.issue(eq(ACCOUNT), any(IssueRequest.class)))
            .thenThrow(IssuingErrors.rateLimited("The daily limit is reached.", 3600));

        Response result = this.resource.issue("{}");

        assertError(result, 429, "rateLimited");
        assertEquals(3600L, result.getMetadata().getFirst("Retry-After"));
        assertEquals("The daily limit is reached.", ((ErrorBody) result.getEntity()).getMessage());
    }

    @Test
    void issueFailure() throws Exception
    {
        when(this.issuingService.issue(eq(ACCOUNT), any(IssueRequest.class)))
            .thenThrow(IssuingErrors.internal("Failed to sign.", new IllegalStateException("cause")));

        Response result = this.resource.issue("{}");

        assertError(result, 500, "internalError");
        assertNull(result.getMetadata().getFirst("Retry-After"));
        assertEquals("License Issuing API request of [xwiki:XWiki.partner] failed: Failed to sign.",
            this.logCapture.getMessage(0));
    }

    @Test
    void unauthenticated() throws Exception
    {
        when(this.xcontext.getUserReference()).thenReturn(null);

        assertError(this.resource.issue("{}"), 401, "unauthenticated");
        assertError(this.resource.list(null, null, null, false), 401, "unauthenticated");
        verify(this.issuingService, never()).issue(any(), any());
    }

    @Test
    void list() throws Exception
    {
        IssueResponse response = new IssueResponse();
        when(this.issuingService.list(ACCOUNT, "instance", "test", "bundle", true)).thenReturn(response);

        Response result = this.resource.list("instance", "test", "bundle", true);

        assertEquals(200, result.getStatus());
        assertSame(response, result.getEntity());
    }

    @Test
    void get() throws Exception
    {
        LicenseKey license = new LicenseKey();
        when(this.issuingService.get(ACCOUNT, "found")).thenReturn(license);
        when(this.issuingService.get(ACCOUNT, "missing")).thenThrow(IssuingErrors.notFound("No license."));

        Response result = this.resource.get("found");
        assertEquals(200, result.getStatus());
        assertSame(license, result.getEntity());

        assertError(this.resource.get("missing"), 404, "notFound");
    }

    static void assertError(Response response, int status, String code)
    {
        assertEquals(status, response.getStatus());
        assertEquals(code, ((ErrorBody) response.getEntity()).getCode());
    }

    static IssuingException forbidden()
    {
        return IssuingErrors.forbidden("noPolicy", "The account has no License Issuing API policy.");
    }
}
