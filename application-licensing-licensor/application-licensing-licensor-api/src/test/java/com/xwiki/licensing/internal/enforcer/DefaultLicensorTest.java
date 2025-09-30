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
package com.xwiki.licensing.internal.enforcer;

import java.util.Date;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.extension.ExtensionId;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseManager;
import com.xwiki.licensing.LicenseValidator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultLicensor}.
 *
 * @version $Id$
 * @since 1.29.1
 */
@ComponentTest
class DefaultLicensorTest
{
    @InjectMockComponents
    DefaultLicensor defaultLicensor;

    @MockComponent
    LicenseManager licenseManager;

    @MockComponent
    EntityLicenseManager entityLicenseManager;

    @MockComponent
    LicenseValidator licenseValidator;

    @MockComponent
    Provider<XWikiContext> contextProvider;

    @Mock
    XWikiContext wikiContext;

    @Mock
    XWikiDocument wikiDocument;

    @Mock
    DocumentReference docRef;

    @Mock
    ExtensionId extensionId;

    @MockComponent
    License license;

    @Mock
    License invalidLicense;

    @BeforeEach
    void configure()
    {
        when(contextProvider.get()).thenReturn(wikiContext);
        when(wikiContext.getDoc()).thenReturn(wikiDocument);
        when(wikiDocument.getDocumentReference()).thenReturn(docRef);
        when(entityLicenseManager.get(docRef)).thenReturn(license);
        when(licenseValidator.isValid(eq(license), any())).thenReturn(true);
        when(licenseManager.get(extensionId)).thenReturn(license);
        when(licenseValidator.isValid(invalidLicense)).thenReturn(false);
    }

    @Test
    void hasLicensureNotFound()
    {
        when(licenseManager.get(extensionId)).thenReturn(null);
        assertTrue(defaultLicensor.hasLicensure(extensionId));
    }

    @Test
    void hasLicensure()
    {
        assertTrue(defaultLicensor.hasLicensure());
    }

    @Test
    void hasLicensureWithEntity()
    {
        assertTrue(defaultLicensor.hasLicensure(docRef));
    }

    @Test
    void hasLicensureWithExtensionId()
    {
        when(licenseManager.get(extensionId)).thenReturn(invalidLicense);
        assertFalse(defaultLicensor.hasLicensure(extensionId));
    }

    @Test
    void isLicenseExpiringAlreadyExpired()
    {
        when(license.getExpirationDate()).thenReturn(new Date().getTime() - 24 * 60 * 60 * 1000L);
        assertFalse(defaultLicensor.isLicenseExpiring(extensionId));
    }

    @Test
    void isLicenseExpiring()
    {

        when(license.getExpirationDate()).thenReturn(new Date().getTime() + 24 * 60 * 60 * 1000L);
        assertTrue(defaultLicensor.isLicenseExpiring(extensionId));
    }

    @Test
    void isLicenseExpiringNotInThreshold()
    {
        when(license.getExpirationDate()).thenReturn(new Date().getTime() + 15 * 24 * 60 * 60 * 1000L);
        assertFalse(defaultLicensor.isLicenseExpiring(extensionId));
    }

    @Test
    void isLicenseExpiringNotFound()
    {
        when(entityLicenseManager.get(docRef)).thenReturn(null);
        assertFalse(defaultLicensor.isLicenseExpiring(extensionId));
    }
}
