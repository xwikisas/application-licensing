package com.xwiki.licensing;

import java.lang.reflect.ParameterizedType;

import org.xwiki.component.annotation.Role;
import org.xwiki.component.util.DefaultParameterizedType;

/**
 * License serializer.
 *
 * @version $Id$
 */
@Role
public interface LicenseSerializer<T>
{
    ParameterizedType TYPE_STRING = new DefaultParameterizedType(null, LicenseSerializer.class, String.class);

    <G extends T> G serialize(License license);
}
