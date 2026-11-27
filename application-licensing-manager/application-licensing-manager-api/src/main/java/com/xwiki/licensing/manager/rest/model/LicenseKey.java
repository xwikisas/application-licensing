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
 * A license issued for an instance, with its key.
 *
 * @version $Id$
 * @since 1.34
 */
@Unstable
public class LicenseKey
{
    private String licenseId;

    private String instanceId;

    private String featureId;

    private String type;

    private String issueDate;

    private String expirationDate;

    private Long maxUserCount;

    private String key;

    /**
     * @return the license id
     */
    public String getLicenseId()
    {
        return this.licenseId;
    }

    /**
     * @param licenseId the license id
     */
    public void setLicenseId(String licenseId)
    {
        this.licenseId = licenseId;
    }

    /**
     * @return the licensed instance
     */
    public String getInstanceId()
    {
        return this.instanceId;
    }

    /**
     * @param instanceId the licensed instance
     */
    public void setInstanceId(String instanceId)
    {
        this.instanceId = instanceId;
    }

    /**
     * @return the licensed feature
     */
    public String getFeatureId()
    {
        return this.featureId;
    }

    /**
     * @param featureId the licensed feature
     */
    public void setFeatureId(String featureId)
    {
        this.featureId = featureId;
    }

    /**
     * @return the license type ({@code PAID} for production, {@code TRIAL} for test)
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * @param type the license type ({@code PAID} for production, {@code TRIAL} for test)
     */
    public void setType(String type)
    {
        this.type = type;
    }

    /**
     * @return when the license was issued (ISO-8601, UTC)
     */
    public String getIssueDate()
    {
        return this.issueDate;
    }

    /**
     * @param issueDate when the license was issued (ISO-8601, UTC)
     */
    public void setIssueDate(String issueDate)
    {
        this.issueDate = issueDate;
    }

    /**
     * @return when the license expires (ISO-8601, UTC)
     */
    public String getExpirationDate()
    {
        return this.expirationDate;
    }

    /**
     * @param expirationDate when the license expires (ISO-8601, UTC)
     */
    public void setExpirationDate(String expirationDate)
    {
        this.expirationDate = expirationDate;
    }

    /**
     * @return the user limit, {@code -1} for unlimited
     */
    public Long getMaxUserCount()
    {
        return this.maxUserCount;
    }

    /**
     * @param maxUserCount the user limit, {@code -1} for unlimited
     */
    public void setMaxUserCount(Long maxUserCount)
    {
        this.maxUserCount = maxUserCount;
    }

    /**
     * @return the license key: the signed license, Base64 encoded on a single line; {@code null} if the signed license
     *     file is missing
     */
    public String getKey()
    {
        return this.key;
    }

    /**
     * @param key the license key: the signed license, Base64 encoded on a single line; {@code null} if the signed
     *     license file is missing
     */
    public void setKey(String key)
    {
        this.key = key;
    }
}
