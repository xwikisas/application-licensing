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

import com.xwiki.licensing.LicenseType;

/**
 * The details of one license, as stored by Sales and the API on {@code License.Data.<licenseId>} with a
 * {@code License.Code.LicenseDetailsClass} object.
 *
 * @version $Id$
 * @since 1.34
 */
public class LicenseRecord
{
    private String licenseId;

    private LicenseType type;

    private String firstName;

    private String lastName;

    private String email;

    private String instanceId;

    private String featureId;

    private List<String> dependentFeatureIds = new ArrayList<>();

    private String extensionVersion;

    private String support;

    private Long maxUserCount;

    private Date issueDate;

    private Date expirationDate;

    private boolean hasLicense;

    private boolean sendEmails;

    /**
     * @return the license id, which is also the name of the record page
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
     * @return the license type
     */
    public LicenseType getType()
    {
        return this.type;
    }

    /**
     * @param type the license type
     */
    public void setType(LicenseType type)
    {
        this.type = type;
    }

    /**
     * @return the licensee first name
     */
    public String getFirstName()
    {
        return this.firstName;
    }

    /**
     * @param firstName the licensee first name
     */
    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }

    /**
     * @return the licensee last name
     */
    public String getLastName()
    {
        return this.lastName;
    }

    /**
     * @param lastName the licensee last name
     */
    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }

    /**
     * @return the licensee email
     */
    public String getEmail()
    {
        return this.email;
    }

    /**
     * @param email the licensee email
     */
    public void setEmail(String email)
    {
        this.email = email;
    }

    /**
     * @return the licensed instance id
     */
    public String getInstanceId()
    {
        return this.instanceId;
    }

    /**
     * @param instanceId the licensed instance id
     */
    public void setInstanceId(String instanceId)
    {
        this.instanceId = instanceId;
    }

    /**
     * @return the licensed feature (extension id, without version)
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
     * @return the paid dependencies of the feature, also covered by the license
     */
    public List<String> getDependentFeatureIds()
    {
        return this.dependentFeatureIds;
    }

    /**
     * @param dependentFeatureIds the paid dependencies of the feature
     */
    public void setDependentFeatureIds(List<String> dependentFeatureIds)
    {
        this.dependentFeatureIds = new ArrayList<>(dependentFeatureIds);
    }

    /**
     * @return the version of the feature used to compute the dependencies
     */
    public String getExtensionVersion()
    {
        return this.extensionVersion;
    }

    /**
     * @param extensionVersion the version of the feature used to compute the dependencies
     */
    public void setExtensionVersion(String extensionVersion)
    {
        this.extensionVersion = extensionVersion;
    }

    /**
     * @return the support level
     */
    public String getSupport()
    {
        return this.support;
    }

    /**
     * @param support the support level
     */
    public void setSupport(String support)
    {
        this.support = support;
    }

    /**
     * @return the user limit, negative for unlimited
     */
    public Long getMaxUserCount()
    {
        return this.maxUserCount;
    }

    /**
     * @param maxUserCount the user limit, negative for unlimited
     */
    public void setMaxUserCount(Long maxUserCount)
    {
        this.maxUserCount = maxUserCount;
    }

    /**
     * @return when the license was issued
     */
    public Date getIssueDate()
    {
        return this.issueDate;
    }

    /**
     * @param issueDate when the license was issued
     */
    public void setIssueDate(Date issueDate)
    {
        this.issueDate = issueDate;
    }

    /**
     * @return when the license expires
     */
    public Date getExpirationDate()
    {
        return this.expirationDate;
    }

    /**
     * @param expirationDate when the license expires
     */
    public void setExpirationDate(Date expirationDate)
    {
        this.expirationDate = expirationDate;
    }

    /**
     * @return whether the signed license was generated and stored
     */
    public boolean hasLicense()
    {
        return this.hasLicense;
    }

    /**
     * @param hasLicense whether the signed license was generated and stored
     */
    public void setHasLicense(boolean hasLicense)
    {
        this.hasLicense = hasLicense;
    }

    /**
     * @return whether the licensee gets the license emails (e.g. the expiration reminders)
     */
    public boolean isSendEmails()
    {
        return this.sendEmails;
    }

    /**
     * @param sendEmails whether the licensee gets the license emails
     */
    public void setSendEmails(boolean sendEmails)
    {
        this.sendEmails = sendEmails;
    }
}
