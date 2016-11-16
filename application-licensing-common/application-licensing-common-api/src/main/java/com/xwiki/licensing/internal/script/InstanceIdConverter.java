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
package com.xwiki.licensing.internal.script;

import java.lang.reflect.Type;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.instance.InstanceId;
import org.xwiki.properties.converter.AbstractConverter;

// TODO: Remove this class once the feature is available in platform in the instance module. Registered with a lower
// than default priority so that an implementation in xwiki platform will have precedence.
// This is to allow depending on XWiki 8.3
@Component
@Singleton
public class InstanceIdConverter extends AbstractConverter<InstanceId>
{
    @Override
    protected InstanceId convertToType(Type targetType, Object value)
    {
        if (value == null) {
            return null;
        }

        return new InstanceId(value.toString());
    }

    @Override
    protected String convertToString(InstanceId value)
    {
        return value.getInstanceId();
    }
}
