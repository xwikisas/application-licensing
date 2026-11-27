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

import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWikiException;
import com.xwiki.licensing.manager.IssuingException;
import com.xwiki.licensing.manager.rest.model.IssueRequest;

import static com.xwiki.licensing.internal.issuing.IssuingErrors.INVALID_REQUEST;
import static com.xwiki.licensing.internal.issuing.IssuingErrors.LIMIT_EXCEEDED;
import static com.xwiki.licensing.internal.issuing.IssuingErrors.badRequest;
import static com.xwiki.licensing.internal.issuing.IssuingErrors.conflict;
import static com.xwiki.licensing.internal.issuing.IssuingErrors.forbidden;
import static com.xwiki.licensing.internal.issuing.IssuingErrors.internal;

/**
 * Checks a License Issuing API request against the caller's policy (spec sections 3.4, 3.5, 4.1 and 4.5).
 *
 * @version $Id$
 * @since 1.34
 */
@Component(roles = PolicyValidator.class)
@Singleton
public class PolicyValidator
{
    /**
     * The maximum length of the free text values of a request (domain, licensee details).
     */
    static final int MAX_TEXT_LENGTH = 255;

    /**
     * XWiki instance ids and license ids are UUIDs ({@code org.xwiki.instance.InstanceId} refuses anything else).
     */
    static final Pattern UUID =
        Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    @Inject
    private ApiConfiguration configuration;

    @Inject
    private LicenseRecordStore licenseRecordStore;

    /**
     * @param account the calling account
     * @return the enabled policy of the account
     * @throws IssuingException if the account has no policy (403 {@code noPolicy}) or if it's disabled (403
     *     {@code policyDisabled})
     */
    public Policy getPolicy(DocumentReference account) throws IssuingException
    {
        Optional<Policy> policy;
        try {
            policy = this.configuration.getPolicy(account);
        } catch (XWikiException e) {
            throw internal("Failed to read the policy.", e);
        }
        if (policy.isEmpty()) {
            throw forbidden(IssuingErrors.NO_POLICY, "The account has no License Issuing API policy.");
        }
        if (!policy.get().isEnabled()) {
            throw forbidden(IssuingErrors.POLICY_DISABLED,
                "The License Issuing API policy of the account is disabled.");
        }
        return policy.get();
    }

    /**
     * Checks everything that doesn't depend on the stored instance records, see
     * {@link #checkInstance(IssuingPlan, DocumentReference)}.
     *
     * @param account the calling account
     * @param request the request
     * @return the validated request
     * @throws IssuingException if the request is invalid or the policy doesn't allow it
     */
    public IssuingPlan validate(DocumentReference account, IssueRequest request) throws IssuingException
    {
        if (request == null) {
            throw badRequest(INVALID_REQUEST, "The request has no body.");
        }
        IssuingPlan plan = new IssuingPlan();
        plan.setPolicy(getPolicy(account));
        plan.setProduct(getProduct(plan.getPolicy(), request.getProduct()));
        plan.setEnvironment(getEnvironment(plan.getPolicy(), request.getEnvironment()));
        setInstances(plan, request);
        plan.setDuration(getDuration(plan.getPolicy(), plan.getEnvironment(), request.getDuration()));
        plan.setSupport(getSupport(plan.getPolicy(), request.getSupport()));
        plan.setMaxUserCount(getMaxUserCount(plan.getPolicy(), request.getMaxUserCount()));
        plan.setLicensee(LicenseeValidator.getLicensee(plan.getPolicy(), request));
        plan.setDomain(getText(request.getDomain(), "domain"));
        checkProdExpirationLimit(plan);
        return plan;
    }

    /**
     * Classifies the instance sent by the client (spec 3.5): a renewal of an instance owned by the account, or a new
     * instance. Called under the account lock, since it depends on the stored records.
     *
     * @param plan the validated request
     * @param account the calling account
     * @throws IssuingException if the instance belongs to another account (409 {@code instanceIdTaken}), is used for
     *     the other environment (409 {@code environmentConflict}) or another product (409 {@code productConflict}),
     *     if the account may not choose a new instance id (403 {@code instanceIdNotAllowed}), or if the instance was
     *     licensed before the API and isn't imported (409 {@code instanceNotImported})
     */
    public void checkInstance(IssuingPlan plan, DocumentReference account) throws IssuingException
    {
        String instanceId = plan.getInstanceId();
        if (instanceId == null) {
            plan.setExistingInstance(null);
            return;
        }
        try {
            Optional<InstanceRecord> instance = this.configuration.getInstance(instanceId);
            if (instance.isPresent()) {
                checkRenewal(plan, instance.get(), account);
                plan.setExistingInstance(instance.get());
            } else {
                checkNewInstance(plan);
                plan.setExistingInstance(null);
            }
        } catch (XWikiException | QueryException e) {
            throw internal(String.format("Failed to read the records of the instance [%s].", instanceId), e);
        }
    }

    private void checkRenewal(IssuingPlan plan, InstanceRecord instance, DocumentReference account)
        throws IssuingException
    {
        if (!account.equals(instance.getAccount())) {
            throw conflict(IssuingErrors.INSTANCE_ID_TAKEN,
                String.format("The instance [%s] is used by another account.", instance.getInstanceId()));
        }
        if (instance.getEnvironment() != plan.getEnvironment()) {
            throw conflict(IssuingErrors.ENVIRONMENT_CONFLICT,
                String.format("The instance [%s] is a [%s] instance, and its environment can't change.",
                    instance.getInstanceId(), instance.getEnvironment() != null
                        ? instance.getEnvironment().getValue() : null));
        }
        if (!plan.getProduct().getId().equals(instance.getProduct())) {
            throw conflict(IssuingErrors.PRODUCT_CONFLICT, String.format("The instance [%s] is licensed for [%s].",
                instance.getInstanceId(), instance.getProduct()));
        }
    }

    private void checkNewInstance(IssuingPlan plan) throws IssuingException, QueryException
    {
        // Checked first, so that an account that can't choose instance ids learns nothing about other instances.
        if (!plan.getPolicy().mayChooseInstanceId()) {
            throw forbidden(IssuingErrors.INSTANCE_ID_NOT_ALLOWED, String.format(
                "The instance [%s] is unknown, and the policy doesn't allow choosing the id of a new instance.",
                plan.getInstanceId()));
        }
        if (this.licenseRecordStore.hasRecords(plan.getInstanceId())) {
            throw conflict(IssuingErrors.INSTANCE_NOT_IMPORTED, String.format(
                "The instance [%s] was licensed before the API and must be imported first.", plan.getInstanceId()));
        }
    }

    private Product getProduct(Policy policy, String productId) throws IssuingException
    {
        if (StringUtils.isBlank(productId)) {
            throw badRequest(INVALID_REQUEST, "The product is required.");
        }
        Optional<Product> product;
        try {
            product = this.configuration.getProduct(productId);
        } catch (XWikiException e) {
            throw internal(String.format("Failed to read the product [%s].", productId), e);
        }
        if (product.isEmpty()) {
            throw badRequest(IssuingErrors.UNKNOWN_PRODUCT, String.format("Unknown product [%s].", productId));
        }
        if (!policy.getProducts().contains(productId)) {
            throw forbidden(IssuingErrors.PRODUCT_NOT_ALLOWED,
                String.format("The policy doesn't allow the product [%s].", productId));
        }
        return product.get();
    }

    private static Environment getEnvironment(Policy policy, String value) throws IssuingException
    {
        Environment environment = Environment.fromValue(value).orElseThrow(
            () -> badRequest(INVALID_REQUEST, "The environment is required and must be [prod] or [test]."));
        if (!policy.getAllowedEnvironments().contains(environment)) {
            throw forbidden(IssuingErrors.ENVIRONMENT_NOT_ALLOWED,
                String.format("The policy doesn't allow the environment [%s].", value));
        }
        return environment;
    }

    private static void setInstances(IssuingPlan plan, IssueRequest request) throws IssuingException
    {
        String instanceId = request.getInstanceId();
        Integer count = request.getCount();
        if (instanceId != null) {
            if (count != null) {
                throw badRequest(INVALID_REQUEST, "The count can't be used together with an instance id.");
            }
            if (!UUID.matcher(instanceId).matches()) {
                throw badRequest(INVALID_REQUEST,
                    String.format("Invalid instance id [%s]: it must be a UUID.", instanceId));
            }
            plan.setInstanceId(instanceId);
            plan.setCount(1);
        } else if (count == null) {
            plan.setCount(1);
        } else if (count < 1) {
            throw badRequest(INVALID_REQUEST, "The count must be at least 1.");
        } else if (count > plan.getPolicy().getMaxBatchSize()) {
            throw forbidden(LIMIT_EXCEEDED, String.format("The count [%d] is over the maximum batch size [%d].",
                count, plan.getPolicy().getMaxBatchSize()));
        } else {
            plan.setCount(count);
        }
    }

    private static LicenseDuration getDuration(Policy policy, Environment environment, String value)
        throws IssuingException
    {
        String duration;
        if (value == null) {
            duration = policy.getDurations(environment).get(0);
        } else {
            duration = value.trim().toLowerCase(Locale.ROOT);
            if (!policy.getDurations(environment).contains(duration)) {
                throw forbidden(IssuingErrors.DURATION_NOT_ALLOWED, String.format(
                    "The policy doesn't allow the duration [%s] for [%s] licenses.", value, environment.getValue()));
            }
        }
        String chosen = duration;
        return LicenseDuration.parse(chosen).orElseThrow(
            () -> internal(String.format("The policy duration [%s] is invalid.", chosen), null));
    }

    private static String getSupport(Policy policy, String value) throws IssuingException
    {
        if (value == null) {
            return policy.getDefaultSupport();
        }
        if (!policy.getSupportLevels().contains(value)) {
            throw forbidden(IssuingErrors.SUPPORT_NOT_ALLOWED,
                String.format("The policy doesn't allow the support level [%s].", value));
        }
        return value;
    }

    private static long getMaxUserCount(Policy policy, Long value) throws IssuingException
    {
        Long max = getMaxUserCount(policy);
        Long requested = value != null ? value : policy.getDefaultUserCount();
        if (requested == null) {
            // Without a default, the largest allowed value.
            return max != null ? max : IssuingPlan.UNLIMITED;
        }
        if (requested == 0 || requested < IssuingPlan.UNLIMITED) {
            throw badRequest(INVALID_REQUEST, "The user count must be positive, or -1 for unlimited.");
        }
        if (max != null && (requested == IssuingPlan.UNLIMITED || requested > max)) {
            throw forbidden(LIMIT_EXCEEDED,
                String.format("The user count [%d] is over the policy maximum [%d].", requested, max));
        }
        return requested;
    }

    private static Long getMaxUserCount(Policy policy)
    {
        Long max = policy.getMaxUserCount();
        return max != null && max >= 0 ? max : null;
    }

    private static void checkProdExpirationLimit(IssuingPlan plan) throws IssuingException
    {
        Date limit = plan.getPolicy().getProdExpirationLimit();
        if (plan.getEnvironment() == Environment.PROD && limit != null && !limit.after(new Date())) {
            throw forbidden(LIMIT_EXCEEDED, "The production expiration limit of the policy is reached.");
        }
    }

    /**
     * @throws IssuingException 500 if license generation isn't configured (no {@code License.Code.LicenseConfig}), so
     *     the records would have no author
     */
    public void checkRecordAuthor() throws IssuingException
    {
        try {
            if (this.configuration.getRecordAuthor() == null) {
                throw internal(
                    "License generation isn't configured: the page License.Code.LicenseConfig is missing.", null);
            }
        } catch (XWikiException e) {
            throw internal("Failed to read the license generation configuration.", e);
        }
    }

    /**
     * @param value a free text value of the request
     * @param name the name of the value, for the error message
     * @return the trimmed value, or {@code null} if it's blank
     * @throws IssuingException if the value is too long
     */
    static String getText(String value, String name) throws IssuingException
    {
        String text = StringUtils.trimToNull(value);
        if (text != null && text.length() > MAX_TEXT_LENGTH) {
            throw badRequest(INVALID_REQUEST,
                String.format("The %s is longer than %d characters.", name, MAX_TEXT_LENGTH));
        }
        return text;
    }
}
