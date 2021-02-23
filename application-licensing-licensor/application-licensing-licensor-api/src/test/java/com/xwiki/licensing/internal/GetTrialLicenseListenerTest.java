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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.internal.installed.DefaultInstalledExtension;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xwiki.licensing.internal.helpers.GetTrialLicenseHandler;

/**
 * Unit tests for {@link GetTrialLicenseListener}.
 * 
 * @version $Id$
 */
public class GetTrialLicenseListenerTest
{
    @Rule
    public MockitoComponentMockingRule<GetTrialLicenseListener> mocker =
        new MockitoComponentMockingRule<>(GetTrialLicenseListener.class);

    GetTrialLicenseHandler getTrialLicenseHandler;

    DefaultInstalledExtension extension;

    URL trialUrl;

    @Before
    public void configure() throws Exception
    {
        this.extension = mock(DefaultInstalledExtension.class);
        ExtensionId extensionId = new ExtensionId("application-test", "1.0");
        when(this.extension.getId()).thenReturn(extensionId);

        this.getTrialLicenseHandler = this.mocker.getInstance(GetTrialLicenseHandler.class);
        when(this.getTrialLicenseHandler.isOwnerDataComplete()).thenReturn(true);
        when(this.getTrialLicenseHandler.isLicensedExtension(extensionId)).thenReturn(true);

        this.trialUrl = new URL("https://url");
        when(this.getTrialLicenseHandler.getTrialURL(extensionId)).thenReturn(trialUrl);
    }

    @Test
    public void onEventWithGetTrialSuccess() throws Exception
    {
        when(this.getTrialLicenseHandler.getURLContent(trialUrl)).thenReturn("success while getting trial license");

        this.mocker.getComponentUnderTest().onEvent(null, extension, null);

        verify(this.getTrialLicenseHandler, times(1)).updateLicenses();
    }

    @Test
    public void onEventWithGetTrialError() throws Exception
    {
        when(this.getTrialLicenseHandler.getURLContent(this.trialUrl)).thenReturn("error while getting trial license");

        this.mocker.getComponentUnderTest().onEvent(null, this.extension, null);

        verify(this.getTrialLicenseHandler, never()).updateLicenses();
    }

    @Test
    public void onEventWithGetTrialException() throws Exception
    {
        when(this.getTrialLicenseHandler.getURLContent(this.trialUrl)).thenThrow(new IOException());

        this.mocker.getComponentUnderTest().onEvent(null, this.extension, null);

        verify(this.getTrialLicenseHandler, never()).updateLicenses();
    }
}
