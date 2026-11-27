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
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xwiki.licensing.manager.IssuingException;

/**
 * Counts what an account issued, to enforce the policy limits (spec 3.7). Only the rate limit for now: the licenses
 * issued for the account's instances in a rolling 24 hours.
 *
 * @version $Id$
 * @since 1.34
 */
@Component(roles = UsageCounter.class)
@Singleton
public class UsageCounter
{
    /**
     * The window of the rate limit.
     */
    static final long WINDOW = TimeUnit.DAYS.toMillis(1);

    private static final long SECOND = TimeUnit.SECONDS.toMillis(1);

    /**
     * The generated licenses of the account's instances issued since a date, oldest first.
     */
    private static final String ISSUE_DATES_QUERY = "select license.issueDate from Document doc, "
        + "doc.object(License.Code.LicenseDetailsClass) license, Document instanceDoc, "
        + "instanceDoc.object(License.Code.API.InstanceClass) instance "
        + "where license.instanceId = instance.instanceId and instance.account in (:accounts) "
        + "and license.hasLicense = 1 and license.issueDate > :since order by license.issueDate";

    @Inject
    private QueryManager queryManager;

    @Inject
    private InstanceRecordStore instanceRecordStore;

    @Inject
    private ApiConfiguration configuration;

    @Inject
    private IssuingAlerts alerts;

    /**
     * Refuses the request if it would take the account over its daily issuance limit, and then alerts the policy's
     * recipients.
     *
     * @param policy the policy of the account
     * @param requested the number of licenses the request issues
     * @param now the issue date
     * @throws IssuingException 403 {@code limitExceeded} if the request alone issues more licenses than the daily
     *     limit, 429 {@code rateLimited} (with the time after which enough licenses left the window) if the account
     *     would go over the limit
     */
    public void checkRateLimit(Policy policy, int requested, Date now) throws IssuingException
    {
        int max = policy.getMaxIssuancesPerDay();
        if (requested > max) {
            throw IssuingErrors.forbidden(IssuingErrors.LIMIT_EXCEEDED, String.format(
                "The request issues %d licenses, more than the daily limit of %d.", requested, max));
        }
        List<Date> issued = getIssueDates(policy.getAccount(), now);
        int excess = issued.size() + requested - max;
        if (excess > 0) {
            // The request fits once the excess oldest licenses have left the window.
            long freedAt = issued.get(excess - 1).getTime() + WINDOW;
            long retryAfter = Math.max(1, (freedAt - now.getTime() + SECOND - 1) / SECOND);
            this.alerts.rateLimitReached(policy);
            throw IssuingErrors.rateLimited(String.format(
                "The daily limit of %d licenses is reached (%d issued in the last 24 hours).", max, issued.size()),
                retryAfter);
        }
    }

    /**
     * @param account an account
     * @param now the current date
     * @return the number of licenses issued for the account's instances in the 24 hours before the date
     * @throws IssuingException if the licenses can't be counted
     */
    public int countIssuances(DocumentReference account, Date now) throws IssuingException
    {
        return getIssueDates(account, now).size();
    }

    private List<Date> getIssueDates(DocumentReference account, Date now) throws IssuingException
    {
        try {
            return this.queryManager.createQuery(ISSUE_DATES_QUERY, Query.XWQL)
                .setWiki(this.configuration.getMainWiki().getName())
                .bindValue("accounts", this.instanceRecordStore.getAccountValues(account))
                .bindValue("since", new Date(now.getTime() - WINDOW)).execute();
        } catch (QueryException e) {
            throw IssuingErrors.internal("Failed to count the licenses issued by the account.", e);
        }
    }
}
