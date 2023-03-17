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
package com.xwiki.licensing.test.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.xwiki.test.docker.junit5.UITest;

/**
 * All UI tests for the Licensing application.
 *
 * @version $Id$
 */
@UITest(
    properties = {
        // Need to see the instance id in the License UI.
        "xwikiDbHbmCommonExtraMappings=instance.hbm.xml,eventstream.hbm.xml,notification-filter-preferences.hbm.xml",

        // Disable the PR checker for the Licensing tests. There are pages that require programming rights, but the
        // Licensing application will be installed all the time on root, so there shouldn't be any issues.
        "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:Licenses?\\..*",

        // Required by the License Manager application to compute the license expiration date.
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.jodatime.JodaTimePlugin"
    },
    extraJARs = {
        // Required for the instance.hbm.xml file.
        "org.xwiki.platform:xwiki-platform-instance:13.10",

        // Required for the eventstream.hbm.xml file.
        "org.xwiki.platform:xwiki-platform-eventstream-store-hibernate:13.10",
        "org.xwiki.platform:xwiki-platform-eventstream-store-solr:13.10",
        "org.xwiki.platform:xwiki-platform-eventstream-store-hibernate:13.10",

        // Required for the notification-filter-preferences.hbm.xml file.
        "org.xwiki.platform:xwiki-platform-notifications-filters-default:13.10",

        // The component manager fails to load WikiMacroEventListener when wikimacro-api and wikimacro-store are
        // installed at runtime. We force them as core extensions.
        "org.xwiki.platform:xwiki-platform-rendering-wikimacro-store:13.10",

        // Required by the Crypto Store script service, which is a core extension and injects the filesystem and wiki
        // implementations directly (so the root component manager is used).
        "org.xwiki.commons:xwiki-commons-crypto-store-filesystem:13.10",
        "org.xwiki.platform:xwiki-platform-crypto-store-wiki:13.10",

        // The JodaTime plugin needs to be in WEB-INF/lib since it's defined in xwiki.cfg and plugins are loaded by
        // XWiki at startup, i.e. before extensions are provisioned for the tests.
        "org.xwiki.platform:xwiki-platform-jodatime:13.10"
    } , resolveExtraJARs = true
)
class AllITs
{
    @Nested
    @DisplayName("Overall Licensing UI")
    class NestedLicensingIT extends LicensingIT
    {
    }
}
