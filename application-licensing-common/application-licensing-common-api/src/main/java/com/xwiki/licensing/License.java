package com.xwiki.licensing;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.xwiki.instance.InstanceId;

/**
 * A software license instance.
 *
 * @version $Id$
 */
public class License implements Comparable<License>
{
    public static final License UNLICENSED = new License();
    public static final boolean INSTANCE_ID_EQUALS_BROKEN = new InstanceId() != new InstanceId();

    static {
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
    public License() {
        // Empty license construction.
    }

    /**
     * Copy constructor.
     *
     * @param license license object to be copied.
     */
    public License(License license) {
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

    public void setFeatureIds(Collection<LicensedFeatureId> ids)
    {
        if (ids != null) {
            featureIds = new HashSet<>();
            featureIds.addAll(ids);
        } else {
            featureIds = null;
        }
    }

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

    public void setInstanceIds(Collection<InstanceId> ids)
    {
        if (ids != null) {
            instanceIds = new HashSet<>();
            instanceIds.addAll(ids);
        } else {
            instanceIds = null;
        }
    }

    public void addInstanceId(InstanceId id)
    {
        if (instanceIds == null) {
            instanceIds = new HashSet<>();
        }
        instanceIds.add(id);
    }

    public boolean isApplicableTo(InstanceId id)
    {
        if (!INSTANCE_ID_EQUALS_BROKEN) {
            return instanceIds.contains(id);
        } else if (id == null || id.getInstanceId() == null) {
            return false;
        } else {
            for (InstanceId instanceId : getInstanceIds()) {
                if (id.getInstanceId().equals(instanceId.getInstanceId())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return the expiration date as a unix timestamp.
     */
    public long getExpirationDate()
    {
        return expirationDate;
    }

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

    public void setLicensee(Map<String, String> licensee)
    {
        if (licensee != null) {
            this.licensee = new HashMap<>();
            this.licensee.putAll(licensee);
        } else {
            this.licensee = null;
        }
    }

    public void addLicenseeInfo(String key, String value)
    {
        if (licensee == null) {
            licensee = new HashMap<>();
        }
        this.licensee.put(key, value);
    }

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

        if (license1.getExpirationDate() != license2.getExpirationDate()) {
            return (license1.getExpirationDate() > license2.getExpirationDate()) ? license1 : license2;
        }

        if (license1.getMaxUserCount() != license2.getMaxUserCount()) {
            return (license1.getMaxUserCount() > license2.getMaxUserCount()) ? license1 : license2;
        }

        if (license1.getType() != license2.getType()) {
            return (license1.getType().compareTo(license2.getType()) > 0) ? license1 : license2;
        }

        if (license1.getFeatureIds().size() != license2.getFeatureIds().size()) {
            return (license1.getFeatureIds().size() > license2.getFeatureIds().size()) ? license1 : license2;
        }

        if (license1.getInstanceIds().size() != license2.getInstanceIds().size()) {
            return (license1.getInstanceIds().size() > license2.getInstanceIds().size()) ? license1 : license2;
        }

        return license1;
    }

    public static License getOptimumLicense(Collection<License> licenses)
    {
        License bestLicense = null;
        for(License license : licenses) {
            bestLicense = getOptimumLicense(bestLicense, license);
        }
        return bestLicense;
    }

    @Override
    public boolean equals(Object obj) {
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
    public int compareTo(License o)
    {
        return getId().compareTo(getId());
    }

    private static class CollectionEqualsBuilder extends EqualsBuilder
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
            Collection<?> clhs = (Collection<?>) lhs;
            Collection<?> crhs = (Collection<?>) lhs;
            if (clhs.size() != crhs.size()) {
                this.setEquals(false);
                return this;
            }
            if (clhs.size() == Integer.MAX_VALUE) {
                if (!clhs.containsAll(crhs) || !crhs.containsAll(clhs)) {
                    this.setEquals(false);
                }
                return this;
            }
            if (!clhs.containsAll(crhs)) {
                this.setEquals(false);
            }
            return this;
        }
    }
}
