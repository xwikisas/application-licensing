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
package com.xwiki.licensing.script;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.environment.Environment;
import org.xwiki.script.service.ScriptService;

import com.xwiki.licensing.FileLicenseStoreReference;
import com.xwiki.licensing.LicenseStore;
import com.xwiki.licensing.LicenseStoreReference;

public abstract class AbstractLicenseScriptService implements ScriptService
{
    @Inject
    private Environment environment;

    @Inject
    @Named("FileSystem")
    protected LicenseStore filesystemLicenseStore;

    protected LicenseStoreReference getFileLicenseStoreReference(String filename, boolean multi)
    {
        File file;
        if(!filename.startsWith("/")) {
            file = new File(this.environment.getPermanentDirectory(), filename);
        } else {
            file = new File(filename);
        }

        return new FileLicenseStoreReference(file, multi);
    }
}
