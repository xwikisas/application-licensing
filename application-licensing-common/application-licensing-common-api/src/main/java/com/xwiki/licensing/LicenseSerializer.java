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
