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

import java.util.Date;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseValidator;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultLicenseValidator}.
 * 
 * @version $Id$
 * @since 1.6
 */
public class DefaultLicenseValidatorTest
{
    @Rule
    public MockitoComponentMockingRule<LicenseValidator> mocker =
        new MockitoComponentMockingRule<>(DefaultLicenseValidator.class);

    @Test
    public void isValid() throws Exception
    {
        License license = new License();
        assertTrue(this.mocker.getComponentUnderTest().isValid(license));

        license.setExpirationDate(new Date().getTime() - 1000);
        assertFalse(this.mocker.getComponentUnderTest().isValid(license));

        license.setExpirationDate(new Date().getTime() + 1000);
        assertTrue(this.mocker.getComponentUnderTest().isValid(license));

        UserCounter userCounter = this.mocker.getInstance(UserCounter.class);
        when(userCounter.getUserCount()).thenReturn(12L);
        license.setMaxUserCount(10);
        assertFalse(this.mocker.getComponentUnderTest().isValid(license));

        license.setMaxUserCount(-1);
        assertTrue(this.mocker.getComponentUnderTest().isValid(license));

        when(userCounter.getUserCount()).thenThrow(new Exception());
        assertTrue(this.mocker.getComponentUnderTest().isValid(license));

        license.setMaxUserCount(Long.MAX_VALUE);
        assertFalse(this.mocker.getComponentUnderTest().isValid(license));
    }
}
