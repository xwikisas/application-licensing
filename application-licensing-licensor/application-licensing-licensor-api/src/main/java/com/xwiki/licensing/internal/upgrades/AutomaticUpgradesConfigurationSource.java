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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.internal.AbstractDocumentConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

/**
 * Configuration source for automatic upgrades settings.
 *
 * @since 1.17
 * @version $Id$
 */
@Component
@Named("LicensedExtensionAutomaticUpgrades")
@Singleton
public class AutomaticUpgradesConfigurationSource extends AbstractDocumentConfigurationSource
{
    private static final List<String> CODE_SPACE = Arrays.asList("Licenses", "Code");

    /**
     * Reference of the document containing licensing configurations.
     */
    protected static final LocalDocumentReference LICENSING_CONFIG_DOC =
        new LocalDocumentReference(CODE_SPACE, "LicensingConfig");

    /**
     * Reference of the class that contains configurations related to automatic upgrades.
     */
    protected static final LocalDocumentReference AUTO_UPGRADES_CLASS =
        new LocalDocumentReference(CODE_SPACE, "AutomaticUpgradesClass");

    @Override
    protected DocumentReference getDocumentReference()
    {
        return new DocumentReference(LICENSING_CONFIG_DOC, this.getCurrentWikiReference());
    }

    @Override
    protected LocalDocumentReference getClassReference()
    {
        return AUTO_UPGRADES_CLASS;
    }

    @Override
    protected String getCacheId()
    {
        return "licensing.autoUpgrade";
    }
}
