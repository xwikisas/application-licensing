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

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.internal.issuing.model.InstanceClassInitializer;
import com.xwiki.licensing.internal.issuing.model.PolicyClassInitializer;
import com.xwiki.licensing.internal.issuing.model.ProductClassInitializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ApiConfiguration}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class ApiConfigurationTest
{
    private static final DocumentReference ACCOUNT = new DocumentReference("xwiki", "XWiki", "partner");

    private static final DocumentReference ADMINS = new DocumentReference("xwiki", "XWiki", "XWikiAdminGroup");

    private static final DocumentReference SALES = new DocumentReference("xwiki", "XWiki", "SalesGroup");

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private ApiConfiguration configuration;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void getPolicy() throws Exception
    {
        Date limit = new Date(4102444800000L);
        Date renewal = new Date(4070908800000L);
        savePolicy("partner", policy -> {
            policy.setLargeStringValue(PolicyClassInitializer.ACCOUNT, "XWiki.partner");
            policy.setIntValue(PolicyClassInitializer.ENABLED, 0);
            policy.setStringListValue(PolicyClassInitializer.PRODUCTS, List.of(" bundle ", "", "other"));
            policy.setStringListValue(PolicyClassInitializer.ALLOWED_ENVIRONMENTS, List.of("test", "unknown"));
            policy.setStringListValue(PolicyClassInitializer.PROD_DURATIONS, List.of("1Y", "2y"));
            policy.setStringListValue(PolicyClassInitializer.TEST_DURATIONS, List.of("3m"));
            policy.setDateValue(PolicyClassInitializer.PROD_EXPIRATION_LIMIT, limit);
            policy.setLongValue(PolicyClassInitializer.MAX_USER_COUNT, 500L);
            policy.setLongValue(PolicyClassInitializer.DEFAULT_USER_COUNT, 100L);
            policy.setIntValue(PolicyClassInitializer.MAY_CHOOSE_INSTANCE_ID, 1);
            policy.setIntValue(PolicyClassInitializer.MAX_BATCH_SIZE, 20);
            policy.setStringListValue(PolicyClassInitializer.LICENSEE_MODES, List.of("customer", "unknown", "none"));
            policy.setStringValue(PolicyClassInitializer.DEFAULT_LICENSEE_MODE, "none");
            policy.setStringValue(PolicyClassInitializer.LICENSEE_FIRST_NAME, "Jane");
            policy.setStringValue(PolicyClassInitializer.LICENSEE_LAST_NAME, "Doe");
            policy.setStringValue(PolicyClassInitializer.LICENSEE_EMAIL, "jane@example.com");
            policy.setStringListValue(PolicyClassInitializer.SUPPORT_LEVELS, List.of("gold", "silver"));
            policy.setStringValue(PolicyClassInitializer.DEFAULT_SUPPORT, "silver");
            policy.setIntValue(PolicyClassInitializer.SEND_EXPIRATION_REMINDERS, 1);
            policy.setStringValue(PolicyClassInitializer.SALES_NOTIFICATION, "monthly");
            policy.setIntValue(PolicyClassInitializer.MAX_ISSUANCES_PER_DAY, 50);
            policy.setIntValue(PolicyClassInitializer.MAX_ACTIVE_PROD_INSTANCES, 30);
            policy.setIntValue(PolicyClassInitializer.MAX_ACTIVE_TEST_INSTANCES, 60);
            policy.setLongValue(PolicyClassInitializer.MAX_TOTAL_USERS, 10000L);
            policy.setStringValue(PolicyClassInitializer.QUOTA_MODE, "reject");
            policy.setStringValue(PolicyClassInitializer.QUOTA_PERIOD, "contract");
            policy.setDateValue(PolicyClassInitializer.CONTRACT_RENEWAL_DATE, renewal);
            policy.setLargeStringValue(PolicyClassInitializer.ALERT_RECIPIENTS, "XWiki.XWikiAdminGroup, XWiki.SalesGroup,");
        });

        Policy policy = this.configuration.getPolicy(ACCOUNT).get();

        assertEquals(ACCOUNT, policy.getAccount());
        assertFalse(policy.isEnabled());
        assertEquals(List.of("bundle", "other"), policy.getProducts());
        assertEquals(Set.of(Environment.TEST), policy.getAllowedEnvironments());
        assertEquals(List.of("1y", "2y"), policy.getDurations(Environment.PROD));
        assertEquals(List.of("3m"), policy.getDurations(Environment.TEST));
        assertEquals(limit, policy.getProdExpirationLimit());
        assertEquals(500L, policy.getMaxUserCount());
        assertEquals(100L, policy.getDefaultUserCount());
        assertTrue(policy.mayChooseInstanceId());
        assertEquals(20, policy.getMaxBatchSize());
        assertEquals(Set.of(Policy.LicenseeMode.CUSTOMER, Policy.LicenseeMode.NONE), policy.getLicenseeModes());
        assertEquals(Policy.LicenseeMode.NONE, policy.getDefaultLicenseeMode());
        assertEquals("Jane", policy.getLicensee().getFirstName());
        assertEquals("Doe", policy.getLicensee().getLastName());
        assertEquals("jane@example.com", policy.getLicensee().getEmail());
        assertEquals(List.of("gold", "silver"), policy.getSupportLevels());
        assertEquals("silver", policy.getDefaultSupport());
        assertTrue(policy.isSendExpirationReminders());
        assertEquals("monthly", policy.getSalesNotification());
        assertEquals(50, policy.getMaxIssuancesPerDay());
        assertEquals(30, policy.getMaxActiveProdInstances());
        assertEquals(60, policy.getMaxActiveTestInstances());
        assertEquals(10000L, policy.getMaxTotalUsers());
        assertEquals(Policy.QuotaMode.REJECT, policy.getQuotaMode());
        assertEquals("contract", policy.getQuotaPeriod());
        assertEquals(renewal, policy.getContractRenewalDate());
        assertEquals(List.of(ADMINS, SALES), policy.getAlertRecipients());
    }

    @Test
    void getPolicyAppliesTheDefaults() throws Exception
    {
        savePolicy("partner", policy -> {
            policy.setLargeStringValue(PolicyClassInitializer.ACCOUNT, "XWiki.partner");
            policy.setStringValue(PolicyClassInitializer.DEFAULT_LICENSEE_MODE, "unknown");
        });

        Policy policy = this.configuration.getPolicy(ACCOUNT).get();

        assertTrue(policy.isEnabled());
        assertEquals(List.of("1y"), policy.getDurations(Environment.PROD));
        assertEquals(List.of("6m"), policy.getDurations(Environment.TEST));
        assertNull(policy.getMaxUserCount());
        assertFalse(policy.mayChooseInstanceId());
        assertEquals(10, policy.getMaxBatchSize());
        assertEquals(Set.of(Policy.LicenseeMode.PARTNER), policy.getLicenseeModes());
        assertEquals(Policy.LicenseeMode.PARTNER, policy.getDefaultLicenseeMode());
        assertNull(policy.getLicensee().getEmail());
        assertEquals(25, policy.getMaxIssuancesPerDay());
        assertNull(policy.getMaxActiveProdInstances());
        assertEquals(Policy.QuotaMode.ALERT, policy.getQuotaMode());
        assertTrue(policy.getAlertRecipients().isEmpty());
    }

    @Test
    void getPolicyWhenThereIsNone() throws Exception
    {
        assertEquals(Optional.empty(), this.configuration.getPolicy(ACCOUNT));

        // A page without a policy object.
        saveObject(PolicyClassInitializer.POLICIES_SPACE, "partner",
            new LocalDocumentReference("XWiki", "OtherClass"), object -> { });
        assertEquals(Optional.empty(), this.configuration.getPolicy(ACCOUNT));
    }

    @Test
    void getPolicyIgnoresThePolicyOfAnotherAccount() throws Exception
    {
        savePolicy("partner",
            policy -> policy.setLargeStringValue(PolicyClassInitializer.ACCOUNT, "XWiki.someoneElse"));

        assertEquals(Optional.empty(), this.configuration.getPolicy(ACCOUNT));
        assertEquals("Ignoring the policy [xwiki:License.Code.API.Policies.partner] because it applies to "
            + "[xwiki:XWiki.someoneElse], not to [xwiki:XWiki.partner].", this.logCapture.getMessage(0));

        // A policy without account applies to nobody.
        savePolicy("partner", policy -> { });
        assertEquals(Optional.empty(), this.configuration.getPolicy(ACCOUNT));
        assertEquals("Ignoring the policy [xwiki:License.Code.API.Policies.partner] because it applies to "
            + "[null], not to [xwiki:XWiki.partner].", this.logCapture.getMessage(1));
    }

    @Test
    void getProduct() throws Exception
    {
        saveObject(ProductClassInitializer.PRODUCTS_SPACE, "bundle", ProductClassInitializer.CLASS_REFERENCE,
            product -> {
                product.setStringListValue(ProductClassInitializer.FEATURE_IDS,
                    List.of("com.xwiki.pro:xwiki-pro-ui", "com.xwiki.swp:injection"));
                product.setLargeStringValue(ProductClassInitializer.EXTENSION_VERSIONS,
                    "com.xwiki.pro:xwiki-pro-ui = 1.2\n\nmalformed\r\nnoVersion=\n");
                product.setStringListValue(ProductClassInitializer.RENEWED_FEATURE_IDS,
                    List.of("com.xwiki.pro:xwiki-pro-ui"));
            });

        Product product = this.configuration.getProduct("bundle").get();

        assertEquals("bundle", product.getId());
        assertEquals(List.of("com.xwiki.pro:xwiki-pro-ui", "com.xwiki.swp:injection"), product.getFeatureIds());
        assertEquals("1.2", product.getExtensionVersion("com.xwiki.pro:xwiki-pro-ui"));
        assertNull(product.getExtensionVersion("noVersion"));
        assertEquals(List.of("com.xwiki.pro:xwiki-pro-ui"), product.getRenewedFeatureIds());

        assertEquals(Optional.empty(), this.configuration.getProduct("unknown"));
        assertEquals(Optional.empty(), this.configuration.getProduct(" "));
    }

    @Test
    void getInstance() throws Exception
    {
        Date created = new Date(1795000000000L);
        String instanceId = "11111111-2222-3333-4444-555555555555";
        saveObject(InstanceClassInitializer.INSTANCES_SPACE, instanceId, InstanceClassInitializer.CLASS_REFERENCE,
            instance -> {
                instance.setStringValue(InstanceClassInitializer.INSTANCE_ID, instanceId);
                instance.setLargeStringValue(InstanceClassInitializer.ACCOUNT, "XWiki.partner");
                instance.setStringValue(InstanceClassInitializer.ENVIRONMENT, "prod");
                instance.setStringValue(InstanceClassInitializer.DOMAIN, "wiki.example.com");
                instance.setStringValue(InstanceClassInitializer.PRODUCT, "bundle");
                instance.setStringValue(InstanceClassInitializer.ORIGIN, "import");
                instance.setDateValue(InstanceClassInitializer.CREATED_DATE, created);
            });

        InstanceRecord instance = this.configuration.getInstance(instanceId).get();

        assertEquals(instanceId, instance.getInstanceId());
        assertEquals(ACCOUNT, instance.getAccount());
        assertEquals(Environment.PROD, instance.getEnvironment());
        assertEquals("wiki.example.com", instance.getDomain());
        assertEquals("bundle", instance.getProduct());
        assertEquals(InstanceRecord.Origin.IMPORT, instance.getOrigin());
        assertEquals(created, instance.getCreatedDate());

        assertEquals(Optional.empty(), this.configuration.getInstance("unknown"));
    }

    @Test
    void getRecordAuthorAndSigningPassword() throws Exception
    {
        UserReference configured = mock(UserReference.class);
        DocumentReference recordAuthor = new DocumentReference("xwiki", "XWiki", "licensingBot");
        when(this.userReferenceResolver.resolve(recordAuthor)).thenReturn(configured);
        saveLicenseConfig(config -> {
            config.setLargeStringValue(ApiConfiguration.RECORD_AUTHOR, "XWiki.licensingBot");
            config.setStringValue("paidPassword", "paid secret");
        });

        assertSame(configured, this.configuration.getRecordAuthor());
        assertEquals("paid secret", this.configuration.getSigningPassword(LicenseType.PAID));
        assertEquals("", this.configuration.getSigningPassword(LicenseType.TRIAL));
        assertEquals("freePassword", ApiConfiguration.getPasswordProperty(LicenseType.FREE));
    }

    @Test
    void getRecordAuthorDefaultsToTheAuthorOfTheConfiguration() throws Exception
    {
        UserReference storeAdmin = saveLicenseConfig(config -> { });

        assertSame(storeAdmin, this.configuration.getRecordAuthor());
    }

    @Test
    void getRecordAuthorWhenTheConfigurationHasNoObject() throws Exception
    {
        XWikiDocument document = createDocument(new DocumentReference(ApiConfiguration.LICENSE_CONFIG,
            this.configuration.getMainWiki()), new LocalDocumentReference("XWiki", "OtherClass"), object -> { });
        UserReference storeAdmin = mock(UserReference.class);
        document.getAuthors().setEffectiveMetadataAuthor(storeAdmin);
        save(document);

        assertSame(storeAdmin, this.configuration.getRecordAuthor());
        assertNull(this.configuration.getSigningPassword(LicenseType.PAID));
    }

    @Test
    void getRecordAuthorWithoutConfiguration() throws Exception
    {
        assertNull(this.configuration.getSigningPassword(LicenseType.PAID));
        // A missing page has no author.
        assertNull(this.configuration.getRecordAuthor());
    }

    private void savePolicy(String name, Consumer<BaseObject> filler) throws Exception
    {
        saveObject(PolicyClassInitializer.POLICIES_SPACE, name, PolicyClassInitializer.CLASS_REFERENCE, filler);
    }

    private UserReference saveLicenseConfig(Consumer<BaseObject> filler) throws Exception
    {
        XWikiDocument document = createDocument(new DocumentReference(ApiConfiguration.LICENSE_CONFIG,
            this.configuration.getMainWiki()), ApiConfiguration.LICENSE_CONFIG_CLASS, filler);
        UserReference author = mock(UserReference.class);
        document.getAuthors().setEffectiveMetadataAuthor(author);
        save(document);
        return author;
    }

    private void saveObject(List<String> space, String name, EntityReference classReference,
        Consumer<BaseObject> filler) throws Exception
    {
        save(createDocument(new DocumentReference("xwiki", space, name), classReference, filler));
    }

    private XWikiDocument createDocument(DocumentReference reference, EntityReference classReference,
        Consumer<BaseObject> filler) throws Exception
    {
        XWikiDocument document =
            this.oldcore.getSpyXWiki().getDocument(reference, this.oldcore.getXWikiContext()).clone();
        document.removeXObjects(classReference);
        BaseObject object = new BaseObject();
        object.setXClassReference(classReference);
        filler.accept(object);
        document.addXObject(object);
        return document;
    }

    private void save(XWikiDocument document) throws Exception
    {
        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());
    }
}
