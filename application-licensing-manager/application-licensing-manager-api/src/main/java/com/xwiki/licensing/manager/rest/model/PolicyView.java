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
package com.xwiki.licensing.manager.rest.model;

import java.util.List;

import org.xwiki.stability.Unstable;

/**
 * What the policy of an account allows, and the account's current usage.
 *
 * @version $Id$
 * @since 1.34
 */
@Unstable
public class PolicyView
{
    private List<String> products;

    private List<String> environments;

    private List<String> prodDurations;

    private List<String> testDurations;

    private String prodExpirationLimit;

    private Long maxUserCount;

    private Long defaultUserCount;

    private Boolean mayChooseInstanceId;

    private Integer maxBatchSize;

    private List<String> licenseeModes;

    private String defaultLicenseeMode;

    private List<String> supportLevels;

    private String defaultSupport;

    private Integer maxIssuancesPerDay;

    private Integer maxActiveProdInstances;

    private Integer maxActiveTestInstances;

    private Long maxTotalUsers;

    private String quotaMode;

    private Usage usage;

    /**
     * @return the products the account may request
     */
    public List<String> getProducts()
    {
        return this.products;
    }

    /**
     * @param products the products the account may request
     */
    public void setProducts(List<String> products)
    {
        this.products = products;
    }

    /**
     * @return the environments the account may request
     */
    public List<String> getEnvironments()
    {
        return this.environments;
    }

    /**
     * @param environments the environments the account may request
     */
    public void setEnvironments(List<String> environments)
    {
        this.environments = environments;
    }

    /**
     * @return the durations allowed for production licenses, the first one being the default
     */
    public List<String> getProdDurations()
    {
        return this.prodDurations;
    }

    /**
     * @param prodDurations the durations allowed for production licenses, the first one being the default
     */
    public void setProdDurations(List<String> prodDurations)
    {
        this.prodDurations = prodDurations;
    }

    /**
     * @return the durations allowed for test licenses, the first one being the default
     */
    public List<String> getTestDurations()
    {
        return this.testDurations;
    }

    /**
     * @param testDurations the durations allowed for test licenses, the first one being the default
     */
    public void setTestDurations(List<String> testDurations)
    {
        this.testDurations = testDurations;
    }

    /**
     * @return the date after which production licenses never expire (ISO-8601, UTC), or {@code null}
     */
    public String getProdExpirationLimit()
    {
        return this.prodExpirationLimit;
    }

    /**
     * @param prodExpirationLimit the date after which production licenses never expire (ISO-8601, UTC), or {@code null}
     */
    public void setProdExpirationLimit(String prodExpirationLimit)
    {
        this.prodExpirationLimit = prodExpirationLimit;
    }

    /**
     * @return the maximum user limit per instance, or {@code null} for unlimited
     */
    public Long getMaxUserCount()
    {
        return this.maxUserCount;
    }

    /**
     * @param maxUserCount the maximum user limit per instance, or {@code null} for unlimited
     */
    public void setMaxUserCount(Long maxUserCount)
    {
        this.maxUserCount = maxUserCount;
    }

    /**
     * @return the user limit used when the request has none, or {@code null} for unlimited
     */
    public Long getDefaultUserCount()
    {
        return this.defaultUserCount;
    }

    /**
     * @param defaultUserCount the user limit used when the request has none, or {@code null} for unlimited
     */
    public void setDefaultUserCount(Long defaultUserCount)
    {
        this.defaultUserCount = defaultUserCount;
    }

    /**
     * @return whether the account may send its own instance ids for new instances
     */
    public Boolean getMayChooseInstanceId()
    {
        return this.mayChooseInstanceId;
    }

    /**
     * @param mayChooseInstanceId whether the account may send its own instance ids for new instances
     */
    public void setMayChooseInstanceId(Boolean mayChooseInstanceId)
    {
        this.mayChooseInstanceId = mayChooseInstanceId;
    }

    /**
     * @return the maximum number of new instances in one request
     */
    public Integer getMaxBatchSize()
    {
        return this.maxBatchSize;
    }

    /**
     * @param maxBatchSize the maximum number of new instances in one request
     */
    public void setMaxBatchSize(Integer maxBatchSize)
    {
        this.maxBatchSize = maxBatchSize;
    }

    /**
     * @return the licensee modes the account may request
     */
    public List<String> getLicenseeModes()
    {
        return this.licenseeModes;
    }

    /**
     * @param licenseeModes the licensee modes the account may request
     */
    public void setLicenseeModes(List<String> licenseeModes)
    {
        this.licenseeModes = licenseeModes;
    }

    /**
     * @return the licensee mode used when the request has none
     */
    public String getDefaultLicenseeMode()
    {
        return this.defaultLicenseeMode;
    }

    /**
     * @param defaultLicenseeMode the licensee mode used when the request has none
     */
    public void setDefaultLicenseeMode(String defaultLicenseeMode)
    {
        this.defaultLicenseeMode = defaultLicenseeMode;
    }

    /**
     * @return the support levels the account may request
     */
    public List<String> getSupportLevels()
    {
        return this.supportLevels;
    }

    /**
     * @param supportLevels the support levels the account may request
     */
    public void setSupportLevels(List<String> supportLevels)
    {
        this.supportLevels = supportLevels;
    }

    /**
     * @return the support level used when the request has none
     */
    public String getDefaultSupport()
    {
        return this.defaultSupport;
    }

    /**
     * @param defaultSupport the support level used when the request has none
     */
    public void setDefaultSupport(String defaultSupport)
    {
        this.defaultSupport = defaultSupport;
    }

    /**
     * @return the maximum number of licenses issued in a rolling 24 hours
     */
    public Integer getMaxIssuancesPerDay()
    {
        return this.maxIssuancesPerDay;
    }

    /**
     * @param maxIssuancesPerDay the maximum number of licenses issued in a rolling 24 hours
     */
    public void setMaxIssuancesPerDay(Integer maxIssuancesPerDay)
    {
        this.maxIssuancesPerDay = maxIssuancesPerDay;
    }

    /**
     * @return the maximum number of active production instances, or {@code null}
     */
    public Integer getMaxActiveProdInstances()
    {
        return this.maxActiveProdInstances;
    }

    /**
     * @param maxActiveProdInstances the maximum number of active production instances, or {@code null}
     */
    public void setMaxActiveProdInstances(Integer maxActiveProdInstances)
    {
        this.maxActiveProdInstances = maxActiveProdInstances;
    }

    /**
     * @return the maximum number of active test instances, or {@code null}
     */
    public Integer getMaxActiveTestInstances()
    {
        return this.maxActiveTestInstances;
    }

    /**
     * @param maxActiveTestInstances the maximum number of active test instances, or {@code null}
     */
    public void setMaxActiveTestInstances(Integer maxActiveTestInstances)
    {
        this.maxActiveTestInstances = maxActiveTestInstances;
    }

    /**
     * @return the maximum sum of the user limits of the active production instances, or {@code null}
     */
    public Long getMaxTotalUsers()
    {
        return this.maxTotalUsers;
    }

    /**
     * @param maxTotalUsers the maximum sum of the user limits of the active production instances, or {@code null}
     */
    public void setMaxTotalUsers(Long maxTotalUsers)
    {
        this.maxTotalUsers = maxTotalUsers;
    }

    /**
     * @return what happens when a volume limit is reached ({@code alert} or {@code reject})
     */
    public String getQuotaMode()
    {
        return this.quotaMode;
    }

    /**
     * @param quotaMode what happens when a volume limit is reached ({@code alert} or {@code reject})
     */
    public void setQuotaMode(String quotaMode)
    {
        this.quotaMode = quotaMode;
    }

    /**
     * @return the current usage
     */
    public Usage getUsage()
    {
        return this.usage;
    }

    /**
     * @param usage the current usage
     */
    public void setUsage(Usage usage)
    {
        this.usage = usage;
    }
}
