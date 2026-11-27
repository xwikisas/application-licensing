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

import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Base class for the classes of the License Issuing API. They live in the admin-only {@code License.Code.API} space of
 * the main wiki, which is where the store generates licenses.
 *
 * @version $Id$
 * @since 1.34
 */
public abstract class AbstractApiClassInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The space holding the API classes and configuration.
     */
    public static final List<String> CODE_SPACE = List.of("License", "Code", "API");

    /**
     * The allowed values of the environment properties.
     */
    protected static final String ENVIRONMENT_VALUES = "prod=Production|test=Test";

    /**
     * The date format used by the date properties.
     */
    protected static final String DATE_FORMAT = "dd/MM/yyyy";

    private static final String SEPARATORS = "|, ";

    /**
     * @param classReference the reference of the class page, in {@link #CODE_SPACE}
     */
    protected AbstractApiClassInitializer(LocalDocumentReference classReference)
    {
        super(classReference);
    }

    @Override
    protected boolean isMainWikiOnly()
    {
        return true;
    }

    /**
     * Adds a multi-valued list property whose values are free text, kept in the order they were entered.
     *
     * @param xclass the class to update
     * @param name the property name
     * @param prettyName the property pretty name
     */
    protected void addTextListField(BaseClass xclass, String name, String prettyName)
    {
        xclass.addStaticListField(name, prettyName, 60, true, false, "", "input", SEPARATORS);
    }

    /**
     * Adds a multi-valued list property with a fixed set of values, displayed as checkboxes.
     *
     * @param xclass the class to update
     * @param name the property name
     * @param prettyName the property pretty name
     * @param values the allowed values, as {@code value=Label|value2=Label2}
     */
    protected void addCheckboxListField(BaseClass xclass, String name, String prettyName, String values)
    {
        xclass.addStaticListField(name, prettyName, 1, true, false, values, "checkbox", SEPARATORS);
    }

    /**
     * Adds a date property that stays empty when no value is entered.
     *
     * @param xclass the class to update
     * @param name the property name
     * @param prettyName the property pretty name
     */
    protected void addOptionalDateField(BaseClass xclass, String name, String prettyName)
    {
        xclass.addDateField(name, prettyName, DATE_FORMAT, 0);
    }
}
