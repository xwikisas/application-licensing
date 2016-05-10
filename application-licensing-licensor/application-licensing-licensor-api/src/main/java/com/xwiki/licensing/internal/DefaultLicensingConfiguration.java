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
 * Default implementation of {@link LicensingConfiguration}
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
            String localStorePath = this.configuration.get().getProperty(CK_PREFIX + "localStorePath");

            if (localStorePath == null) {
                this.localStorePath = new File(this.environment.getPermanentDirectory(), "licenses/");
            } else {
                this.localStorePath = new File(localStorePath);
            }
        }

        return this.localStorePath;
    }

}
