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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.licensing.internal.upgrades.AutomaticUpgradesConfigurationSource;

@ComponentTest
class DefaultLicensingConfigurationTest
{
    @InjectMockComponents
    private DefaultLicensingConfiguration licensingConfiguration;

    @MockComponent
    @Named("LicensedExtensionAutomaticUpgrades")
    private ConfigurationSource autoUpgradesConfig;

    @MockComponent
    private Environment environment;

    @MockComponent
    private Logger logger;

    @MockComponent
    private Provider<ConfigurationSource> configurationSourceProvider;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @Mock
    private ConfigurationSource configurationSource;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiDocument licensingDoc;

    @Mock
    private BaseObject autoUpgradesObject;

    @BeforeEach
    void configure() throws Exception
    {
        when(this.configurationSourceProvider.get()).thenReturn(this.configurationSource);

        when(this.xcontextProvider.get()).thenReturn(xcontext);
        when(this.xcontext.getWiki()).thenReturn(xwiki);
        when(this.xwiki.getDocument(AutomaticUpgradesConfigurationSource.LICENSING_CONFIG_DOC, xcontext))
            .thenReturn(licensingDoc);
        when(this.licensingDoc.getXObject(AutomaticUpgradesConfigurationSource.AUTO_UPGRADES_CLASS))
            .thenReturn(autoUpgradesObject);
    }

    @Test
    void getAllowList() throws Exception
    {
        // Since getProperty method returns a list of objects, we check also that the conversion to string is done
        // correctly.
        List<Object> allowlist = Arrays.asList(1, 2, null);

        when(this.autoUpgradesConfig.getProperty("allowlist")).thenReturn(allowlist);

        assertEquals(Arrays.asList("1", "2", null), this.licensingConfiguration.getAutoUpgradeAllowList());
    }

    @Test
    void getAllowListWithException() throws Exception
    {
        try {
            when(this.autoUpgradesConfig.getProperty("allowlist")).thenReturn("not a list");
            this.licensingConfiguration.getAutoUpgradeAllowList();
            fail("Should have thrown an exception.");
        } catch (RuntimeException expected) {
            assertEquals("Cannot convert [not a list] to List", expected.getMessage());
        }
    }

    @Test
    void getAllowListWithEmptyList() throws Exception
    {
        when(this.autoUpgradesConfig.getProperty("allowlist")).thenReturn(null);

        assertEquals(Collections.emptyList(), this.licensingConfiguration.getAutoUpgradeAllowList());
    }

    @Test
    void getLocalStorePath() throws Exception
    {
        when(this.configurationSource.getProperty("licensing.localStorePath")).thenReturn("storePath");
        File storeFile = new File("storePath");

        assertEquals(storeFile, this.licensingConfiguration.getLocalStorePath());
    }

    @Test
    void getLocalStorePathWithNullProperty() throws Exception
    {
        when(this.configurationSource.getProperty("licensing.localStorePath")).thenReturn(null);

        File permanentDirectoryFile = new File("permanentDirectoryPath");
        File storeFile = new File(permanentDirectoryFile, "licenses");
        when(this.environment.getPermanentDirectory()).thenReturn(permanentDirectoryFile);

        assertEquals(storeFile, this.licensingConfiguration.getLocalStorePath());
    }

    @Test
    void getNewVersionNotifiedExtensions() throws Exception
    {
        // Since getProperty method returns a list of objects, we check also that the conversion to string is done
        // correctly.
        List<Object> newVersionNotifiedExtensions = Arrays.asList(1, 2, null);

        when(this.autoUpgradesConfig.getProperty("newVersionNotifiedExtensions"))
            .thenReturn(newVersionNotifiedExtensions);

        assertEquals(Arrays.asList("1", "2", null), this.licensingConfiguration.getNewVersionNotifiedExtensions());
    }
    
    @Test
    void getNewVersionNotifiedExtensionsWithException() throws Exception
    {
        try {
            when(this.autoUpgradesConfig.getProperty("newVersionNotifiedExtensions")).thenReturn("not a list");
            this.licensingConfiguration.getNewVersionNotifiedExtensions();
            fail("Should have thrown an exception.");
        } catch (RuntimeException expected) {
            assertEquals("Cannot convert [not a list] to List", expected.getMessage());
        }
    }

    @Test
    void getNewVersionNotifiedExtensionsWithEmptyList() throws Exception
    {
        when(this.autoUpgradesConfig.getProperty("newVersionNotifiedExtensions")).thenReturn(null);

        assertEquals(Collections.emptyList(), this.licensingConfiguration.getNewVersionNotifiedExtensions());
    }

    @Test
    void setNewVersionNotifiedExtensions() throws Exception
    {
        ArrayList<String> listValues = new ArrayList<String>(Arrays.asList("extensionId-wiki:test-1.1"));

        this.licensingConfiguration.setNewVersionNotifiedExtensions(listValues);

        verify(this.logger, never()).warn(any(String.class));
    }
}
