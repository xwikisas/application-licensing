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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.licensing.LicensingConfiguration;
import com.xwiki.licensing.internal.upgrades.AutomaticUpgradesConfigurationSource;

/**
 * Default implementation of {@link LicensingConfiguration}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultLicensingConfiguration implements LicensingConfiguration
{
    private static final String NEW_VERSION_NOTIFIED_EXTENSIONS = "newVersionNotifiedExtensions";

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
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

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
    public List<String> getNewVersionNotifiedExtensions()
    {
        // Since you cannot pass a default value and a target type to getProperty, the class of defaultValue is used
        // for converting the result. In this case there is no converter for EmptyList, so we manage the result
        // manually.
        return convertObjectToStringList(this.automaticUpgradesConfig.getProperty(NEW_VERSION_NOTIFIED_EXTENSIONS));
    }

    @Override
    public void setNewVersionNotifiedExtensions(List<String> value)
    {
        // Use {@link org.xwiki.configuration.internal.AbstractDocumentConfigurationSource#setProperties(Map<String,
        // Object>)} once Licensing starts depending on a XWiki version >= 12.4, to include the fix from XCOMMONS-1934:
        // Add ability to modify configuration source properties.
        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();
        try {
            XWikiDocument licensingConfigDoc =
                xwiki.getDocument(AutomaticUpgradesConfigurationSource.LICENSING_CONFIG_DOC, context);
            licensingConfigDoc.getXObject(AutomaticUpgradesConfigurationSource.AUTO_UPGRADES_CLASS)
                .set(NEW_VERSION_NOTIFIED_EXTENSIONS, String.join(",", value), context);
            xwiki.saveDocument(licensingConfigDoc,
                "Updated the list of new available extensions versions that users have been notified of.", context);
        } catch (XWikiException e) {
            logger.warn("Failed to save the list of new available extensions versions. Root cause is [{}]",
                ExceptionUtils.getRootCauseMessage(e));
        }
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
