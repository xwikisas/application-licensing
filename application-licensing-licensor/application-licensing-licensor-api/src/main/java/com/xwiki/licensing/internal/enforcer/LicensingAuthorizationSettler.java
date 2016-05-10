package com.xwiki.licensing.internal.enforcer;

import java.util.Collection;
import java.util.Deque;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.AuthorizationSettler;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RuleState;
import org.xwiki.security.authorization.SecurityAccess;
import org.xwiki.security.authorization.SecurityAccessEntry;
import org.xwiki.security.authorization.SecurityRuleEntry;
import org.xwiki.security.authorization.internal.AbstractSecurityAccessEntry;
import org.xwiki.security.authorization.internal.XWikiSecurityAccess;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseValidator;

/**
 * Wrapper of the authorization settler that implement licensing control
 *
 * @version $Id$
 */
@Component
@Named(LicensingAuthorizationSettler.HINT)
@Singleton
public class LicensingAuthorizationSettler implements AuthorizationSettler, Initializable, Disposable
{
    public static final String HINT = "licensing";

    private static final String AUTHORIZATION_SETTLER_FIELD = "authorizationSettler";

    @Inject
    private Logger logger;

    @Inject
    private Provider<AuthorizationSettler> authorizationSettlerProvider;

    @Inject
    private LicenseValidator licenseValidator;

    private AuthorizationSettler authorizationSettler;

    /**
     * Private implementation of the {@link SecurityAccessEntry}.
     */
    private final class InternalSecurityAccessEntry extends AbstractSecurityAccessEntry
    {
        /** User reference. */
        private final UserSecurityReference userReference;

        /** Entity reference. */
        private final SecurityReference reference;

        /** Security access. */
        private final SecurityAccess access;

        /**
         * @param entry an existing entry to swap the access of
         * @param access access
         */
        InternalSecurityAccessEntry(SecurityAccessEntry entry, SecurityAccess access)
        {
            this.userReference = entry.getUserReference();
            this.reference = entry.getReference();
            this.access = access;
        }

        @Override
        public UserSecurityReference getUserReference()
        {
            return this.userReference;
        }

        @Override
        public SecurityAccess getAccess()
        {
            return this.access;
        }

        @Override
        public SecurityReference getReference()
        {
            return this.reference;
        }
    }

    private class InternalSecurityAccess extends XWikiSecurityAccess
    {
        InternalSecurityAccess(SecurityAccess access)
        {
            for (Right right : Right.values()) {
                set(right, access.get(right));
            }
        }

        void set(Right right, RuleState state)
        {
            switch (state) {
                case ALLOW:
                    allow(right);
                    break;
                case DENY:
                    deny(right);
                    break;
                case UNDETERMINED:
                    clear(right);
                    break;
                default:
                    break;
            }
        }

        void allow(Right right)
        {
            allowed.add(right);
            denied.remove(right);
        }

        void deny(Right right)
        {
            denied.add(right);
            allowed.remove(right);
        }

        void clear(Right right)
        {
            allowed.remove(right);
            denied.remove(right);
        }
    }

    @Override
    public void initialize() throws InitializationException
    {
        if (!LicensingUtils.isPristineImpl(licenseValidator)) {
            licenseValidator = null;
        }

        // Get the original authorization settler actually used and configured
        this.authorizationSettler = authorizationSettlerProvider.get();

        // If an old version of myself is already hooking the settler, get the real settler out of it to avoid
        // chaining licensing settlers
        if (authorizationSettler instanceof LicensingAuthorizationSettler) {
            logger.debug("Getting the original authorization settler out of a copy of myself.");
            this.authorizationSettler = ((LicensingAuthorizationSettler) authorizationSettler).authorizationSettler;
        }

        logger.debug("Replacing [{}] with myself.", this.authorizationSettler);
        // Force this licensing settler to be returned by the provider
        ReflectionUtils.setFieldValue(authorizationSettlerProvider, AUTHORIZATION_SETTLER_FIELD, this);
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        if (authorizationSettlerProvider.get() == this) {
            logger.debug("Replacing myself by [{}].", this.authorizationSettler);
            // Restore the original settler
            ReflectionUtils.setFieldValue(authorizationSettlerProvider, AUTHORIZATION_SETTLER_FIELD, authorizationSettler);
        }
    }

    @Override
    public SecurityAccessEntry settle(UserSecurityReference userSecurityReference,
        Collection<GroupSecurityReference> collection, Deque<SecurityRuleEntry> deque)
    {
        SecurityAccessEntry accessEntry = authorizationSettler.settle(userSecurityReference, collection, deque);
        SecurityRuleEntry ruleEntry = deque.peek();

        DocumentReference user = userSecurityReference.getOriginalDocumentReference();
        SecurityReference secRef = deque.peek().getReference();
        EntityReference ref = secRef.getOriginalReference();

        if (ruleEntry instanceof LicensedEntity) {
            License license = ((LicensedEntity) ruleEntry).getLicense();

            InternalSecurityAccess access = new InternalSecurityAccess(accessEntry.getAccess());

            if (licenseValidator == null || !(licenseValidator.isValid(license))) {
                logger.debug("Applying invalid license [{}] to [{}].", license.getId().toString(), ref);
                access.deny(Right.VIEW);
                access.deny(Right.COMMENT);
                access.deny(Right.PROGRAM);
            } else {
                logger.debug("Applying valid license [{}] to [{}].", license.getId().toString(), ref);
            }
            access.deny(Right.EDIT);
            access.deny(Right.DELETE);
            access.deny(Right.ADMIN);
            access.deny(Right.CREATOR);
            accessEntry = new InternalSecurityAccessEntry(accessEntry, access);
        }

        logger.debug("Settling done for user [{}] and entity [{}] with access [{}].",
            user, ref, accessEntry.getAccess());
        return accessEntry;
    }
}
