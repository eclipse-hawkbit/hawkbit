/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.ql;

import static org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison.Operator.EQ;
import static org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison.Operator.GT;
import static org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison.Operator.GTE;
import static org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison.Operator.IN;
import static org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison.Operator.LIKE;
import static org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison.Operator.LT;
import static org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison.Operator.LTE;
import static org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison.Operator.NE;
import static org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison.Operator.NOT_IN;
import static org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison.Operator.NOT_LIKE;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

import org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison.Operator;

/**
 * Provides entity matcher that matches an entity object against a filter (a {@link Node} or an RSQL string).
 */
public class EntityMatcher {

    private final Node root;

    private EntityMatcher(final Node root) {
        this.root = root;
    }

    public static EntityMatcher of(final Node root) {
        return new EntityMatcher(root);
    }

    public <T> boolean match(final T t) {
        return match(t, root);
    }

    @SuppressWarnings({"java:S3776", "java:S3358", "java:S1125", "java:S6541"}) // better readable this way
    private static <T> boolean match(final T t, final Node node) {
        if (node instanceof Node.Comparison comparison) {
            final String[] split = comparison.getKey().split("\\.", 2);
            try {
                final Getter fieldGetter = getGetter(t.getClass(), split[0]);
                final Object fieldValue = fieldGetter.get(t);
                final Operator op = comparison.getOp();
                if (Map.class.isAssignableFrom(getReturnType(fieldGetter))) {
                    if ((op == NE || op == NOT_IN || op == NOT_LIKE)
                            && (fieldValue == null || !((Map<?, ?>) fieldValue).containsKey(split[1]))) {
                        // TODO / recheck - when missing entity shall it be included or not in != or =out=? - now it's not
                        return false;
                    }
                    return compare(
                            fieldValue == null ? null : ((Map<?, ?>) fieldValue).get(split[1]),
                            op,
                            map(
                                    comparison.getValue(),
                                    (Class<?>) ((ParameterizedType) fieldGetter.type()).getActualTypeArguments()[1]));
                } else if (Collection.class.isAssignableFrom(getReturnType(fieldGetter))) { // Set / List
                    final Object value;
                    final BiPredicate<Object, Operator> compare;
                    if (split.length == 1) {
                        value = map(comparison.getValue(), getReturnType(fieldGetter));
                        compare = (e, operator) -> compare(e, operator, value);
                    } else {
                        final Getter valueGetter = getGetter(
                                (Class<?>) ((ParameterizedType) fieldGetter.type()).getActualTypeArguments()[0], split[1]);
                        value = map(comparison.getValue(), getReturnType(valueGetter));
                        compare = (e, operator) -> {
                            try {
                                return compare(map(e == null ? null : valueGetter.get(e), getReturnType(valueGetter)), operator, value);
                            } catch (final IllegalAccessException | InvocationTargetException ex) {
                                throw new IllegalArgumentException(ex);
                            }
                        };
                    }
                    final Collection<?> set = (Collection<?>) fieldValue;
                    return switch (op) {
                        case EQ, GT, GTE, LT, LTE, IN, LIKE -> set == null
                                ? false
                                : set.stream().anyMatch(e -> compare.test(e, op));
                        case NE, NOT_IN, NOT_LIKE -> set == null
                                ? true
                                : set.stream().noneMatch(e -> compare.test(e, op == NE ? EQ : op == NOT_IN ? IN : LIKE));
                    };
                } else {
                    if (split.length == 1) {
                        return compare(fieldValue, op, map(comparison.getValue(), getReturnType(fieldGetter)));
                    } else {
                        if (split[1].contains(".")) {
                            // nested field access
                            final String[] nestedSplit = split[1].split("\\.", 2);
                            final Getter nestedFieldGetter = getGetter(getReturnType(fieldGetter), nestedSplit[0]);
                            final Getter valueGetter = getGetter(getReturnType(nestedFieldGetter), nestedSplit[1]);
                            final Object nestedFieldValue = fieldValue == null ? null : nestedFieldGetter.get(fieldValue);
                            return compare(
                                    nestedFieldValue == null ? null : valueGetter.get(nestedFieldValue),
                                    op,
                                    map(comparison.getValue(), getReturnType(valueGetter)));
                        } else {
                            final Getter valueGetter = getGetter(getReturnType(fieldGetter), split[1]);
                            return compare(
                                    fieldValue == null ? null : valueGetter.get(fieldValue),
                                    op,
                                    map(comparison.getValue(), getReturnType(valueGetter)));
                        }
                    }
                }
            } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new IllegalArgumentException(e);
            }
        } else if (node instanceof Node.Logical logical) {
            return switch (logical.getOp()) {
                case AND -> logical.getChildren().stream().allMatch(child -> match(t, child));
                case OR -> logical.getChildren().stream().anyMatch(child -> match(t, child));
            };
        } else {
            throw new IllegalArgumentException("Unsupported node type: " + node.getClass());
        }
    }

    @SuppressWarnings("java:S3011") // java:S3011 uses reflection to private members antway
    private static <T> Getter getGetter(final Class<T> t, final String fieldName) throws NoSuchMethodException {
        final String[] parts = fieldName.split("\\.");
        if (parts.length > 1) {
            final Getter firstGetter = getGetter(t, parts[0]);
            final Getter nextGetter = getGetter(t, fieldName.substring(parts[0].length() + 1));
            return new Getter() {

                @Override
                public Object get(final Object obj) throws IllegalAccessException, InvocationTargetException {
                    return nextGetter.get(firstGetter.get(obj));
                }

                @Override
                public Type type() {
                    return nextGetter.type();
                }
            };
        }

        final String getterLowercase = "get" + fieldName.toLowerCase();
        return Arrays.stream(t.getMethods())
                .filter(method -> getterLowercase.equals(method.getName().toLowerCase()))
                .findFirst()
                .map(Method::getName)
                .map(getterName -> {
                    try {
                        // gets method via Class.getMethod(String, Class<?>...) because in listing it might have not
                        // the correct return type, but the type got from a declaring generic type
                        final Method getter = t.getMethod(getterName);
                        getter.setAccessible(true);
                        return new Getter() {

                            @Override
                            public Object get(final Object obj) throws IllegalAccessException, InvocationTargetException {
                                return getter.invoke(obj);
                            }

                            @Override
                            public Type type() {
                                return getter.getGenericReturnType();
                            }
                        };
                    } catch (final NoSuchMethodException e) {
                        throw new IllegalStateException("Unexpected: No getter found for field: " + fieldName + " in class: " + t.getName(), e);
                    }
                }).orElseThrow(() -> new NoSuchMethodException("No getter found for field: " + fieldName + " in class: " + t.getName()));
    }

    private static Class<?> getReturnType(final Getter getter) {
        return getter.type() instanceof Class<?> clazz ? clazz : (Class<?>) ((ParameterizedType) getter.type()).getRawType();
    }

    @SuppressWarnings({ "unchecked", "rawtypes", "java:S3776" }) // java:S3776 - better readable this way
    private static Object map(final Object value, final Class<?> type) {
        if (value instanceof Collection<?> collection) { // in / out
            return collection.stream().map(e -> map(e, type)).toList();
        }

        if (value == null) {
            return null;
        } else if (type.isInstance(value)) {
            return value;
        } else if (type.isEnum()) {
            return Enum.valueOf((Class<Enum>) type, value.toString());
        } else if (type == Boolean.class || type == boolean.class) {
            return Boolean.parseBoolean(value.toString());
        } else if (type == Integer.class || type == int.class) {
            return Integer.parseInt(value.toString());
        } else if (type == Long.class || type == long.class) {
            return Long.parseLong(value.toString());
        } else if (type == Float.class || type == float.class) {
            return Float.parseFloat(value.toString());
        } else if (type == Double.class || type == double.class) {
            return Double.parseDouble(value.toString());
        } else if (type == String.class) {
            return String.valueOf(value);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    private static boolean compare(final Object o1, final Operator op, final Object o2) {
        if ((o1 == null || o2 == null) && // null is not comparable!
                (op == GT || op == GTE || op == LT || op == LTE)) {
            return false;
        }
        return switch (op) {
            case EQ -> Objects.equals(o1, o2);
            case NE -> !Objects.equals(o1, o2);
            case GT -> compare(o1, o2) > 0;
            case GTE -> compare(o1, o2) >= 0;
            case LT -> compare(o1, o2) < 0;
            case LTE -> compare(o1, o2) <= 0;
            case IN -> in(o1, o2);
            case NOT_IN -> !in(o1, o2);
            case LIKE -> like(o2, o1);
            case NOT_LIKE -> !like(o2, o1);
        };
    }

    @SuppressWarnings("unchecked")
    private static int compare(final Object o1, final Object o2) {
        return toComparable(o1).compareTo(toComparable(o2));
    }

    @SuppressWarnings("rawtypes")
    private static Comparable toComparable(final Object o) {
        if (o instanceof Comparable<?> comparable) {
            return comparable;
        } else {
            throw new IllegalArgumentException("Can't cast " + o.getClass() + " to Comparable");
        }
    }

    private static boolean in(final Object o, final Object elementOrCollection) {
        if (elementOrCollection instanceof Collection<?> collection) {
            return collection.contains(o);
        } else {
            return Objects.equals(o, elementOrCollection);
        }
    }

    private static boolean like(final Object pattern, final Object value) {
        if (pattern instanceof String patternStr) {
            if (value instanceof String valueStr) {
                return valueStr.matches(patternStr.replace("\\*", "$").replace("*", ".*").replace("$", "\\*"));
            } else if (value == null) {
                return false; // null value cannot match any pattern
            } else {
                throw new IllegalArgumentException("LIKE value must be String. Found: " + value.getClass());
            }
        } else {
            throw new IllegalArgumentException("LIKE pattern must be String. Found: " + (pattern == null ? null : pattern.getClass()));
        }
    }

    private interface Getter {

        Object get(Object obj) throws IllegalAccessException, InvocationTargetException;

        Type type();
    }
}