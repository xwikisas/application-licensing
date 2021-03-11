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
package com.xwiki.licensing.internal.helpers;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.internal.AbstractDocumentConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

/**
 * Configuration source for owner information.
 *
 * @since 1.17
 * @version $Id$
 */
@Component
@Named("LicensingOwnerConfigurationSource")
@Singleton
public class LicensingOwnerConfigurationSource extends AbstractDocumentConfigurationSource
{
    protected static final List<String> CODE_SPACE = Arrays.asList("Licenses", "Code");

    protected static final LocalDocumentReference LICENSING_CONFIG_DOC =
        new LocalDocumentReference(CODE_SPACE, "LicensingConfig");

    protected static final LocalDocumentReference OWNER_CLASS =
        new LocalDocumentReference(CODE_SPACE, "LicensingOwnerClass");

    @Override
    protected DocumentReference getDocumentReference()
    {
        return new DocumentReference(LICENSING_CONFIG_DOC, this.getCurrentWikiReference());
    }

    @Override
    protected LocalDocumentReference getClassReference()
    {
        return OWNER_CLASS;
    }

    @Override
    protected String getCacheId()
    {
        return "licensing.configuration.owner";
    }
}
