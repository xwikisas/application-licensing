package com.xwiki.licensing;

import java.io.File;

import org.xwiki.component.annotation.Role;

/**
 * Configuration of the licensing module.
 *
 * @version $Id$
 */
@Role
public interface LicensingConfiguration
{
    /**
     * @return the configured path where to store licenses.
     */
    File getLocalStorePath();
}
