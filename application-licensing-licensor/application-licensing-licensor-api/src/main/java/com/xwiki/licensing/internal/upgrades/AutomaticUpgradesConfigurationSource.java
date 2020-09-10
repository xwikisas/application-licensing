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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.internal.AbstractDocumentConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Configuration source for automatic upgrades settings.
 *
 * @since 1.17
 */
@Component(roles = AutomaticUpgradesConfigurationSource.class)
@Singleton
public class AutomaticUpgradesConfigurationSource extends AbstractDocumentConfigurationSource
{
    private static final List<String> SPACE_NAMES = Arrays.asList("Licenses", "Code");

    private static final LocalDocumentReference DOCUMENT_REFERENCE =
        new LocalDocumentReference(SPACE_NAMES, "LicensingConfig");

    private static final LocalDocumentReference CLASS_REFERENCE =
        new LocalDocumentReference(SPACE_NAMES, "AutomaticUpgradesBlocklistClass");

    @Inject
    private Logger logger;

    @Override
    protected DocumentReference getDocumentReference()
    {
        return new DocumentReference(DOCUMENT_REFERENCE, this.getCurrentWikiReference());
    }

    @Override
    protected LocalDocumentReference getClassReference()
    {
        return CLASS_REFERENCE;
    }

    @Override
    protected String getCacheId()
    {
        return "licensing.autoUpgrade";
    }

    /**
     * @return
     */
    public List<String> getUpgradesBlocklist()
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiDocument document;
        List<String> upgradesBlocklist = new ArrayList<String>();
        try {
            document = xcontext.getWiki().getDocument(getDocumentReference(), xcontext);
            BaseObject blocklistObject = document.getXObject(CLASS_REFERENCE);
            // TODO: getListValue returns a List?
            upgradesBlocklist = blocklistObject.getListValue("upgradesBlocklist");
        } catch (XWikiException e) {
            logger.error("Error while getting the upgrades blocklist from configuration document", e);
        }
        return upgradesBlocklist;
    }

}
