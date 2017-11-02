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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtensionRepository;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseManager;
import com.xwiki.licensing.internal.DefaultLicenseManager;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultEntityLicenseManager}.
 * 
 * @version $Id$
 * @since 1.6
 */
public class DefaultEntityLicenseManagerTest
{
    @Rule
    public MockitoComponentMockingRule<EntityLicenseManager> mocker =
        new MockitoComponentMockingRule<>(DefaultEntityLicenseManager.class);

    private XarInstalledExtensionRepository xarInstalledExtensionRepository =
        mock(XarInstalledExtensionRepository.class);

    private XarInstalledExtension diagramApplication = mock(XarInstalledExtension.class, "diagram");

    private XarInstalledExtension forumApplication = mock(XarInstalledExtension.class, "forum");

    private XarInstalledExtension blogApplication = mock(XarInstalledExtension.class, "blog");

    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    private Map<ExtensionId, License> extensionToLicense = new HashMap<>();

    @Before
    @SuppressWarnings("unchecked")
    public void configure() throws Exception
    {
        this.mocker.registerComponent(InstalledExtensionRepository.class, XarExtensionHandler.TYPE,
            this.xarInstalledExtensionRepository);

        when(this.diagramApplication.getId()).thenReturn(mock(ExtensionId.class, "diagram"));
        when(this.diagramApplication.getProperty(anyString(), eq(""))).thenReturn("");
        when(this.forumApplication.getId()).thenReturn(mock(ExtensionId.class, "forum"));
        when(this.forumApplication.getProperty(anyString(), eq(""))).thenReturn("");
        when(this.blogApplication.getId()).thenReturn(mock(ExtensionId.class, "blog"));
        when(this.blogApplication.getProperty(anyString(), eq(""))).thenReturn("");

        LicenseManager licenseManager = new DefaultLicenseManager();
        this.extensionToLicense =
            (Map<ExtensionId, License>) FieldUtils.readDeclaredField(licenseManager, "extensionToLicense", true);

        DefaultParameterizedType licenseManagerProviderType =
            new DefaultParameterizedType(null, Provider.class, LicenseManager.class);
        Provider<LicenseManager> licenseManagerProvider = this.mocker.registerMockComponent(licenseManagerProviderType);
        when(licenseManagerProvider.get()).thenReturn(licenseManager);

        this.localEntityReferenceSerializer = this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "local");
    }

    @Test
    public void get() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        AttachmentReference attachmentReference = new AttachmentReference("image.png", documentReference);

        License diagramLicense = mock(License.class, "diagram");
        License blogLicense = mock(License.class, "blog");

        when(this.xarInstalledExtensionRepository.getXarInstalledExtensions(documentReference))
            .thenReturn(Arrays.asList(this.diagramApplication, this.forumApplication, this.blogApplication));

        this.extensionToLicense.put(this.diagramApplication.getId(), diagramLicense);
        this.extensionToLicense.put(this.blogApplication.getId(), blogLicense);

        assertNull(this.mocker.getComponentUnderTest().get(documentReference.getWikiReference()));
        assertNull(this.mocker.getComponentUnderTest().get(documentReference.getLastSpaceReference()));
        assertNull(this.mocker.getComponentUnderTest().get(new DocumentReference("wiki", "Space", "Page")));
        assertSame(diagramLicense, this.mocker.getComponentUnderTest().get(attachmentReference));

        when(this.blogApplication.getProperty("xwiki.extension.licensing.publicDocuments", ""))
            .thenReturn("before, Path.To.Page  ,after");
        when(this.localEntityReferenceSerializer.serialize(documentReference)).thenReturn("Path.To.Page");

        assertSame(DefaultEntityLicenseManager.FREE, this.mocker.getComponentUnderTest().get(attachmentReference));

        when(this.blogApplication.getProperty("xwiki.extension.licensing.excludedDocuments", ""))
            .thenReturn("  Path.To.Page,\nafter");
        // The document is still covered by the license of the Diagram extension.
        assertSame(diagramLicense, this.mocker.getComponentUnderTest().get(attachmentReference));

        when(this.diagramApplication.getProperty("xwiki.extension.licensing.excludedDocuments", ""))
            .thenReturn("before,\nPath.To.Page ");
        // Both extensions exclude the document.
        assertNull(this.mocker.getComponentUnderTest().get(attachmentReference));
    }
}
