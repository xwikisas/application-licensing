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

import javax.inject.Inject;
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

}
