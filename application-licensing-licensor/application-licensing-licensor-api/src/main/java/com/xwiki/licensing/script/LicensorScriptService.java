package com.xwiki.licensing.script;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.avalon.framework.activity.Initializable;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.script.service.ScriptService;

import com.xwiki.licensing.Licensor;
import com.xwiki.licensing.internal.enforcer.LicensingUtils;

/**
 * Script service for the licensor.
 *
 * @version $Id$
 */
@Component
@Singleton
public class LicensorScriptService implements ScriptService, Initializable
{
    @Inject
    private Licensor licensor;

    @Override
    public void initialize() throws InitializationException
    {
        if (!LicensingUtils.isPristineImpl(licensor)) {
            throw new InitializationException("Integrity check failed while loading the licensor.");
        }
    }

    public Licensor getLicensor()
    {
        return licensor;
    }

    public void checkLicense()
    {
        if (!licensor.hasLicensure()) {
            // TODO: redirect to a buy license stuff
        }
    }
}
