/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import static org.eclipse.hawkbit.repository.QueryField.SUB_ATTRIBUTE_SEPARATOR;
import static org.eclipse.hawkbit.repository.QueryField.SUB_ATTRIBUTE_SPLIT_REGEX;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import jakarta.annotation.Nullable;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.FieldValueConverter;
import org.eclipse.hawkbit.repository.QueryField;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.jpa.ql.Node;
import org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison;

/**
 * {@link RsqlParser} parses RSQL query stings to {@link Node} objects. Doing that it does the following:
 * <ul>
 *     <li>check for null value and pass it properly (IS, NOT)</li>
 *     <li>check for * and convert EQ/NEQ to LIKE/NOT_LIKE</li>
 *     <li>replace the rsql fields (enum values) with the JPA entity field names</li>
 *     <li>checks sub-attributes (if allowed)</li>
 *     <li>append the default sub-attributes if needed</li>
 *     <li>apply value conversion for implementing with FieldValueConverter</li>
 * </ul>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class RsqlParser {

    public static final Character LIKE_WILDCARD = '*';

    public static final ComparisonOperator IS = new ComparisonOperator("=is=", "=eq=");
    public static final ComparisonOperator NOT = new ComparisonOperator("=not=", "=ne=");

    private static final RSQLParser RSQL_PARSER;

    static {
        final Set<ComparisonOperator> operators = new HashSet<>(RSQLOperators.defaultOperators());
        // == and != alternatives just treating "null" string as null not as a "null"
        operators.add(IS);
        operators.add(NOT);
        RSQL_PARSER = new RSQLParser(operators);
    }

    public static Node parse(final String rsql) {
        return parse(rsql, (Function<String, Key>) null);
    }

    public static <T extends Enum<T> & QueryField> Node parse(final String rsql, final Class<T> queryFieldType) {
        return parse(rsql, queryFieldType == null ? null : key -> resolveKey(key, queryFieldType));
    }

    private static Node parse(final String rsql, final Function<String, Key> keyResolver) {
        try {
            return RSQL_PARSER
                    .parse(rsql)
                    .accept(new RsqlVisitor(keyResolver));
        } catch (final RSQLParserException e) {
            throw new RSQLParameterSyntaxException(e);
        }
    }

    @SuppressWarnings("java:S3776") // java:S3776 - group in single method for easier read of whole logic
    private static <T extends Enum<T> & QueryField> Key resolveKey(final String key, final Class<T> rsqlQueryFieldType) {
        final int firstSeparatorIndex = key.indexOf(SUB_ATTRIBUTE_SEPARATOR);
        final String enumName = (firstSeparatorIndex == -1 ? key : key.substring(0, firstSeparatorIndex)).toUpperCase();
        log.debug("Get field identifier by name {} of enum type {}", enumName, rsqlQueryFieldType);

        final T enumValue;
        try {
            enumValue = Enum.valueOf(rsqlQueryFieldType, enumName);
        } catch (final IllegalArgumentException e) {
            throw new RSQLParameterUnsupportedFieldException(e);
        }

        final String attribute;
        if (firstSeparatorIndex == -1) { // just field name without sub-attribute
            if (enumValue.getSubEntityAttributes().isEmpty()) {
                // no sub-attributes -> simple field
                attribute = enumValue.getJpaEntityFieldName();
            } else {
                // just enum name for a complex type (with sub-attributes), should have single (default!) sub-attribute
                if (enumValue.isMap()) {
                    throw new RSQLParameterUnsupportedFieldException("No key specified for a map type " + enumValue);
                } else {
                    final String defaultSubEntityAttribute = enumValue.getDefaultSubEntityAttribute();
                    if (defaultSubEntityAttribute != null) {
                        // single sub attribute - so, treat it as a default
                        attribute = enumValue.getJpaEntityFieldName() + SUB_ATTRIBUTE_SEPARATOR + defaultSubEntityAttribute;
                    } else {
                        throw new RSQLParameterUnsupportedFieldException(
                                String.format(
                                        "The given search parameter field {%s} requires one of the following sub-attributes %s",
                                        key, enumValue.getSubEntityAttributes()));
                    }
                }
            }
        } else { // field name with sub-attribute
            if (enumValue.isMap()) {
                // map, the part after the enum name is the key of the map
                attribute = enumValue.getJpaEntityFieldName() + SUB_ATTRIBUTE_SEPARATOR + key.substring(firstSeparatorIndex + 1);
            } else if (enumValue.getSubEntityAttributes().isEmpty()) {
                // simple type without sub-attributes, so the sub-attribute is not allowed
                throw new RSQLParameterUnsupportedFieldException("Sub-attributes not supported for simple field " + enumValue);
            } else {
                final String[] subAttribute = key.substring(firstSeparatorIndex + 1).split(SUB_ATTRIBUTE_SPLIT_REGEX, 2);
                attribute = enumValue.getJpaEntityFieldName() + SUB_ATTRIBUTE_SEPARATOR + enumValue.getSubEntityAttributes().stream()
                        .filter(attr -> attr.equalsIgnoreCase(subAttribute[0])) // case normalized
                        .findFirst()
                        .map(attr -> subAttribute.length == 1 ? attr : attr + key.substring(firstSeparatorIndex + 1 + attr.length()))
                        .orElseThrow(() -> new RSQLParameterUnsupportedFieldException(
                                String.format(
                                        "The given search field {%s} has unsupported sub-attributes. Supported sub-attributes are %s",
                                        key, enumValue.getSubEntityAttributes())));
            }
        }

        return new Key(attribute, RsqlVisitor.valueConverter(enumValue));
    }

    private record Key(String path, UnaryOperator<Object> converter) {}

    private static class RsqlVisitor implements RSQLVisitor<Node, String> {

        private final Function<String, Key> keyResolver;

        private RsqlVisitor(final Function<String, Key> keyResolver) {
            this.keyResolver = keyResolver == null ? str -> new Key(str, UnaryOperator.identity()) : keyResolver;
        }

        @Override
        public Node visit(final AndNode node, final String param) {
            return node.getChildren().stream()
                    .map(child -> child.accept(this))
                    .reduce(Node::and)
                    .orElseThrow();
        }

        @Override
        public Node visit(final OrNode node, final String param) {
            return node.getChildren().stream()
                    .map(child -> child.accept(this))
                    .reduce(Node::or)
                    .orElseThrow();
        }

        @SuppressWarnings("java:S3776") // java:S3776 - group in single method for easier read of whole logic
        @Override
        public Node visit(final ComparisonNode node, final String param) {
            final String nodeSelector = node.getSelector();
            final Key key = keyResolver.apply(nodeSelector);
            final String path = key.path();
            final Object value = toValue(node, key);
            final ComparisonOperator op = node.getOperator();
            if (op == IS || op == RSQLOperators.EQUAL) {
                if ("".equals(value)) {
                    // keep special backward compatible behaviour tags == '' means "null / has not or ''
                    return new Comparison(path, EQ, null, nodeSelector).or(new Comparison(path, EQ, "", nodeSelector));
                }
                return new Comparison(path, isLike(value) ? LIKE : EQ, value, nodeSelector);
            } else if (op == NOT || op == RSQLOperators.NOT_EQUAL) {
                if ("".equals(value)) {
                    // keep special backward compatible behaviour. != '' means "not null / has and not ''
                    return new Comparison(path, LIKE, "*", nodeSelector).and(new Comparison(path, NE, "", nodeSelector));
                }
                return new Comparison(path, isLike(value) ? NOT_LIKE : NE, value, nodeSelector);
            } else if (op == RSQLOperators.GREATER_THAN) {
                return new Comparison(path, GT, value, nodeSelector);
            } else if (op == RSQLOperators.GREATER_THAN_OR_EQUAL) {
                return new Comparison(path, GTE, value, nodeSelector);
            } else if (op == RSQLOperators.LESS_THAN) {
                return new Comparison(path, LT, value, nodeSelector);
            } else if (op == RSQLOperators.LESS_THAN_OR_EQUAL) {
                return new Comparison(path, LTE, value, nodeSelector);
            } else if (op == RSQLOperators.IN) {
                return new Comparison(path, IN, value, nodeSelector);
            } else if (op == RSQLOperators.NOT_IN) {
                return new Comparison(path, NOT_IN, value, nodeSelector);
            } else {
                throw new IllegalArgumentException("Unsupported operator: " + node.getOperator());
            }
        }

        private Object toValue(final ComparisonNode node, final Key key) {
            final List<String> arguments = node.getArguments();
            final ComparisonOperator operator = node.getOperator();
            if (arguments.isEmpty()) {
                throw new IllegalArgumentException("Operator " + operator + " requires at least one argument");
            }

            if (arguments.size() == 1 && "null".equals(arguments.get(0)) && (operator == IS || operator == NOT)) {
                return null;
            } else {
                if (operator == RSQLOperators.IN || operator == RSQLOperators.NOT_IN) {
                    return arguments.stream().map(key.converter()).toList();
                } else if (arguments.size() > 1) {
                    throw new IllegalArgumentException(
                            "Operator " + operator + " requires exactly one argument, but got: " + arguments.size());
                } else {
                    return key.converter().apply(arguments.get(0));
                }
            }
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private static <T extends Enum<T>> UnaryOperator<Object> valueConverter(@Nullable final T enumValue) {
            if (enumValue instanceof FieldValueConverter fieldValueConverter) {
                return value -> {
                    if (value instanceof String strValue) {
                        try {
                            return fieldValueConverter.convertValue(enumValue, strValue);
                        } catch (final Exception e) {
                            throw new RSQLParameterUnsupportedFieldException(e.getMessage(), null);
                        }
                    } else {
                        return value;
                    }
                };
            } else {
                return UnaryOperator.identity();
            }
        }

        private static final String ESCAPE_CHAR_WITH_ASTERISK = "\\" + LIKE_WILDCARD;

        private static boolean isLike(final Object value) {
            if (value instanceof String valueStr) {
                if (valueStr.contains(ESCAPE_CHAR_WITH_ASTERISK)) {
                    return valueStr.replace(ESCAPE_CHAR_WITH_ASTERISK, "$").indexOf(LIKE_WILDCARD) != -1;
                } else {
                    return valueStr.indexOf(LIKE_WILDCARD) != -1;
                }
            } else {
                return false;
            }
        }
    }
}