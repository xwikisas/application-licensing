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
 * Declares the class of the instance records of the License Issuing API. Each instance id used through the API has
 * one record, on the page {@code License.API.Instances.<instanceId>}, which says which account owns it and in which
 * environment it runs.
 *
 * @version $Id$
 * @since 1.34
 */
@Component
@Named(InstanceClassInitializer.CLASS_NAME)
@Singleton
public class InstanceClassInitializer extends AbstractApiClassInitializer
{
    /**
     * The serialized local reference of the class.
     */
    public static final String CLASS_NAME = "License.Code.API.InstanceClass";

    /**
     * The local reference of the class.
     */
    public static final LocalDocumentReference CLASS_REFERENCE =
        new LocalDocumentReference(CODE_SPACE, "InstanceClass");

    /**
     * The space holding the instance record pages.
     */
    public static final List<String> INSTANCES_SPACE = List.of("License", "API", "Instances");

    /**
     * The instance id.
     */
    public static final String INSTANCE_ID = "instanceId";

    /**
     * The account owning the instance.
     */
    public static final String ACCOUNT = "account";

    /**
     * The environment of the instance ({@code prod} or {@code test}), fixed for its whole life.
     */
    public static final String ENVIRONMENT = "environment";

    /**
     * The domain declared by the client, for tracking only.
     */
    public static final String DOMAIN = "domain";

    /**
     * The product licensed on the instance.
     */
    public static final String PRODUCT = "product";

    /**
     * How the record was created ({@code api} or {@code import}).
     */
    public static final String ORIGIN = "origin";

    /**
     * When the record was created.
     */
    public static final String CREATED_DATE = "createdDate";

    /**
     * Default constructor.
     */
    public InstanceClassInitializer()
    {
        super(CLASS_REFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addTextField(INSTANCE_ID, "Instance id", 60);
        xclass.addUsersField(ACCOUNT, "Account", false);
        xclass.addStaticListField(ENVIRONMENT, "Environment", ENVIRONMENT_VALUES);
        xclass.addTextField(DOMAIN, "Domain", 60);
        xclass.addTextField(PRODUCT, "Product", 30);
        xclass.addStaticListField(ORIGIN, "Origin", "api=API|import=Import");
        xclass.addDateField(CREATED_DATE, "Created", null, 0);
    }
}
