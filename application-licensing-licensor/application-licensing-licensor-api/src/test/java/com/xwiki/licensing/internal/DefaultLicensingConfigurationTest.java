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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

@ComponentTest
public class DefaultLicensingConfigurationTest
{
    @InjectMockComponents
    private DefaultLicensingConfiguration licensingConfiguration;

    @MockComponent
    @Named("LicensedExtensionAutomaticUpgrades")
    private ConfigurationSource autoUpgradesConfig;

    @MockComponent
    private Environment environment;

    @MockComponent
    private Provider<ConfigurationSource> configurationSourceProvider;

    @Mock
    private ConfigurationSource configurationSource;

    @BeforeEach
    void configure()
    {
        when(this.configurationSourceProvider.get()).thenReturn(this.configurationSource);
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
}
