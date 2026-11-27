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

import org.xwiki.model.reference.DocumentReference;

/**
 * Describes an instance licensed through the API, as stored on {@code License.API.Instances.<instanceId>}.
 *
 * @version $Id$
 * @since 1.34
 */
public class InstanceRecord
{
    /**
     * How an instance record was created.
     */
    public enum Origin
    {
        /**
         * Created by a first API call.
         */
        API,

        /**
         * Imported from the instances licensed before the API.
         */
        IMPORT
    }

    private final String instanceId;

    private final DocumentReference account;

    private final Environment environment;

    private final String domain;

    private final String product;

    private final Origin origin;

    private final Date createdDate;

    /**
     * @param instanceId the instance id
     * @param account the account owning the instance
     * @param environment the environment of the instance
     * @param domain the domain declared by the client, may be {@code null}
     * @param product the product licensed on the instance
     * @param origin how the record was created
     * @param createdDate when the record was created
     */
    public InstanceRecord(String instanceId, DocumentReference account, Environment environment, String domain,
        String product, Origin origin, Date createdDate)
    {
        this.instanceId = instanceId;
        this.account = account;
        this.environment = environment;
        this.domain = domain;
        this.product = product;
        this.origin = origin;
        this.createdDate = createdDate;
    }

    /**
     * @return the instance id
     */
    public String getInstanceId()
    {
        return this.instanceId;
    }

    /**
     * @return the account owning the instance
     */
    public DocumentReference getAccount()
    {
        return this.account;
    }

    /**
     * @return the environment of the instance, fixed for its whole life
     */
    public Environment getEnvironment()
    {
        return this.environment;
    }

    /**
     * @return the domain declared by the client, or {@code null}
     */
    public String getDomain()
    {
        return this.domain;
    }

    /**
     * @return the product licensed on the instance
     */
    public String getProduct()
    {
        return this.product;
    }

    /**
     * @return how the record was created
     */
    public Origin getOrigin()
    {
        return this.origin;
    }

    /**
     * @return when the record was created
     */
    public Date getCreatedDate()
    {
        return this.createdDate;
    }
}
