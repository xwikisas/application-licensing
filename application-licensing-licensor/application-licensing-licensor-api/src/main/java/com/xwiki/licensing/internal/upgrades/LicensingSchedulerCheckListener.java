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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.quartz.SchedulerException;
import org.quartz.Trigger.TriggerState;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.extension.repository.internal.installed.DefaultInstalledExtension;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.scheduler.JobState;
import com.xpn.xwiki.plugin.scheduler.SchedulerPlugin;
import com.xpn.xwiki.plugin.scheduler.SchedulerPluginException;

/**
 * Assures that AutomaticDependenciesUpgrade job is scheduled after licensing install. Also, reschedules it
 * after an licensing upgrade to avoid possible errors.
 * 
 * @since 1.17
 */
@Component
@Named(LicensingSchedulerCheckListener.ROLE_HINT)
@Singleton
public class LicensingSchedulerCheckListener extends AbstractEventListener
{
    public static final String ROLE_HINT = "LicensingSchedulerCheckListener";

    private static final String LICENSOR_API_ID = "com.xwiki.licensing:application-licensing-licensor-api";

    private static final List<Event> EVENTS =
        Arrays.asList(new ExtensionInstalledEvent(), new ExtensionUpgradedEvent(LICENSOR_API_ID));

    private static final LocalDocumentReference DOC =
        new LocalDocumentReference(Arrays.asList("Licenses", "Code"), "AutomaticDependenciesUpgrade");

    @Inject
    private Provider<XWikiContext> contextProvider;

    public LicensingSchedulerCheckListener()
    {
        super(ROLE_HINT, EVENTS);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        String extensionId = ((DefaultInstalledExtension) source).getId().getId();

        if (extensionId.equals(LICENSOR_API_ID)) {
            try {
                XWikiContext xcontext = contextProvider.get();
                SchedulerPlugin scheduler =
                    (SchedulerPlugin) xcontext.getWiki().getPluginManager().getPlugin("scheduler");
                XWikiDocument doc = xcontext.getWiki().getDocument(DOC, xcontext);
                BaseObject job = doc.getXObject(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE);

                if (event instanceof ExtensionInstalledEvent) {
                    scheduleAtInstall(scheduler, job, xcontext);
                } else if (event instanceof ExtensionUpgradedEvent) {
                    scheduleAtUpgrade(scheduler, job, xcontext);
                }
            } catch (XWikiException e) {
                e.printStackTrace();
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
        }
    }

    private void scheduleAtInstall(SchedulerPlugin scheduler, BaseObject job, XWikiContext xcontext)
        throws SchedulerException, SchedulerPluginException
    {
        JobState jobState = scheduler.getJobStatus(job, xcontext);
        if (jobState.getQuartzState().equals(TriggerState.NONE)) {
            scheduler.scheduleJob(job, xcontext);
        }
    }

    private void scheduleAtUpgrade(SchedulerPlugin scheduler, BaseObject job, XWikiContext xcontext)
        throws SchedulerException, SchedulerPluginException
    {
        JobState jobState = scheduler.getJobStatus(job, xcontext);
        if (jobState.getQuartzState().equals(TriggerState.NORMAL)) {
            scheduler.unscheduleJob(job, xcontext);
            scheduler.scheduleJob(job, xcontext);
        }
    }

}
