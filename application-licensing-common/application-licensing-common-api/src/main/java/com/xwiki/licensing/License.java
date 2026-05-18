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
package com.xwiki.licensing;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.xwiki.instance.InstanceId;

/**
 * A software license instance.
 *
 * @version $Id$
 */
public class License implements Comparable<License>
{
    /**
     * The key used to store the licensee first name.
     */
    public static final String LICENSEE_FIRST_NAME = "firstName";

    /**
     * The key used to store the licensee last name.
     */
    public static final String LICENSEE_LAST_NAME = "lastName";

    /**
     * The key used to store the licensee email.
     */
    public static final String LICENSEE_EMAIL = "email";

    /**
     * The key used to store the licensee issue date.
     */
    public static final String LICENSE_ISSUE_DATE = "issueDate";

    /**
     * An empty constant object used when you need to explicitly assign no license.
     */
    public static final License UNLICENSED = new License();

    static {
        UNLICENSED.setId(new LicenseId("00000000-0000-0000-0000-000000000000"));
        UNLICENSED.setExpirationDate(0L);
        UNLICENSED.setMaxUserCount(0L);
    }

    private LicenseId licenseId;

    private Collection<LicensedFeatureId> featureIds;

    private Collection<InstanceId> instanceIds;

    private long expirationDate = Long.MAX_VALUE;

    private long maxUserCount = Long.MAX_VALUE;

    private LicenseType licenseType = LicenseType.FREE;

    private Map<String, String> licensee;

    /**
     * Default constructor.
     */
    public License()
    {
        // Empty license construction.
    }

    /**
     * Copy constructor.
     *
     * @param license license object to be copied.
     */
    public License(License license)
    {
        this.setId(license.getId());
        this.setType(license.getType());
        this.setFeatureIds(license.getFeatureIds());
        this.setInstanceIds(license.getInstanceIds());
        this.setExpirationDate(license.getExpirationDate());
        this.setMaxUserCount(license.getMaxUserCount());
        this.setLicensee(license.getLicensee());
    }

    /**
     * @return a unique identifier for this license ("00000000-0000-0000-0000-000000000000").
     */
    public LicenseId getId()
    {
        if (licenseId == null) {
            licenseId = new LicenseId();
        }
        return licenseId;
    }

    /**
     * Set the unique identifier for this license object.
     *
     * @param id the identifier to assign.
     */
    public void setId(LicenseId id)
    {
        this.licenseId = id;
    }

    /**
     * @return a collection of feature ids that this license could be applied to.
     */
    public Collection<LicensedFeatureId> getFeatureIds()
    {
        return featureIds != null ? Collections.unmodifiableCollection(featureIds) : Collections.emptySet();
    }

    /**
     * Set the set of features covered by this license.
     *
     * @param ids a set of feature ids to be assigned.
     */
    public void setFeatureIds(Collection<LicensedFeatureId> ids)
    {
        if (ids != null) {
            featureIds = new HashSet<>();
            featureIds.addAll(ids);
        } else {
            featureIds = null;
        }
    }

    /**
     * Add a feature to the actual feature set covered by this license.
     *
     * @param id the feature to be added.
     */
    public void addFeatureId(LicensedFeatureId id)
    {
        if (featureIds == null) {
            featureIds = new HashSet<>();
        }
        featureIds.add(id);
    }

    /**
     * @return a collection of instance ids that this license could be applied to.
     */
    public Collection<InstanceId> getInstanceIds()
    {
        return instanceIds != null ? Collections.unmodifiableCollection(instanceIds) : Collections.emptySet();
    }

    /**
     * Set the set of instances this license could be applied to.
     *
     * @param ids a set of InstanceId to be assigned.
     */
    public void setInstanceIds(Collection<InstanceId> ids)
    {
        if (ids != null) {
            instanceIds = new HashSet<>();
            instanceIds.addAll(ids);
        } else {
            instanceIds = null;
        }
    }

    /**
     * Add an instance for which this license is applicable.
     *
     * @param id the identifier of the instance to be added.
     */
    public void addInstanceId(InstanceId id)
    {
        if (instanceIds == null) {
            instanceIds = new HashSet<>();
        }
        instanceIds.add(id);
    }

    /**
     * Check if this license is applicable for a given instance.
     *
     * @param id the identifier of the instance to be checked.
     * @return true if this license is applicable for the given instance.
     */
    public boolean isApplicableTo(InstanceId id)
    {
        return this.instanceIds != null && this.instanceIds.contains(id);
    }

    /**
     * @return the expiration date as a unix timestamp.
     */
    public long getExpirationDate()
    {
        return expirationDate;
    }

    /**
     * Set the expiration date of this license.
     *
     * @param date the date to be assigned. Use Long.MAX_VALUE for an infinite license.
     */
    public void setExpirationDate(Long date)
    {
        expirationDate = date;
    }

    /**
     * @return the maximum number of user, that the wiki this license is applied to, could have.
     */
    public long getMaxUserCount()
    {
        return maxUserCount;
    }

    /**
     * Set the maximum number of user covered by this license.
     *
     * @param userCount the maximum number of user to be assigned. Use Long.MAX_VALUE for no limitation.
     */
    public void setMaxUserCount(long userCount)
    {
        maxUserCount = userCount;
    }

    /**
     * @return the type of this license.
     */
    public LicenseType getType()
    {
        return licenseType;
    }

    /**
     * Set the license type.
     *
     * @param type the license type to be assigned.
     */
    public void setType(LicenseType type)
    {
        licenseType = type;
    }

    /**
     * @return the licensee information.
     */
    public Map<String, String> getLicensee()
    {
        return licensee != null ? Collections.unmodifiableMap(licensee) : Collections.emptyMap();
    }

    /**
     * Set licensee information.
     *
     * @param licensee a map of licensee information to be assigned.
     */
    public void setLicensee(Map<String, String> licensee)
    {
        if (licensee != null) {
            this.licensee = new LinkedHashMap<>();
            this.licensee.putAll(licensee);
        } else {
            this.licensee = null;
        }
    }

    /**
     * Add a licensee information (or change it).
     *
     * @param key the key of the licensee information.
     * @param value the value of the licensee information.
     */
    public void addLicenseeInfo(String key, String value)
    {
        if (licensee == null) {
            licensee = new LinkedHashMap<>();
        }
        this.licensee.put(key, value);
    }

    /**
     * Compare two licenses and return the one that has the larger coverage.
     *
     * @param license1 the first license to be compared
     * @param license2 the second license to be compared
     * @return the license that has the larger coverage, only meaningful if both license cover at least a common target.
     */
    public static License getOptimumLicense(License license1, License license2)
    {
        if (license1 == null) {
            return license2;
        }

        if (license2 == null) {
            return license1;
        }

        if (license1 instanceof SignedLicense) {
            if (!(license2 instanceof SignedLicense)) {
                return license1;
            }
        } else if (license2 instanceof SignedLicense) {
            return license2;
        }

        // The licenses are either both signed or both not signed.

        if (Objects.equals(license1.getId(), license2.getId())) {
            return license1;
        }

        boolean license1HasIssueDate = license1.getLicensee().containsKey(LICENSE_ISSUE_DATE);
        boolean license2HasIssueDate = license2.getLicensee().containsKey(LICENSE_ISSUE_DATE);

        if (license1HasIssueDate && !license2HasIssueDate) {
            return license1;
        }

        if (!license1HasIssueDate && license2HasIssueDate) {
            return license2;
        }

        if (license1HasIssueDate && license2HasIssueDate) {
            long license1IssueDate = Long.parseLong(license1.getLicensee().get(LICENSE_ISSUE_DATE));
            long license2IssueDate = Long.parseLong(license2.getLicensee().get(LICENSE_ISSUE_DATE));

            return license1IssueDate > license2IssueDate ? license1 : license2;
        }

        if (license1.getExpirationDate() != license2.getExpirationDate()) {
            return (license1.getExpirationDate() >= license2.getExpirationDate()) ? license1 : license2;
        }

        if (license1.getMaxUserCount() != license2.getMaxUserCount()) {
            return (license1.getMaxUserCount() >= license2.getMaxUserCount()) ? license1 : license2;
        }

        if (license1.getType() != license2.getType()) {
            return (license1.getType().compareTo(license2.getType()) >= 0) ? license1 : license2;
        }

        if (license1.getFeatureIds().size() != license2.getFeatureIds().size()) {
            return (license1.getFeatureIds().size() >= license2.getFeatureIds().size()) ? license1 : license2;
        }

        if (license1.getInstanceIds().size() != license2.getInstanceIds().size()) {
            return (license1.getInstanceIds().size() >= license2.getInstanceIds().size()) ? license1 : license2;
        }

        return license1;
    }

    /**
     * Compare several licenses and return the one that has the larger coverage.
     *
     * @param licenses a set of licenses to be compared
     * @return the license that has the larger coverage, only meaningful if all licenses cover at least a common target.
     */
    public static License getOptimumLicense(Collection<License> licenses)
    {
        License bestLicense = null;
        for (License license : licenses) {
            bestLicense = getOptimumLicense(bestLicense, license);
        }
        return bestLicense;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }

        License license = (License) obj;

        return new CollectionEqualsBuilder()
            .append(getId(), license.getId())
            .append(getType(), license.getType())
            .append(getInstanceIds(), license.getInstanceIds())
            .append(getFeatureIds(), license.getFeatureIds())
            .append(getExpirationDate(), license.getExpirationDate())
            .append(getMaxUserCount(), license.getMaxUserCount())
            .append(getLicensee(), license.getLicensee())
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    @Override
    public int compareTo(License o)
    {
        return getId().compareTo(getId());
    }

    private static final class CollectionEqualsBuilder extends EqualsBuilder
    {
        @Override
        public EqualsBuilder append(Object lhs, Object rhs)
        {
            if (!this.isEquals()) {
                return this;
            }
            if (lhs == rhs) {
                return this;
            }
            if (!(lhs instanceof Collection) || lhs.getClass() != rhs.getClass()) {
                return super.append(lhs, rhs);
            }
            return append((Collection<?>) lhs, (Collection<?>) rhs);
        }

        private EqualsBuilder append(Collection<?> lhs, Collection<?> rhs)
        {
            if (lhs.size() != rhs.size()) {
                this.setEquals(false);
                return this;
            }

            if (lhs.size() > 0) {
                return appendCollection((Collection<?>) lhs, (Collection<?>) rhs);
            }
            return this;
        }

        private EqualsBuilder appendCollection(Collection<?> lhs, Collection<?> rhs)
        {
            for (Object lobj : lhs) {
                boolean found = false;
                for (Object robj : rhs) {
                    if (lobj.equals(robj)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    this.setEquals(false);
                    return this;
                }
            }
            return this;
        }
    }
}
