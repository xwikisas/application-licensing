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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

/**
 * Expose licensing related configurations.
 *
 * @since 1.17
 * @version $Id$
 */
@Component(roles = AutomaticUpgradesConfiguration.class)
@Singleton
public class AutomaticUpgradesConfiguration
{
    @Inject
    @Named("LicensedExtensionAutomaticUpgrades")
    private ConfigurationSource automaticUpgradesConfig;

    /**
     * Get a list of extensions that should not be upgraded automatically.
     *
     * @return the list of blocklisted extensions for upgrade
     */
    @SuppressWarnings("unchecked")
    public List<String> getBlocklist()
    {
        // Since you cannot pass a default value and a target type to getProperty, the class of defaultValue is used
        // for converting the result. In this case there is no converter for EmptyList, so we manage the result
        // manually.
        Object blocklist = this.automaticUpgradesConfig.getProperty("blocklist");
        if (blocklist instanceof List) {
            return ((List<Object>) blocklist).stream().map(item -> Objects.toString(item, null))
                .collect(Collectors.toList());
        } else if (blocklist == null) {
            return Collections.emptyList();
        } else {
            throw new RuntimeException(String.format("Cannot convert [%s] to List", blocklist));
        }
    }

}
