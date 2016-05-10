package com.xwiki.licensing.internal.enforcer;

import java.lang.reflect.Field;

import org.xwiki.component.phase.InitializationException;

/**
 * Some basic tools that ease checking the integrity of the licensing system.
 *
 * @version $Id$
 */
public class LicensingUtils
{
    private LicensingUtils()
    {
        // This is a utility class, never create an instance of it.
    }

    public static void checkIntegrity(Object... instances) throws InitializationException
    {
        for(Object instance : instances) {
            if (!isPristineImpl(instance)) {
                throw new InitializationException("Integrity check failed while loading the licensor.");
            }
        }
    }

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
        Class<?> targetClass = instanceContainingField.getClass();
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
