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
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.internal.issuing.model.InstanceClassInitializer;
import com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer;
import com.xwiki.licensing.internal.issuing.model.ProductClassInitializer;

import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.ALERT_RECIPIENTS;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.ALLOWED_ENVIRONMENTS;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.CONTRACT_RENEWAL_DATE;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.DEFAULT_SUPPORT;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.DEFAULT_USER_COUNT;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.ENABLED;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.LICENSEE_EMAIL;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.LICENSEE_FIRST_NAME;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.LICENSEE_LAST_NAME;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.LICENSEE_MODE;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.MAX_ACTIVE_PROD_INSTANCES;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.MAX_ACTIVE_TEST_INSTANCES;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.MAX_BATCH_SIZE;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.MAX_ISSUANCES_PER_DAY;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.MAX_TOTAL_USERS;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.MAX_USER_COUNT;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.MAY_CHOOSE_INSTANCE_ID;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.PRODUCTS;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.PROD_DURATIONS;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.PROD_EXPIRATION_LIMIT;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.QUOTA_MODE;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.QUOTA_PERIOD;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.SALES_NOTIFICATION;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.SEND_EXPIRATION_REMINDERS;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.SUPPORT_LEVELS;
import static com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer.TEST_DURATIONS;

/**
 * Reads the configuration of the License Issuing API from the main wiki: the policies, the products and the instance
 * records, plus the user the records are saved as.
 *
 * @version $Id$
 * @since 1.34
 */
@Component(roles = ApiConfiguration.class)
@Singleton
public class ApiConfiguration
{
    /**
     * The page holding the license generation configuration (signing passwords, record author).
     */
    public static final LocalDocumentReference LICENSE_CONFIG =
        new LocalDocumentReference(LicenseRecordStore.CODE_SPACE, "LicenseConfig");

    /**
     * The class of the license generation configuration.
     */
    public static final LocalDocumentReference LICENSE_CONFIG_CLASS =
        new LocalDocumentReference(LicenseRecordStore.CODE_SPACE, "LicenseConfigurationClass");

    /**
     * The property of {@link #LICENSE_CONFIG_CLASS} naming the user the API records are saved as.
     */
    public static final String RECORD_AUTHOR = "apiRecordAuthor";

    private static final String LIST_SEPARATOR = ",";

    private static final String VERSION_SEPARATOR = "=";

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    /**
     * @param account a user
     * @return the policy of the account, or empty if it has none (or if its policy page names another account)
     * @throws XWikiException if the policy page can't be read
     */
    public Optional<Policy> getPolicy(DocumentReference account) throws XWikiException
    {
        Optional<BaseObject> object = getObject(PolicyClassInitializer.POLICIES_SPACE, account.getName(),
            PolicyClassInitializer.CLASS_REFERENCE);
        if (object.isEmpty()) {
            return Optional.empty();
        }
        BaseObject policy = object.get();
        // The page name only locates the policy. The account property says who it applies to, so that a user with
        // the same name on another wiki, or a misnamed page, never gets a policy by accident.
        DocumentReference configuredAccount = resolveUser(policy.getStringValue(PolicyClassInitializer.ACCOUNT));
        if (!account.equals(configuredAccount)) {
            this.logger.warn("Ignoring the policy [{}] because it applies to [{}], not to [{}].",
                policy.getDocumentReference(), configuredAccount, account);
            return Optional.empty();
        }

        return Optional.of(Policy.builder(account)
            .enabled(getBoolean(policy, ENABLED))
            .products(getList(policy, PRODUCTS))
            .allowedEnvironments(getEnvironments(policy))
            .durations(Environment.PROD, normalize(getList(policy, PROD_DURATIONS)))
            .durations(Environment.TEST, normalize(getList(policy, TEST_DURATIONS)))
            .prodExpirationLimit(policy.getDateValue(PROD_EXPIRATION_LIMIT))
            .userCounts(getLong(policy, MAX_USER_COUNT), getLong(policy, DEFAULT_USER_COUNT))
            .mayChooseInstanceId(Boolean.TRUE.equals(getBoolean(policy, MAY_CHOOSE_INSTANCE_ID)))
            .maxBatchSize(getInteger(policy, MAX_BATCH_SIZE))
            .licensee(getEnum(policy, LICENSEE_MODE, Policy.LicenseeMode.class),
                new Policy.Licensee(getString(policy, LICENSEE_FIRST_NAME), getString(policy, LICENSEE_LAST_NAME),
                    getString(policy, LICENSEE_EMAIL)))
            .support(getList(policy, SUPPORT_LEVELS), getString(policy, DEFAULT_SUPPORT))
            .notifications(Boolean.TRUE.equals(getBoolean(policy, SEND_EXPIRATION_REMINDERS)),
                getString(policy, SALES_NOTIFICATION))
            .maxIssuancesPerDay(getInteger(policy, MAX_ISSUANCES_PER_DAY))
            .volumeLimits(getInteger(policy, MAX_ACTIVE_PROD_INSTANCES), getInteger(policy, MAX_ACTIVE_TEST_INSTANCES),
                getLong(policy, MAX_TOTAL_USERS))
            .quota(getEnum(policy, QUOTA_MODE, Policy.QuotaMode.class), getString(policy, QUOTA_PERIOD),
                policy.getDateValue(CONTRACT_RENEWAL_DATE))
            .alertRecipients(getUserList(policy, ALERT_RECIPIENTS))
            .build());
    }

    /**
     * @param productId a product id
     * @return the product, or empty if it isn't configured
     * @throws XWikiException if the product page can't be read
     */
    public Optional<Product> getProduct(String productId) throws XWikiException
    {
        Optional<BaseObject> object =
            getObject(ProductClassInitializer.PRODUCTS_SPACE, productId, ProductClassInitializer.CLASS_REFERENCE);
        if (object.isEmpty()) {
            return Optional.empty();
        }
        BaseObject product = object.get();
        return Optional.of(new Product(productId, getList(product, ProductClassInitializer.FEATURE_IDS),
            parseVersions(product.getLargeStringValue(ProductClassInitializer.EXTENSION_VERSIONS)),
            getList(product, ProductClassInitializer.RENEWED_FEATURE_IDS)));
    }

    /**
     * @param instanceId an instance id
     * @return the instance record, or empty if the instance id was never used through the API
     * @throws XWikiException if the instance record page can't be read
     */
    public Optional<InstanceRecord> getInstance(String instanceId) throws XWikiException
    {
        Optional<BaseObject> object = getObject(InstanceClassInitializer.INSTANCES_SPACE, instanceId,
            InstanceClassInitializer.CLASS_REFERENCE);
        if (object.isEmpty()) {
            return Optional.empty();
        }
        BaseObject instance = object.get();
        return Optional.of(new InstanceRecord(instanceId,
            resolveUser(instance.getStringValue(InstanceClassInitializer.ACCOUNT)),
            Environment.fromValue(instance.getStringValue(InstanceClassInitializer.ENVIRONMENT)).orElse(null),
            getString(instance, InstanceClassInitializer.DOMAIN),
            getString(instance, InstanceClassInitializer.PRODUCT),
            getEnum(instance, InstanceClassInitializer.ORIGIN, InstanceRecord.Origin.class),
            instance.getDateValue(InstanceClassInitializer.CREATED_DATE)));
    }

    /**
     * The license, policy and instance records written by the API are saved as this user, because the calling
     * account has no rights on the {@code License} space.
     *
     * @return the user configured on {@code License.Code.LicenseConfig}, or else the last author of that page (the
     *     store admin who configured license generation), or {@code null} if that page doesn't exist
     * @throws XWikiException if the configuration page can't be read
     */
    public UserReference getRecordAuthor() throws XWikiException
    {
        XWikiDocument configDocument = getLicenseConfig();
        if (configDocument.isNew()) {
            return null;
        }
        BaseObject config = configDocument.getXObject(LICENSE_CONFIG_CLASS);
        DocumentReference author = config != null ? resolveUser(config.getStringValue(RECORD_AUTHOR)) : null;
        return author != null ? this.userReferenceResolver.resolve(author)
            : configDocument.getAuthors().getEffectiveMetadataAuthor();
    }

    /**
     * @param type a license type
     * @return the password of the key that signs the licenses of that type, configured on
     *     {@code License.Code.LicenseConfig} (the {@code #generateLicense} macro reads the same property)
     * @throws XWikiException if the configuration page can't be read
     */
    public String getSigningPassword(LicenseType type) throws XWikiException
    {
        BaseObject config = getLicenseConfig().getXObject(LICENSE_CONFIG_CLASS);
        return config != null ? config.getStringValue(getPasswordProperty(type)) : null;
    }

    /**
     * @param type a license type
     * @return the name of the {@code License.Code.LicenseConfigurationClass} property holding the key password for
     *     the type (e.g. {@code paidPassword})
     */
    static String getPasswordProperty(LicenseType type)
    {
        return type.name().toLowerCase(Locale.ROOT) + "Password";
    }

    /**
     * @return the main wiki, where the API configuration and the records are stored
     */
    public WikiReference getMainWiki()
    {
        return new WikiReference(this.contextProvider.get().getMainXWiki());
    }

    private XWikiDocument getLicenseConfig() throws XWikiException
    {
        XWikiContext xcontext = this.contextProvider.get();
        return xcontext.getWiki().getDocument(new DocumentReference(LICENSE_CONFIG, getMainWiki()), xcontext);
    }

    private Optional<BaseObject> getObject(List<String> space, String name, LocalDocumentReference classReference)
        throws XWikiException
    {
        if (StringUtils.isBlank(name)) {
            return Optional.empty();
        }
        XWikiContext xcontext = this.contextProvider.get();
        DocumentReference reference = new DocumentReference(getMainWiki().getName(), space, name);
        XWikiDocument document = xcontext.getWiki().getDocument(reference, xcontext);
        return Optional.ofNullable(document.getXObject(classReference));
    }

    private DocumentReference resolveUser(String value)
    {
        return StringUtils.isBlank(value) ? null : this.documentReferenceResolver.resolve(value, getMainWiki());
    }

    private List<DocumentReference> getUserList(BaseObject object, String name)
    {
        List<DocumentReference> users = new ArrayList<>();
        for (String value : StringUtils.split(object.getStringValue(name), LIST_SEPARATOR)) {
            DocumentReference user = resolveUser(value.trim());
            if (user != null) {
                users.add(user);
            }
        }
        return users;
    }

    private static String getString(BaseObject object, String name)
    {
        return StringUtils.trimToNull(object.getStringValue(name));
    }

    @SuppressWarnings("unchecked")
    private static List<String> getList(BaseObject object, String name)
    {
        List<String> values = new ArrayList<>();
        for (Object value : (List<Object>) object.getListValue(name)) {
            String trimmed = StringUtils.trimToNull(String.valueOf(value));
            if (trimmed != null) {
                values.add(trimmed);
            }
        }
        return values;
    }

    private static List<String> normalize(List<String> values)
    {
        List<String> normalized = new ArrayList<>(values.size());
        for (String value : values) {
            normalized.add(value.toLowerCase(Locale.ROOT));
        }
        return normalized;
    }

    private static Set<Environment> getEnvironments(BaseObject object)
    {
        Set<Environment> environments = EnumSet.noneOf(Environment.class);
        for (String value : getList(object, ALLOWED_ENVIRONMENTS)) {
            Environment.fromValue(value).ifPresent(environments::add);
        }
        return environments;
    }

    private static <E extends Enum<E>> E getEnum(BaseObject object, String name, Class<E> type)
    {
        String value = getString(object, name);
        if (value != null) {
            for (E constant : type.getEnumConstants()) {
                if (constant.name().equalsIgnoreCase(value)) {
                    return constant;
                }
            }
        }
        return null;
    }

    private static Object getValue(BaseObject object, String name)
    {
        BaseProperty<?> property = (BaseProperty<?>) object.safeget(name);
        return property != null ? property.getValue() : null;
    }

    private static Boolean getBoolean(BaseObject object, String name)
    {
        Object value = getValue(object, name);
        return value instanceof Number ? ((Number) value).intValue() != 0 : null;
    }

    private static Integer getInteger(BaseObject object, String name)
    {
        Object value = getValue(object, name);
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    private static Long getLong(BaseObject object, String name)
    {
        Object value = getValue(object, name);
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    private static Map<String, String> parseVersions(String text)
    {
        Map<String, String> versions = new LinkedHashMap<>();
        for (String line : StringUtils.split(StringUtils.defaultString(text), "\r\n")) {
            String featureId = StringUtils.trimToNull(StringUtils.substringBefore(line, VERSION_SEPARATOR));
            String version = StringUtils.trimToNull(StringUtils.substringAfter(line, VERSION_SEPARATOR));
            if (featureId != null && version != null) {
                versions.put(featureId, version);
            }
        }
        return versions;
    }
}
