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

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiException;
import com.xwiki.licensing.LicenseId;
import com.xwiki.licensing.SignedLicense;
import com.xwiki.licensing.manager.IssuingException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link IssuanceWriter}: everything written is deleted when a write fails.
 *
 * @version $Id$
 */
@ComponentTest
class IssuanceWriterTest
{
    private static final DocumentReference ACCOUNT = new DocumentReference("xwiki", "XWiki", "partner");

    private static final InstanceRecord INSTANCE = new InstanceRecord("instance1", ACCOUNT, Environment.PROD, null,
        "bundle", InstanceRecord.Origin.API, new Date());

    @InjectMockComponents
    private IssuanceWriter writer;

    @MockComponent
    private InstanceRecordStore instanceRecordStore;

    @MockComponent
    private LicenseRecordStore licenseRecordStore;

    @MockComponent
    private GeneratedLicenseStore generatedLicenseStore;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private final PendingLicense first = license("license1");

    private final PendingLicense second = license("license2");

    @Test
    void write() throws Exception
    {
        this.writer.write(List.of(INSTANCE), List.of(this.first, this.second), null, null);

        verify(this.instanceRecordStore).create(INSTANCE);
        verify(this.generatedLicenseStore).store(this.first.getSignedLicense());
        verify(this.licenseRecordStore).create(this.first.getRecord());
        verify(this.generatedLicenseStore).store(this.second.getSignedLicense());
        verify(this.licenseRecordStore).create(this.second.getRecord());
        verify(this.instanceRecordStore, never()).updateDomain(anyString(), anyString());
        verify(this.licenseRecordStore, never()).delete(anyString());
    }

    @Test
    void writeRenewalWithANewDomain() throws Exception
    {
        this.writer.write(List.of(), List.of(this.first), "instance1", "new.example.com");

        verify(this.licenseRecordStore).create(this.first.getRecord());
        verify(this.instanceRecordStore).updateDomain("instance1", "new.example.com");
    }

    @Test
    void rollbackOnARecordFailure() throws Exception
    {
        doThrow(new XWikiException()).when(this.licenseRecordStore).create(this.second.getRecord());

        IssuingException exception = assertThrows(IssuingException.class,
            () -> this.writer.write(List.of(INSTANCE), List.of(this.first, this.second), null, null));

        assertEquals(500, exception.getStatus());
        // Everything written is deleted.
        verify(this.generatedLicenseStore).delete(this.second.getSignedLicense().getId());
        verify(this.licenseRecordStore).delete("license1");
        verify(this.generatedLicenseStore).delete(this.first.getSignedLicense().getId());
        verify(this.instanceRecordStore).delete("instance1");
        verify(this.licenseRecordStore, never()).delete("license2");
    }

    @Test
    void rollbackOnADomainFailureContinuesAfterAnUndoFailure() throws Exception
    {
        doThrow(new XWikiException()).when(this.instanceRecordStore).updateDomain("instance1", "new.example.com");
        doThrow(new XWikiException()).when(this.licenseRecordStore).delete("license1");

        assertThrows(IssuingException.class,
            () -> this.writer.write(List.of(), List.of(this.first), "instance1", "new.example.com"));

        verify(this.generatedLicenseStore).delete(this.first.getSignedLicense().getId());
        assertEquals("Failed to undo a write of a failed license issuance.", this.logCapture.getMessage(0));
    }

    @Test
    void rollbackOnAFileFailure() throws Exception
    {
        doThrow(new IOException()).when(this.generatedLicenseStore).store(this.first.getSignedLicense());

        assertThrows(IssuingException.class,
            () -> this.writer.write(List.of(INSTANCE), List.of(this.first), null, null));

        verify(this.instanceRecordStore).delete("instance1");
        verify(this.licenseRecordStore, never()).create(any());
        verify(this.generatedLicenseStore, never()).delete(any());
    }

    private static PendingLicense license(String licenseId)
    {
        LicenseRecord licenseRecord = new LicenseRecord();
        licenseRecord.setLicenseId(licenseId);
        SignedLicense signedLicense = mock(SignedLicense.class);
        when(signedLicense.getId()).thenReturn(new LicenseId());
        return new PendingLicense(licenseRecord, signedLicense, "KEY");
    }
}
