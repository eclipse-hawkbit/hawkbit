/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rsqllegacy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.PluralJoin;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

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
 * An implementation of the {@link RSQLVisitor} to visit the parsed tokens and
 * build JPA where clauses.
 *
 * @param <A> the enum for providing the field name of the entity field to filter on.
 * @param <T> the entity type referenced by the root
 * @deprecated Old implementation of RSQL Visitor. Deprecated in favour of next gen implementation - {@link JpaQueryRsqlVisitorG2}.
 *         It will be kept for some time in order to keep backward compatibility and to allow for a smooth transition. Also, in case of
 *         problems with the new implementation, this one can be used as a fallback.
 */
@Deprecated(forRemoval = true, since = "0.6.0")
@Slf4j
public class JpaQueryRsqlVisitor<A extends Enum<A> & RsqlQueryField, T> extends AbstractRSQLVisitor<A>
        implements RSQLVisitor<List<Predicate>, String> {

    public static final Character LIKE_WILDCARD = '*';
    private static final char ESCAPE_CHAR = '\\';
    private static final List<String> NO_JOINS_OPERATOR = List.of("!=", "=out=");
    private static final String ESCAPE_CHAR_WITH_ASTERISK = ESCAPE_CHAR + "*";

    private final Map<Integer, Set<Join<Object, Object>>> joinsInLevel = new HashMap<>(3);

    private final CriteriaBuilder cb;
    private final CriteriaQuery<?> query;
    private final Database database;
    private final boolean ensureIgnoreCase;
    private final Root<T> root;
    private final VirtualPropertyReplacer virtualPropertyReplacer;
    private final SimpleTypeConverter simpleTypeConverter;

    private int level;
    private boolean isOrLevel;
    private boolean joinsNeeded;

    public JpaQueryRsqlVisitor(
            final Root<T> root, final CriteriaBuilder cb, final Class<A> enumType,
            final VirtualPropertyReplacer virtualPropertyReplacer, final Database database,
            final CriteriaQuery<?> query, final boolean ensureIgnoreCase) {
        super(enumType);
        this.root = root;
        this.cb = cb;
        this.query = query;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.simpleTypeConverter = new SimpleTypeConverter();
        this.database = database;
        this.ensureIgnoreCase = ensureIgnoreCase;
    }

    @Override
    public List<Predicate> visit(final AndNode node, final String param) {
        beginLevel(false);
        final List<Predicate> childs = acceptChildren(node);
        endLevel();
        if (!childs.isEmpty()) {
            return toSingleList(cb.and(childs.toArray(new Predicate[0])));
        }
        return toSingleList(cb.conjunction());
    }

    @Override
    public List<Predicate> visit(final OrNode node, final String param) {
        beginLevel(true);
        final List<Predicate> childs = acceptChildren(node);
        endLevel();
        if (!childs.isEmpty()) {
            return toSingleList(cb.or(childs.toArray(new Predicate[0])));
        }
        return toSingleList(cb.conjunction());
    }

    @Override
    // Exception squid:S2095 - see
    // https://jira.sonarsource.com/browse/SONARJAVA-1478
    @SuppressWarnings({ "squid:S2095" })
    public List<Predicate> visit(final ComparisonNode node, final String param) {
        final QueryPath queryPath = getQueryPath(node);

        final List<String> values = node.getArguments();
        final List<Object> transformedValues = new ArrayList<>();
        final Path<Object> fieldPath = getFieldPath(queryPath.getEnumValue(), queryPath);

        for (final String value : values) {
            transformedValues.add(convertValueIfNecessary(node, queryPath.getEnumValue(), value, fieldPath));
        }

        this.joinsNeeded = this.joinsNeeded || areJoinsNeeded(node);

        return mapToPredicate(node, fieldPath, node.getArguments(), transformedValues, queryPath);
    }

    private static List<Predicate> toSingleList(final Predicate predicate) {
        return Collections.singletonList(predicate);
    }

    private static Optional<Path<?>> getFieldPath(final Root<?> root, final String[] split, final boolean isMapKeyField,
            final BiFunction<Path<?>, String, Path<?>> joinFieldPathProvider) {
        Path<?> fieldPath = null;
        for (int i = 0; i < split.length; i++) {
            if (!(isMapKeyField && i == (split.length - 1))) {
                final String fieldNameSplit = split[i];
                fieldPath = (fieldPath != null) ? fieldPath.get(fieldNameSplit) : root.get(fieldNameSplit);
                fieldPath = joinFieldPathProvider.apply(fieldPath, fieldNameSplit);
            }
        }
        return Optional.ofNullable(fieldPath);
    }

    private static boolean areJoinsNeeded(final ComparisonNode node) {
        return !NO_JOINS_OPERATOR.contains(node.getOperator().getSymbol());
    }

    // Exception squid:S2095 - see
    // https://jira.sonarsource.com/browse/SONARJAVA-1478
    @SuppressWarnings({ "rawtypes", "unchecked", "squid:S2095" })
    private static Object transformEnumValue(final ComparisonNode node, final String value, final Class<?> javaType) {
        final Class<? extends Enum> tmpEnumType = (Class<? extends Enum>) javaType;
        try {
            return Enum.valueOf(tmpEnumType, value.toUpperCase());
        } catch (final IllegalArgumentException e) {
            // we could not transform the given string value into the enum
            // type, so ignore it and return null and do not filter
            log.info("given value {} cannot be transformed into the correct enum type {}", value.toUpperCase(),
                    javaType);
            log.debug("value cannot be transformed to an enum", e);

            throw new RSQLParameterUnsupportedFieldException("field {" + node.getSelector()
                    + "} must be one of the following values {" + Arrays.stream(tmpEnumType.getEnumConstants())
                    .map(v -> v.name().toLowerCase()).toList()
                    + "}", e);
        }
    }

    private static boolean isSimpleField(final String[] split, final boolean isMapKeyField) {
        return split.length == 1 || (split.length == 2 && isMapKeyField);
    }

    private static Path<?> getInnerFieldPath(final Root<?> subqueryRoot, final String[] split,
            final boolean isMapKeyField) {
        return getFieldPath(subqueryRoot, split, isMapKeyField,
                (fieldPath, fieldNameSplit) -> getInnerJoinFieldPath(subqueryRoot, fieldPath, fieldNameSplit))
                .orElseThrow(() -> new RSQLParameterUnsupportedFieldException("RSQL field path cannot be empty",
                        null));
    }

    private static Path<?> getInnerJoinFieldPath(final Root<?> subqueryRoot, final Path<?> fieldPath,
            final String fieldNameSplit) {
        if (fieldPath instanceof Join) {
            return subqueryRoot.join(fieldNameSplit, JoinType.INNER);
        }
        return fieldPath;
    }

    private static boolean isPattern(final String transformedValue) {
        if (transformedValue.contains(ESCAPE_CHAR_WITH_ASTERISK)) {
            return transformedValue.replace(ESCAPE_CHAR_WITH_ASTERISK, "$").indexOf(LIKE_WILDCARD) != -1;
        } else {
            return transformedValue.indexOf(LIKE_WILDCARD) != -1;
        }
    }

    @SuppressWarnings("unchecked")
    private static <Y> Path<Y> pathOfString(final Path<?> path) {
        return (Path<Y>) path;
    }

    private void beginLevel(final boolean isOr) {
        level++;
        isOrLevel = isOr;
        joinsInLevel.put(level, new HashSet<>(2));
    }

    private void endLevel() {
        joinsInLevel.remove(level);
        level--;
        isOrLevel = false;
    }

    private Set<Join<Object, Object>> getCurrentJoins() {
        if (level > 0) {
            return joinsInLevel.get(level);
        }
        return Collections.emptySet();
    }

    private Optional<Join<Object, Object>> findCurrentJoinOfType(final Class<?> type) {
        return getCurrentJoins().stream().filter(j -> type.equals(j.getJavaType())).findAny();
    }

    private void addCurrentJoin(final Join<Object, Object> join) {
        if (level > 0) {
            getCurrentJoins().add(join);
        }
    }

    /**
     * Resolves the Path for a field in the persistence layer and joins the
     * required models. This operation is part of a tree traversal through an
     * RSQL expression. It creates for every field that is not part of the root
     * model a join to the foreign model. This behavior is optimized when
     * several joins happen directly under an OR node in the traversed tree. The
     * same foreign model is only joined once.
     *
     * Example: tags.name==M;(tags.name==A,tags.name==B,tags.name==C) This
     * example joins the tags model only twice, because for the OR node in
     * brackets only one join is used.
     *
     * @param enumField field from a FieldNameProvider to resolve on the persistence
     *         layer
     * @param queryPath RSQL field
     * @return the Path for a field
     */
    @SuppressWarnings("unchecked")
    private Path<Object> getFieldPath(final A enumField, final QueryPath queryPath) {
        return (Path<Object>) getFieldPath(root, queryPath.getJpaPath(), enumField.isMap(),
                this::getJoinFieldPath).orElseThrow(
                () -> new RSQLParameterUnsupportedFieldException("RSQL field path cannot be empty", null));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Path<?> getJoinFieldPath(final Path<?> fieldPath, final String fieldNameSplit) {
        if (fieldPath instanceof PluralJoin join) {
            final From joinParent = join.getParent();
            final Optional<Join<Object, Object>> currentJoinOfType = findCurrentJoinOfType(join.getJavaType());
            if (currentJoinOfType.isPresent() && isOrLevel) {
                // remove the additional join and use the existing one
                joinParent.getJoins().remove(join);
                return currentJoinOfType.get();
            } else {
                final Join newJoin = joinParent.join(fieldNameSplit, JoinType.LEFT);
                addCurrentJoin(newJoin);
                return newJoin;
            }
        }
        return fieldPath;
    }

    private Object convertValueIfNecessary(final ComparisonNode node, final A fieldName, final String value,
            final Path<Object> fieldPath) {
        // in case the value of an RSQL query e.g. type==application is an
        // enum we need to handle it separately because JPA needs the
        // correct java-type to build an expression. So String and numeric
        // values JPA can do it by it's own but not for classes like enums.
        // So we need to transform the given value string into the enum
        // class.
        final Class<?> javaType = fieldPath.getJavaType();
        if (javaType != null && javaType.isEnum()) {
            return transformEnumValue(node, value, javaType);
        }
        if (fieldName instanceof FieldValueConverter) {
            return convertFieldConverterValue(fieldName, value);
        }

        if (Boolean.TYPE.equals(javaType)) {
            return convertBooleanValue(node, value, javaType);
        }

        return value;
    }

    private Object convertBooleanValue(final ComparisonNode node, final String value, final Class<?> javaType) {
        try {
            return simpleTypeConverter.convertIfNecessary(value, javaType);
        } catch (final TypeMismatchException e) {
            throw new RSQLParameterUnsupportedFieldException(
                    "The value of the given search parameter field {" + node.getSelector()
                            + "} is not well formed. Only a boolean (true or false) value will be expected",
                    e);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Object convertFieldConverterValue(final A fieldName, final String value) {
        try {
            return ((FieldValueConverter) fieldName).convertValue(fieldName, value);
        } catch (final Exception e) {
            throw new RSQLParameterSyntaxException(e.getMessage(), null);
        }
    }

    private List<Predicate> mapToPredicate(final ComparisonNode node, final Path<Object> fieldPath,
            final List<String> values, final List<Object> transformedValues, final QueryPath queryPath) {

        String value = values.get(0);
        // if lookup is available, replace macros ...
        if (virtualPropertyReplacer != null) {
            value = virtualPropertyReplacer.replace(value);
        }

        final Predicate mapPredicate = mapToMapPredicate(fieldPath, queryPath);


        final Predicate valuePredicate = addOperatorPredicate(node, fieldPath, transformedValues, value, queryPath);
        return toSingleList(mapPredicate != null ? cb.and(mapPredicate, valuePredicate) : valuePredicate);
    }

    private Predicate addOperatorPredicate(final ComparisonNode node, final Path<Object> fieldPath,
            final List<Object> transformedValues, final String value, final QueryPath queryPath) {

        // only 'equal' and 'notEqual' can handle transformed value like
        // enums. The JPA API cannot handle object types for greaterThan etc
        // methods.
        final Object transformedValue = transformedValues.get(0);
        final String operator = node.getOperator().getSymbol();

        switch (operator) {
            case "==":
                return getEqualToPredicate(transformedValue, fieldPath);
            case "!=":
                return getNotEqualToPredicate(transformedValue, fieldPath, queryPath);
            case "=gt=":
                return cb.greaterThan(pathOfString(fieldPath), value);
            case "=ge=":
                return cb.greaterThanOrEqualTo(pathOfString(fieldPath), value);
            case "=lt=":
                return cb.lessThan(pathOfString(fieldPath), value);
            case "=le=":
                return cb.lessThanOrEqualTo(pathOfString(fieldPath), value);
            case "=in=":
                return in(pathOfString(fieldPath), transformedValues);
            case "=out=":
                return getOutPredicate(transformedValues, queryPath, fieldPath);
            default:
                throw new RSQLParameterSyntaxException(
                        "operator symbol {" + operator + "} is either not supported or not implemented");
        }
    }

    private Predicate getOutPredicate(
            final List<Object> transformedValues,
            final QueryPath queryPath, final Path<Object> fieldPath) {
        final String[] fieldNames = queryPath.getJpaPath();

        if (isSimpleField(fieldNames, queryPath.getEnumValue().isMap())) {
            final Path<String> pathOfString = pathOfString(fieldPath);
            return cb.or(cb.isNull(pathOfString), cb.not(in(pathOfString, transformedValues)));
        }

        clearOuterJoinsIfNotNeeded();

        return toNotExistsSubQueryPredicate(fieldNames, queryPath.getEnumValue(),
                expressionToCompare -> in(expressionToCompare, transformedValues));
    }

    @SuppressWarnings("unchecked")
    private Predicate mapToMapPredicate(final Path<Object> fieldPath, final QueryPath queryPath) {
        if (!queryPath.getEnumValue().isMap()) {
            return null;
        }

        final String[] graph = queryPath.getJpaPath();
        final String keyValue = graph[graph.length - 1];
        // Currently we support only string key. So below cast is safe.
        return equal((Expression<String>) (((MapJoin<?, ?, ?>) fieldPath).key()), keyValue);
    }

    private Predicate getEqualToPredicate(final Object transformedValue, final Path<Object> fieldPath) {
        if (transformedValue == null) {
            return cb.isNull(pathOfString(fieldPath));
        }

        if ((transformedValue instanceof String transformedValueStr) && !NumberUtils.isCreatable(transformedValueStr)) {
            if (ObjectUtils.isEmpty(transformedValue)) {
                return cb.or(cb.isNull(pathOfString(fieldPath)), cb.equal(pathOfString(fieldPath), ""));
            }

            if (isPattern(transformedValueStr)) { // a pattern, use like
                return like(pathOfString(fieldPath), toSQL(transformedValueStr));
            } else {
                return equal(pathOfString(fieldPath), transformedValueStr);
            }
        }

        return cb.equal(fieldPath, transformedValue);
    }

    private Predicate getNotEqualToPredicate(final Object transformedValue, final Path<Object> fieldPath,
            final QueryPath queryPath) {

        if (transformedValue == null) {
            return cb.isNotNull(pathOfString(fieldPath));
        }

        if ((transformedValue instanceof String transformedValueStr) && !NumberUtils.isCreatable(transformedValueStr)) {
            if (ObjectUtils.isEmpty(transformedValue)) {
                return cb.and(cb.isNotNull(pathOfString(fieldPath)), cb.notEqual(pathOfString(fieldPath), ""));
            }

            final String[] fieldNames = queryPath.getJpaPath();

            if (isSimpleField(fieldNames, queryPath.getEnumValue().isMap())) {
                if (isPattern(transformedValueStr)) { // a pattern, use like
                    return cb.or(cb.isNull(pathOfString(fieldPath)), notLike(pathOfString(fieldPath), toSQL(transformedValueStr)));
                } else {
                    return toNullOrNotEqualPredicate(fieldPath, transformedValueStr);
                }
            }

            clearOuterJoinsIfNotNeeded();

            if (isPattern(transformedValueStr)) { // a pattern, use like
                return toNotExistsSubQueryPredicate(fieldNames, queryPath.getEnumValue(),
                        expressionToCompare -> like(expressionToCompare, toSQL(transformedValueStr)));
            } else {
                return toNotExistsSubQueryPredicate(fieldNames, queryPath.getEnumValue(),
                        expressionToCompare -> equal(expressionToCompare, transformedValueStr));
            }
        }

        return toNullOrNotEqualPredicate(fieldPath, transformedValue);
    }

    private void clearOuterJoinsIfNotNeeded() {
        if (!joinsNeeded) {
            root.getJoins().clear();
        }
    }

    private Predicate toNullOrNotEqualPredicate(final Path<Object> fieldPath, final Object transformedValue) {
        return cb.or(
                cb.isNull(pathOfString(fieldPath)),
                transformedValue instanceof String transformedValueStr
                        ? notEqual(pathOfString(fieldPath), transformedValueStr)
                        : cb.notEqual(fieldPath, transformedValue));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Predicate toNotExistsSubQueryPredicate(final String[] fieldNames, final A enumField,
            final Function<Expression<String>, Predicate> subQueryPredicateProvider) {
        final Class<?> javaType = root.getJavaType();
        final Subquery<?> subquery = query.subquery(javaType);
        final Root subqueryRoot = subquery.from(javaType);
        final Predicate equalPredicate = cb.equal(root.get(enumField.identifierFieldName()),
                subqueryRoot.get(enumField.identifierFieldName()));
        final Path innerFieldPath = getInnerFieldPath(subqueryRoot, fieldNames, enumField.isMap());
        final Expression<String> expressionToCompare = getExpressionToCompare(innerFieldPath, enumField);
        final Predicate subQueryPredicate = subQueryPredicateProvider.apply(expressionToCompare);
        subquery.select(subqueryRoot).where(cb.and(equalPredicate, subQueryPredicate));
        return cb.not(cb.exists(subquery));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Expression<String> getExpressionToCompare(final Path innerFieldPath, final A enumField) {
        if (enumField.isMap()){
            // Currently we support only string key. So below cast is safe.
            return (Expression<String>) (((MapJoin<?, ?, ?>) pathOfString(innerFieldPath)).value());
        } else {
            return pathOfString(innerFieldPath);
        }
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
        final List<Node> children = node.getChildren();
        final List<Predicate> childs = new ArrayList<>();
        for (final Node node2 : children) {
            final List<Predicate> accept = node2.accept(this);
            if (!CollectionUtils.isEmpty(accept)) {
                childs.addAll(accept);
            } else {
                log.debug("visit logical node children but could not parse it, ignoring {}", node2);
            }
        }
        return childs;
    }

    @SuppressWarnings("java:S1221") // java:S1221 - intentionally to match the SQL wording
    private Predicate equal(final Expression<String> expressionToCompare, final String sqlValue) {
        return cb.equal(caseWise(cb, expressionToCompare), caseWise(sqlValue));
    }

    private Predicate notEqual(final Expression<String> expressionToCompare, String transformedValueStr) {
        return cb.notEqual(caseWise(cb, expressionToCompare), caseWise(transformedValueStr));
    }

    private Predicate like(final Expression<String> expressionToCompare, final String sqlValue) {
        return cb.like(caseWise(cb, expressionToCompare), caseWise(sqlValue), ESCAPE_CHAR);
    }

    private Predicate notLike(final Expression<String> expressionToCompare, final String sqlValue) {
        return cb.notLike(caseWise(cb, expressionToCompare), caseWise(sqlValue), ESCAPE_CHAR);
    }

    private Predicate in(final Expression<String> expressionToCompare, final List<Object> transformedValues) {
        final List<String> inParams = transformedValues.stream().filter(String.class::isInstance)
                .map(String.class::cast).map(this::caseWise).toList();
        return inParams.isEmpty() ? expressionToCompare.in(transformedValues) : caseWise(cb, expressionToCompare).in(inParams);
    }

    private Expression<String> caseWise(final CriteriaBuilder cb, final Expression<String> expression) {
        return ensureIgnoreCase ? cb.upper(expression) : expression;
    }

    private String caseWise(final String str) {
        return ensureIgnoreCase ? str.toUpperCase() : str;
    }
}