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

import java.util.List;

/**
 * A request validated against the policy: every value is resolved (request or policy default) and allowed.
 *
 * @version $Id$
 * @since 1.34
 */
public final class IssuingPlan
{
    /**
     * The user count written into unlimited licenses.
     */
    public static final long UNLIMITED = -1L;

    private Policy policy;

    private Product product;

    private Environment environment;

    private LicenseDuration duration;

    private String support;

    private Policy.Licensee licensee;

    private long maxUserCount;

    private String instanceId;

    private int count;

    private String domain;

    private InstanceRecord existingInstance;

    /**
     * @return the policy of the calling account
     */
    public Policy getPolicy()
    {
        return this.policy;
    }

    void setPolicy(Policy policy)
    {
        this.policy = policy;
    }

    /**
     * @return the product to license
     */
    public Product getProduct()
    {
        return this.product;
    }

    void setProduct(Product product)
    {
        this.product = product;
    }

    /**
     * @return the environment of the instances
     */
    public Environment getEnvironment()
    {
        return this.environment;
    }

    void setEnvironment(Environment environment)
    {
        this.environment = environment;
    }

    /**
     * @return the license duration
     */
    public LicenseDuration getDuration()
    {
        return this.duration;
    }

    void setDuration(LicenseDuration duration)
    {
        this.duration = duration;
    }

    /**
     * @return the support level, or {@code null}
     */
    public String getSupport()
    {
        return this.support;
    }

    void setSupport(String support)
    {
        this.support = support;
    }

    /**
     * @return the licensee written into the licenses (its fields are {@code null} in the {@code none} mode)
     */
    public Policy.Licensee getLicensee()
    {
        return this.licensee;
    }

    void setLicensee(Policy.Licensee licensee)
    {
        this.licensee = licensee;
    }

    /**
     * @return the user limit per instance, {@link #UNLIMITED} for unlimited
     */
    public long getMaxUserCount()
    {
        return this.maxUserCount;
    }

    void setMaxUserCount(long maxUserCount)
    {
        this.maxUserCount = maxUserCount;
    }

    /**
     * @return the instance sent by the client, or {@code null} to generate {@link #getCount()} new instance ids
     */
    public String getInstanceId()
    {
        return this.instanceId;
    }

    void setInstanceId(String instanceId)
    {
        this.instanceId = instanceId;
    }

    /**
     * @return the number of instances to license
     */
    public int getCount()
    {
        return this.count;
    }

    void setCount(int count)
    {
        this.count = count;
    }

    /**
     * @return the domain declared by the client, or {@code null}
     */
    public String getDomain()
    {
        return this.domain;
    }

    void setDomain(String domain)
    {
        this.domain = domain;
    }

    /**
     * @return the record of the instance to renew, or {@code null} for new instances
     */
    public InstanceRecord getExistingInstance()
    {
        return this.existingInstance;
    }

    void setExistingInstance(InstanceRecord existingInstance)
    {
        this.existingInstance = existingInstance;
    }

    /**
     * @return whether the request renews an instance owned by the account
     */
    public boolean isRenewal()
    {
        return this.existingInstance != null;
    }

    /**
     * @return the features to license on each instance: all the product features for a new instance, the renewed
     *     ones for a renewal
     */
    public List<String> getFeatureIds()
    {
        return isRenewal() ? this.product.getRenewedFeatureIds() : this.product.getFeatureIds();
    }

    /**
     * @return the number of licenses the request issues
     */
    public int getLicenseCount()
    {
        return this.count * getFeatureIds().size();
    }
}
