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

import org.xwiki.stability.Unstable;

/**
 * A request to issue licenses for new instances or to renew the licenses of an instance.
 *
 * @version $Id$
 * @since 1.34
 */
@Unstable
public class IssueRequest
{
    private String product;

    private String environment;

    private String instanceId;

    private Integer count;

    private String domain;

    private Long maxUserCount;

    private String duration;

    private String support;

    private String licenseeMode;

    private LicenseeDetails licensee;

    /**
     * @return the product to license, one of the policy's products
     */
    public String getProduct()
    {
        return this.product;
    }

    /**
     * @param product the product to license, one of the policy's products
     */
    public void setProduct(String product)
    {
        this.product = product;
    }

    /**
     * @return the environment of the instances, {@code prod} or {@code test}
     */
    public String getEnvironment()
    {
        return this.environment;
    }

    /**
     * @param environment the environment of the instances, {@code prod} or {@code test}
     */
    public void setEnvironment(String environment)
    {
        this.environment = environment;
    }

    /**
     * @return the instance to renew, or the new instance id to use (needs {@code mayChooseInstanceId}); {@code null} to
     *     let the store generate new instance ids
     */
    public String getInstanceId()
    {
        return this.instanceId;
    }

    /**
     * @param instanceId the instance to renew, or the new instance id to use (needs {@code mayChooseInstanceId});
     *     {@code null} to let the store generate new instance ids
     */
    public void setInstanceId(String instanceId)
    {
        this.instanceId = instanceId;
    }

    /**
     * @return the number of new instances to license, only without instance id; {@code null} for one
     */
    public Integer getCount()
    {
        return this.count;
    }

    /**
     * @param count the number of new instances to license, only without instance id; {@code null} for one
     */
    public void setCount(Integer count)
    {
        this.count = count;
    }

    /**
     * @return the domain of the instances, recorded for tracking only
     */
    public String getDomain()
    {
        return this.domain;
    }

    /**
     * @param domain the domain of the instances, recorded for tracking only
     */
    public void setDomain(String domain)
    {
        this.domain = domain;
    }

    /**
     * @return the user limit per instance, {@code -1} for unlimited; {@code null} for the policy default
     */
    public Long getMaxUserCount()
    {
        return this.maxUserCount;
    }

    /**
     * @param maxUserCount the user limit per instance, {@code -1} for unlimited; {@code null} for the policy default
     */
    public void setMaxUserCount(Long maxUserCount)
    {
        this.maxUserCount = maxUserCount;
    }

    /**
     * @return the license duration, one of the policy's durations for the environment (e.g. {@code 1y}); {@code null}
     *     for the first one
     */
    public String getDuration()
    {
        return this.duration;
    }

    /**
     * @param duration the license duration, one of the policy's durations for the environment (e.g. {@code 1y}); {@code
     *     null} for the first one
     */
    public void setDuration(String duration)
    {
        this.duration = duration;
    }

    /**
     * @return the support level, one of the policy's support levels; {@code null} for the policy default
     */
    public String getSupport()
    {
        return this.support;
    }

    /**
     * @param support the support level, one of the policy's support levels; {@code null} for the policy default
     */
    public void setSupport(String support)
    {
        this.support = support;
    }

    /**
     * @return who is written into the licenses as licensee ({@code partner}, {@code customer} or {@code none}); {@code
     *     null} for the policy default
     */
    public String getLicenseeMode()
    {
        return this.licenseeMode;
    }

    /**
     * @param licenseeMode who is written into the licenses as licensee ({@code partner}, {@code customer} or {@code
     *     none}); {@code null} for the policy default
     */
    public void setLicenseeMode(String licenseeMode)
    {
        this.licenseeMode = licenseeMode;
    }

    /**
     * @return the customer details, required with the {@code customer} licensee mode and refused with the others
     */
    public LicenseeDetails getLicensee()
    {
        return this.licensee;
    }

    /**
     * @param licensee the customer details, required with the {@code customer} licensee mode and refused with the
     *     others
     */
    public void setLicensee(LicenseeDetails licensee)
    {
        this.licensee = licensee;
    }
}
