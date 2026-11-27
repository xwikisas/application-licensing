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

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import com.xwiki.licensing.manager.IssuingException;
import com.xwiki.licensing.manager.rest.model.IssueRequest;
import com.xwiki.licensing.manager.rest.model.LicenseeDetails;

import static com.xwiki.licensing.internal.issuing.IssuingErrors.LICENSEE_NOT_ALLOWED;
import static com.xwiki.licensing.internal.issuing.IssuingErrors.badRequest;
import static com.xwiki.licensing.internal.issuing.IssuingErrors.forbidden;

/**
 * Resolves the licensee of a request (spec 3.2 {@code licenseeMode}): the mode is the request's or the policy
 * default, and must be allowed by the policy; {@code customer} needs the customer details in the request,
 * {@code partner} needs the partner details on the policy.
 *
 * @version $Id$
 * @since 1.34
 */
final class LicenseeValidator
{
    private static final String FIRST_NAME = "licensee first name";

    private static final String LAST_NAME = "licensee last name";

    private static final String EMAIL = "licensee email";

    private LicenseeValidator()
    {
    }

    static Policy.Licensee getLicensee(Policy policy, IssueRequest request) throws IssuingException
    {
        Policy.LicenseeMode mode = getMode(policy, request.getLicenseeMode());
        LicenseeDetails details = request.getLicensee();
        if (mode == Policy.LicenseeMode.CUSTOMER) {
            return getCustomer(details);
        }
        if (details != null) {
            throw forbidden(LICENSEE_NOT_ALLOWED, String.format(
                "A licensee can only be sent with the [customer] licensee mode, not [%s].", toValue(mode)));
        }
        if (mode == Policy.LicenseeMode.PARTNER) {
            return getPartner(policy);
        }
        return new Policy.Licensee(null, null, null);
    }

    private static Policy.LicenseeMode getMode(Policy policy, String value) throws IssuingException
    {
        if (value == null) {
            return policy.getDefaultLicenseeMode();
        }
        Policy.LicenseeMode mode = null;
        for (Policy.LicenseeMode constant : Policy.LicenseeMode.values()) {
            if (toValue(constant).equals(value)) {
                mode = constant;
            }
        }
        if (mode == null) {
            throw badRequest(IssuingErrors.INVALID_REQUEST,
                String.format("Unknown licensee mode [%s]: use [partner], [customer] or [none].", value));
        }
        if (!policy.getLicenseeModes().contains(mode)) {
            throw forbidden(LICENSEE_NOT_ALLOWED,
                String.format("The policy doesn't allow the licensee mode [%s].", value));
        }
        return mode;
    }

    private static Policy.Licensee getCustomer(LicenseeDetails details) throws IssuingException
    {
        if (details == null) {
            throw badRequest(IssuingErrors.LICENSEE_REQUIRED,
                "The [customer] licensee mode needs the licensee first name, last name and email.");
        }
        return new Policy.Licensee(getRequired(details.getFirstName(), FIRST_NAME),
            getRequired(details.getLastName(), LAST_NAME), getRequired(details.getEmail(), EMAIL));
    }

    private static String getRequired(String value, String name) throws IssuingException
    {
        String text = PolicyValidator.getText(value, name);
        if (text == null) {
            throw badRequest(IssuingErrors.LICENSEE_REQUIRED, String.format("The %s is required.", name));
        }
        return text;
    }

    private static Policy.Licensee getPartner(Policy policy) throws IssuingException
    {
        Policy.Licensee partner = policy.getLicensee();
        if (StringUtils.isAnyBlank(partner.getFirstName(), partner.getLastName(), partner.getEmail())) {
            throw IssuingErrors.internal(
                "The policy uses the [partner] licensee mode but doesn't have the partner details.", null);
        }
        return partner;
    }

    private static String toValue(Policy.LicenseeMode mode)
    {
        return mode.name().toLowerCase(Locale.ROOT);
    }
}
