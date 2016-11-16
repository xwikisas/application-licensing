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
package com.xwiki.licensing.internal.enforcer;

import java.lang.reflect.Field;

import org.xwiki.component.phase.InitializationException;

/**
 * Some basic tools that ease checking the integrity of the licensing system.
 *
 * @version $Id$
 */
public final class LicensingUtils
{
    private LicensingUtils()
    {
        // This is a utility class, never create an instance of it.
    }

    /**
     * Utility function that check if some given component are really coming from this package, to prevent easy
     * overriding of the license enforcing components.
     *
     * @param instances the component instances to be checked.
     * @throws InitializationException if any instances is not from this package.
     */
    public static void checkIntegrity(Object... instances) throws InitializationException
    {
        for (Object instance : instances) {
            if (!isPristineImpl(instance)) {
                throw new InitializationException("Integrity check failed while loading the licensor.");
            }
        }
    }

    /**
     * Utility function that check if a given object instance is from a Class of this package.
     *
     * @param instance the instance to check.
     * @return true if the instance is from a Class of this package.
     */
    public static boolean isPristineImpl(Object instance)
    {
        return (LicensingUtils.class.getProtectionDomain().getCodeSource()
            .equals(instance.getClass().getProtectionDomain().getCodeSource()));
    }

    /**
     * Gets a value from a field using reflection even if the field is private.
     *
     * @param instanceContainingField the object containing the field
     * @param fieldName the name of the field in the object
     */
    static Object getFieldValue(Object instanceContainingField, String fieldName)
    {
        // Find the class containing the field to set
        Class< ? > targetClass = instanceContainingField.getClass();
        while (targetClass != null) {
            for (Field field : targetClass.getDeclaredFields()) {
                if (field.getName().equalsIgnoreCase(fieldName)) {
                    try {
                        boolean isAccessible = field.isAccessible();
                        try {
                            field.setAccessible(true);
                            return field.get(instanceContainingField);
                        } finally {
                            field.setAccessible(isAccessible);
                        }
                    } catch (Exception e) {
                        // This shouldn't happen but if it does then the Component manager will not function properly
                        // and we need to abort. It probably means the Java security manager has been configured to
                        // prevent accessing private fields.
                        throw new RuntimeException("Failed to get field [" + fieldName + "] in instance of ["
                            + instanceContainingField.getClass().getName() + "]. The Java Security Manager has "
                            + "probably been configured to prevent getting private field values. This extension "
                            + "requires this ability to work.", e);
                    }
                }
            }
            targetClass = targetClass.getSuperclass();
        }
        return null;
    }
}
