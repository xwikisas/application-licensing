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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xwiki.licensing.LicensingConfiguration;

public class DefaultLicensingConfigurationTest
{
    @Rule
    public MockitoComponentMockingRule<LicensingConfiguration> mocker =
        new MockitoComponentMockingRule<>(DefaultLicensingConfiguration.class);

    private ConfigurationSource autoUpgradesConfig;

    private ConfigurationSource configurationSource;

    private Environment environment;

    @Before
    public void configure() throws Exception
    {
        this.autoUpgradesConfig =
            this.mocker.getInstance(ConfigurationSource.class, "LicensedExtensionAutomaticUpgrades");

        DefaultParameterizedType configurationSourceProviderType =
            new DefaultParameterizedType(null, Provider.class, ConfigurationSource.class);
        Provider<ConfigurationSource> configurationSourceProvider =
            this.mocker.registerMockComponent(configurationSourceProviderType);

        configurationSource = mock(ConfigurationSource.class);
        when(configurationSourceProvider.get()).thenReturn(configurationSource);

        environment = this.mocker.getInstance(Environment.class);
    }

    @Test
    public void getAllowlist() throws Exception
    {
        // Since getProperty method returns a list of objects, we check also that the conversion to string is done
        // correctly.
        List<Object> allowlist = Arrays.asList(1, 2, null);

        when(this.autoUpgradesConfig.getProperty("allowlist")).thenReturn(allowlist);

        assertEquals(Arrays.asList("1", "2", null), mocker.getComponentUnderTest().getAutoUpgradeAllowlist());
    }

    @Test(expected = RuntimeException.class)
    public void getAllowlistWithException() throws Exception
    {
        when(this.autoUpgradesConfig.getProperty("allowlist")).thenReturn("not a list");
        mocker.getComponentUnderTest().getAutoUpgradeAllowlist();
    }

    @Test
    public void getAllowlistWithEmptyList() throws Exception
    {
        when(this.autoUpgradesConfig.getProperty("allowlist")).thenReturn(null);

        assertEquals(Collections.emptyList(), mocker.getComponentUnderTest().getAutoUpgradeAllowlist());
    }

    @Test
    public void getLocalStorePath() throws Exception
    {
        when(this.configurationSource.getProperty("licensing.localStorePath")).thenReturn("storePath");
        File storeFile = new File("storePath");

        assertEquals(storeFile, this.mocker.getComponentUnderTest().getLocalStorePath());
    }

    @Test
    public void getLocalStorePathWithNullProperty() throws Exception
    {
        when(this.configurationSource.getProperty("licensing.localStorePath")).thenReturn(null);

        File permanentDirectoryFile = new File("permanentDirectoryPath");
        File storeFile = new File(permanentDirectoryFile, "licenses");
        when(this.environment.getPermanentDirectory()).thenReturn(permanentDirectoryFile);

        assertEquals(storeFile, this.mocker.getComponentUnderTest().getLocalStorePath());
    }
}
