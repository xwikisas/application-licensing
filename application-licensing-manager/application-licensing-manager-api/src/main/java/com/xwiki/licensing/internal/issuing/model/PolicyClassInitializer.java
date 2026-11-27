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
package com.xwiki.licensing.internal.issuing.model;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Declares the class of the per-account policies of the License Issuing API. A policy is stored on the page
 * {@code License.Code.API.Policies.<account>} and says what the account may issue.
 *
 * @version $Id$
 * @since 1.34
 */
@Component
@Named(PolicyClassInitializer.CLASS_NAME)
@Singleton
public class PolicyClassInitializer extends AbstractApiClassInitializer
{
    /**
     * The serialized local reference of the class.
     */
    public static final String CLASS_NAME = "License.Code.API.PolicyClass";

    /**
     * The local reference of the class.
     */
    public static final LocalDocumentReference CLASS_REFERENCE = new LocalDocumentReference(CODE_SPACE, "PolicyClass");

    /**
     * The space holding the policy pages.
     */
    public static final List<String> POLICIES_SPACE = List.of("License", "Code", "API", "Policies");

    /**
     * The user the policy applies to.
     */
    public static final String ACCOUNT = "account";

    /**
     * Master switch of the policy.
     */
    public static final String ENABLED = "enabled";

    /**
     * The ids of the products the account may request.
     */
    public static final String PRODUCTS = "products";

    /**
     * The environments the account may request ({@code prod}, {@code test}).
     */
    public static final String ALLOWED_ENVIRONMENTS = "allowedEnvironments";

    /**
     * The durations allowed for production licenses (e.g. {@code 1y}), the first one being the default.
     */
    public static final String PROD_DURATIONS = "prodDurations";

    /**
     * The durations allowed for test licenses, the first one being the default.
     */
    public static final String TEST_DURATIONS = "testDurations";

    /**
     * The date after which production licenses never expire.
     */
    public static final String PROD_EXPIRATION_LIMIT = "prodExpirationLimit";

    /**
     * The maximum user limit per instance.
     */
    public static final String MAX_USER_COUNT = "maxUserCount";

    /**
     * The user limit used when the request has none.
     */
    public static final String DEFAULT_USER_COUNT = "defaultUserCount";

    /**
     * Whether the account may send its own instance ids for new instances.
     */
    public static final String MAY_CHOOSE_INSTANCE_ID = "mayChooseInstanceId";

    /**
     * The maximum number of instances in one request.
     */
    public static final String MAX_BATCH_SIZE = "maxBatchSize";

    /**
     * Who the account may write into the license as licensee ({@code partner}, {@code customer} or {@code none}).
     */
    public static final String LICENSEE_MODES = "licenseeModes";

    /**
     * The licensee mode used when the request has none.
     */
    public static final String DEFAULT_LICENSEE_MODE = "defaultLicenseeMode";

    /**
     * The licensee first name used in {@code partner} mode.
     */
    public static final String LICENSEE_FIRST_NAME = "licenseeFirstName";

    /**
     * The licensee last name used in {@code partner} mode.
     */
    public static final String LICENSEE_LAST_NAME = "licenseeLastName";

    /**
     * The licensee email used in {@code partner} mode.
     */
    public static final String LICENSEE_EMAIL = "licenseeEmail";

    /**
     * The support levels the account may request.
     */
    public static final String SUPPORT_LEVELS = "supportLevels";

    /**
     * The support level used when the request has none.
     */
    public static final String DEFAULT_SUPPORT = "defaultSupport";

    /**
     * Whether the licensee gets the expiration reminder emails (not enforced yet).
     */
    public static final String SEND_EXPIRATION_REMINDERS = "sendExpirationReminders";

    /**
     * How Sales is told about the production licenses issued by the account (not enforced yet).
     */
    public static final String SALES_NOTIFICATION = "salesNotification";

    /**
     * The maximum number of licenses issued by the account in a rolling 24 hours.
     */
    public static final String MAX_ISSUANCES_PER_DAY = "maxIssuancesPerDay";

    /**
     * The maximum number of active production instances.
     */
    public static final String MAX_ACTIVE_PROD_INSTANCES = "maxActiveProdInstances";

    /**
     * The maximum number of active test instances.
     */
    public static final String MAX_ACTIVE_TEST_INSTANCES = "maxActiveTestInstances";

    /**
     * The maximum sum of the user limits of the active production instances.
     */
    public static final String MAX_TOTAL_USERS = "maxTotalUsers";

    /**
     * What happens when a volume limit is reached ({@code alert} or {@code reject}).
     */
    public static final String QUOTA_MODE = "quotaMode";

    /**
     * When the volume counters restart ({@code none}, {@code yearly} or {@code contract}; not enforced yet).
     */
    public static final String QUOTA_PERIOD = "quotaPeriod";

    /**
     * The contract renewal date, used when the quota period is {@code contract}.
     */
    public static final String CONTRACT_RENEWAL_DATE = "contractRenewalDate";

    /**
     * The groups emailed for rate and volume alerts.
     */
    public static final String ALERT_RECIPIENTS = "alertRecipients";

    private static final String NONE = "none";

    private static final String LONG = "long";

    private static final String INTEGER = "integer";

    private static final String YES_NO = "yesno";

    private static final String LICENSEE_MODE_VALUES = "partner=Partner|customer=Customer|none=None";

    /**
     * Default constructor.
     */
    public PolicyClassInitializer()
    {
        super(CLASS_REFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addUsersField(ACCOUNT, "Account", false);
        xclass.addBooleanField(ENABLED, "Enabled", YES_NO, true);
        addTextListField(xclass, PRODUCTS, "Products");
        addCheckboxListField(xclass, ALLOWED_ENVIRONMENTS, "Allowed environments", ENVIRONMENT_VALUES);
        addTextListField(xclass, PROD_DURATIONS, "Production license durations");
        addTextListField(xclass, TEST_DURATIONS, "Test license durations");
        addOptionalDateField(xclass, PROD_EXPIRATION_LIMIT, "Production expiration limit");
        xclass.addNumberField(MAX_USER_COUNT, "Maximum user count per instance", 10, LONG);
        xclass.addNumberField(DEFAULT_USER_COUNT, "Default user count per instance", 10, LONG);
        xclass.addBooleanField(MAY_CHOOSE_INSTANCE_ID, "May choose instance ids", YES_NO, false);
        xclass.addNumberField(MAX_BATCH_SIZE, "Maximum batch size", 10, INTEGER);
        addCheckboxListField(xclass, LICENSEE_MODES, "Licensee modes", LICENSEE_MODE_VALUES);
        xclass.addStaticListField(DEFAULT_LICENSEE_MODE, "Default licensee mode", LICENSEE_MODE_VALUES, "partner");
        xclass.addTextField(LICENSEE_FIRST_NAME, "Licensee first name", 60);
        xclass.addTextField(LICENSEE_LAST_NAME, "Licensee last name", 60);
        xclass.addEmailField(LICENSEE_EMAIL, "Licensee email", 60);
        addTextListField(xclass, SUPPORT_LEVELS, "Support levels");
        xclass.addTextField(DEFAULT_SUPPORT, "Default support level", 30);
        xclass.addBooleanField(SEND_EXPIRATION_REMINDERS, "Send expiration reminders", YES_NO, false);
        xclass.addStaticListField(SALES_NOTIFICATION, "Sales notification",
            "none=None|perLicense=Per license|monthly=Monthly|quarterly=Quarterly", NONE);
        xclass.addNumberField(MAX_ISSUANCES_PER_DAY, "Maximum licenses issued per day", 10, INTEGER);
        xclass.addNumberField(MAX_ACTIVE_PROD_INSTANCES, "Maximum active production instances", 10, INTEGER);
        xclass.addNumberField(MAX_ACTIVE_TEST_INSTANCES, "Maximum active test instances", 10, INTEGER);
        xclass.addNumberField(MAX_TOTAL_USERS, "Maximum total users", 10, LONG);
        xclass.addStaticListField(QUOTA_MODE, "Quota mode", "alert=Alert|reject=Reject", "alert");
        xclass.addStaticListField(QUOTA_PERIOD, "Quota period", "none=None|yearly=Yearly|contract=Contract",
            NONE);
        addOptionalDateField(xclass, CONTRACT_RENEWAL_DATE, "Contract renewal date");
        xclass.addGroupsField(ALERT_RECIPIENTS, "Alert recipients");
    }
}
