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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.xwiki.model.reference.DocumentReference;

/**
 * What an account may issue through the API, as configured on {@code License.Code.API.Policies.<account>}. The
 * defaults of the specification are applied to the fields left empty, so the getters never return an unset value
 * except for the optional limits, where {@code null} means no limit.
 *
 * @version $Id$
 * @since 1.34
 */
public final class Policy
{
    /**
     * The default production license duration.
     */
    public static final String DEFAULT_PROD_DURATION = "1y";

    /**
     * The default test license duration.
     */
    public static final String DEFAULT_TEST_DURATION = "6m";

    /**
     * The default maximum number of instances in one request.
     */
    public static final int DEFAULT_MAX_BATCH_SIZE = 10;

    /**
     * The default maximum number of licenses issued in a rolling 24 hours.
     */
    public static final int DEFAULT_MAX_ISSUANCES_PER_DAY = 25;

    private static final String NONE = "none";

    /**
     * Who is written into the license as licensee. A policy allows one or more modes and the request picks one.
     */
    public enum LicenseeMode
    {
        /**
         * The partner details configured on the policy.
         */
        PARTNER,

        /**
         * The customer details sent with each request.
         */
        CUSTOMER,

        /**
         * No licensee.
         */
        NONE
    }

    /**
     * What happens when a volume limit is reached.
     */
    public enum QuotaMode
    {
        /**
         * Issue anyway and send an alert.
         */
        ALERT,

        /**
         * Refuse the request.
         */
        REJECT
    }

    private DocumentReference account;

    private boolean enabled = true;

    private List<String> products = List.of();

    private Set<Environment> allowedEnvironments = EnumSet.noneOf(Environment.class);

    private List<String> prodDurations = List.of(DEFAULT_PROD_DURATION);

    private List<String> testDurations = List.of(DEFAULT_TEST_DURATION);

    private Date prodExpirationLimit;

    private Long maxUserCount;

    private Long defaultUserCount;

    private boolean mayChooseInstanceId;

    private int maxBatchSize = DEFAULT_MAX_BATCH_SIZE;

    private Set<LicenseeMode> licenseeModes = EnumSet.of(LicenseeMode.PARTNER);

    private LicenseeMode defaultLicenseeMode = LicenseeMode.PARTNER;

    private Licensee licensee = new Licensee(null, null, null);

    private List<String> supportLevels = List.of();

    private String defaultSupport;

    private boolean sendExpirationReminders;

    private String salesNotification = NONE;

    private int maxIssuancesPerDay = DEFAULT_MAX_ISSUANCES_PER_DAY;

    private Integer maxActiveProdInstances;

    private Integer maxActiveTestInstances;

    private Long maxTotalUsers;

    private QuotaMode quotaMode = QuotaMode.ALERT;

    private String quotaPeriod = NONE;

    private Date contractRenewalDate;

    private List<DocumentReference> alertRecipients = List.of();

    private Policy()
    {
    }

    /**
     * @param account the account the policy applies to
     * @return a builder for a policy with the default values
     */
    public static Builder builder(DocumentReference account)
    {
        return new Builder(account);
    }

    /**
     * @return the account the policy applies to
     */
    public DocumentReference getAccount()
    {
        return this.account;
    }

    /**
     * @return whether the account may use the API
     */
    public boolean isEnabled()
    {
        return this.enabled;
    }

    /**
     * @return the ids of the products the account may request
     */
    public List<String> getProducts()
    {
        return this.products;
    }

    /**
     * @return the environments the account may request
     */
    public Set<Environment> getAllowedEnvironments()
    {
        return this.allowedEnvironments;
    }

    /**
     * @param environment an environment
     * @return the durations allowed for that environment, the first one being the default
     */
    public List<String> getDurations(Environment environment)
    {
        return environment == Environment.PROD ? this.prodDurations : this.testDurations;
    }

    /**
     * @return the date after which production licenses never expire, or {@code null} for no cap
     */
    public Date getProdExpirationLimit()
    {
        return this.prodExpirationLimit;
    }

    /**
     * @return the maximum user limit per instance, or {@code null} for unlimited
     */
    public Long getMaxUserCount()
    {
        return this.maxUserCount;
    }

    /**
     * @return the user limit used when the request has none, or {@code null} for unlimited
     */
    public Long getDefaultUserCount()
    {
        return this.defaultUserCount;
    }

    /**
     * @return whether the account may send its own instance ids for new instances
     */
    public boolean mayChooseInstanceId()
    {
        return this.mayChooseInstanceId;
    }

    /**
     * @return the maximum number of instances in one request
     */
    public int getMaxBatchSize()
    {
        return this.maxBatchSize;
    }

    /**
     * @return the licensee modes the account may request
     */
    public Set<LicenseeMode> getLicenseeModes()
    {
        return this.licenseeModes;
    }

    /**
     * @return the licensee mode used when the request has none, always one of {@link #getLicenseeModes()}
     */
    public LicenseeMode getDefaultLicenseeMode()
    {
        return this.defaultLicenseeMode;
    }

    /**
     * @return the partner details, used in {@link LicenseeMode#PARTNER} mode
     */
    public Licensee getLicensee()
    {
        return this.licensee;
    }

    /**
     * @return the support levels the account may request
     */
    public List<String> getSupportLevels()
    {
        return this.supportLevels;
    }

    /**
     * @return the support level used when the request has none: the configured one, else the first allowed level,
     *     else {@code null}
     */
    public String getDefaultSupport()
    {
        if (this.defaultSupport != null) {
            return this.defaultSupport;
        }
        return this.supportLevels.isEmpty() ? null : this.supportLevels.get(0);
    }

    /**
     * @return whether the licensee gets the expiration reminder emails (not enforced yet)
     */
    public boolean isSendExpirationReminders()
    {
        return this.sendExpirationReminders;
    }

    /**
     * @return how Sales is told about the production licenses issued by the account (not enforced yet)
     */
    public String getSalesNotification()
    {
        return this.salesNotification;
    }

    /**
     * @return the maximum number of licenses issued by the account in a rolling 24 hours
     */
    public int getMaxIssuancesPerDay()
    {
        return this.maxIssuancesPerDay;
    }

    /**
     * @return the maximum number of active production instances, or {@code null} for no limit
     */
    public Integer getMaxActiveProdInstances()
    {
        return this.maxActiveProdInstances;
    }

    /**
     * @return the maximum number of active test instances, or {@code null} for no limit
     */
    public Integer getMaxActiveTestInstances()
    {
        return this.maxActiveTestInstances;
    }

    /**
     * @return the maximum sum of the user limits of the active production instances, or {@code null} for no limit
     */
    public Long getMaxTotalUsers()
    {
        return this.maxTotalUsers;
    }

    /**
     * @return what happens when a volume limit is reached
     */
    public QuotaMode getQuotaMode()
    {
        return this.quotaMode;
    }

    /**
     * @return when the volume counters restart (not enforced yet)
     */
    public String getQuotaPeriod()
    {
        return this.quotaPeriod;
    }

    /**
     * @return the contract renewal date, or {@code null}
     */
    public Date getContractRenewalDate()
    {
        return this.contractRenewalDate;
    }

    /**
     * @return the groups emailed for rate and volume alerts; empty means the store admins
     */
    public List<DocumentReference> getAlertRecipients()
    {
        return this.alertRecipients;
    }

    /**
     * The licensee details written into the licenses.
     *
     * @version $Id$
     */
    public static final class Licensee
    {
        private final String firstName;

        private final String lastName;

        private final String email;

        /**
         * @param firstName the first name
         * @param lastName the last name
         * @param email the email
         */
        public Licensee(String firstName, String lastName, String email)
        {
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
        }

        /**
         * @return the first name
         */
        public String getFirstName()
        {
            return this.firstName;
        }

        /**
         * @return the last name
         */
        public String getLastName()
        {
            return this.lastName;
        }

        /**
         * @return the email
         */
        public String getEmail()
        {
            return this.email;
        }
    }

    /**
     * Builds a {@link Policy}. A {@code null} or empty value keeps the default.
     *
     * @version $Id$
     */
    public static final class Builder
    {
        private final Policy policy = new Policy();

        private Builder(DocumentReference account)
        {
            this.policy.account = account;
        }

        /**
         * @param enabled whether the account may use the API, {@code null} for the default ({@code true})
         * @return this builder
         */
        public Builder enabled(Boolean enabled)
        {
            if (enabled != null) {
                this.policy.enabled = enabled;
            }
            return this;
        }

        /**
         * @param products the ids of the products the account may request
         * @return this builder
         */
        public Builder products(List<String> products)
        {
            this.policy.products = List.copyOf(products);
            return this;
        }

        /**
         * @param environments the environments the account may request
         * @return this builder
         */
        public Builder allowedEnvironments(Set<Environment> environments)
        {
            this.policy.allowedEnvironments =
                environments.isEmpty() ? EnumSet.noneOf(Environment.class) : EnumSet.copyOf(environments);
            return this;
        }

        /**
         * @param environment an environment
         * @param durations the durations allowed for that environment, empty for the default
         * @return this builder
         */
        public Builder durations(Environment environment, List<String> durations)
        {
            if (!durations.isEmpty()) {
                if (environment == Environment.PROD) {
                    this.policy.prodDurations = List.copyOf(durations);
                } else {
                    this.policy.testDurations = List.copyOf(durations);
                }
            }
            return this;
        }

        /**
         * @param date the date after which production licenses never expire
         * @return this builder
         */
        public Builder prodExpirationLimit(Date date)
        {
            this.policy.prodExpirationLimit = date;
            return this;
        }

        /**
         * @param maxUserCount the maximum user limit per instance
         * @param defaultUserCount the user limit used when the request has none
         * @return this builder
         */
        public Builder userCounts(Long maxUserCount, Long defaultUserCount)
        {
            this.policy.maxUserCount = maxUserCount;
            this.policy.defaultUserCount = defaultUserCount;
            return this;
        }

        /**
         * @param mayChooseInstanceId whether the account may send its own instance ids
         * @return this builder
         */
        public Builder mayChooseInstanceId(boolean mayChooseInstanceId)
        {
            this.policy.mayChooseInstanceId = mayChooseInstanceId;
            return this;
        }

        /**
         * @param maxBatchSize the maximum number of instances in one request
         * @return this builder
         */
        public Builder maxBatchSize(Integer maxBatchSize)
        {
            if (maxBatchSize != null) {
                this.policy.maxBatchSize = maxBatchSize;
            }
            return this;
        }

        /**
         * @param modes the licensee modes the account may request, {@code null} or empty for the default
         *            ({@link LicenseeMode#PARTNER} only)
         * @param defaultMode the licensee mode used when the request has none; when it isn't one of the allowed modes,
         *            the first allowed mode (in declaration order) is used
         * @param licensee the partner details
         * @return this builder
         */
        public Builder licensee(Set<LicenseeMode> modes, LicenseeMode defaultMode, Licensee licensee)
        {
            if (modes != null && !modes.isEmpty()) {
                this.policy.licenseeModes = EnumSet.copyOf(modes);
            }
            this.policy.defaultLicenseeMode = this.policy.licenseeModes.contains(defaultMode) ? defaultMode
                : this.policy.licenseeModes.iterator().next();
            this.policy.licensee = licensee;
            return this;
        }

        /**
         * @param supportLevels the support levels the account may request
         * @param defaultSupport the support level used when the request has none
         * @return this builder
         */
        public Builder support(List<String> supportLevels, String defaultSupport)
        {
            this.policy.supportLevels = List.copyOf(supportLevels);
            this.policy.defaultSupport = defaultSupport;
            return this;
        }

        /**
         * @param sendExpirationReminders whether the licensee gets the expiration reminder emails
         * @param salesNotification how Sales is told about production licenses
         * @return this builder
         */
        public Builder notifications(boolean sendExpirationReminders, String salesNotification)
        {
            this.policy.sendExpirationReminders = sendExpirationReminders;
            if (salesNotification != null) {
                this.policy.salesNotification = salesNotification;
            }
            return this;
        }

        /**
         * @param maxIssuancesPerDay the maximum number of licenses issued in a rolling 24 hours
         * @return this builder
         */
        public Builder maxIssuancesPerDay(Integer maxIssuancesPerDay)
        {
            if (maxIssuancesPerDay != null) {
                this.policy.maxIssuancesPerDay = maxIssuancesPerDay;
            }
            return this;
        }

        /**
         * @param maxActiveProdInstances the maximum number of active production instances
         * @param maxActiveTestInstances the maximum number of active test instances
         * @param maxTotalUsers the maximum sum of the user limits of the active production instances
         * @return this builder
         */
        public Builder volumeLimits(Integer maxActiveProdInstances, Integer maxActiveTestInstances,
            Long maxTotalUsers)
        {
            this.policy.maxActiveProdInstances = maxActiveProdInstances;
            this.policy.maxActiveTestInstances = maxActiveTestInstances;
            this.policy.maxTotalUsers = maxTotalUsers;
            return this;
        }

        /**
         * @param mode what happens when a volume limit is reached
         * @param period when the volume counters restart
         * @param contractRenewalDate the contract renewal date
         * @return this builder
         */
        public Builder quota(QuotaMode mode, String period, Date contractRenewalDate)
        {
            if (mode != null) {
                this.policy.quotaMode = mode;
            }
            if (period != null) {
                this.policy.quotaPeriod = period;
            }
            this.policy.contractRenewalDate = contractRenewalDate;
            return this;
        }

        /**
         * @param alertRecipients the groups emailed for rate and volume alerts
         * @return this builder
         */
        public Builder alertRecipients(List<DocumentReference> alertRecipients)
        {
            this.policy.alertRecipients = List.copyOf(alertRecipients);
            return this;
        }

        /**
         * @return the policy
         */
        public Policy build()
        {
            return this.policy;
        }
    }
}
