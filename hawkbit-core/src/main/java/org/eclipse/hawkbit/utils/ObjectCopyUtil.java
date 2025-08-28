/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

import jakarta.validation.constraints.NotNull;

import lombok.NoArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class ObjectCopyUtil {

    private static final Map<FromTo, CopyFunction> FROM_TO_SETTER = new HashMap<>();

    // java:S4276 - it is intended to be generic - not specialized
    @SuppressWarnings("java:S4276")
    public static boolean copy(final Object from, final Object to, boolean setNullValues, final UnaryOperator<Object> propertyProcessor) {
        final FromTo fromTo = new FromTo(from.getClass(), to.getClass());
        CopyFunction fromToFunction;
        synchronized (FROM_TO_SETTER) {
            fromToFunction = FROM_TO_SETTER.get(fromTo);
            if (fromToFunction == null) {
                fromToFunction = fromToFunction(from.getClass(), to.getClass());
                FROM_TO_SETTER.put(fromTo, fromToFunction);
            }
        }
        return fromToFunction.apply(from, to, setNullValues, propertyProcessor);
    }

    // java:S4276 - it is intended to be generic - not specialized
    // java:S3776 - complexity is due to reflection and dynamic method invocation
    // java:S1141 - better readable that way
    // java:S3011 - low-level, reflection utility. intentionally changes the accessibility
    @SuppressWarnings({ "java:S4276", "java:S3776", "java:S1141", "java:S3011" })
    private static CopyFunction fromToFunction(final Class<?> fromClass, final Class<?> toClass) {
        final List<PropertyCopyFunction> propertySetters = new ArrayList<>();

        for (final Method fromMethod : getMethods(fromClass)) {
            if (fromMethod.getParameterCount() == 0 && fromMethod.getReturnType() != void.class) {
                final String methodName = fromMethod.getName();
                if (methodName.equals("getClass")) {
                    continue; // skip
                }
                final boolean isGet = isGet(methodName);
                if (isGet || isIs(fromMethod, methodName)) {
                    final String fieldName = Character.toLowerCase(methodName.charAt(isGet ? 3 : 2)) + methodName.substring(isGet ? 4 : 3);
                    fromMethod.setAccessible(true); // if needed
                    final UnaryOperator<Object> toGetter = toGetter(toClass, fromMethod.getName(), fieldName);
                    final String setterName = "set" + methodName.substring(isGet ? 3 : 2);
                    final ToSetter toSetter = toSetter(toClass, setterName, fieldName, fromMethod.getReturnType());
                    if (toSetter == null && toGetter == null) {
                        // we allow toSetter to be null, but in that case the toGetter must not be null and the
                        // 'from' value shall always match the to value (without setting it)
                        throw new IllegalStateException("Setter counterpart for " + fromMethod + " is not found in " + toClass.getName());
                    }
                    propertySetters.add(new PropertyCopyFunction(fromMethod, toSetter, toGetter));
                }
            }
        }

        Collections.sort(propertySetters);

        return (from, to, setNullValues, entityManager) -> {
            boolean updated = false;
            for (final CopyFunction fieldSetter : propertySetters) {
                updated = fieldSetter.apply(from, to, setNullValues, entityManager) || updated;
            }
            return updated;
        };
    }

    // java:S3776 - complexity is due to reflection and dynamic method invocation
    // java:S3011 - low-level, reflection utility. intentionally changes the accessibility
    @SuppressWarnings({ "java:S3776", "java:S3011" })
    private static ToSetter toSetter(final Class<?> toClass, final String setterName, final String fieldName, final Class<?> type) {
        try {
            final Method toSetterMethod = getMethod(toClass, setterName, type);
            final Order order = toSetterMethod.getAnnotation(Order.class);
            return new ToSetter((to, value) -> {
                try {
                    toSetterMethod.invoke(to, value instanceof String str ? str.trim() : value);
                } catch (final InvocationTargetException e) {
                    final Throwable targetException = e.getTargetException() == null ? e : e.getTargetException();
                    throw targetException instanceof RuntimeException re
                            ? re
                            : new IllegalStateException("Error invoking " + toSetterMethod, targetException);
                } catch (final IllegalAccessException | IllegalArgumentException e) {
                    throw new IllegalStateException("Error invoking " + toSetterMethod, e);
                }
            }, order == null ? Ordered.LOWEST_PRECEDENCE : order.value());
        } catch (final NoSuchMethodException nsme) {
            final Field field = getField(toClass, fieldName);
            if (field == null) {
                return null;
            } else {
                final Order order = field.getAnnotation(Order.class);
                return new ToSetter((to, value) -> {
                    try {
                        field.set(to, value);
                    } catch (final IllegalAccessException | IllegalArgumentException e) {
                        throw new IllegalStateException("Error setting field " + field, e);
                    }
                }, order == null ? Ordered.LOWEST_PRECEDENCE : order.value());
            }
        }
    }

    private static UnaryOperator<Object> toGetter(final Class<?> toClass, final String getterName, final String fieldName) {
        try {
            final Method toGetterMethod = getMethod(toClass, toClass, getterName);
            return to -> {
                try {
                    return toGetterMethod.invoke(to);
                } catch (final Exception e) {
                    throw new IllegalStateException("Error invoking " + toGetterMethod, e);
                }
            };
        } catch (final NoSuchMethodException e) {
            // no method to get current value of the target field, try with field access
            final Field field = getField(toClass, fieldName); // is or get
            return field == null ? null : to -> {
                try {
                    return field.get(to);
                } catch (final Exception e2) {
                    throw new IllegalStateException("Error getting field " + field, e2);
                }
            };
        }
    }

    private static boolean isIs(final Method fromMethod, final String methodName) {
        return methodName.startsWith("is") && methodName.length() > 2 && Character.isUpperCase(methodName.charAt(2)) &&
                fromMethod.getReturnType() == boolean.class;
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

    @NotNull
    private static Method getMethod(final Class<?> clazz, final String methodName, final Class<?> parameterType) throws NoSuchMethodException {
        try {
            return getMethod(clazz, clazz, methodName, parameterType);
        } catch (final NoSuchMethodException e) {
            final Method assignable = getMethodAssignable(clazz, methodName, parameterType);
            if (assignable != null) {
                return assignable;
            }
            final Method moreSpecific = getMethodMoreSpecific(clazz, methodName, parameterType);
            if (moreSpecific != null) {
                return moreSpecific;
            }
            throw e;
        }
    }

    @SuppressWarnings("java:S3011") // low-level, reflection utility. intentionally changes the accessibility
    @NotNull
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

    // java:S3011 - low-level, reflection utility. intentionally changes the accessibility
    // java:S3776 - complexity is due to reflection and dynamic method invocation
    @SuppressWarnings({"java:S3011", "java:S3776"})
    private static Method getMethodAssignable(final Class<?> clazz, final String methodName, final Class<?> parameterType) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> methodName.equals(method.getName()))
                .filter(method -> {
                    if (method.getParameterCount() == 1) {
                        if (method.getParameterTypes()[0].isAssignableFrom(parameterType)) {
                            return true;
                        }
                        if ((parameterType == boolean.class && method.getParameterTypes()[0] == Boolean.class) ||
                                (parameterType == Boolean.class && method.getParameterTypes()[0] == boolean.class) ||
                                (parameterType == long.class && method.getParameterTypes()[0] == Long.class) ||
                                (parameterType == Long.class && method.getParameterTypes()[0] == long.class)) {
                            return true; // in/out boxing
                        }
                    }
                    return false;
                })
                .findFirst()
                .map(method -> {
                    method.setAccessible(true);
                    return method;
                })
                .orElseGet(() -> {
                    final Class<?> superClass = clazz.getSuperclass();
                    if (superClass == null) {
                        return null;
                    } else {
                        return getMethodAssignable(superClass, methodName, parameterType);
                    }
                });
    }

    @SuppressWarnings("java:S3011") // low-level, reflection utility. intentionally changes the accessibility
    private static Method getMethodMoreSpecific(final Class<?> clazz, final String methodName, final Class<?> parameterType) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> methodName.equals(method.getName()))
                .filter(method -> method.getParameterCount() == 1 && parameterType.isAssignableFrom(method.getParameterTypes()[0]))
                .findFirst()
                .map(method -> {
                    method.setAccessible(true);
                    return method;
                })
                .orElseGet(() -> {
                    final Class<?> superClass = clazz.getSuperclass();
                    if (superClass == null) {
                        return null;
                    } else {
                        return getMethodMoreSpecific(superClass, methodName, parameterType);
                    }
                });
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

    // key for copy of class to class function
    private record FromTo(Class<?> from, Class<?> to) {}

    private record ToSetter(BiConsumer<Object, Object> toSetter, int order) {}

    @SuppressWarnings("java:S1210") // java:S1210 - return 0 only when default equals return equals, assume equal hashCodes for equal objects
        private record PropertyCopyFunction(Method fromMethod, ToSetter toSetter, UnaryOperator<Object> toGetter)
            implements CopyFunction, Comparable<PropertyCopyFunction> {

        public boolean apply(final Object from, final Object to, final boolean setNullValues, final UnaryOperator<Object> propertyProcessor) {
                final Object value;
                try {
                    value = fromMethod.invoke(from);
                } catch (final IllegalAccessException e) {
                    throw new IllegalStateException("Failed to get source value", e);
                } catch (final InvocationTargetException e) {
                    throw new IllegalStateException("Failed to get source value", e.getTargetException() == null ? e : e.getTargetException());
                }
                if (value == null && !setNullValues) { // if !setNullValues null means no change
                    return false;
                }
                if (toGetter != null) {
                    final Object currentValue = toGetter.apply(to);
                    if (Objects.equals(value, currentValue)) {
                        return false; // no change
                    }
                }
                if (toSetter == null) {
                    throw new IllegalStateException(
                            "Setter counterpart for " + fromMethod + " is not found in " + to.getClass().getName() +
                                    " and the 'from' value is not equal to the 'to' value");
                }
                toSetter.toSetter().accept(to, propertyProcessor.apply(value));
                return true;
            }

            public int compareTo(final PropertyCopyFunction other) {
                final int orderCompare = Integer.compare(this.toSetter.order(), other.toSetter.order());
                return orderCompare == 0 ? Integer.compare(this.hashCode(), other.hashCode()) : orderCompare;
            }
        }

    // functional interface to apply the copy operation
    private interface CopyFunction {

        boolean apply(Object from, Object to, boolean setNullValues, UnaryOperator<Object> propertyProcessor);
    }
}