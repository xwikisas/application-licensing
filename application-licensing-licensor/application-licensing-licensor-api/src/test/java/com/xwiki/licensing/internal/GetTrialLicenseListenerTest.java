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

import java.net.URL;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.internal.installed.DefaultInstalledExtension;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

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

    TrialLicenseGenerator trialLicenseGenerator;

    DefaultInstalledExtension extension;

    ExtensionId extensionId;

    URL trialUrl;

    @Before
    public void configure() throws Exception
    {
        this.extension = mock(DefaultInstalledExtension.class);
        this.extensionId = new ExtensionId("application-test", "1.0");
        when(this.extension.getId()).thenReturn(extensionId);

        this.trialLicenseGenerator = this.mocker.getInstance(TrialLicenseGenerator.class);
    }

    @Test
    public void onEventWithCompleteData() throws Exception
    {
        when(this.trialLicenseGenerator.canGenerateTrialLicense(this.extensionId)).thenReturn(true);

        this.mocker.getComponentUnderTest().onEvent(null, extension, null);

        verify(this.trialLicenseGenerator, times(1)).generateTrialLicense(this.extensionId);
    }

    @Test
    public void onEventWithoutCompleteData() throws Exception
    {
        when(this.trialLicenseGenerator.canGenerateTrialLicense(this.extensionId)).thenReturn(false);

        this.mocker.getComponentUnderTest().onEvent(null, this.extension, null);

        verify(this.trialLicenseGenerator, never()).generateTrialLicense(this.extensionId);
    }
}
