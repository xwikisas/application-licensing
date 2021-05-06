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

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.job.event.JobFinishedEvent;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

/**
 * Generate a trial license for paid extensions after install or upgrade, if the data necessary for it is already filled
 * up and there isn't already an existing license.
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

    protected static final List<Event> EVENTS = Arrays.asList(new JobFinishedEvent("install"));

    @Inject
    private TrialLicenseGenerator trialLicenseGenerator;

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
        List<ExtensionId> extensions = ((JobFinishedEvent) event).getRequest().getProperty("extensions");
        // Retrieve license updates to be sure that it doesn't override an existing license.
        trialLicenseGenerator.updateLicenses();

        for (ExtensionId extensionId : extensions) {
            if (trialLicenseGenerator.canGenerateTrialLicense(extensionId)) {
                trialLicenseGenerator.generateTrialLicense(extensionId);
            }
        }
    }
}
