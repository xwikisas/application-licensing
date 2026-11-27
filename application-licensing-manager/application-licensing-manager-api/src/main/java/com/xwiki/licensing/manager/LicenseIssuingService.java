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
package com.xwiki.licensing.manager;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

import com.xwiki.licensing.manager.rest.model.IssueRequest;
import com.xwiki.licensing.manager.rest.model.IssueResponse;
import com.xwiki.licensing.manager.rest.model.LicenseKey;
import com.xwiki.licensing.manager.rest.model.PolicyView;

/**
 * Issues, renews and lists licenses for an account, within the account's policy. The licenses are the same as the
 * ones Sales generates: same records, same signing, same file store. Used by the License Issuing API REST resources.
 *
 * @version $Id$
 * @since 1.34
 */
@Role
@Unstable
public interface LicenseIssuingService
{
    /**
     * Issues licenses for new instances, or renews the licenses of an instance owned by the account. Either all the
     * licenses of the request are issued, or none is.
     *
     * @param account the calling account
     * @param request what to issue
     * @return the issued licenses, per instance
     * @throws IssuingException if the policy doesn't allow the request, if the request is invalid, or if the licenses
     *     can't be signed or stored
     */
    IssueResponse issue(DocumentReference account, IssueRequest request) throws IssuingException;

    /**
     * Lists the licenses of the instances owned by the account, including the licenses issued before the API for
     * imported instances.
     *
     * @param account the calling account
     * @param instanceId only the licenses of this instance, or {@code null} for all the instances
     * @param environment only the instances of this environment ({@code prod} or {@code test}), or {@code null}
     * @param product only the instances of this product, or {@code null}
     * @param activeOnly whether to leave out the expired licenses
     * @return the licenses, per instance
     * @throws IssuingException if the account has no enabled policy, or if the filter is invalid
     */
    IssueResponse list(DocumentReference account, String instanceId, String environment, String product,
        boolean activeOnly) throws IssuingException;

    /**
     * @param account the calling account
     * @param licenseId a license id
     * @return the license, with its key
     * @throws IssuingException if the account has no enabled policy, or if the license doesn't exist or doesn't belong
     *     to one of the account's instances (404)
     */
    LicenseKey get(DocumentReference account, String licenseId) throws IssuingException;

    /**
     * @param account the calling account
     * @return the account's policy and its current usage
     * @throws IssuingException if the account has no enabled policy
     */
    PolicyView getPolicy(DocumentReference account) throws IssuingException;
}
