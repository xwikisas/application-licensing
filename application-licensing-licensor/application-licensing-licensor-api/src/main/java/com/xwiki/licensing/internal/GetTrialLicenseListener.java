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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.plexus.util.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.repository.internal.installed.DefaultInstalledExtension;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xwiki.licensing.internal.helpers.GetTrialLicenseHandler;

/**
 * Generate trial license for paid extensions at install step if the data necessary for it is already filled up.
 *
 * @since 1.17
 * @version $Id$
 */
@Component
@Named(GetTrialLicenseListener.NAME)
@Singleton
public class GetTrialLicenseListener implements EventListener
{
    protected static final String NAME = "GetTrialLicenseListener";

    protected static final List<Event> EVENTS = Arrays.asList(new ExtensionInstalledEvent());

    @Inject
    private Logger logger;

    @Inject
    private GetTrialLicenseHandler getTrialLicenseHandler;

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        DefaultInstalledExtension extension = (DefaultInstalledExtension) source;

        try {
            if (!getTrialLicenseHandler.isOwnerDataIncomplete()
                && getTrialLicenseHandler.isLicensedExtension(extension.getId())) {
                String getTrialResponse =
                    getTrialLicenseHandler.getURLContent(getTrialLicenseHandler.getTrialURL(extension.getId()));

                if (getTrialResponse.contains("error")) {
                    logger.info("Failed to add trial license");
                } else {
                    logger.info("Added trial license");
                    getTrialLicenseHandler.updateLicenses();
                }
            }
        } catch (Exception e) {
            logger.info("Failed to get trial license for [{}]. Root cause is [{}]", extension.getId(),
                ExceptionUtils.getRootCause(e));
        }
    }
}
