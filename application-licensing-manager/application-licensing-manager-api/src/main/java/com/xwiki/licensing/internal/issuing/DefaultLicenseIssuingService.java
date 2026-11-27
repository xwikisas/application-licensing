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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;

import com.xwiki.licensing.manager.IssuingException;
import com.xwiki.licensing.manager.LicenseIssuingService;
import com.xwiki.licensing.manager.rest.model.InstanceLicenses;
import com.xwiki.licensing.manager.rest.model.IssueRequest;
import com.xwiki.licensing.manager.rest.model.IssueResponse;
import com.xwiki.licensing.manager.rest.model.LicenseKey;
import com.xwiki.licensing.manager.rest.model.PolicyView;

/**
 * Default {@link LicenseIssuingService} (spec 5.1). The requests of an account are served one at a time, so that two
 * concurrent requests can't both pass the rate limit or both create the same instance.
 *
 * @version $Id$
 * @since 1.34
 */
@Component
@Singleton
public class DefaultLicenseIssuingService implements LicenseIssuingService
{
    private final Map<DocumentReference, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Inject
    private PolicyValidator validator;

    @Inject
    private UsageCounter usageCounter;

    @Inject
    private LicenseFactory licenseFactory;

    @Inject
    private IssuanceWriter writer;

    @Inject
    private LicenseViews views;

    @Override
    public IssueResponse issue(DocumentReference account, IssueRequest request) throws IssuingException
    {
        IssuingPlan plan = this.validator.validate(account, request);
        this.validator.checkRecordAuthor();
        ReentrantLock lock = this.locks.computeIfAbsent(account, key -> new ReentrantLock());
        lock.lock();
        try {
            this.validator.checkInstance(plan, account);
            Date issueDate = new Date();
            this.usageCounter.checkRateLimit(plan.getPolicy(), plan.getLicenseCount(), issueDate);
            return issue(account, plan, issueDate);
        } finally {
            lock.unlock();
        }
    }

    private IssueResponse issue(DocumentReference account, IssuingPlan plan, Date issueDate) throws IssuingException
    {
        // Sign everything in memory first, so that a signing failure writes nothing.
        List<InstanceRecord> newInstances = new ArrayList<>();
        List<PendingLicense> pendingLicenses = new ArrayList<>();
        List<InstanceLicenses> instances = new ArrayList<>();
        for (InstanceRecord instance : getInstances(account, plan, issueDate)) {
            if (!plan.isRenewal()) {
                newInstances.add(instance);
            }
            List<LicenseKey> licenses = new ArrayList<>();
            for (String featureId : plan.getFeatureIds()) {
                PendingLicense license =
                    this.licenseFactory.create(plan, instance.getInstanceId(), featureId, issueDate);
                pendingLicenses.add(license);
                licenses.add(LicenseViews.toLicenseKey(license.getRecord(), license.getKey()));
            }
            instances.add(LicenseViews.toInstanceLicenses(instance, licenses));
        }

        this.writer.write(newInstances, pendingLicenses, getRenewedInstanceWithNewDomain(plan), plan.getDomain());

        IssueResponse response = new IssueResponse();
        response.setInstances(instances);
        return response;
    }

    private List<InstanceRecord> getInstances(DocumentReference account, IssuingPlan plan, Date issueDate)
    {
        if (plan.isRenewal()) {
            InstanceRecord existing = plan.getExistingInstance();
            String domain = plan.getDomain() != null ? plan.getDomain() : existing.getDomain();
            return List.of(new InstanceRecord(existing.getInstanceId(), existing.getAccount(),
                existing.getEnvironment(), domain, existing.getProduct(), existing.getOrigin(),
                existing.getCreatedDate()));
        }
        List<InstanceRecord> instances = new ArrayList<>();
        for (int i = 0; i < plan.getCount(); i++) {
            String instanceId = plan.getInstanceId() != null ? plan.getInstanceId() : UUID.randomUUID().toString();
            instances.add(new InstanceRecord(instanceId, account, plan.getEnvironment(), plan.getDomain(),
                plan.getProduct().getId(), InstanceRecord.Origin.API, issueDate));
        }
        return instances;
    }

    private static String getRenewedInstanceWithNewDomain(IssuingPlan plan)
    {
        if (plan.isRenewal() && plan.getDomain() != null
            && !Objects.equals(plan.getDomain(), plan.getExistingInstance().getDomain())) {
            return plan.getExistingInstance().getInstanceId();
        }
        return null;
    }

    @Override
    public IssueResponse list(DocumentReference account, String instanceId, String environment, String product,
        boolean activeOnly) throws IssuingException
    {
        this.validator.getPolicy(account);
        IssueResponse response = new IssueResponse();
        response.setInstances(this.views.list(account, instanceId, environment, product, activeOnly));
        return response;
    }

    @Override
    public LicenseKey get(DocumentReference account, String licenseId) throws IssuingException
    {
        this.validator.getPolicy(account);
        return this.views.get(account, licenseId);
    }

    @Override
    public PolicyView getPolicy(DocumentReference account) throws IssuingException
    {
        Policy policy = this.validator.getPolicy(account);
        return PolicyViews.toPolicyView(policy, this.usageCounter.countIssuances(account, new Date()));
    }
}
