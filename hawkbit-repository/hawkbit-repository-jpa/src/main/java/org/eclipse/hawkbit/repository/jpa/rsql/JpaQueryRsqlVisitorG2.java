/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import cz.jirutka.rsql.parser.ast.LogicalNode;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.eclipse.hawkbit.repository.FieldValueConverter;
import org.eclipse.hawkbit.repository.RsqlQueryField;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * An implementation of the {@link RSQLVisitor} to visit the parsed tokens and build JPA where clauses.
 *
 * @param <A> the enum for providing the field name of the entity field to filter on.
 * @param <T> the entity type referenced by the root
 */
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

    private final SimpleTypeConverter simpleTypeConverter = new SimpleTypeConverter();
    private final Map<String, Path<?>> attributeToPath = new HashMap<>();
    private boolean inOr;

    public JpaQueryRsqlVisitorG2(final Class<A> enumType,
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
        final QuertPath queryField = getQuertPath(node);

        final List<String> values = node.getArguments();
        final List<Object> transformedValues = new ArrayList<>();
        final Path<?> fieldPath = getFieldPath(root, queryField);

        for (final String value : values) {
            transformedValues.add(convertValueIfNecessary(node, queryField.getEnumValue(), fieldPath, value));
        }

        return mapToPredicate(node, queryField, fieldPath, node.getArguments(), transformedValues);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Object transformEnumValue(final ComparisonNode node, final Class<?> javaType, final String value) {
        final Class<? extends Enum> tmpEnumType = (Class<? extends Enum>) javaType;
        try {
            return Enum.valueOf(tmpEnumType, value.toUpperCase());
        } catch (final IllegalArgumentException e) {
            // we could not transform the given string value into the enum type, so ignore it and return null and do not filter
            log.info("given value {} cannot be transformed into the correct enum type {}", value.toUpperCase(), javaType);
            log.debug("value cannot be transformed to an enum", e);

            throw new RSQLParameterUnsupportedFieldException(
                    "field {" + node.getSelector() + "} must be one of the following values {" + Arrays.stream(tmpEnumType.getEnumConstants())
                            .map(v -> v.name().toLowerCase())
                            .toList() + "}", e);
        }
    }

    private static boolean isSimpleField(final String[] split, final boolean isMapKeyField) {
        return split.length == 1 || (split.length == 2 && isMapKeyField);
    }

    @SuppressWarnings("unchecked")
    private static Path<String> pathOfString(final Path<?> path) {
        return (Path<String>) path;
    }

    private static boolean isPattern(final String transformedValue) {
        if (transformedValue.contains(ESCAPE_CHAR_WITH_ASTERISK)) {
            return transformedValue.replace(ESCAPE_CHAR_WITH_ASTERISK, "$").indexOf(LIKE_WILDCARD) != -1;
        } else {
            return transformedValue.indexOf(LIKE_WILDCARD) != -1;
        }
    }

    private List<Predicate> mapToPredicate(
            final ComparisonNode node, final QuertPath queryField,
            final Path<?> fieldPath,
            final List<String> values, final List<Object> transformedValues) {
        // if lookup is available, replace macros ...
        final String value = virtualPropertyReplacer == null ? values.get(0) : virtualPropertyReplacer.replace(values.get(0));

        final Predicate mapPredicate;
        if (queryField.getEnumValue().isMap()) {
            if (node.getOperator() == RSQLUtility.IS) {
                if (transformedValues.size() != 1) {
                    throw new RSQLParameterSyntaxException("The operator '" + RSQLUtility.IS + "' can only be used with one value");
                }
                if ("null".equals(transformedValues.get(0))) {
                    // IS operator for maps and null value is treated as doesn't exist correspondingly
                    ((PluralJoin<?, ?, ?>) fieldPath).on(mapEntryKey(queryField, fieldPath));
                    return Collections.singletonList(cb.isNull(getValueFieldPath(queryField, fieldPath)));
                }
            } else if (node.getOperator() == RSQLUtility.NOT) {
                if (transformedValues.size() != 1) {
                    throw new RSQLParameterSyntaxException("The operator '" + RSQLUtility.NOT + "' can only be used with one value");
                }
                // NOT operator for maps and null value is treated as does exist correspondingly
                ((PluralJoin<?, ?, ?>) fieldPath).on(mapEntryKey(queryField, fieldPath));
                final Path<?> valueFieldPath = getValueFieldPath(queryField, fieldPath);
                if ("null".equals(transformedValues.get(0))) {
                    return Collections.singletonList(cb.isNotNull(valueFieldPath));
                } else {
                    return Collections.singletonList(getNotEqualToPredicate(queryField, valueFieldPath, transformedValues.get(0)));
                }
            }
            mapPredicate = mapEntryKey(queryField, fieldPath);
        } else {
            mapPredicate = null;
        }

        final Predicate valuePredicate = addOperatorPredicate(
                node, queryField, getValueFieldPath(queryField, fieldPath), transformedValues, value);

        return Collections.singletonList(mapPredicate == null ? valuePredicate : cb.and(mapPredicate, valuePredicate));
    }

    private Path<?> getValueFieldPath(final QuertPath quertPath, final Path<?> fieldPath) {
        final A enumField = quertPath.getEnumValue();
        if (enumField.isMap()) {
            final Path<?> mapValuePath = enumField.getSubEntityMapTuple().map(Entry::getValue).map(fieldPath::get).orElse(null);
            return mapValuePath == null ? fieldPath : mapValuePath;
        } else {
            return fieldPath;
        }
    }

    private Predicate mapEntryKey(final QuertPath queryField, final Path<?> fieldPath) {
        return equal(mapEntryKeyPath(queryField, fieldPath), mapEntryKeyValue(queryField));
    }

    private String mapEntryKeyValue(final QuertPath queryField) {
        final String[] graph = queryField.getJpaPath();
        return graph[graph.length - 1];
    }

    @SuppressWarnings("unchecked")
    private Path<String> mapEntryKeyPath(final QuertPath queryField, final Path<?> fieldPath) {
        if (fieldPath instanceof MapJoin) {
            // Currently we support only string key. So below cast is safe.
            return (Path<String>) ((MapJoin<?, ?, ?>) fieldPath).key();
        }

        return fieldPath.get(queryField.getEnumValue().getSubEntityMapTuple()
                .map(Entry::getKey)
                .orElseThrow(() -> new UnsupportedOperationException(String.format(
                        "For the fields, defined as Map, only %s java type or tuple in the form of %s are allowed!",
                        Map.class.getName(), AbstractMap.SimpleImmutableEntry.class.getName()))));
    }

    private Predicate addOperatorPredicate(
            final ComparisonNode node, final QuertPath queryField,
            final Path<?> fieldPath, final List<Object> transformedValues, final String value) {
        // only 'equal' and 'notEqual' can handle transformed value like enums.
        // The JPA API cannot handle object types for greaterThan etc. methods.
        final Object transformedValue = transformedValues.get(0);
        final String operator = node.getOperator().getSymbol();
        return switch (operator) {
            case "==" -> getEqualToPredicate(fieldPath, transformedValue);
            case "=is=", "=eq=" -> getEqualToPredicate(fieldPath, "null".equals(transformedValue) ? null : transformedValue);
            case "!=" -> getNotEqualToPredicate(queryField, fieldPath, transformedValue);
            case "=not=", "=ne=" -> getNotEqualToPredicate(queryField, fieldPath, "null".equals(transformedValue) ? null : transformedValue);
            case "=gt=" -> cb.greaterThan(pathOfString(fieldPath), value);
            case "=ge=" -> cb.greaterThanOrEqualTo(pathOfString(fieldPath), value);
            case "=lt=" -> cb.lessThan(pathOfString(fieldPath), value);
            case "=le=" -> cb.lessThanOrEqualTo(pathOfString(fieldPath), value);
            case "=in=" -> in(pathOfString(fieldPath), transformedValues);
            case "=out=" -> getOutPredicate(queryField, fieldPath, transformedValues);
            default -> throw new RSQLParameterSyntaxException("Operator symbol {" + operator + "} is either not supported or not implemented");
        };
    }

    private Predicate getEqualToPredicate(final Path<?> fieldPath, final Object transformedValue) {
        if (transformedValue == null) {
            return cb.isNull(fieldPath);
        }

        if ((transformedValue instanceof String transformedValueStr) && !NumberUtils.isCreatable(transformedValueStr)) {
            if (ObjectUtils.isEmpty(transformedValue)) {
                return cb.or(cb.isNull(fieldPath), cb.equal(pathOfString(fieldPath), ""));
            }

            final Path<String> stringExpression = pathOfString(fieldPath);
            if (isPattern(transformedValueStr)) { // a pattern, use like
                return like(stringExpression, toSQL(transformedValueStr));
            } else {
                return equal(stringExpression, transformedValueStr);
            }
        }

        return cb.equal(fieldPath, transformedValue);
    }

    // if transformedValue is null -> not null
    // if transformedValue is not null -> null or not equal value
    private Predicate getNotEqualToPredicate(final QuertPath queryField, final Path<?> fieldPath, final Object transformedValue) {
        if (transformedValue == null) {
            return cb.isNotNull(fieldPath);
        }

        if (transformedValue instanceof String transformedValueStr && !NumberUtils.isCreatable(transformedValueStr)) {
            if (ObjectUtils.isEmpty(transformedValue)) {
                return cb.and(cb.isNotNull(fieldPath), cb.notEqual(pathOfString(fieldPath), ""));
            }

            final String[] fieldNames = queryField.getJpaPath();
            if (isSimpleField(fieldNames, queryField.getEnumValue().isMap())) {
                if (isPattern(transformedValueStr)) { // a pattern, use like
                    return cb.or(cb.isNull(fieldPath), notLike(pathOfString(fieldPath), toSQL(transformedValueStr)));
                } else {
                    return toNullOrNotEqualPredicate(fieldPath, transformedValueStr);
                }
            }

            return toNotExistsSubQueryPredicate(queryField, fieldPath, expressionToCompare ->
                    isPattern(transformedValueStr) ? // a pattern, use like
                            like(expressionToCompare, toSQL(transformedValueStr)) :
                            equal(expressionToCompare, transformedValueStr));
        }

        return toNullOrNotEqualPredicate(fieldPath, transformedValue);
    }

    private Predicate getOutPredicate(final QuertPath queryField, final Path<?> fieldPath, final List<Object> transformedValues) {
        final String[] subAttributes = queryField.getJpaPath();

        if (isSimpleField(subAttributes, queryField.getEnumValue().isMap())) {
            return cb.or(cb.isNull(fieldPath), cb.not(in(pathOfString(fieldPath), transformedValues)));
        }

        return toNotExistsSubQueryPredicate(queryField, fieldPath, expressionToCompare -> in(expressionToCompare, transformedValues));
    }

    private Path<?> getFieldPath(final Root<?> root, final QuertPath queryField) {
        final String[] split = queryField.getJpaPath();
        Path<?> fieldPath = null;
        for (int i = 0, end = queryField.getEnumValue().isMap() ? split.length - 1 : split.length; i < end; i++) {
            final String fieldNameSplit = split[i];
            fieldPath = fieldPath == null ? getPath(root, fieldNameSplit) : fieldPath.get(fieldNameSplit);
        }
        if (fieldPath == null) {
            throw new RSQLParameterUnsupportedFieldException("RSQL field path cannot be empty", null);
        }
        return fieldPath;
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
        } // if a collection - it is a join
        if (inOr && root == this.root) { // try to reuse join of the same "or" level and no subquery
            return attributeToPath.computeIfAbsent(attribute.getName(), k -> root.join(fieldNameSplit, JoinType.LEFT));
        } else {
            return root.join(fieldNameSplit, JoinType.LEFT);
        }
    }

    private Object convertValueIfNecessary(final ComparisonNode node, final A fieldName, final Path<?> fieldPath, final String value) {
        // in case the value of an RSQL query e.g. type==application is an enum we need to handle it separately because JPA needs the
        // correct java-type to build an expression. So String and numeric values JPA can do it by its own but not for classes like enums.
        // So we need to transform the given value string into the enum class.
        final Class<?> javaType = fieldPath.getJavaType();
        if (javaType != null && javaType.isEnum()) {
            return transformEnumValue(node, javaType, value);
        }
        if (fieldName instanceof FieldValueConverter) {
            return convertFieldConverterValue(node, fieldName, value);
        }

        if (Boolean.TYPE.equals(javaType) || Boolean.class.equals(javaType)) {
            return convertBooleanValue(node, javaType, value);
        }

        return value;
    }

    private Object convertBooleanValue(final ComparisonNode node, final Class<?> javaType, final String value) {
        try {
            return simpleTypeConverter.convertIfNecessary(value, javaType);
        } catch (final TypeMismatchException e) {
            throw new RSQLParameterSyntaxException(
                    "The value of the given search parameter field {" + node.getSelector() + "} is not well formed. " +
                            "Only a boolean (true or false) value will be expected {",
                    e);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Object convertFieldConverterValue(final ComparisonNode node, final A fieldName, final String value) {
        final Object convertedValue = ((FieldValueConverter) fieldName).convertValue(fieldName, value);
        if (convertedValue == null) {
            throw new RSQLParameterUnsupportedFieldException(
                    "field {" + node.getSelector() + "} must be one of the following values " +
                            "{" + Arrays.toString(((FieldValueConverter) fieldName).possibleValues(fieldName)) + "}",
                    null);
        } else {
            return convertedValue;
        }
    }

    private Predicate toNullOrNotEqualPredicate(final Path<?> fieldPath, final Object transformedValue) {
        return cb.or(
                cb.isNull(fieldPath),
                transformedValue instanceof String transformedValueStr
                        ? notEqual(pathOfString(fieldPath), transformedValueStr)
                        : cb.notEqual(fieldPath, transformedValue));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Predicate toNotExistsSubQueryPredicate(final QuertPath queryField, final Path<?> fieldPath,
            final Function<Path<String>, Predicate> subQueryPredicateProvider) {
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
                                        root.get(queryField.getEnumValue().identifierFieldName()),
                                        subqueryRoot.get(queryField.getEnumValue().identifierFieldName())),
                                subQueryPredicateProvider.apply(
                                        getExpressionToCompare(queryField.getEnumValue(), getFieldPath(subqueryRoot, queryField)))))));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Path<String> getExpressionToCompare(final A enumField, final Path fieldPath) {
        if (!enumField.isMap()) {
            return pathOfString(fieldPath);
        }
        if (fieldPath instanceof MapJoin) {
            // Currently we support only string key. So below cast is safe.
            return (Path<String>) (((MapJoin<?, ?, ?>) fieldPath).value());
        }
        return enumField.getSubEntityMapTuple()
                .map(Entry::getValue)
                .map(fieldPath::<String>get)
                .orElseThrow(() ->
                        new UnsupportedOperationException(
                                "For the fields, defined as Map, only Map java type or tuple in the form of SimpleImmutableEntry are allowed." +
                                        " Neither of those could be found!"));
    }

    private String toSQL(final String transformedValue) {
        final String escaped;
        if (database == Database.SQL_SERVER) {
            escaped = transformedValue.replace("%", "[%]").replace("_", "[_]");
        } else {
            escaped = transformedValue.replace("%", ESCAPE_CHAR + "%").replace("_", ESCAPE_CHAR + "_");
        }
        return replaceIfRequired(escaped);
    }

    private String replaceIfRequired(final String escapedValue) {
        final String finalizedValue;
        if (escapedValue.contains(ESCAPE_CHAR_WITH_ASTERISK)) {
            finalizedValue = escapedValue.replace(ESCAPE_CHAR_WITH_ASTERISK, "$").replace(LIKE_WILDCARD, '%')
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

    private Predicate notEqual(final Path<String> expressionToCompare, String transformedValueStr) {
        if (caseWise(expressionToCompare)) {
            return cb.notEqual(cb.upper(expressionToCompare), transformedValueStr.toUpperCase());
        } else {
            return cb.notEqual(expressionToCompare, transformedValueStr);
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

    private Predicate in(final Path<String> expressionToCompare, final List<Object> transformedValues) {
        if (ensureIgnoreCase && expressionToCompare.getJavaType() == String.class) {
            final List<String> inParams = transformedValues.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast).map(String::toUpperCase).toList();
            return inParams.isEmpty() ? expressionToCompare.in(transformedValues) : cb.upper(expressionToCompare).in(inParams);
        } else {
            return expressionToCompare.in(transformedValues);
        }
    }

    private boolean caseWise(final Path<?> fieldPath) {
        return ensureIgnoreCase && fieldPath.getJavaType() == String.class;
    }
}