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

import org.junit.jupiter.api.Test;

import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.DateClass;
import com.xpn.xwiki.objects.classes.GroupsClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.objects.classes.UsersClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PolicyClassInitializer}, {@link ProductClassInitializer} and {@link InstanceClassInitializer}.
 *
 * @version $Id$
 */
class ApiClassInitializersTest
{
    @Test
    void policyClass()
    {
        PolicyClassInitializer initializer = new PolicyClassInitializer();
        BaseClass xclass = new BaseClass();

        initializer.createClass(xclass);

        assertTrue(initializer.isMainWikiOnly());
        assertEquals(List.of("License", "Code", "API"), PolicyClassInitializer.CODE_SPACE);
        assertEquals(28, xclass.getPropertyNames().length);
        assertFalse(((UsersClass) xclass.get(PolicyClassInitializer.ACCOUNT)).isMultiSelect());
        assertEquals(1, ((BooleanClass) xclass.get(PolicyClassInitializer.ENABLED)).getDefaultValue());
        assertEquals(0, ((BooleanClass) xclass.get(PolicyClassInitializer.MAY_CHOOSE_INSTANCE_ID)).getDefaultValue());

        // Lists of free text keep their order (the first duration is the default) and aren't stored relationally.
        StaticListClass durations = (StaticListClass) xclass.get(PolicyClassInitializer.PROD_DURATIONS);
        assertTrue(durations.isMultiSelect());
        assertFalse(durations.isRelationalStorage());
        assertEquals("input", durations.getDisplayType());

        StaticListClass environments = (StaticListClass) xclass.get(PolicyClassInitializer.ALLOWED_ENVIRONMENTS);
        assertEquals("prod=Production|test=Test", environments.getValues());
        StaticListClass licenseeModes = (StaticListClass) xclass.get(PolicyClassInitializer.LICENSEE_MODES);
        assertTrue(licenseeModes.isMultiSelect());
        assertEquals("checkbox", licenseeModes.getDisplayType());
        assertEquals("partner=Partner|customer=Customer|none=None", licenseeModes.getValues());
        assertEquals("partner",
            ((StaticListClass) xclass.get(PolicyClassInitializer.DEFAULT_LICENSEE_MODE)).getDefaultValue());
        assertEquals("long", ((NumberClass) xclass.get(PolicyClassInitializer.MAX_USER_COUNT)).getNumberType());
        DateClass expirationLimit = (DateClass) xclass.get(PolicyClassInitializer.PROD_EXPIRATION_LIMIT);
        assertEquals(0, expirationLimit.getEmptyIsToday());
        assertEquals("dd/MM/yyyy", expirationLimit.getDateFormat());
        assertInstanceOf(GroupsClass.class, xclass.get(PolicyClassInitializer.ALERT_RECIPIENTS));
    }

    @Test
    void productClass()
    {
        ProductClassInitializer initializer = new ProductClassInitializer();
        BaseClass xclass = new BaseClass();

        initializer.createClass(xclass);

        assertTrue(initializer.isMainWikiOnly());
        assertEquals(List.of(ProductClassInitializer.FEATURE_IDS, ProductClassInitializer.EXTENSION_VERSIONS,
            ProductClassInitializer.RENEWED_FEATURE_IDS), List.of(xclass.getPropertyNames()));
        assertEquals(TextAreaClass.ContentType.PURE_TEXT, TextAreaClass.ContentType.getByValue(
            ((TextAreaClass) xclass.get(ProductClassInitializer.EXTENSION_VERSIONS)).getContentType()));
    }

    @Test
    void instanceClass()
    {
        InstanceClassInitializer initializer = new InstanceClassInitializer();
        BaseClass xclass = new BaseClass();

        initializer.createClass(xclass);

        assertTrue(initializer.isMainWikiOnly());
        assertEquals(List.of("instanceId", "account", "environment", "domain", "product", "origin", "createdDate"),
            List.of(xclass.getPropertyNames()));
        assertEquals("api=API|import=Import",
            ((StaticListClass) xclass.get(InstanceClassInitializer.ORIGIN)).getValues());
        // The creation date keeps the platform default format, which includes the time.
        assertEquals("dd/MM/yyyy HH:mm:ss",
            ((DateClass) xclass.get(InstanceClassInitializer.CREATED_DATE)).getDateFormat());
    }
}
