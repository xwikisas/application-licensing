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
package com.xwiki.licensing;

import java.io.File;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Unit tests for {@link FileLicenseStoreReference}.
 * 
 * @version $Id$
 */
public class FileLicenseStoreReferenceTest
{
    @Test
    public void equalsAndHashCode()
    {
        assertEquals(new FileLicenseStoreReference(new File("licenses"), true),
            new FileLicenseStoreReference(new File("licenses"), true));
        assertEquals(new FileLicenseStoreReference(new File("licenses"), true).hashCode(),
            new FileLicenseStoreReference(new File("licenses"), true).hashCode());

        assertNotEquals(new FileLicenseStoreReference(new File("foo"), true),
            new FileLicenseStoreReference(new File("bar"), true));

        assertNotEquals(new FileLicenseStoreReference(new File("licenses"), false),
            new FileLicenseStoreReference(new File("licenses"), true));

        assertNotEquals(new FileLicenseStoreReference(new File("foo"), false),
            new FileLicenseStoreReference(new File("bar"), true));

        assertNotEquals(new FileLicenseStoreReference(new File("licenses")), null);
        assertNotEquals(new FileLicenseStoreReference(new File("licenses")), new LicenseStoreReference()
        {
        });
    }
}
