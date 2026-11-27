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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiException;
import com.xwiki.licensing.manager.IssuingException;

/**
 * Writes the result of an issuance: the new instance records, the signed license files and the license records, and
 * then the new domain of a renewed instance. All or nothing: on a failure, what was written is deleted (spec 4.5).
 *
 * @version $Id$
 * @since 1.34
 */
@Component(roles = IssuanceWriter.class)
@Singleton
public class IssuanceWriter
{
    @Inject
    private Logger logger;

    @Inject
    private InstanceRecordStore instanceRecordStore;

    @Inject
    private LicenseRecordStore licenseRecordStore;

    @Inject
    private GeneratedLicenseStore generatedLicenseStore;

    /**
     * Something written that can be undone.
     */
    @FunctionalInterface
    private interface Undo
    {
        void run() throws XWikiException;
    }

    /**
     * @param newInstances the instance records to create
     * @param licenses the licenses to store
     * @param renewedInstance the renewed instance whose domain changes, or {@code null}
     * @param domain the new domain of the renewed instance
     * @throws IssuingException 500 if something can't be written, after deleting what was written
     */
    public void write(List<InstanceRecord> newInstances, List<PendingLicense> licenses, String renewedInstance,
        String domain) throws IssuingException
    {
        Deque<Undo> written = new ArrayDeque<>();
        try {
            for (InstanceRecord instance : newInstances) {
                this.instanceRecordStore.create(instance);
                written.push(() -> this.instanceRecordStore.delete(instance.getInstanceId()));
            }
            for (PendingLicense license : licenses) {
                this.generatedLicenseStore.store(license.getSignedLicense());
                written.push(() -> this.generatedLicenseStore.delete(license.getSignedLicense().getId()));
                this.licenseRecordStore.create(license.getRecord());
                written.push(() -> this.licenseRecordStore.delete(license.getRecord().getLicenseId()));
            }
            if (renewedInstance != null) {
                // Last, since it can't be undone.
                this.instanceRecordStore.updateDomain(renewedInstance, domain);
            }
        } catch (XWikiException | IOException | RuntimeException e) {
            rollback(written);
            throw IssuingErrors.internal("Failed to store the licenses. Nothing was issued.", e);
        }
    }

    private void rollback(Deque<Undo> written)
    {
        while (!written.isEmpty()) {
            try {
                written.pop().run();
            } catch (XWikiException | RuntimeException e) {
                this.logger.error("Failed to undo a write of a failed license issuance.", e);
            }
        }
    }
}
