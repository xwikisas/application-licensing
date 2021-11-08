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
package com.xwiki.licensing.test.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import com.xwiki.licensing.Licensor;

/**
 * Script service for the licensor, to be used when running functional tests of the licensed applications.
 *
 * @version $Id$
 * @since 1.8
 */
@Component
@Named("licensing.licensor")
@Singleton
@Unstable
public class LicensorScriptService implements ScriptService
{
    @Inject
    private Licensor licensor;

    /**
     * Check if the given entity is covered by a valid license. Equivalent to licensor.hasLicensure(EntityReference)
     * call.
     *
     * @param reference the reference of the entity for which licensure should be checked
     * @return true if the given reference has a valid license or is not subject to licensing
     */
    public boolean hasLicensureForEntity(EntityReference reference)
    {
        return this.licensor.hasLicensure(reference);
    }

    /**
     * Check if the given extension is covered by a valid license. Equivalent to licensor.hasLicensure(ExtensionId)
     * call.
     *
     * @param extensionId the identifier of the extension for which licensure should be checked
     * @return true if the given extension has a valid license or is not subject to licensing
     */
    public boolean hasLicensureForExtension(ExtensionId extensionId)
    {
        return this.licensor.hasLicensure(extensionId);
    }
}
