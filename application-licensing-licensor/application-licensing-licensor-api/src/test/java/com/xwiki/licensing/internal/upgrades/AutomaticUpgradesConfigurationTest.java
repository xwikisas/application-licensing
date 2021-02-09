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
package com.xwiki.licensing.internal.upgrades;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Unit tests for {@link AutomaticUpgradesConfiguration}.
 *
 * @version $Id$
 * @since 1.17
 */
public class AutomaticUpgradesConfigurationTest
{
    @Rule
    public MockitoComponentMockingRule<AutomaticUpgradesConfiguration> mocker =
        new MockitoComponentMockingRule<>(AutomaticUpgradesConfiguration.class);

    private ConfigurationSource autoUpgradesConfig;

    @Before
    public void configure() throws Exception
    {
        this.autoUpgradesConfig =
            this.mocker.getInstance(ConfigurationSource.class, "LicensedExtensionAutomaticUpgrades");
    }

    @Test
    public void getBlocklist() throws Exception
    {
        // Since getProperty method returns a list of objects, we check also that the conversion to string is done
        // correctly.
        List<Object> blocklist = Arrays.asList(1, 2, null);

        when(this.autoUpgradesConfig.getProperty("blocklist")).thenReturn(blocklist);

        assertEquals(Arrays.asList("1", "2", null), mocker.getComponentUnderTest().getBlocklist());
    }

    @Test(expected = RuntimeException.class)
    public void getBlocklistWithException() throws Exception
    {
        when(this.autoUpgradesConfig.getProperty("blocklist")).thenReturn("null");
        mocker.getComponentUnderTest().getBlocklist();
    }

    @Test
    public void getBlocklistWithEmptyList() throws Exception
    {
        when(this.autoUpgradesConfig.getProperty("blocklist")).thenReturn(null);

        assertEquals(Collections.emptyList(), mocker.getComponentUnderTest().getBlocklist());
    }
}
