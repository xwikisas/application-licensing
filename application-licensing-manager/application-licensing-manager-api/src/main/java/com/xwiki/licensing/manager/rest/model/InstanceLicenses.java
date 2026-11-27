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
 * The licenses of one instance.
 *
 * @version $Id$
 * @since 1.34
 */
@Unstable
public class InstanceLicenses
{
    private String instanceId;

    private String environment;

    private String domain;

    private String product;

    private String expirationDate;

    private List<LicenseKey> licenses;

    private String keys;

    /**
     * @return the instance id
     */
    public String getInstanceId()
    {
        return this.instanceId;
    }

    /**
     * @param instanceId the instance id
     */
    public void setInstanceId(String instanceId)
    {
        this.instanceId = instanceId;
    }

    /**
     * @return the environment of the instance, {@code prod} or {@code test}
     */
    public String getEnvironment()
    {
        return this.environment;
    }

    /**
     * @param environment the environment of the instance, {@code prod} or {@code test}
     */
    public void setEnvironment(String environment)
    {
        this.environment = environment;
    }

    /**
     * @return the domain of the instance, or {@code null}
     */
    public String getDomain()
    {
        return this.domain;
    }

    /**
     * @param domain the domain of the instance, or {@code null}
     */
    public void setDomain(String domain)
    {
        this.domain = domain;
    }

    /**
     * @return the product licensed on the instance
     */
    public String getProduct()
    {
        return this.product;
    }

    /**
     * @param product the product licensed on the instance
     */
    public void setProduct(String product)
    {
        this.product = product;
    }

    /**
     * @return the earliest expiration date of the keys in {@link #getKeys()}, i.e. when the instance needs a renewal
     *     (ISO-8601, UTC)
     */
    public String getExpirationDate()
    {
        return this.expirationDate;
    }

    /**
     * @param expirationDate the earliest expiration date of the keys in {@link #getKeys()}, i.e. when the instance
     *     needs a renewal (ISO-8601, UTC)
     */
    public void setExpirationDate(String expirationDate)
    {
        this.expirationDate = expirationDate;
    }

    /**
     * @return the licenses, the most recently issued first
     */
    public List<LicenseKey> getLicenses()
    {
        return this.licenses;
    }

    /**
     * @param licenses the licenses, the most recently issued first
     */
    public void setLicenses(List<LicenseKey> licenses)
    {
        this.licenses = licenses;
    }

    /**
     * @return the keys of the latest license of each feature, separated by commas, ready to paste into a
     *     comma-separated setting
     */
    public String getKeys()
    {
        return this.keys;
    }

    /**
     * @param keys the keys of the latest license of each feature, separated by commas, ready to paste into a
     *     comma-separated setting
     */
    public void setKeys(String keys)
    {
        this.keys = keys;
    }
}
