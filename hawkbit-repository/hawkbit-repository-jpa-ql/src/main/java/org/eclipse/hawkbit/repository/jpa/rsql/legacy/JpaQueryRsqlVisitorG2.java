/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rsql.legacy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.PluralJoin;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.LogicalNode;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.eclipse.hawkbit.repository.FieldValueConverter;
import org.eclipse.hawkbit.repository.RsqlQueryField;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.jpa.ql.SpecificationBuilder;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * An implementation of the {@link RSQLVisitor} to visit the parsed tokens and build JPA where clauses.
 *
 * @param <A> the enum for providing the field name of the entity field to filter on.
 * @param <T> the entity type referenced by the root
 * @deprecated Old implementation of RSQL Visitor (G2). Deprecated in favour of next gen implementation -
 *            {@link SpecificationBuilder}.
 *            It will be kept for some time in order to keep backward compatibility and to allow for a smooth transition. Also, in case of
 *            problems with the new implementation, this one can be used as a fallback.
 */
@Deprecated(forRemoval = true, since = "0.9.0")
@Slf4j
public class JpaQueryRsqlVisitorG2<A extends Enum<A> & RsqlQueryField, T>
        extends AbstractRSQLVisitor<A> implements RSQLVisitor<List<Predicate>, String> {

    public static final Character LIKE_WILDCARD = '*';
    private static final char ESCAPE_CHAR = '\\';
    private static final String ESCAPE_CHAR_WITH_ASTERISK = ESCAPE_CHAR + "*";

    private final Root<T> root;
    private final CriteriaQuery<?> query;
    private final CriteriaBuilder cb;
    private final Database database;
    private final VirtualPropertyReplacer virtualPropertyReplacer;
    private final boolean ensureIgnoreCase;

    private final Map<String, Path<?>> attributeToPath = new HashMap<>();
    private boolean inOr;

    public JpaQueryRsqlVisitorG2(
            final Class<A> enumType,
            final Root<T> root, final CriteriaQuery<?> query, final CriteriaBuilder cb,
            final Database database, final VirtualPropertyReplacer virtualPropertyReplacer, final boolean ensureIgnoreCase) {
        super(enumType);
        this.root = root;
        this.cb = cb;
        this.query = query;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.database = database;
        this.ensureIgnoreCase = ensureIgnoreCase;
    }

    @Override
    public List<Predicate> visit(final AndNode node, final String param) {
        final List<Predicate> children = acceptChildren(node);
        return Collections.singletonList(children.isEmpty() ? cb.conjunction() : cb.and(children.toArray(new Predicate[0])));
    }

    @Override
    public List<Predicate> visit(final OrNode node, final String param) {
        inOr = true;
        try {
            final List<Predicate> children = acceptChildren(node);
            return Collections.singletonList(children.isEmpty() ? cb.conjunction() : cb.or(children.toArray(new Predicate[0])));
        } finally {
            inOr = false;
            attributeToPath.clear();
        }
    }

    @Override
    public List<Predicate> visit(final ComparisonNode node, final String param) {
        final QueryPath queryPath = getQueryPath(node);
        final Path<?> fieldPath = getFieldPath(root, queryPath);
        return Collections.singletonList(toPredicate(node, queryPath, fieldPath, getValues(node, queryPath, fieldPath)));
    }

    @SuppressWarnings("java:S3776") // java:S3776 - easier to read at one place
    private Predicate toPredicate(final ComparisonNode node, final QueryPath queryPath, final Path<?> fieldPath, final List<Object> values) {
        final Predicate mapEntryKeyPredicate;
        if (queryPath.getEnumValue().isMap()) {
            if (node.getOperator() == IS) {
                // special handling of "not-exists"
                if (values.size() != 1) {
                    throw new RSQLParameterSyntaxException("The operator '" + IS + "' can only be used with one value");
                }
                if (values.get(0) == null) {
                    // IS operator for maps and null value is treated as doesn't exist correspondingly
                    ((PluralJoin<?, ?, ?>) fieldPath).on(toMapEntryKeyPredicate(queryPath, fieldPath));
                    return cb.isNull(fieldPath);
                }
            } else if (node.getOperator() == NOT) {
                if (values.size() != 1) {
                    throw new RSQLParameterSyntaxException("The operator '" + NOT + "' can only be used with one value");
                }
                // NOT operator for maps and null value is treated as does exist correspondingly
                ((PluralJoin<?, ?, ?>) fieldPath).on(toMapEntryKeyPredicate(queryPath, fieldPath));
                if (values.get(0) == null) {
                    // special handling of "exists"
                    return cb.isNotNull(fieldPath);
                } else {
                    // special handling or "not equal" or null (same as != but with possible optimized join - no subquery)
                    return toNotEqualToPredicate(queryPath, fieldPath, values.get(0));
                }
            }
            mapEntryKeyPredicate = toMapEntryKeyPredicate(queryPath, fieldPath);
        } else {
            mapEntryKeyPredicate = null;
        }

        final Predicate valuePredicate = toOperatorAndValuePredicate(node, queryPath, fieldPath, values);

        return mapEntryKeyPredicate == null ? valuePredicate : cb.and(mapEntryKeyPredicate, valuePredicate);
    }

    @SuppressWarnings("unchecked")
    private Predicate toMapEntryKeyPredicate(final QueryPath queryPath, final Path<?> fieldPath) {
        final String[] graph = queryPath.getJpaPath();
        return equal((Path<String>) ((MapJoin<?, ?, ?>) fieldPath).key(), graph[graph.length - 1]);
    }

    private Predicate toOperatorAndValuePredicate(
            final ComparisonNode node, final QueryPath queryPath, final Path<?> fieldPath, final List<Object> values) {
        // only 'equal' and 'notEqual' can handle transformed value like enums.
        // The JPA API cannot handle object types for greaterThan etc. methods. For them, it shall be a string.
        final Object value = values.get(0);
        final String operator = node.getOperator().getSymbol();
        return switch (operator) {
            case "==", "=is=", "=eq=" -> toEqualToPredicate(fieldPath, value);
            case "!=", "=not=", "=ne=" -> toNotEqualToPredicate(queryPath, fieldPath, value);
            case "=gt=" -> cb.greaterThan(pathOfString(fieldPath), String.valueOf(value)); // JPA handles numbers
            case "=ge=" -> cb.greaterThanOrEqualTo(pathOfString(fieldPath), String.valueOf(value));
            case "=lt=" -> cb.lessThan(pathOfString(fieldPath), String.valueOf(value));
            case "=le=" -> cb.lessThanOrEqualTo(pathOfString(fieldPath), String.valueOf(value));
            case "=in=" -> in(pathOfString(fieldPath), values);
            case "=out=" -> toOutPredicate(queryPath, fieldPath, values);
            default -> throw new RSQLParameterSyntaxException("Operator symbol {" + operator + "} is either not supported or not implemented");
        };
    }

    private Predicate toEqualToPredicate(final Path<?> fieldPath, final Object value) {
        if (value == null) {
            return cb.isNull(fieldPath);
        }

        if ((value instanceof String valueStr) && !NumberUtils.isCreatable(valueStr)) {
            if (ObjectUtils.isEmpty(value)) {
                return cb.or(cb.isNull(fieldPath), cb.equal(pathOfString(fieldPath), ""));
            }

            final Path<String> stringExpression = pathOfString(fieldPath);
            if (isPattern(valueStr)) { // a pattern, use like
                return like(stringExpression, toSQL(valueStr));
            } else {
                return equal(stringExpression, valueStr);
            }
        }

        return cb.equal(fieldPath, value);
    }

    // if value is null -> not null
    // if value is not null -> null or not equal value
    private Predicate toNotEqualToPredicate(final QueryPath queryPath, final Path<?> fieldPath, final Object value) {
        if (value == null) {
            return cb.isNotNull(fieldPath);
        }

        if (value instanceof String valueStr && !NumberUtils.isCreatable(valueStr)) {
            if (ObjectUtils.isEmpty(value)) {
                return cb.and(cb.isNotNull(fieldPath), cb.notEqual(pathOfString(fieldPath), ""));
            }

            if (isSimpleField(queryPath)) {
                if (isPattern(valueStr)) { // a pattern, use like
                    return cb.or(cb.isNull(fieldPath), notLike(pathOfString(fieldPath), toSQL(valueStr)));
                } else {
                    return toNullOrNotEqualPredicate(fieldPath, valueStr);
                }
            }

            return toNotExistsSubQueryPredicate(
                    queryPath, fieldPath, expressionToCompare ->
                            isPattern(valueStr)
                                    ? like(expressionToCompare, toSQL(valueStr)) // a pattern, use like
                                    : equal(expressionToCompare, valueStr));
        }

        return toNullOrNotEqualPredicate(fieldPath, value);
    }

    private Predicate toOutPredicate(final QueryPath queryPath, final Path<?> fieldPath, final List<Object> values) {
        if (isSimpleField(queryPath)) {
            return cb.or(cb.isNull(fieldPath), cb.not(in(pathOfString(fieldPath), values)));
        }

        return toNotExistsSubQueryPredicate(queryPath, fieldPath, expressionToCompare -> in(expressionToCompare, values));
    }

    private Path<?> getFieldPath(final Root<?> root, final QueryPath queryPath) {
        final String[] split = queryPath.getJpaPath();
        Path<?> fieldPath = null;
        for (int i = 0, end = queryPath.getEnumValue().isMap() ? split.length - 1 : split.length; i < end; i++) {
            final String fieldNameSplit = split[i];
            fieldPath = fieldPath == null ? getPath(root, fieldNameSplit) : fieldPath.get(fieldNameSplit);
        }
        if (fieldPath == null) {
            throw new RSQLParameterUnsupportedFieldException("RSQL field path must not be null", null);
        }
        return fieldPath;
    }

    @SuppressWarnings("java:S3776") // java:S3776 - easier to read at one place
    private List<Object> getValues(final ComparisonNode node, final AbstractRSQLVisitor<A>.QueryPath queryPath, final Path<?> fieldPath) {
        final List<Object> values = node.getArguments().stream()
                // if lookup is available, replace macros ...
                .map(value -> virtualPropertyReplacer == null ? value : virtualPropertyReplacer.replace(value))
                // converts value to the correct type
                .map(value -> convertValueIfNecessary(node, queryPath.getEnumValue(), fieldPath, value))
                .toList();
        if (values.isEmpty()) {
            throw new RSQLParameterSyntaxException("RSQL values must not be empty", null);
        } else if (values.size() == 1) {
            if (!(values.get(0) instanceof String)) { // enum or boolean or null - doesn's support >, >=, <, <=
                final ComparisonOperator operator = node.getOperator();
                if (operator == RSQLOperators.GREATER_THAN ||
                        operator == RSQLOperators.GREATER_THAN_OR_EQUAL ||
                        operator == RSQLOperators.LESS_THAN ||
                        operator == RSQLOperators.LESS_THAN_OR_EQUAL) {
                    final String errorMsg = values.get(0) == null ? "to null value" : "to enum or boolean field";
                    throw new RSQLParameterSyntaxException(operator.getSymbol() + " operator could not be applied " + errorMsg, null);
                }
            }
        } else {
            final ComparisonOperator operator = node.getOperator();
            if (operator != RSQLOperators.IN && operator != RSQLOperators.NOT_IN) {
                throw new RSQLParameterSyntaxException(operator.getSymbol() + " operator shall have exactly one value", null);
            }
        }
        return values;
    }

    // if root.get creates a join we call join directly in order to specify LEFT JOIN type, to include rows for missing in particular
    // table / criteria (root.get creates INNER JOIN) (see org.eclipse.persistence.internal.jpa.querydef.FromImpl implementation
    // for more details) otherwise delegate to root.get
    @SuppressWarnings("java:S1066") // java:S1066 - better reading this way
    private Path<?> getPath(final Root<?> root, final String fieldNameSplit) {
        // see org.eclipse.persistence.internal.jpa.querydef.FromImpl implementation for more details when root.get creates a join
        final Attribute<?, ?> attribute = root.getModel().getAttribute(fieldNameSplit);
        if (!attribute.isCollection()) {
            // it is a SingularAttribute and not join if it is of basic or entity persistence type
            final Type.PersistenceType persistenceType = ((SingularAttribute<?, ?>) attribute).getType().getPersistenceType();
            if (persistenceType == Type.PersistenceType.BASIC) {
                return root.get(fieldNameSplit);
            } else if (persistenceType == Type.PersistenceType.ENTITY) {
                return root.getJoins().stream()
                        .filter(join -> join.getAttribute().equals(attribute))
                        .findFirst()
                        .orElseGet(() -> root.join(fieldNameSplit, JoinType.LEFT));
            }
        }
        // if a collection - it is a join
        if (inOr && root == this.root) { // try to reuse join of the same "or" level and no subquery
            return attributeToPath.computeIfAbsent(attribute.getName(), k -> root.join(fieldNameSplit, JoinType.LEFT));
        } else {
            return root.join(fieldNameSplit, JoinType.LEFT);
        }
    }

    private Predicate toNullOrNotEqualPredicate(final Path<?> fieldPath, final Object value) {
        return cb.or(
                cb.isNull(fieldPath),
                value instanceof String valueStr ? notEqual(pathOfString(fieldPath), valueStr) : cb.notEqual(fieldPath, value));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Predicate toNotExistsSubQueryPredicate(
            final QueryPath queryPath, final Path<?> fieldPath, final Function<Path<String>, Predicate> subQueryPredicateProvider) {
        // if a subquery the field's parent joins are not actually used
        if (!inOr) {
            // so, if not in or (hence not reused) we remove them. Parent shall be a Join
            root.getJoins().remove(fieldPath.getParentPath());
        }

        final Class<?> javaType = root.getJavaType();
        final Subquery<?> subquery = query.subquery(javaType);
        final Root subqueryRoot = subquery.from(javaType);
        return cb.not(cb.exists(
                subquery.select(subqueryRoot)
                        .where(cb.and(
                                cb.equal(
                                        root.get(queryPath.getEnumValue().identifierFieldName()),
                                        subqueryRoot.get(queryPath.getEnumValue().identifierFieldName())),
                                subQueryPredicateProvider.apply(
                                        getExpressionToCompare(queryPath.getEnumValue(), getFieldPath(subqueryRoot, queryPath)))))));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Path<String> getExpressionToCompare(final A enumField, final Path fieldPath) {
        if (enumField.isMap()) {
            // Currently we support only string key. So below cast is safe.
            return (Path<String>) (((MapJoin<?, ?, ?>) fieldPath).value());
        } else {
            return pathOfString(fieldPath);
        }
    }

    // result is String, enum value, boolean or null
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Object convertValueIfNecessary(final ComparisonNode node, final A enumValue, final Path<?> fieldPath, final String value) {
        // in case the value of an RSQL query is an enum we need to transform the given value to the correspondent java type object
        final Class<?> javaType = fieldPath.getJavaType();

        if (javaType != null && javaType.isEnum()) {
            return toEnumValue(node, javaType, value);
        }

        if (enumValue instanceof FieldValueConverter fieldValueConverter) {
            try {
                return fieldValueConverter.convertValue(enumValue, value);
            } catch (final Exception e) {
                throw new RSQLParameterUnsupportedFieldException(e.getMessage(), null);
            }
        }

        if (boolean.class.equals(javaType) || Boolean.class.equals(javaType)) {
            if ("true".equals(value) || "false".equals(value)) {
                return Boolean.valueOf(value);
            } else {
                throw new RSQLParameterSyntaxException(
                        "The value of the given search parameter field {" + node.getSelector() + "} is not well formed. " +
                                "Only a boolean (true or false) value will be expected");
            }
        }

        if ("null".equals(value)) {
            final ComparisonOperator operator = node.getOperator();
            if (operator == IS || operator == NOT) {
                return null;
            }
        }

        return value;
    }

    private boolean isSimpleField(final QueryPath queryPath) {
        return queryPath.getJpaPath().length == 1 || (queryPath.getJpaPath().length == 2 && queryPath.getEnumValue().isMap());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Object toEnumValue(final ComparisonNode node, final Class<?> javaType, final String value) {
        final Class<? extends Enum> tmpEnumType = (Class<? extends Enum>) javaType;
        try {
            return Enum.valueOf(tmpEnumType, value.toUpperCase());
        } catch (final IllegalArgumentException e) {
            // we could not transform the given string value into the enum type, so ignore it and return null and do not filter
            log.info("given value {} cannot be transformed into the correct enum type {}", value.toUpperCase(), javaType);
            log.debug("value cannot be transformed to an enum", e);
            throw new RSQLParameterUnsupportedFieldException(
                    "field '" + node.getSelector() + "' must be one of the following values " +
                            "{" + Arrays.stream(tmpEnumType.getEnumConstants()).map(Enum::name).map(String::toLowerCase).toList() + "}",
                    e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Path<String> pathOfString(final Path<?> path) {
        return (Path<String>) path;
    }

    private static boolean isPattern(final String value) {
        if (value.contains(ESCAPE_CHAR_WITH_ASTERISK)) {
            return value.replace(ESCAPE_CHAR_WITH_ASTERISK, "$").indexOf(LIKE_WILDCARD) != -1;
        } else {
            return value.indexOf(LIKE_WILDCARD) != -1;
        }
    }

    private String toSQL(final String value) {
        final String escaped;
        if (database == Database.SQL_SERVER) {
            escaped = value.replace("%", "[%]").replace("_", "[_]");
        } else {
            escaped = value.replace("%", ESCAPE_CHAR + "%").replace("_", ESCAPE_CHAR + "_");
        }
        return replaceIfRequired(escaped);
    }

    private static String replaceIfRequired(final String escapedValue) {
        final String finalizedValue;
        if (escapedValue.contains(ESCAPE_CHAR_WITH_ASTERISK)) {
            finalizedValue = escapedValue.replace(ESCAPE_CHAR_WITH_ASTERISK, "$")
                    .replace(LIKE_WILDCARD, '%')
                    .replace("$", ESCAPE_CHAR_WITH_ASTERISK);
        } else {
            finalizedValue = escapedValue.replace(LIKE_WILDCARD, '%');
        }
        return finalizedValue;
    }

    private List<Predicate> acceptChildren(final LogicalNode node) {
        final List<Predicate> children = new ArrayList<>();
        for (final Node child : node.getChildren()) {
            final List<Predicate> accept = child.accept(this);
            if (!CollectionUtils.isEmpty(accept)) {
                children.addAll(accept);
            } else {
                log.debug("visit logical node children but could not parse it, ignoring {}", child);
            }
        }
        return children;
    }

    @SuppressWarnings("java:S1221") // java:S1221 - intentionally to match the SQL wording
    private Predicate equal(final Path<String> expressionToCompare, final String sqlValue) {
        if (caseWise(expressionToCompare)) {
            return cb.equal(cb.upper(expressionToCompare), sqlValue.toUpperCase());
        } else {
            return cb.equal(expressionToCompare, sqlValue);
        }
    }

    private Predicate notEqual(final Path<String> expressionToCompare, String valueStr) {
        if (caseWise(expressionToCompare)) {
            return cb.notEqual(cb.upper(expressionToCompare), valueStr.toUpperCase());
        } else {
            return cb.notEqual(expressionToCompare, valueStr);
        }
    }

    private Predicate like(final Path<String> expressionToCompare, final String sqlValue) {
        if (caseWise(expressionToCompare)) {
            return cb.like(cb.upper(expressionToCompare), sqlValue.toUpperCase(), ESCAPE_CHAR);
        } else {
            return cb.like(expressionToCompare, sqlValue, ESCAPE_CHAR);
        }
    }

    private Predicate notLike(final Path<String> expressionToCompare, final String sqlValue) {
        if (caseWise(expressionToCompare)) {
            return cb.notLike(cb.upper(expressionToCompare), sqlValue.toUpperCase(), ESCAPE_CHAR);
        } else {
            return cb.notLike(expressionToCompare, sqlValue, ESCAPE_CHAR);
        }
    }

    private Predicate in(final Path<String> expressionToCompare, final List<Object> values) {
        if (ensureIgnoreCase && expressionToCompare.getJavaType() == String.class) {
            final List<String> inParams = values.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast).map(String::toUpperCase).toList();
            return inParams.isEmpty() ? expressionToCompare.in(values) : cb.upper(expressionToCompare).in(inParams);
        } else {
            return expressionToCompare.in(values);
        }
    }

    private boolean caseWise(final Path<?> fieldPath) {
        return ensureIgnoreCase && fieldPath.getJavaType() == String.class;
    }
}