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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWikiException;
import com.xwiki.licensing.LicenseId;
import com.xwiki.licensing.SignedLicense;
import com.xwiki.licensing.manager.IssuingException;
import com.xwiki.licensing.manager.rest.model.InstanceLicenses;
import com.xwiki.licensing.manager.rest.model.LicenseKey;

/**
 * Reads the licenses of an account's instances and turns them into the API model (spec 4.2 and 4.3).
 *
 * @version $Id$
 * @since 1.34
 */
@Component(roles = LicenseViews.class)
@Singleton
public class LicenseViews
{
    @Inject
    private Logger logger;

    @Inject
    private ApiConfiguration configuration;

    @Inject
    private InstanceRecordStore instanceRecordStore;

    @Inject
    private LicenseRecordStore licenseRecordStore;

    @Inject
    private GeneratedLicenseStore generatedLicenseStore;

    /**
     * @param account the calling account
     * @param instanceId only this instance, or {@code null}
     * @param environment only the instances of this environment ({@code prod} or {@code test}), or {@code null}
     * @param product only the instances of this product, or {@code null}
     * @param activeOnly whether to leave out the expired licenses
     * @return the licenses of the account's instances
     * @throws IssuingException if the environment is invalid (400), or if the records can't be read
     */
    public List<InstanceLicenses> list(DocumentReference account, String instanceId, String environment,
        String product, boolean activeOnly) throws IssuingException
    {
        Environment filter = null;
        if (environment != null) {
            filter = Environment.fromValue(environment).orElseThrow(() -> IssuingErrors.badRequest(
                IssuingErrors.INVALID_REQUEST, "The environment must be [prod] or [test]."));
        }
        Date now = new Date();
        List<InstanceLicenses> result = new ArrayList<>();
        try {
            for (InstanceRecord instance : this.instanceRecordStore.getInstances(account, filter, product)) {
                if (instanceId == null || instanceId.equals(instance.getInstanceId())) {
                    List<LicenseKey> licenses = new ArrayList<>();
                    for (LicenseRecord licenseRecord : this.licenseRecordStore.getGenerated(instance.getInstanceId())) {
                        if (!activeOnly || licenseRecord.getExpirationDate().after(now)) {
                            licenses.add(toLicenseKey(licenseRecord, getKey(licenseRecord)));
                        }
                    }
                    result.add(toInstanceLicenses(instance, licenses));
                }
            }
        } catch (XWikiException | QueryException e) {
            throw IssuingErrors.internal("Failed to read the licenses of the account.", e);
        }
        return result;
    }

    /**
     * @param account the calling account
     * @param licenseId a license id
     * @return the license, if it belongs to one of the account's instances
     * @throws IssuingException 404 if the license doesn't exist or doesn't belong to one of the account's instances
     */
    public LicenseKey get(DocumentReference account, String licenseId) throws IssuingException
    {
        String notFound = String.format("No license [%s] among the licenses of the account.", licenseId);
        if (!PolicyValidator.UUID.matcher(licenseId).matches()) {
            throw IssuingErrors.notFound(notFound);
        }
        try {
            Optional<LicenseRecord> licenseRecord = this.licenseRecordStore.get(licenseId);
            if (licenseRecord.isEmpty() || !licenseRecord.get().hasLicense()) {
                throw IssuingErrors.notFound(notFound);
            }
            Optional<InstanceRecord> instance = this.configuration.getInstance(licenseRecord.get().getInstanceId());
            if (instance.isEmpty() || !account.equals(instance.get().getAccount())) {
                throw IssuingErrors.notFound(notFound);
            }
            return toLicenseKey(licenseRecord.get(), getKey(licenseRecord.get()));
        } catch (XWikiException e) {
            throw IssuingErrors.internal(String.format("Failed to read the license [%s].", licenseId), e);
        }
    }

    /**
     * @param licenseRecord a license record
     * @param key the license key, or {@code null}
     * @return the license in the API model
     */
    public static LicenseKey toLicenseKey(LicenseRecord licenseRecord, String key)
    {
        LicenseKey license = new LicenseKey();
        license.setLicenseId(licenseRecord.getLicenseId());
        license.setInstanceId(licenseRecord.getInstanceId());
        license.setFeatureId(licenseRecord.getFeatureId());
        license.setType(licenseRecord.getType() != null ? licenseRecord.getType().name() : null);
        license.setIssueDate(format(licenseRecord.getIssueDate()));
        license.setExpirationDate(format(licenseRecord.getExpirationDate()));
        license.setMaxUserCount(licenseRecord.getMaxUserCount());
        license.setKey(key);
        return license;
    }

    /**
     * @param instance an instance record
     * @param licenses the licenses of the instance, the latest issued first
     * @return the instance and its licenses in the API model; {@code keys} holds the key of the latest license of
     *     each feature
     */
    public static InstanceLicenses toInstanceLicenses(InstanceRecord instance, List<LicenseKey> licenses)
    {
        InstanceLicenses result = new InstanceLicenses();
        result.setInstanceId(instance.getInstanceId());
        result.setEnvironment(instance.getEnvironment() != null ? instance.getEnvironment().getValue() : null);
        result.setDomain(instance.getDomain());
        result.setProduct(instance.getProduct());
        result.setLicenses(licenses);

        Map<String, LicenseKey> latest = new LinkedHashMap<>();
        for (LicenseKey license : licenses) {
            if (license.getKey() != null) {
                latest.putIfAbsent(license.getFeatureId(), license);
            }
        }
        List<String> keys = new ArrayList<>();
        String expirationDate = null;
        for (LicenseKey license : latest.values()) {
            keys.add(license.getKey());
            if (expirationDate == null
                || Instant.parse(license.getExpirationDate()).isBefore(Instant.parse(expirationDate))) {
                expirationDate = license.getExpirationDate();
            }
        }
        result.setKeys(String.join(",", keys));
        result.setExpirationDate(expirationDate);
        return result;
    }

    /**
     * @param date a date, or {@code null}
     * @return the date in ISO-8601, in UTC, or {@code null}
     */
    public static String format(Date date)
    {
        return date != null ? date.toInstant().toString() : null;
    }

    private String getKey(LicenseRecord licenseRecord)
    {
        try {
            SignedLicense signedLicense = this.generatedLicenseStore.get(new LicenseId(licenseRecord.getLicenseId()));
            if (signedLicense != null) {
                return this.generatedLicenseStore.encode(signedLicense);
            }
            this.logger.warn("The signed license [{}] is missing from the store.", licenseRecord.getLicenseId());
        } catch (IOException e) {
            this.logger.warn("Failed to read the signed license [{}].", licenseRecord.getLicenseId(), e);
        }
        return null;
    }
}
