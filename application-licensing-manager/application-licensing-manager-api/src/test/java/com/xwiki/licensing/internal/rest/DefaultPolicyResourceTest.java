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

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.licensing.manager.LicenseIssuingService;
import com.xwiki.licensing.manager.rest.model.PolicyView;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultPolicyResource}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultPolicyResourceTest
{
    private static final DocumentReference ACCOUNT = new DocumentReference("xwiki", "XWiki", "partner");

    @InjectMockComponents
    private DefaultPolicyResource resource;

    @MockComponent
    private LicenseIssuingService issuingService;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    private final XWikiContext xcontext = mock(XWikiContext.class);

    @BeforeComponent
    void beforeComponent()
    {
        when(this.contextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getUserReference()).thenReturn(ACCOUNT);
    }

    @Test
    void getPolicy() throws Exception
    {
        PolicyView policy = new PolicyView();
        when(this.issuingService.getPolicy(ACCOUNT)).thenReturn(policy);

        Response response = this.resource.getPolicy();

        assertEquals(200, response.getStatus());
        assertSame(policy, response.getEntity());
    }

    @Test
    void getPolicyWithoutPolicy() throws Exception
    {
        when(this.issuingService.getPolicy(ACCOUNT)).thenThrow(DefaultLicensesResourceTest.forbidden());

        DefaultLicensesResourceTest.assertError(this.resource.getPolicy(), 403, "noPolicy");
    }
}
