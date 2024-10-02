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

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;

import com.xwiki.licensing.LicensingConfiguration;

/**
 * Default implementation of {@link LicensingConfiguration}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultLicensingConfiguration implements LicensingConfiguration
{
    /**
     * The prefix of all the extension related properties.
     */
    private static final String CK_PREFIX = "licensing.";

    /**
     * Used to get permanent directory.
     */
    @Inject
    private Environment environment;

    /**
     * The configuration.
     */
    @Inject
    private Provider<ConfigurationSource> configuration;

    @Inject
    @Named("LicensedExtensionAutomaticUpgrades")
    private ConfigurationSource automaticUpgradesConfig;

    @Inject
    @Named("LicensingStoreConfigurationSource")
    private ConfigurationSource storeConfig;

    @Inject
    @Named("LicensingOwnerConfigurationSource")
    private ConfigurationSource ownerConfig;

    private File localStorePath;

    @Override
    public File getLocalStorePath()
    {
        if (this.localStorePath == null) {
            String storePath = this.configuration.get().getProperty(CK_PREFIX + "localStorePath");

            if (storePath == null) {
                this.localStorePath = new File(this.environment.getPermanentDirectory(), "licenses");
            } else {
                this.localStorePath = new File(storePath);
            }
        }

        return this.localStorePath;
    }

    @Override
    public List<String> getAutoUpgradeAllowList()
    {
        // Since you cannot pass a default value and a target type to getProperty, the class of defaultValue is used
        // for converting the result. In this case there is no converter for EmptyList, so we manage the result
        // manually.
        return convertObjectToStringList(this.automaticUpgradesConfig.getProperty("allowlist"));
    }

    @Override
    public String getStoreTrialURL()
    {
        return this.storeConfig.getProperty("storeTrialURL");
    }

    @Override
    public String getStoreUpdateURL()
    {
        return this.storeConfig.getProperty("storeUpdateURL");
    }

    @Override
    public String getLicensingOwnerFirstName()
    {
        return this.ownerConfig.getProperty("firstName");
    }

    @Override
    public String getLicensingOwnerLastName()
    {
        return this.ownerConfig.getProperty("lastName");
    }

    @Override
    public String getLicensingOwnerEmail()
    {
        return this.ownerConfig.getProperty("email");
    }

    @Override
    public String getStoreLicenseRenewURL()
    {
        return this.storeConfig.getProperty("storeLicenseRenewURL");
    }

    @SuppressWarnings("unchecked")
    private List<String> convertObjectToStringList(Object list)
    {
        if (list instanceof List) {
            return ((List<Object>) list).stream().map(item -> Objects.toString(item, null))
                .collect(Collectors.toList());
        } else if (list == null) {
            return Collections.emptyList();
        } else {
            throw new RuntimeException(String.format("Cannot convert [%s] to List", list));
        }
    }
}
