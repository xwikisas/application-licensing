package com.xwiki.licensing.internal.enforcer;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.SecurityEntryReader;
import org.xwiki.security.authorization.SecurityRule;
import org.xwiki.security.authorization.SecurityRuleEntry;
import org.xwiki.security.authorization.cache.SecurityCacheLoader;
import org.xwiki.security.authorization.internal.AbstractSecurityRuleEntry;

import com.xwiki.licensing.License;

/**
 * Wrapper of the security entry reader that implement licensing injection.
 *
 * @version $Id$
 */
@Component
@Named(LicensingSecurityEntryReader.HINT)
@Singleton
public class LicensingSecurityEntryReader implements SecurityEntryReader, Initializable, Disposable
{
    /**
     * Hint of this component.
     */
    public static final String HINT = "licensing";

    private static final String SECURITY_ENTRY_READER_FIELD = "securityEntryReader";

    @Inject
    private Logger logger;

    @Inject
    private SecurityCacheLoader securityCacheLoader;

    /**
     * The default security entry reader.
     */
    @Inject
    private SecurityEntryReader securityEntryReader;

    @Inject
    private EntityLicenseManager licenseManager;

    /**
     * Internal implementation of the SecurityRuleEntry.
     */
    private final class InternalSecurityRuleEntry extends AbstractSecurityRuleEntry implements LicensedEntity
    {
        /** Reference of the related entity. */
        private final SecurityReference reference;

        /** The list of objects. */
        private final Collection<SecurityRule> rules;

        /** The license attached to this entity. **/
        private final License license;

        /**
         * @param entry the original entry to licence
         * @param license the licence to be associated with this entry
         */
        private InternalSecurityRuleEntry(SecurityRuleEntry entry, License license)
        {
            this.reference = entry.getReference();
            this.rules = entry.getRules();
            this.license = license;
        }

        /**
         * @return the reference of the related entity
         */
        @Override
        public SecurityReference getReference()
        {
            return reference;
        }

        /**
         * @return all rules available for this entity
         */
        @Override
        public Collection<SecurityRule> getRules()
        {
            return rules;
        }

        /**
         * @return the license
         */
        @Override
        public License getLicense()
        {
            return license;
        }
    }

    @Override
    public void initialize() throws InitializationException
    {
        if (!LicensingUtils.isPristineImpl(licenseManager)) {
            licenseManager = null;
        }

        logger.debug("Replacing [{}] with myself.", this.securityEntryReader);
        // Force this licensing reader to be returned by the provider
        ReflectionUtils.setFieldValue(securityCacheLoader, SECURITY_ENTRY_READER_FIELD, this);
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        if (LicensingUtils.getFieldValue(securityCacheLoader, SECURITY_ENTRY_READER_FIELD) == this) {
            logger.debug("Replacing myself by [{}].", this.securityEntryReader);
            // Restore the original reader
            ReflectionUtils.setFieldValue(securityCacheLoader, SECURITY_ENTRY_READER_FIELD, securityEntryReader);
        }
    }

    @Override
    public SecurityRuleEntry read(SecurityReference securityReference) throws AuthorizationException
    {
        EntityReference ref = securityReference.getOriginalReference();

        if (ref != null) {
            logger.debug("Checking license applicability for [{}].", ref);
            License license = (licenseManager != null) ? licenseManager.get(ref) : License.UNLICENSED;

            if (license != null) {
                return new InternalSecurityRuleEntry(securityEntryReader.read(securityReference), license);
            }
        }

        return securityEntryReader.read(securityReference);
    }
}
