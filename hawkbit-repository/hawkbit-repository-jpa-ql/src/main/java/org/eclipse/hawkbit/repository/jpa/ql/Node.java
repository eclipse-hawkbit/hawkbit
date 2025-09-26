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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import lombok.Builder;
import lombok.Value;

/**
 * Node is always in Disjunctive Normal Form (DNF). This means:
 * <ul>
 *     <li>single comparison</li>
 *     <li>and composition of comparisons</li>
 *     <li>or of comparisons or and composition of comparisons</li>
 * </ul>
 * Node: if there are custom implementation they shall also follow this contract!
 */
public interface Node {

    default Logical and(final Node other) {
        return op(this, Logical.Operator.AND, other);
    }

    default Logical or(final Node other) {
        return op(this, Logical.Operator.OR, other);
    }

    // utility method that maps this node with a mapper that could modify comparisons - e.g. change keys, values, operators, or whatever
    // if there are no changes the same instance is returned
    default Node map(final UnaryOperator<Comparison> mapper) {
        if (this instanceof Comparison comparison) {
            return mapper.apply(comparison);
        } else {
            final List<Node> mappedChildren = new ArrayList<>();
            boolean modified = false;
            for (final Node child : ((Logical) this).getChildren()) {
                final Node mapped = child.map(mapper);
                mappedChildren.add(mapped);
                if (!mapped.equals(child)) {
                    modified = true;
                }
            }
            return modified ? new Logical(((Logical) this).getOp(), mappedChildren) : this;
        }
    }

    static Logical op(final Node node1, final Logical.Operator op, final Node node2) {
        if (node1 instanceof Logical logical1 && logical1.getOp() == op) {
            if (node2 instanceof Logical logical2 && logical2.getOp() == op) { // same op
                final List<Node> children = new ArrayList<>(logical1.getChildren().size() + logical2.getChildren().size());
                children.addAll(logical1.getChildren());
                children.addAll(logical2.getChildren());
                return new Logical(op, children);
            } else { // node2 is not a logical
                final List<Node> children = new ArrayList<>(logical1.getChildren().size() + 1);
                children.addAll(logical1.getChildren());
                children.add(node2);
                return new Logical(op, children);
            }
        } else if (node2 instanceof Logical logical2 && logical2.getOp() == op) {
            final List<Node> children = new ArrayList<>(logical2.getChildren().size() + 1);
            children.add(node1);
            children.addAll(logical2.getChildren());
            return new Logical(op, children);
        } else {
            return new Logical(op, List.of(node1, node2));
        }
    }

    @Value
    class Logical implements Node {

        public enum Operator {
            AND("&&"),
            OR("||");

            private final String symbol;

            Operator(String symbol) {
                this.symbol = symbol;
            }

            @Override
            public String toString() {
                return symbol;
            }
        }

        Logical.Operator op;
        List<Node> children;

        public Logical(final Operator op, final List<Node> children) {
            Objects.requireNonNull(op, "Operator must not be null");
            if (children == null || children.size() < 2) {
                throw new IllegalArgumentException("Children of a logical must not be null or empty or single element list");
            }
            this.op = op;
            this.children = children;
        }

        @Override
        public String toString() {
            return children.stream()
                    .map(child -> op == Operator.OR || child instanceof Comparison
                            ? child.toString()
                            : "(" + child.toString() + ")")
                    .reduce((a, b) -> a + " " + op + " " + b)
                    .orElse("");
        }
    }

    @Value
    @Builder(builderClassName = "Builder")
    class Comparison implements Node {

        public enum Operator {
            EQ("=="),
            NE("!="),
            GT(">"),
            LT("<"),
            GTE(">="),
            LTE("<="),
            IN("in"),
            NOT_IN("not in"),
            LIKE("like"),
            NOT_LIKE("not like");

            private final String symbol;

            Operator(final String symbol) {
                this.symbol = symbol;
            }

            @Override
            public String toString() {
                return symbol;
            }
        }

        @Nonnull
        String key;
        @Nonnull
        Operator op;
        @Nullable
        Object value; // String, Number, Boolean, String[] or Number[] or null

        @Nullable
        Object context;

        public Comparison(@Nonnull final String key, @Nonnull final Operator op, @Nullable final Object value) {
            this(key, op, value, null);
        }

        public Comparison(@Nonnull final String key, @Nonnull final Operator op, @Nullable final Object value, @Nullable final Object context) {
            Objects.requireNonNull(key, "Key must not be null");
            if (key.trim().isEmpty()) {
                throw new IllegalArgumentException("Key must not be empty");
            }
            Objects.requireNonNull(op, "Operator must not be null");
            if (value == null) {
                switch (op) {
                    case GT, LT, LIKE, NOT_LIKE:
                        throw new IllegalArgumentException("Value must not be null for operator " + op);
                    default: // ok
                }
            } else if (value instanceof List<?> list) {
                switch (op) {
                    case IN, NOT_IN:
                        break;
                    default:
                        throw new IllegalArgumentException("Value must not be a list for operator " + op);
                }
                if (list.isEmpty()) {
                    throw new IllegalArgumentException("List value must not be empty");
                }
                list.forEach(listElement -> {
                    if (notValid(listElement)) {
                        throw new IllegalArgumentException("List type value must be String or Number");
                    }
                });
            } else if (notValid(value)) {
                throw new IllegalArgumentException(
                        "Value must be String, Number, Boolean, non-empty String list, non-empty Number list or null");
            }

            this.key = key.trim();
            this.op = op;
            this.value = value;

            this.context = context;
        }

        @Override
        public String toString() {
            return key + " " + op + " " + valueToString(value);
        }

        private static String valueToString(final Object value) {
            if (value == null) {
                return "null";
            } else if (value instanceof String) {
                return String.format("\"%s\"", value);
            } else if (value instanceof List<?> list) {
                return "(" + list.stream()
                        .map(Comparison::valueToString)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("") + ")";
            } else {
                return value.toString();
            }
        }

        private static boolean notValid(final Object value) {
            return !(value instanceof String) && !(value instanceof Number) && !(value instanceof Boolean);
        }
    }
}