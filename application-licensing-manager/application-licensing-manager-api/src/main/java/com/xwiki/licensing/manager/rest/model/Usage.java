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
package com.xwiki.licensing.manager.rest.model;

import org.xwiki.stability.Unstable;

/**
 * The current usage of the account, to compare with the policy limits.
 *
 * @version $Id$
 * @since 1.34
 */
@Unstable
public class Usage
{
    private Integer issuancesLast24Hours;

    /**
     * @return the number of licenses issued for the account's instances in the last 24 hours
     */
    public Integer getIssuancesLast24Hours()
    {
        return this.issuancesLast24Hours;
    }

    /**
     * @param issuancesLast24Hours the number of licenses issued for the account's instances in the last 24 hours
     */
    public void setIssuancesLast24Hours(Integer issuancesLast24Hours)
    {
        this.issuancesLast24Hours = issuancesLast24Hours;
    }
}
