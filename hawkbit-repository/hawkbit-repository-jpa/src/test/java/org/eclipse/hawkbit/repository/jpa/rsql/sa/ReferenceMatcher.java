/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rsql.sa;

import static org.eclipse.hawkbit.repository.jpa.rsql.Node.Comparison.Operator.EQ;
import static org.eclipse.hawkbit.repository.jpa.rsql.Node.Comparison.Operator.GT;
import static org.eclipse.hawkbit.repository.jpa.rsql.Node.Comparison.Operator.GTE;
import static org.eclipse.hawkbit.repository.jpa.rsql.Node.Comparison.Operator.IN;
import static org.eclipse.hawkbit.repository.jpa.rsql.Node.Comparison.Operator.LIKE;
import static org.eclipse.hawkbit.repository.jpa.rsql.Node.Comparison.Operator.LT;
import static org.eclipse.hawkbit.repository.jpa.rsql.Node.Comparison.Operator.LTE;
import static org.eclipse.hawkbit.repository.jpa.rsql.Node.Comparison.Operator.NE;
import static org.eclipse.hawkbit.repository.jpa.rsql.Node.Comparison.Operator.NOT_IN;
import static org.eclipse.hawkbit.repository.jpa.rsql.Node.Comparison.Operator.NOT_LIKE;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import org.eclipse.hawkbit.repository.jpa.rsql.Node;
import org.eclipse.hawkbit.repository.jpa.rsql.Node.Comparison.Operator;
import org.eclipse.hawkbit.repository.jpa.rsql.RsqlParser;

/**
 * Provides matching reference for if an object matches a {@link Node}.
 */
class ReferenceMatcher {

    private final Node root;

    private ReferenceMatcher(final Node root) {
        this.root = root;
    }

    static ReferenceMatcher of(final Node root) {
        return new ReferenceMatcher(root);
    }

    static ReferenceMatcher ofRsql(final String rsql) {
        return of(RsqlParser.parse(rsql));
    }

    <T> boolean match(final T t) {
        return match(t, root);
    }

    private static <T> boolean match(final T t, final Node node) {
        if (node instanceof Node.Comparison comparison) {
            final String[] split = comparison.getKey().split("\\.", 2);
            try {
                final Method fieldGetter = getGetter(t.getClass(), split[0]);
                fieldGetter.setAccessible(true);
                final Object fieldValue = fieldGetter.invoke(t);
                final Operator op = comparison.getOp();
                if (Map.class.isAssignableFrom(fieldGetter.getReturnType())) {
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
                                    (Class<?>) ((ParameterizedType) fieldGetter.getGenericReturnType()).getActualTypeArguments()[1]));
                } else if (Collection.class.isAssignableFrom(fieldGetter.getReturnType())) { // Set / List
                    final Object value;
                    final BiFunction<Object, Operator, Boolean> compare;
                    if (split.length == 1) {
                        value = map(comparison.getValue(), fieldGetter.getReturnType());
                        compare = (e, operator) -> compare(e, operator, value);
                    } else {
                        final Method valueGetter = getGetter(
                                (Class<?>) ((ParameterizedType) fieldGetter.getGenericReturnType()).getActualTypeArguments()[0], split[1]);
                        value = map(comparison.getValue(), valueGetter.getReturnType());
                        compare = (e, operator) -> {
                            try {
                                return compare(map(e == null ? null : valueGetter.invoke(e), valueGetter.getReturnType()), operator, value);
                            } catch (final IllegalAccessException | InvocationTargetException ex) {
                                throw new IllegalArgumentException(ex);
                            }
                        };
                    }
                    final Collection<?> set = (Collection<?>) fieldValue;
                    return switch (op) {
                        case EQ, GT, GTE, LT, LTE, IN, LIKE -> set == null
                                ? false
                                : set.stream().anyMatch(e -> compare.apply(e, op));
                        case NE, NOT_IN, NOT_LIKE -> set == null
                                ? true
                                : set.stream().noneMatch(e -> compare.apply(e, op == NE ? EQ : op == NOT_IN ? IN : LIKE));
                    };
                } else {
                    if (split.length == 1) {
                        return compare(fieldValue, op, map(comparison.getValue(), fieldGetter.getReturnType()));
                    } else {
                        final Method valueGetter = getGetter(fieldGetter.getReturnType(), split[1]);
                        return compare(fieldValue == null ? null : valueGetter.invoke(fieldValue), op,
                                map(comparison.getValue(), valueGetter.getReturnType()));
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

    private static <T> Method getGetter(final Class<T> t, final String fieldName) throws NoSuchMethodException {
        final String getterLowercase = "get" + fieldName.toLowerCase();
        return Arrays.stream(t.getMethods())
                .filter(method -> getterLowercase.equals(method.getName().toLowerCase()))
                .findFirst()
                .map(method -> {
                    method.setAccessible(true);
                    return method;
                }).orElseThrow(() -> new NoSuchMethodException("No getter found for field: " + fieldName + " in class: " + t.getName()));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
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
}