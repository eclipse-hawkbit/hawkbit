/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.jpa.Jpa;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class Utils {

    private static final Map<FromTo, CopyFunction> FROM_TO_SETTER = new HashMap<>();

    // java:S4276 - it is intended to be generic - not specialized
    @SuppressWarnings("java:S4276")
    public static boolean copy(final Object from, final Object to, final EntityManager entityManager) {
        final FromTo fromTo = new FromTo(from.getClass(), to.getClass());
        CopyFunction fromToFunction;
        synchronized (FROM_TO_SETTER) {
            fromToFunction = FROM_TO_SETTER.get(fromTo);
            if (fromToFunction == null) {
                fromToFunction = fromToFunction(from.getClass(), to.getClass());
                FROM_TO_SETTER.put(fromTo, fromToFunction);
            }
        }
        return fromToFunction.apply(from, to, entityManager);
    }

    // java:S4276 - it is intended to be generic - not specialized
    // java:S3776 - complexity is due to reflection and dynamic method invocation
    // java:S1141 - better readable that way
    // java:S3011 - low-level, reflection utility. intentionally changes the accessibility
    @SuppressWarnings({ "java:S4276", "java:S3776", "java:S1141", "java:S3011" })
    private static CopyFunction fromToFunction(final Class<?> fromClass, final Class<?> toClass) {
        final List<CopyFunction> propertySetters = new ArrayList<>();

        for (final Method fromMethod : getMethods(fromClass)) {
            if (fromMethod.getParameterCount() == 0 && fromMethod.getReturnType() != void.class) {
                final String methodName = fromMethod.getName();
                if (methodName.equals("getClass") || methodName.equals("getId")) {
                    continue; // skip
                }
                final boolean isGet = isGet(methodName);
                if (isGet || isIs(fromMethod, methodName)) {
                    fromMethod.setAccessible(true);
                    // getter method
                    final String setterName = "set" + methodName.substring(isGet ? 3 : 2);
                    Method toGetterMethod0;
                    try {
                        toGetterMethod0 = getMethod(toClass, fromMethod.getName());
                    } catch (final NoSuchMethodException e) {
                        // no method to get current value of the target field
                        toGetterMethod0 = null;
                    }
                    final Method toGetterMethod = toGetterMethod0;
                    try {
                        final Method toSetterMethod = getMethod(toClass, setterName, fromMethod.getReturnType());
                        // method access
                        propertySetters.add((from, to, entityManager) -> {
                            try {
                                final Object value = fromMethod.invoke(from);
                                if (value == null) { // null means no change
                                    return false;
                                }
                                if (toGetterMethod != null) {
                                    final Object currentValue = toGetterMethod.invoke(to);
                                    if (Objects.equals(value, currentValue)) {
                                        return false; // no change
                                    }
                                }
                                toSetterMethod.invoke(to, attach(value, entityManager));
                                return true;
                            } catch (final Exception e) {
                                throw new IllegalStateException("Error invoking " + fromMethod + " or " + toSetterMethod, e);
                            }
                        });
                    } catch (final NoSuchMethodException e) {
                        final String fieldName = Character.toLowerCase(methodName.charAt(isGet ? 3 : 2)) + methodName.substring(isGet ? 4 : 3);
                        final Field field = getField(toClass, fieldName);
                        if (field == null) {
                            throw new IllegalStateException(
                                    "Setter method counterpart for " + fromMethod + " in " + toClass.getSimpleName() + " not found");
                        } else {
                            // field access
                            propertySetters.add((from, to, entityManager) -> {
                                try {
                                    final Object value = fromMethod.invoke(from);
                                    if (value == null) { // null means no change
                                        return false;
                                    }
                                    if (toGetterMethod != null) {
                                        final Object currentValue = toGetterMethod.invoke(to);
                                        if (Objects.equals(value, currentValue)) {
                                            return false; // no change
                                        }
                                    }
                                    field.set(to, attach(value, entityManager));
                                    return true;
                                } catch (final Exception e2) {
                                    throw new IllegalStateException("Error invoking " + fromMethod + " or setting " + field, e2);
                                }
                            });
                        }
                    }
                }
            }
        }

        return (from, to, entityManager) -> {
            boolean updated = false;
            for (final CopyFunction fieldSetter : propertySetters) {
                updated = fieldSetter.apply(from, to, entityManager) || updated;
            }
            return updated;
        };
    }

    private static boolean isIs(final Method fromMethod, final String methodName) {
        return methodName.startsWith("is") && methodName.length() > 2 && Character.isUpperCase(
                methodName.charAt(2)) && fromMethod.getReturnType() == boolean.class;
    }

    private static boolean isGet(final String methodName) {
        return methodName.startsWith("get") && methodName.length() > 3 && Character.isUpperCase(methodName.charAt(3));
    }

    @SuppressWarnings("java:S3011") // low-level, reflection utility. intentionally changes the accessibility
    private static Field getField(final Class<?> clazz, final String fieldName) {
        for (final Field field : clazz.getDeclaredFields()) {
            if (fieldName.equals(field.getName())) {
                field.setAccessible(true);
                return field;
            }
        }
        final Class<?> superClass = clazz.getSuperclass();
        if (superClass == null) {
            return null;
        } else {
            return getField(superClass, fieldName);
        }
    }

    private static Method getMethod(final Class<?> clazz, final String methodName, final Class<?>... parameterTypes)
            throws NoSuchMethodException {
        try {
            return getMethod(clazz, clazz, methodName, parameterTypes);
        } catch (final NoSuchMethodException e) {
            if (parameterTypes.length == 1 && parameterTypes[0] == Boolean.class) {
                try {
                    return getMethod(clazz, methodName, boolean.class);
                } catch (final NoSuchMethodException e2) {
                    throw e;
                }
            }
            throw e;
        }
    }

    @SuppressWarnings("java:S3011") // low-level, reflection utility. intentionally changes the accessibility
    private static Method getMethod(final Class<?> target, final Class<?> clazz, final String methodName, final Class<?>... parameterTypes)
            throws NoSuchMethodException {
        try {
            final Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (final NoSuchMethodException e) {
            final Class<?> superClass = clazz.getSuperclass();
            if (superClass == null) {
                throw new NoSuchMethodException("Method " + methodName + " not found in " + target.getSimpleName());
            } else {
                return getMethod(target, superClass, methodName, parameterTypes);
            }
        }
    }

    private static List<Method> getMethods(final Class<?> clazz) {
        final List<Method> methods = new ArrayList<>();
        for (final Method method : clazz.getDeclaredMethods()) {
            if (!method.isSynthetic() && !method.isBridge()) {
                methods.add(method);
            }
        }
        final Class<?> superClass = clazz.getSuperclass();
        if (superClass != null) {
            methods.addAll(getMethods(superClass));
        }
        return methods;
    }

    private static Object attach(final Object value, final EntityManager entityManager) {
        if (Jpa.JPA_VENDOR != Jpa.JpaVendor.HIBERNATE) {
            return value; // no need to attach, only Hibernate supports this
        }

        if (value instanceof List<?> list) {
            return list.stream().map(e -> attach(e, entityManager)).toList();
        } else if (value instanceof Set<?> set) {
            return set.stream().map(e -> attach(e, entityManager)).collect(Collectors.toSet());
        } else if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream().collect(Collectors.toMap(
                    entry -> attach(entry.getKey(), entityManager),
                    entry -> attach(entry.getValue(), entityManager)));
        } else if (attachable(value, entityManager)) {
            // hibernate require detached entities to be attached before setting to jpa entity as a sub-property
            return entityManager.merge(value);
        } else {
            return value; // no change
        }
    }

    private static boolean attachable(final Object value, final EntityManager entityManager) {
        if (value == null) {
            return false;
        }

        final Class<?> clazz = value.getClass();
        return !clazz.isPrimitive() && !clazz.isEnum() &&
                clazz != String.class && !Number.class.isAssignableFrom(clazz) && !Boolean.class.isAssignableFrom(clazz) &&
                !entityManager.contains(value); // no need to attach
    }

    // key for copy of class to class function
    private record FromTo(Class<?> from, Class<?> to) {}

    // functional interface to apply the copy operation
    private interface CopyFunction {

        boolean apply(Object from, Object to, EntityManager entityManager);
    }
}