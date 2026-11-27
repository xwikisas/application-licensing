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
package com.xwiki.licensing.internal.issuing;

import java.io.File;
import java.util.Arrays;
import java.util.Base64;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xwiki.crypto.internal.encoder.Base64BinaryStringEncoder;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.licensing.FileLicenseStoreReference;
import com.xwiki.licensing.LicenseId;
import com.xwiki.licensing.LicenseStore;
import com.xwiki.licensing.SignedLicense;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GeneratedLicenseStore}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList(Base64BinaryStringEncoder.class)
class GeneratedLicenseStoreTest
{
    @InjectMockComponents
    private GeneratedLicenseStore store;

    @MockComponent
    @Named("FileSystem")
    private LicenseStore licenseStore;

    @MockComponent
    private org.xwiki.environment.Environment environment;

    @TempDir
    private File permanentDirectory;

    @Test
    void storeAndDeleteInTheGeneratedLicensesDirectory() throws Exception
    {
        when(this.environment.getPermanentDirectory()).thenReturn(this.permanentDirectory);
        SignedLicense license = mock(SignedLicense.class);
        LicenseId licenseId = new LicenseId();

        this.store.store(license);
        this.store.delete(licenseId);

        // Same store as $services.licensing.license.getFileLicenseStore('generated-licenses').
        File storeFile = new File(this.permanentDirectory, "generated-licenses");
        verify(this.licenseStore).store(argThat(reference -> isMultiStore(reference, storeFile)), eq(license));
        verify(this.licenseStore).delete(argThat(reference -> isMultiStore(reference, storeFile)), eq(licenseId));
    }

    @Test
    void encodeOnASingleLine() throws Exception
    {
        // Long enough to be wrapped by $services.licensing.license.encode, which the license key must not be.
        byte[] encoded = new byte[300];
        Arrays.fill(encoded, (byte) 42);
        SignedLicense license = mock(SignedLicense.class);
        when(license.getEncoded()).thenReturn(encoded);

        String key = this.store.encode(license);

        assertFalse(key.contains("\n"));
        assertArrayEquals(encoded, Base64.getDecoder().decode(key));
    }

    private static boolean isMultiStore(Object reference, File file)
    {
        return reference instanceof FileLicenseStoreReference
            && ((FileLicenseStoreReference) reference).getFile().equals(file)
            && ((FileLicenseStoreReference) reference).isMulti();
    }
}
