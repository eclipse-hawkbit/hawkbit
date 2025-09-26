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

import static org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison.Operator.GT;
import static org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison.Operator.GTE;
import static org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison.Operator.IN;
import static org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison.Operator.LIKE;
import static org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison.Operator.LT;
import static org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison.Operator.LTE;
import static org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison.Operator.NE;
import static org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison.Operator.NOT_IN;
import static org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison.Operator.NOT_LIKE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SetAttribute;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison;
import org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison.Operator;
import org.eclipse.hawkbit.repository.jpa.ql.Node.Logical;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.util.ObjectUtils;

@Slf4j
public class SpecificationBuilder<T> {

    private static final char ESCAPE_CHAR = '\\';

    private final boolean ensureIgnoreCase;
    private final Database database;

    public SpecificationBuilder(final boolean ensureIgnoreCase, final Database database) {
        this.ensureIgnoreCase = ensureIgnoreCase;
        this.database = database;
    }

    public Specification<T> specification(final Node node) {
        return (root, query, cb) -> {
            Objects.requireNonNull(query).distinct(true);
            return new PredicateBuilder(root, query, cb).build(node);
        };
    }

    private class PredicateBuilder {

        private static final char LIKE_WILDCARD = '*';
        private static final String LIKE_WILDCARD_STR = String.valueOf(LIKE_WILDCARD);
        private static final String ESCAPE_CHAR_WITH_ASTERISK = ESCAPE_CHAR + LIKE_WILDCARD_STR;

        private static final Predicate[] PREDICATES_ARRAY_0 = new Predicate[0];
        private static final List<Object> NULL_VALUE;

        static {
            final List<Object> nullList = new ArrayList<>(1);
            nullList.add(null);
            NULL_VALUE = Collections.unmodifiableList(nullList);
        }

        private final Root<T> root;
        private final CriteriaQuery<?> query;
        private final CriteriaBuilder cb;

        private final PathResolver pathResolver = new PathResolver();

        private PredicateBuilder(final Root<T> root, final CriteriaQuery<?> query, final CriteriaBuilder cb) {
            this.root = root;
            this.query = query;
            this.cb = cb;
        }

        public Predicate build(final Node node) {
            if (node instanceof Comparison comparison) {
                return predicate(comparison);
            } else if (node instanceof Logical logical) {
                final Logical.Operator op = Objects.requireNonNull(logical.getOp());
                if (op == Logical.Operator.AND) {
                    return cb.and(logical.getChildren().stream()
                            .map(this::build)
                            .toList()
                            .toArray(PREDICATES_ARRAY_0));
                } else if (op == Logical.Operator.OR) {
                    final Map<String, Integer> state = pathResolver.getState();
                    return cb.or(logical.getChildren().stream()
                            .map(child -> {
                                pathResolver.reset(state);
                                return build(child); // for or path resolver joins could be reused
                            })
                            .toList()
                            .toArray(PREDICATES_ARRAY_0));
                } else {
                    throw new IllegalArgumentException("Unsupported logical operator: " + op);
                }
            } else {
                throw new IllegalArgumentException("Unsupported node type: " + node.getClass());
            }
        }

        @SuppressWarnings({ "unchecked", "java:S3776" }) // java:S3776 - easier to read at one place
        private Predicate predicate(final Comparison comparison) {
            final String[] split = comparison.getKey().split("\\.", 2); // { attributeName [, sub attribute / map key]
            final Attribute<? super T, ?> attribute = root.getModel().getAttribute(split[0]);
            final Operator op = comparison.getOp();
            if (attribute instanceof MapAttribute<?, ?, ?>) {
                if (split.length < 2 || ObjectUtils.isEmpty(split[1])) {
                    throw new RSQLParameterUnsupportedFieldException(
                            String.format("No key for the map field found. Syntax is: %s.<key name>", getPathContext(comparison)));
                }
                if (comparison.getValue() == null) {
                    // map entry with key is null (doesn't exist) / is not null (exists) - use left join with on
                    return switch (op) {
                        case EQ -> cb.isNull(toMapValuePath(pathResolver.getJoinOn(attribute, split[1])));
                        case NE -> cb.isNotNull(toMapValuePath(pathResolver.getJoinOn(attribute, split[1])));
                        default -> throw new RSQLParameterSyntaxException(
                                String.format("Operator %s is not supported for map fields with value null", op));
                    };
                } else {
                    final MapJoin<?, ?, ?> mapPath = (MapJoin<?, ?, ?>) pathResolver.getPath(attribute);
                    return isNot(op)
                            ? compare(comparison, toMapValuePath(pathResolver.getJoinOnInner(attribute, split[1])))
                            : cb.and(equal(mapPath.key(), split[1]), compare(comparison, toMapValuePath(mapPath)));
                }
            } else if (attribute instanceof SetAttribute<?, ?> setAttribute) {
                if (split.length < 2 || ObjectUtils.isEmpty(split[1])) {
                    throw new RSQLParameterUnsupportedFieldException(
                            String.format("No mapping key for a set field found. Syntax is: %s.<ref name>", getPathContext(comparison)));
                }
                if (isNot(op)) {
                    if (op == NOT_LIKE && LIKE_WILDCARD_STR.equals(comparison.getValue())) {
                        // optimized (?) has non-null/empty attribute - do or with on instead of subquery
                        return cb.and(cb.isNull(pathResolver.getPath(attribute).get(split[1])));
                    }
                    return notEqualInLike(comparison, setAttribute, split[1]);
                } else {
                    if (op == LIKE && LIKE_WILDCARD_STR.equals(comparison.getValue())) {
                        // optimized (?) has non-null/empty attribute - do or with on instead of subquery
                        return cb.and(cb.isNotNull(deepGetPath(pathResolver.getPath(attribute), split[1])));
                    }
                    return compare(comparison, deepGetPath(pathResolver.getPath(attribute), split[1]));
                }
            } else { // singular attribute (BASIC and EMBEDDABLE) or plural (ListAttribute of entities)
                final Path<?> attributePath = pathResolver.getPath(attribute);
                return compare(comparison, split.length > 1 ? deepGetPath(attributePath, split[1]) : attributePath);
            }
        }

        @SuppressWarnings("unchecked")
        private static Path<String> toMapValuePath(final Path<?> mapJoin) {
            final Path<?> valuePath = ((MapJoin<?, ?, ?>) mapJoin).value();
            return valuePath.getJavaType() == String.class ? (Path<String>) valuePath : valuePath.get("value");
        }

        private Predicate compare(final Comparison comparison, final Path<?> fieldPath) {
            final List<Object> values = getValues(comparison, fieldPath.getJavaType());
            final Object firstValue = values.get(0);
            return switch (comparison.getOp()) {
                case EQ -> firstValue == null ? cb.isNull(fieldPath) : equal(fieldPath, firstValue);
                case NE -> firstValue == null ? cb.isNotNull(fieldPath) : cb.or(cb.isNull(fieldPath), notEqual(fieldPath, firstValue));
                case GT -> cb.greaterThan(stringPath(fieldPath), String.valueOf(firstValue)); // JPA handles numbers
                case GTE -> cb.greaterThanOrEqualTo(stringPath(fieldPath), String.valueOf(firstValue));
                case LT -> cb.lessThan(stringPath(fieldPath), String.valueOf(firstValue));
                case LTE -> cb.lessThanOrEqualTo(stringPath(fieldPath), String.valueOf(firstValue));
                case IN -> in(stringPath(fieldPath), values);
                case NOT_IN -> cb.or(cb.isNull(fieldPath), cb.not(in(stringPath(fieldPath), values)));
                case LIKE -> like(stringPath(fieldPath), toSqlLikeValue(String.valueOf(firstValue)));
                case NOT_LIKE -> cb.or(cb.isNull(fieldPath), notLike(stringPath(fieldPath), toSqlLikeValue(String.valueOf(firstValue))));
            };
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private Predicate notEqualInLike( // NE, NOT_IN, NOT_LIKE
                final Comparison comparison, final PluralAttribute<?, ?, ?> pluralAttribute, final String subAttributeName) {
            final List<Object> values = getValues(comparison, pluralAttribute.getElementType().getJavaType());
            final Object value = values.get(0);
            final EntityType<T> model = root.getModel();
            if (!model.hasSingleIdAttribute()) {
                throw new IllegalStateException("Root entity has no single id attribute");
            }
            final String idAttributeName = model.getId(model.getIdType().getJavaType()).getName();
            final Class<? extends T> javaType = root.getJavaType();
            final Subquery<? extends T> subquery = query.subquery(javaType);
            final Root<? extends T> subqueryRoot = subquery.from(javaType);
            final Path<?> joinPath = subAttributeName == null // if null it is a map
                    ? ((MapJoin<?, ?, ?>) subqueryRoot.join(pluralAttribute.getName(), JoinType.LEFT)).value()
                    : deepGetPath(subqueryRoot.join(pluralAttribute.getName(), JoinType.LEFT), subAttributeName);
            final Path<String> fieldPath = joinPath instanceof MapJoin<?, ?, ?> mapJoin
                    ? (Path<String>) mapJoin.value()
                    : stringPath(joinPath);
            return cb.not(cb.exists(
                    subquery.select((Expression) subqueryRoot)
                            .where(cb.and(
                                    cb.equal(root.get(idAttributeName), subqueryRoot.get(idAttributeName)),
                                    switch (comparison.getOp()) {
                                        case NE -> equal(fieldPath, value);
                                        case NOT_IN -> in(fieldPath, values);
                                        case NOT_LIKE -> like(fieldPath, toSqlLikeValue((String) value));
                                        default -> throw new IllegalStateException("Uncovered flow. Operator: " + comparison.getOp());
                                    }))));
        }

        // java:S1066 - easier to understand separately
        // java:S3776, java:S3358 - easier to read at one place
        @SuppressWarnings({ "java:S1066", "java:S3776", "java:S3358" })
        private List<Object> getValues(final Comparison comparison, final Class<?> javaType) {
            final Object value = comparison.getValue();
            final List<Object> values = (value == null ? NULL_VALUE : (value instanceof List<?> list ? list : List.of(value))).stream()
                    // converts value to the correct type
                    .map(element -> element instanceof String strElement
                            ? convertValueIfNecessary(strElement, javaType, comparison)
                            : element)
                    .toList();
            if (values.isEmpty()) {
                throw new RSQLParameterSyntaxException("RSQL values must not be empty", null);
            } else if (values.size() == 1) {
                final Operator op = comparison.getOp();
                // enum, boolean or null - doesn't support >, >=, <, <=
                if (!(values.get(0) instanceof String)) {
                    if (values.get(0) instanceof Number) {
                        if (op == LIKE || op == NOT_LIKE) {
                            throw new RSQLParameterSyntaxException(op + " operator could not be applied number", null);
                        }
                    } else if (op == GT || op == GTE || op == LT || op == LTE || op == LIKE || op == NOT_LIKE) {
                        final String errorMsg = values.get(0) == null ? "null value" : "enum or boolean field";
                        throw new RSQLParameterSyntaxException(op + " operator could not be applied to " + errorMsg, null);
                    }
                }
            } else {
                final Operator op = comparison.getOp();
                if (op != IN && op != NOT_IN) {
                    throw new RSQLParameterSyntaxException(op + " operator shall have exactly one value", null);
                }
            }
            return values;
        }

        // result is String, enum value, boolean or null
        private Object convertValueIfNecessary(final String value, final Class<?> javaType, final Comparison comparison) {
            if (javaType != null && javaType.isEnum()) {
                return toEnumValue(value, javaType, comparison);
            }

            if (boolean.class.equals(javaType) || Boolean.class.equals(javaType)) {
                if ("true".equals(value) || "false".equals(value)) {
                    return Boolean.valueOf(value);
                } else {
                    throw new RSQLParameterSyntaxException(
                            String.format(
                                    "The value of %S is not well formed. Only a boolean (true or false) value will be expected",
                                    getPathContext(comparison)));
                }
            }

            return value;
        }

        @Nonnull
        private static Object getPathContext(final Comparison comparison) {
            return comparison.getContext() == null ? comparison.getKey() : comparison.getContext();
        }

        private static boolean isNot(final Operator op) {
            return op == NE || op == NOT_IN || op == NOT_LIKE;
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private static Object toEnumValue(final String value, final Class<?> javaType, final Comparison comparison) {
            final Class<? extends Enum> tmpEnumType = (Class<? extends Enum>) javaType;
            try {
                return Enum.valueOf(tmpEnumType, value.toUpperCase());
            } catch (final IllegalArgumentException e) {
                // we could not transform the given string value into the enum type, so ignore it and return null and do not filter
                if (log.isInfoEnabled()) {
                    log.info("Provided value '{}' cannot be transformed into the correct enum type {}", value.toUpperCase(), javaType);
                } else {
                    log.debug("Provided value '{}' cannot be transformed into the correct enum type {}", value.toUpperCase(), javaType, e);
                }
                throw new RSQLParameterUnsupportedFieldException(
                        String.format(
                                "Value of %s must be one of the following: %s",
                                getPathContext(comparison),
                                Arrays.stream(tmpEnumType.getEnumConstants())
                                        .map(Enum::name)
                                        .map(String::toLowerCase)
                                        .toList()),
                        e);
            }
        }

        private static Path<?> deepGetPath(final Path<?> path, final String subAttributeName) {
            return deepGetPath(path, subAttributeName.split("\\."), 0);
        }

        private static Path<?> deepGetPath(final Path<?> path, final String[] subAttributeNameSplit, int startIndex) {
            final String subAttributeName = subAttributeNameSplit[startIndex++];
            if (startIndex == subAttributeNameSplit.length) {
                return path.get(subAttributeName);
            } else { // else its a deeper path so request left join
                if (path instanceof Join<?, ?> join) {
                    return deepGetPath(join.join(subAttributeName, JoinType.LEFT), subAttributeNameSplit, startIndex);
                } else {
                    throw new RSQLParameterSyntaxException("Unexpected sub attribute " + subAttributeName);
                }
            }
        }

        @SuppressWarnings("unchecked")
        private static Path<String> stringPath(final Path<?> path) {
            return (Path<String>) path;
        }

        private String toSqlLikeValue(final String value) {
            final String escaped;
            if (database == Database.SQL_SERVER) {
                escaped = value.replace("%", "[%]").replace("_", "[_]");
            } else {
                escaped = value.replace("%", ESCAPE_CHAR + "%").replace("_", ESCAPE_CHAR + "_");
            }
            final String finalizedValue;
            if (escaped.contains(ESCAPE_CHAR_WITH_ASTERISK)) {
                finalizedValue = escaped.replace(ESCAPE_CHAR_WITH_ASTERISK, "$")
                        .replace(LIKE_WILDCARD, '%')
                        .replace("$", ESCAPE_CHAR_WITH_ASTERISK);
            } else {
                finalizedValue = escaped.replace(LIKE_WILDCARD, '%');
            }
            return finalizedValue;
        }

        @SuppressWarnings("java:S1221") // java:S1221 - intentionally to match the SQL wording
        private Predicate equal(final Path<?> fieldPath, final Object value) {
            if (value instanceof String valueStr && caseWise(fieldPath)) {
                return cb.equal(cb.upper(stringPath(fieldPath)), valueStr.toUpperCase());
            } else {
                return cb.equal(fieldPath, value);
            }
        }

        private Predicate notEqual(final Path<?> fieldPath, Object value) {
            if (value instanceof String valueStr && caseWise(fieldPath)) {
                return cb.notEqual(cb.upper(stringPath(fieldPath)), valueStr.toUpperCase());
            } else {
                return cb.notEqual(fieldPath, value);
            }
        }

        private Predicate like(final Path<String> fieldPath, final String sqlValue) {
            if (caseWise(fieldPath)) {
                return cb.like(cb.upper(fieldPath), sqlValue.toUpperCase(), ESCAPE_CHAR);
            } else {
                return cb.like(fieldPath, sqlValue, ESCAPE_CHAR);
            }
        }

        private Predicate notLike(final Path<String> fieldPath, final String sqlValue) {
            if (caseWise(fieldPath)) {
                return cb.notLike(cb.upper(fieldPath), sqlValue.toUpperCase(), ESCAPE_CHAR);
            } else {
                return cb.notLike(fieldPath, sqlValue, ESCAPE_CHAR);
            }
        }

        private Predicate in(final Path<String> fieldPath, final List<Object> values) {
            if (caseWise(fieldPath)) {
                final List<String> inParams = values.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast).map(String::toUpperCase).toList();
                return inParams.isEmpty() ? fieldPath.in(values) : cb.upper(fieldPath).in(inParams);
            } else {
                return fieldPath.in(values);
            }
        }

        private boolean caseWise(final Path<?> fieldPath) {
            return ensureIgnoreCase && fieldPath.getJavaType() == String.class;
        }

        private class PathResolver {

            private final Map<String, CollectionPathResolver> attributeToPathResolver = new HashMap<>();

            private Path<?> getPath(final Attribute<? super T, ?> attribute) {
                return switch (attribute.getPersistentAttributeType()) {
                    case BASIC -> root.get(attribute.getName());
                    case MANY_TO_ONE, ONE_TO_ONE -> root.getJoins().stream()
                            .filter(join -> join.getAttribute().equals(attribute))
                            .findFirst()
                            .orElseGet(() -> root.join(attribute.getName(), JoinType.LEFT));
                    case MANY_TO_MANY, ONE_TO_MANY, ELEMENT_COLLECTION -> getCollectionPathResolver(attribute.getName()).getPath();
                    default -> throw new IllegalArgumentException("Unsupported attribute type: " + attribute.getPersistentAttributeType());
                };
            }

            private MapJoin<?, ?, ?> getJoinOn(final Attribute<?, ?> attribute, final Object value) {
                return getCollectionPathResolver(attribute.getName()).getJoinOn(value);
            }

            private MapJoin<?, ?, ?> getJoinOnInner(final Attribute<?, ?> attribute, final Object value) {
                return getCollectionPathResolver(attribute.getName()).getJoinOnInner(value);
            }

            private Map<String, Integer> getState() {
                return attributeToPathResolver.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, resolver -> resolver.getValue().getPos()));
            }

            private void reset(final Map<String, Integer> toState) {
                attributeToPathResolver.forEach((attribute, resolver) -> resolver.setPos(toState.getOrDefault(attribute, 0)));
            }

            @Nonnull
            private CollectionPathResolver getCollectionPathResolver(final String attributeName) {
                return attributeToPathResolver.computeIfAbsent(attributeName, CollectionPathResolver::new);
            }

            private class CollectionPathResolver {

                private final String attributeName;
                private final List<Path<?>> paths = new ArrayList<>();
                @Getter
                @Setter
                private int pos;
                private final Map<Object, MapJoin<?, ?, ?>> joinOnCache = new HashMap<>();
                private final Map<Object, MapJoin<?, ?, ?>> joinOnInnerCache = new HashMap<>();

                private CollectionPathResolver(final String attributeName) {
                    this.attributeName = attributeName;
                }

                private Path<?> getPath() {
                    if (pos < paths.size()) {
                        return paths.get(pos++);
                    } else {
                        final Path<?> path = root.join(attributeName, JoinType.LEFT);
                        paths.add(path);
                        pos++;
                        return path;
                    }
                }

                private MapJoin<?, ?, ?> getJoinOn(final Object value) {
                    return joinOnCache.computeIfAbsent(value, k -> {
                        final MapJoin<?, ?, ?> mapPath = (MapJoin<?, ?, ?>) root.join(attributeName, JoinType.LEFT);
                        mapPath.on(equal(mapPath.key(), k));
                        return mapPath;
                    });
                }

                private MapJoin<?, ?, ?> getJoinOnInner(final Object value) {
                    return joinOnInnerCache.computeIfAbsent(value, k -> {
                        final MapJoin<?, ?, ?> mapPath = (MapJoin<?, ?, ?>) root.join(attributeName, JoinType.INNER);
                        mapPath.on(equal(mapPath.key(), k));
                        return mapPath;
                    });
                }
            }
        }
    }
}