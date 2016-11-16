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
package com.xwiki.licensing;

import java.lang.reflect.ParameterizedType;

import org.xwiki.component.annotation.Role;
import org.xwiki.component.util.DefaultParameterizedType;

/**
 * License serializer to type T.
 * @param <T> the type to serialize to.
 *
 * @version $Id$
 */
@Role
public interface LicenseSerializer<T>
{
    /**
     * Type for a serializer to String.
     */
    ParameterizedType TYPE_STRING = new DefaultParameterizedType(null, LicenseSerializer.class, String.class);

    /**
     * Serialize the given license to type T.
     * @param license The license to be serialized
     * @param <G> The returned type that extends T.
     * @return a serialized license.
     */
    <G extends T> G serialize(License license);
}
