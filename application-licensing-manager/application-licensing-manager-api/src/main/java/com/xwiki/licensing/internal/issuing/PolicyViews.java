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
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import com.xwiki.licensing.manager.rest.model.PolicyView;
import com.xwiki.licensing.manager.rest.model.Usage;

/**
 * Turns a policy into the API model (spec 4.4). The partner details and the alert recipients are left out.
 *
 * @version $Id$
 * @since 1.34
 */
public final class PolicyViews
{
    private PolicyViews()
    {
    }

    /**
     * @param policy a policy
     * @param issuancesLast24Hours the number of licenses issued for the account in the last 24 hours
     * @return the policy in the API model
     */
    public static PolicyView toPolicyView(Policy policy, int issuancesLast24Hours)
    {
        Usage usage = new Usage();
        usage.setIssuancesLast24Hours(issuancesLast24Hours);
        PolicyView view = new PolicyView();
        view.setProducts(policy.getProducts());
        view.setEnvironments(toValues(policy.getAllowedEnvironments()));
        view.setProdDurations(policy.getDurations(Environment.PROD));
        view.setTestDurations(policy.getDurations(Environment.TEST));
        view.setProdExpirationLimit(LicenseViews.format(policy.getProdExpirationLimit()));
        view.setMaxUserCount(policy.getMaxUserCount());
        view.setDefaultUserCount(policy.getDefaultUserCount());
        view.setMayChooseInstanceId(policy.mayChooseInstanceId());
        view.setMaxBatchSize(policy.getMaxBatchSize());
        view.setLicenseeModes(toValues(policy.getLicenseeModes()));
        view.setDefaultLicenseeMode(toValue(policy.getDefaultLicenseeMode()));
        view.setSupportLevels(policy.getSupportLevels());
        view.setDefaultSupport(policy.getDefaultSupport());
        view.setMaxIssuancesPerDay(policy.getMaxIssuancesPerDay());
        view.setMaxActiveProdInstances(policy.getMaxActiveProdInstances());
        view.setMaxActiveTestInstances(policy.getMaxActiveTestInstances());
        view.setMaxTotalUsers(policy.getMaxTotalUsers());
        view.setQuotaMode(toValue(policy.getQuotaMode()));
        view.setUsage(usage);
        return view;
    }

    private static List<String> toValues(Collection<? extends Enum<?>> constants)
    {
        List<String> values = new ArrayList<>();
        for (Enum<?> constant : constants) {
            values.add(toValue(constant));
        }
        return values;
    }

    private static String toValue(Enum<?> constant)
    {
        return constant.name().toLowerCase(Locale.ROOT);
    }
}
