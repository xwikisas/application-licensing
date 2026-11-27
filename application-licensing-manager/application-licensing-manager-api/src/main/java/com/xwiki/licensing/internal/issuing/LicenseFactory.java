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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.instance.InstanceId;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWikiException;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseId;
import com.xwiki.licensing.LicensedFeatureId;
import com.xwiki.licensing.SignedLicense;
import com.xwiki.licensing.manager.IssuingException;

/**
 * Builds and signs one license, in memory, the way the {@code #generateLicense} macro does (spec 5.1). Nothing is
 * written: see {@link IssuanceWriter}.
 *
 * @version $Id$
 * @since 1.34
 */
@Component(roles = LicenseFactory.class)
@Singleton
public class LicenseFactory
{
    @Inject
    private LicensedDependencyResolver dependencyResolver;

    @Inject
    private LicenseRecordStore licenseRecordStore;

    @Inject
    private LicenseSigner signer;

    @Inject
    private GeneratedLicenseStore generatedLicenseStore;

    /**
     * @param plan the validated request
     * @param instanceId the licensed instance
     * @param featureId the licensed feature
     * @param issueDate the issue date, the same for all the licenses of the request
     * @return the signed license and its record
     * @throws IssuingException if the feature can't be resolved, or if the license can't be signed
     */
    public PendingLicense create(IssuingPlan plan, String instanceId, String featureId, Date issueDate)
        throws IssuingException
    {
        LicensedDependencyResolver.LicensedDependencies dependencies =
            this.dependencyResolver.resolve(featureId, plan.getProduct().getExtensionVersion(featureId))
                .orElseThrow(() -> IssuingErrors.internal(
                    String.format("Failed to resolve the licensed dependencies of [%s].", featureId), null));

        LicenseRecord licenseRecord = new LicenseRecord();
        licenseRecord.setLicenseId(UUID.randomUUID().toString());
        licenseRecord.setType(plan.getEnvironment().getLicenseType());
        licenseRecord.setFirstName(plan.getLicensee().getFirstName());
        licenseRecord.setLastName(plan.getLicensee().getLastName());
        licenseRecord.setEmail(plan.getLicensee().getEmail());
        licenseRecord.setInstanceId(instanceId);
        licenseRecord.setFeatureId(featureId);
        licenseRecord.setDependentFeatureIds(new ArrayList<>(dependencies.getDependentFeatureIds()));
        licenseRecord.setExtensionVersion(dependencies.getExtensionVersion());
        licenseRecord.setSupport(plan.getSupport());
        licenseRecord.setMaxUserCount(plan.getMaxUserCount());
        licenseRecord.setIssueDate(issueDate);
        licenseRecord.setExpirationDate(getExpirationDate(plan, instanceId, featureId, issueDate));
        licenseRecord.setHasLicense(true);
        licenseRecord.setSendEmails(plan.getPolicy().isSendExpirationReminders());

        ZonedDateTime issue = issueDate.toInstant().atZone(ZoneId.systemDefault());
        try {
            SignedLicense signedLicense = this.signer.sign(toLicense(licenseRecord), issue.getYear());
            return new PendingLicense(licenseRecord, signedLicense, this.generatedLicenseStore.encode(signedLicense));
        } catch (LicenseSigningException | IOException e) {
            throw IssuingErrors.internal(String.format("Failed to sign the license of [%s].", featureId), e);
        }
    }

    /**
     * New instance: issue date plus the duration. Renewal: the later of the issue date and the current expiration
     * date of the feature, plus the duration, so that renewing early loses no days. Production licenses never expire
     * after the policy's production expiration limit.
     */
    private Date getExpirationDate(IssuingPlan plan, String instanceId, String featureId, Date issueDate)
        throws IssuingException
    {
        Date start = issueDate;
        if (plan.isRenewal()) {
            try {
                Optional<LicenseRecord> current = this.licenseRecordStore.getLatest(instanceId, featureId);
                if (current.isPresent() && current.get().getExpirationDate() != null
                    && current.get().getExpirationDate().after(start)) {
                    start = current.get().getExpirationDate();
                }
            } catch (XWikiException | QueryException e) {
                throw IssuingErrors.internal(
                    String.format("Failed to read the current license of [%s] on [%s].", featureId, instanceId), e);
            }
        }
        Date expirationDate =
            Date.from(plan.getDuration().addTo(start.toInstant().atZone(ZoneId.systemDefault())).toInstant());
        Date limit = plan.getPolicy().getProdExpirationLimit();
        if (plan.getEnvironment() == Environment.PROD && limit != null && expirationDate.after(limit)) {
            expirationDate = limit;
        }
        return expirationDate;
    }

    static License toLicense(LicenseRecord licenseRecord)
    {
        License license = new License();
        license.setId(new LicenseId(licenseRecord.getLicenseId()));
        license.setType(licenseRecord.getType());
        license.addInstanceId(new InstanceId(licenseRecord.getInstanceId()));
        license.setExpirationDate(licenseRecord.getExpirationDate().getTime());
        license.setMaxUserCount(licenseRecord.getMaxUserCount());
        // Same licensee information as the macro. Missing values are written empty, as the macro writes the empty
        // fields of the record.
        license.addLicenseeInfo(License.LICENSE_ISSUE_DATE, String.valueOf(licenseRecord.getIssueDate().getTime()));
        license.addLicenseeInfo(License.LICENSEE_FIRST_NAME, StringUtils.defaultString(licenseRecord.getFirstName()));
        license.addLicenseeInfo(License.LICENSEE_LAST_NAME, StringUtils.defaultString(licenseRecord.getLastName()));
        license.addLicenseeInfo(License.LICENSEE_EMAIL, StringUtils.defaultString(licenseRecord.getEmail()));
        license.addLicenseeInfo("support", StringUtils.defaultString(licenseRecord.getSupport()));
        license.addFeatureId(new LicensedFeatureId(licenseRecord.getFeatureId()));
        for (String dependentFeatureId : licenseRecord.getDependentFeatureIds()) {
            license.addFeatureId(new LicensedFeatureId(dependentFeatureId));
        }
        return license;
    }
}
