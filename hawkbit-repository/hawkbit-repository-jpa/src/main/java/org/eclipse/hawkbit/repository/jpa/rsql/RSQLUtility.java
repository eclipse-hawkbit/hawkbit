/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.PluralJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.text.StrLookup;
import org.eclipse.hawkbit.repository.FieldNameProvider;
import org.eclipse.hawkbit.repository.FieldValueConverter;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.LogicalNode;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;

/**
 * A utility class which is able to parse RSQL strings into an spring data
 * {@link Specification} which then can be enhanced sql queries to filter
 * entities. RSQL parser library: https://github.com/jirutka/rsql-parser
 *
 * <ul>
 * <li>Equal to : ==</li>
 * <li>Not equal to : !=</li>
 * <li>Less than : =lt= or <</li>
 * <li>Less than or equal to : =le= or <=</li>
 * <li>Greater than operator : =gt= or ></li>
 * <li>Greater than or equal to : =ge= or >=</li>
 * </ul>
 * <p>
 * Examples of RSQL expressions in both FIQL-like and alternative notation:
 * <ul>
 * <li>version==2.0.0</li>
 * <li>name==targetId1;description==plugAndPlay</li>
 * <li>name==targetId1 and description==plugAndPlay</li>
 * <li>name==targetId1,description==plugAndPlay,updateStatus==UNKNOWN</li>
 * <li>name==targetId1 or description==plugAndPlay or updateStatus==UNKNOWN</li>
 * </ul>
 * <p>
 * There is also a mechanism that allows to refer to known macros that can
 * resolved by an optional {@link StrLookup} (cp.
 * {@link VirtualPropertyResolver}).<br>
 * An example that queries for all overdue targets using the ${OVERDUE_TS}
 * placeholder introduced by {@link VirtualPropertyResolver} looks like
 * this:<br>
 * <em>lastControllerRequestAt=le=${OVERDUE_TS}</em><br>
 * It is possible to escape a macro expression by using a second '$':
 * $${OVERDUE_TS} would prevent the ${OVERDUE_TS} token from being expanded.
 *
 */
public final class RSQLUtility {

    private static final Logger LOGGER = LoggerFactory.getLogger(RSQLUtility.class);

    /**
     * private constructor due utility class.
     */
    private RSQLUtility() {

    }

    /**
     * parses an RSQL valid string into an JPA {@link Specification} which then
     * can be used to filter for JPA entities with the given RSQL query.
     *
     * @param rsql
     *            the rsql query
     * @param fieldNameProvider
     *            the enum class type which implements the
     *            {@link FieldNameProvider}
     * @param virtualPropertyReplacer
     *            holds the logic how the known macros have to be resolved; may
     *            be <code>null</code>
     * @return an specification which can be used with JPA
     * @throws RSQLParameterUnsupportedFieldException
     *             if a field in the RSQL string is used but not provided by the
     *             given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException
     *             if the RSQL syntax is wrong
     */
    public static <A extends Enum<A> & FieldNameProvider, T> Specification<T> parse(final String rsql,
            final Class<A> fieldNameProvider, final VirtualPropertyReplacer virtualPropertyReplacer) {
        return new RSQLSpecification<>(rsql.toLowerCase(), fieldNameProvider, virtualPropertyReplacer);
    }

    /**
     * Validate the given rsql string regarding existence and correct syntax.
     *
     * @param rsql
     *            the rsql string to get validated
     *
     */
    public static void isValid(final String rsql) {
        parseRsql(rsql);
    }

    private static Node parseRsql(final String rsql) {
        try {
            LOGGER.debug("parsing rsql string {}", rsql);
            final Set<ComparisonOperator> operators = RSQLOperators.defaultOperators();
            operators.add(new ComparisonOperator("=li=", false));
            return new RSQLParser(operators).parse(rsql);
        } catch (final IllegalArgumentException e) {
            throw new RSQLParameterSyntaxException("rsql filter must not be null", e);
        } catch (final RSQLParserException e) {
            throw new RSQLParameterSyntaxException(e);
        }
    }

    private static final class RSQLSpecification<A extends Enum<A> & FieldNameProvider, T> implements Specification<T> {

        private final String rsql;
        private final Class<A> enumType;
        private final VirtualPropertyReplacer virtualPropertyReplacer;

        private RSQLSpecification(final String rsql, final Class<A> enumType,
                final VirtualPropertyReplacer virtualPropertyReplacer) {
            this.rsql = rsql;
            this.enumType = enumType;
            this.virtualPropertyReplacer = virtualPropertyReplacer;
        }

        @Override
        public Predicate toPredicate(final Root<T> root, final CriteriaQuery<?> query, final CriteriaBuilder cb) {
            final Node rootNode = parseRsql(rsql);

            final JpqQueryRSQLVisitor<A, T> jpqQueryRSQLVisitor = new JpqQueryRSQLVisitor<>(root, cb, enumType,
                    virtualPropertyReplacer);
            final List<Predicate> accept = rootNode.<List<Predicate>, String> accept(jpqQueryRSQLVisitor);

            if (!CollectionUtils.isEmpty(accept)) {
                return cb.and(accept.toArray(new Predicate[accept.size()]));
            }
            return cb.conjunction();

        }
    }

    /**
     * An implementation of the {@link RSQLVisitor} to visit the parsed tokens
     * and build jpa where clauses.
     *
     *
     *
     * @param <A>
     *            the enum for providing the field name of the entity field to
     *            filter on.
     * @param <T>
     *            the entity type referenced by the root
     */
    private static final class JpqQueryRSQLVisitor<A extends Enum<A> & FieldNameProvider, T>
            implements RSQLVisitor<List<Predicate>, String> {
        public static final Character LIKE_WILDCARD = '*';

        private final Root<T> root;
        private final CriteriaBuilder cb;
        private final Class<A> enumType;
        private final VirtualPropertyReplacer virtualPropertyReplacer;
        private int level;
        private boolean isOrLevel;
        private final Map<Integer, Set<Join<Object, Object>>> joinsInLevel = new HashMap<>(3);

        private final SimpleTypeConverter simpleTypeConverter;

        private JpqQueryRSQLVisitor(final Root<T> root, final CriteriaBuilder cb, final Class<A> enumType,
                final VirtualPropertyReplacer virtualPropertyReplacer) {
            this.root = root;
            this.cb = cb;
            this.enumType = enumType;
            this.virtualPropertyReplacer = virtualPropertyReplacer;
            simpleTypeConverter = new SimpleTypeConverter();
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

        @Override
        public List<Predicate> visit(final AndNode node, final String param) {
            beginLevel(false);
            final List<Predicate> childs = acceptChilds(node);
            endLevel();
            if (!childs.isEmpty()) {
                return toSingleList(cb.and(childs.toArray(new Predicate[childs.size()])));
            }
            return toSingleList(cb.conjunction());
        }

        @Override
        public List<Predicate> visit(final OrNode node, final String param) {
            beginLevel(true);
            final List<Predicate> childs = acceptChilds(node);
            endLevel();
            if (!childs.isEmpty()) {
                return toSingleList(cb.or(childs.toArray(new Predicate[childs.size()])));
            }
            return toSingleList(cb.conjunction());
        }

        private static List<Predicate> toSingleList(final Predicate predicate) {
            return Collections.singletonList(predicate);
        }

        private String getAndValidatePropertyFieldName(final A propertyEnum, final ComparisonNode node) {

            final String[] graph = node.getSelector().split("\\" + FieldNameProvider.SUB_ATTRIBUTE_SEPERATOR);

            validateMapParamter(propertyEnum, node, graph);

            // sub entity need minium 1 dot
            if (!propertyEnum.getSubEntityAttributes().isEmpty() && graph.length < 2) {
                throw createRSQLParameterUnsupportedException(node);
            }

            final StringBuilder fieldNameBuilder = new StringBuilder(propertyEnum.getFieldName());

            for (int i = 1; i < graph.length; i++) {

                final String propertyField = graph[i];
                fieldNameBuilder.append(FieldNameProvider.SUB_ATTRIBUTE_SEPERATOR).append(propertyField);

                // the key of map is not in the graph
                if (propertyEnum.isMap() && graph.length == (i + 1)) {
                    continue;
                }

                if (!propertyEnum.containsSubEntityAttribute(propertyField)) {
                    throw createRSQLParameterUnsupportedException(node);
                }
            }

            return fieldNameBuilder.toString();
        }

        private void validateMapParamter(final A propertyEnum, final ComparisonNode node, final String[] graph) {
            if (!propertyEnum.isMap()) {
                return;

            }
            if (!propertyEnum.getSubEntityAttributes().isEmpty()) {
                throw new UnsupportedOperationException("Currently subentity attributes for maps are not supported");
            }

            // enum.key
            final int minAttributeForMap = 2;
            if (graph.length != minAttributeForMap) {
                throw new RSQLParameterUnsupportedFieldException("The syntax of the given map search parameter field {"
                        + node.getSelector() + "} is wrong. Syntax is: fieldname.keyname", new Exception());
            }
        }

        private RSQLParameterUnsupportedFieldException createRSQLParameterUnsupportedException(
                final ComparisonNode node) {
            return new RSQLParameterUnsupportedFieldException(
                    "The given search parameter field {" + node.getSelector()
                            + "} does not exist, must be one of the following fields {" + getExpectedFieldList() + "}",
                    new Exception());
        }

        /**
         * Resolves the Path for a field in the persistence layer and joins the
         * required models. This operation is part of a tree traversal through
         * an RSQL expression. It creates for every field that is not part of
         * the root model a join to the foreign model. This behavior is
         * optimized when several joins happen directly under an OR node in the
         * traversed tree. The same foreign model is only joined once.
         *
         * Example: tags.name==M;(tags.name==A,tags.name==B,tags.name==C) This
         * example joins the tags model only twice, because for the OR node in
         * brackets only one join is used.
         *
         * @param enumField
         *            field from a FieldNameProvider to resolve on the
         *            persistence layer
         * @param finalProperty
         *            dot notated field path
         * @return the Path for a field
         */
        private Path<Object> getFieldPath(final A enumField, final String finalProperty) {
            Path<Object> fieldPath = null;
            final String[] split = finalProperty.split("\\" + FieldNameProvider.SUB_ATTRIBUTE_SEPERATOR);

            for (int i = 0; i < split.length; i++) {
                final boolean isMapKeyField = enumField.isMap() && i == (split.length - 1);
                if (isMapKeyField) {
                    return fieldPath;
                }

                final String fieldNameSplit = split[i];
                fieldPath = (fieldPath != null) ? fieldPath.get(fieldNameSplit) : root.get(fieldNameSplit);
                if (fieldPath instanceof PluralJoin) {
                    final Join<Object, ?> join = (Join<Object, ?>) fieldPath;
                    final From<?, Object> joinParent = join.getParent();
                    final Optional<Join<Object, Object>> currentJoinOfType = findCurrentJoinOfType(join.getJavaType());
                    if (currentJoinOfType.isPresent() && isOrLevel) {
                        // remove the additional join and use the existing one
                        joinParent.getJoins().remove(join);
                        fieldPath = currentJoinOfType.get();
                    } else {
                        final Join<Object, Object> newJoin = joinParent.join(fieldNameSplit, JoinType.LEFT);
                        addCurrentJoin(newJoin);
                        fieldPath = newJoin;
                    }

                }
            }
            return fieldPath;
        }

        @Override
        // Exception squid:S2095 - see
        // https://jira.sonarsource.com/browse/SONARJAVA-1478
        @SuppressWarnings({ "squid:S2095" })
        public List<Predicate> visit(final ComparisonNode node, final String param) {
            A fieldName = null;
            try {
                fieldName = getFieldEnumByName(node);
            } catch (final IllegalArgumentException e) {
                throw new RSQLParameterUnsupportedFieldException("The given search parameter field {"
                        + node.getSelector() + "} does not exist, must be one of the following fields {"
                        + Arrays.stream(enumType.getEnumConstants()).map(v -> v.name().toLowerCase())
                                .collect(Collectors.toList())
                        + "}", e);

            }
            final String finalProperty = getAndValidatePropertyFieldName(fieldName, node);

            final List<String> values = node.getArguments();
            final List<Object> transformedValue = new ArrayList<>();
            final Path<Object> fieldPath = getFieldPath(fieldName, finalProperty);

            for (final String value : values) {
                transformedValue.add(convertValueIfNecessary(node, fieldName, value, fieldPath));
            }

            return mapToPredicate(node, fieldPath, node.getArguments(), transformedValue, fieldName);
        }

        // Exception squid:S2095 - see
        // https://jira.sonarsource.com/browse/SONARJAVA-1478
        @SuppressWarnings({ "squid:S2095" })
        private List<String> getExpectedFieldList() {
            final List<String> expectedFieldList = Arrays.stream(enumType.getEnumConstants())
                    .filter(enumField -> enumField.getSubEntityAttributes().isEmpty()).map(enumField -> {
                        final String enumFieldName = enumField.name().toLowerCase();

                        if (enumField.isMap()) {
                            return enumFieldName + FieldNameProvider.SUB_ATTRIBUTE_SEPERATOR + "keyName";
                        }

                        return enumFieldName;
                    }).collect(Collectors.toList());

            final List<String> expectedSubFieldList = Arrays.stream(enumType.getEnumConstants())
                    .filter(enumField -> !enumField.getSubEntityAttributes().isEmpty()).flatMap(enumField -> {
                        final List<String> subEntity = enumField.getSubEntityAttributes().stream()
                                .map(fieldName -> enumField.name().toLowerCase()
                                        + FieldNameProvider.SUB_ATTRIBUTE_SEPERATOR + fieldName)
                                .collect(Collectors.toList());

                        return subEntity.stream();
                    }).collect(Collectors.toList());
            expectedFieldList.addAll(expectedSubFieldList);
            return expectedFieldList;
        }

        private A getFieldEnumByName(final ComparisonNode node) {
            String enumName = node.getSelector();
            final String[] graph = enumName.split("\\" + FieldNameProvider.SUB_ATTRIBUTE_SEPERATOR);
            if (graph.length != 0) {
                enumName = graph[0];
            }
            LOGGER.debug("get fieldidentifier by name {} of enum type {}", enumName, enumType);
            return Enum.valueOf(enumType, enumName.toUpperCase());
        }

        private Object convertValueIfNecessary(final ComparisonNode node, final A fieldName, final String value,
                final Path<Object> fieldPath) {
            // in case the value of an rsql query e.g. type==application is an
            // enum we need to handle it separately because JPA needs the
            // correct java-type to build an expression. So String and numeric
            // values JPA can do it by it's own but not for classes like enums.
            // So we need to transform the given value string into the enum
            // class.
            final Class<? extends Object> javaType = fieldPath.getJavaType();
            if (javaType != null && javaType.isEnum()) {
                return transformEnumValue(node, value, javaType);
            }
            if (fieldName instanceof FieldValueConverter) {
                return convertFieldConverterValue(node, fieldName, value);
            }

            if (Boolean.TYPE.equals(javaType)) {
                return convertBooleanValue(node, value, javaType);
            }

            return value;
        }

        private Object convertBooleanValue(final ComparisonNode node, final String value,
                final Class<? extends Object> javaType) {
            try {
                return simpleTypeConverter.convertIfNecessary(value, javaType);
            } catch (final TypeMismatchException e) {
                throw new RSQLParameterSyntaxException(
                        "The value of the given search parameter field {" + node.getSelector()
                                + "} is not well formed. Only a boolean (true or false) value will be expected {",
                        e);
            }
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private Object convertFieldConverterValue(final ComparisonNode node, final A fieldName, final String value) {
            final Object convertedValue = ((FieldValueConverter) fieldName).convertValue(fieldName, value);
            if (convertedValue == null) {
                throw new RSQLParameterUnsupportedFieldException(
                        "field {" + node.getSelector() + "} must be one of the following values {"
                                + Arrays.toString(((FieldValueConverter) fieldName).possibleValues(fieldName)) + "}",
                        null);
            } else {
                return convertedValue;
            }
        }

        // Exception squid:S2095 - see
        // https://jira.sonarsource.com/browse/SONARJAVA-1478
        @SuppressWarnings({ "rawtypes", "unchecked", "squid:S2095" })
        private static Object transformEnumValue(final ComparisonNode node, final String value,
                final Class<? extends Object> javaType) {
            final Class<? extends Enum> tmpEnumType = (Class<? extends Enum>) javaType;
            try {
                return Enum.valueOf(tmpEnumType, value.toUpperCase());
            } catch (final IllegalArgumentException e) {
                // we could not transform the given string value into the enum
                // type, so ignore it and return null and do not filter
                LOGGER.info("given value {} cannot be transformed into the correct enum type {}", value.toUpperCase(),
                        javaType);
                LOGGER.debug("value cannot be transformed to an enum", e);

                throw new RSQLParameterUnsupportedFieldException("field {" + node.getSelector()
                        + "} must be one of the following values {" + Arrays.stream(tmpEnumType.getEnumConstants())
                                .map(v -> v.name().toLowerCase()).collect(Collectors.toList())
                        + "}", e);
            }
        }

        private List<Predicate> mapToPredicate(final ComparisonNode node, final Path<Object> fieldPath,
                final List<String> values, final List<Object> transformedValues, final A enumField) {
            // only 'equal' and 'notEqual' can handle transformed value like
            // enums. The JPA API cannot handle object types for greaterThan etc
            // methods.
            final Object transformedValue = transformedValues.get(0);

            String value = values.get(0);
            // if lookup is available, replace macros ...
            if (virtualPropertyReplacer != null) {
                value = virtualPropertyReplacer.replace(value);
            }

            final List<Predicate> singleList = new ArrayList<>();

            final Predicate mapPredicate = mapToMapPredicate(node, fieldPath, enumField);
            if (mapPredicate != null) {
                singleList.add(mapPredicate);
            }

            addOperatorPredicate(node, getMapValueFieldPath(enumField, fieldPath), transformedValues, transformedValue,
                    value, singleList);
            return Collections.unmodifiableList(singleList);
        }

        private void addOperatorPredicate(final ComparisonNode node, final Path<Object> fieldPath,
                final List<Object> transformedValues, final Object transformedValue, final String value,
                final List<Predicate> singleList) {
            switch (node.getOperator().getSymbol()) {
            case "==":
                singleList.add(getEqualToPredicate(transformedValue, fieldPath));
                break;
            case "!=":
                singleList.add(getNotEqualToPredicate(transformedValue, fieldPath));
                break;
            case "=gt=":
                singleList.add(cb.greaterThan(pathOfString(fieldPath), value));
                break;
            case "=ge=":
                singleList.add(cb.greaterThanOrEqualTo(pathOfString(fieldPath), value));
                break;
            case "=lt=":
                singleList.add(cb.lessThan(pathOfString(fieldPath), value));
                break;
            case "=le=":
                singleList.add(cb.lessThanOrEqualTo(pathOfString(fieldPath), value));
                break;
            case "=in=":
                singleList.add(getInPredicate(transformedValues, fieldPath));
                break;
            case "=out=":
                singleList.add(getOutPredicate(transformedValues, fieldPath));
                break;
            default:
                LOGGER.info("operator symbol {} is either not supported or not implemented");
            }
        }

        private Predicate getInPredicate(final List<Object> transformedValues, final Path<Object> fieldPath) {
            final List<String> inParams = new ArrayList<>();
            for (final Object param : transformedValues) {
                if (param instanceof String) {
                    inParams.add(((String) param).toUpperCase());
                }
            }
            if (!inParams.isEmpty()) {
                return cb.upper(pathOfString(fieldPath)).in(inParams);
            } else {
                return fieldPath.in(transformedValues);

            }
        }

        private Predicate getOutPredicate(final List<Object> transformedValues, final Path<Object> fieldPath) {
            final List<String> outParams = new ArrayList<>();
            for (final Object param : transformedValues) {
                if (param instanceof String) {
                    outParams.add(((String) param).toUpperCase());
                }
            }
            if (!outParams.isEmpty()) {
                return cb.not(cb.upper(pathOfString(fieldPath)).in(outParams));
            } else {
                return cb.not(fieldPath.in(transformedValues));

            }
        }

        private Path<Object> getMapValueFieldPath(final A enumField, final Path<Object> fieldPath) {
            if (!enumField.isMap() || enumField.getValueFieldName() == null) {
                return fieldPath;
            }
            return fieldPath.get(enumField.getValueFieldName());
        }

        @SuppressWarnings("unchecked")
        private Predicate mapToMapPredicate(final ComparisonNode node, final Path<Object> fieldPath,
                final A enumField) {
            if (!enumField.isMap()) {
                return null;
            }
            final String[] graph = node.getSelector().split("\\" + FieldNameProvider.SUB_ATTRIBUTE_SEPERATOR);
            final String keyValue = graph[graph.length - 1];
            if (fieldPath instanceof MapJoin) {
                // Currently we support only string key .So below cast is safe.
                return cb.equal(cb.upper((Expression<String>) (((MapJoin<?, ?, ?>) fieldPath).key())),
                        keyValue.toUpperCase());
            }

            return cb.equal(cb.upper(fieldPath.get(enumField.getKeyFieldName())), keyValue.toUpperCase());
        }

        private Predicate getEqualToPredicate(final Object transformedValue, final Path<Object> fieldPath) {
            if (transformedValue instanceof String) {
                final String preFormattedValue = escapeValueToSQL((String) transformedValue);
                return cb.like(cb.upper(pathOfString(fieldPath)), preFormattedValue.toUpperCase());
            }
            return cb.equal(fieldPath, transformedValue);
        }

        private Predicate getNotEqualToPredicate(final Object transformedValue, final Path<Object> fieldPath) {
            if (transformedValue instanceof String) {
                final String preFormattedValue = escapeValueToSQL((String) transformedValue);
                return cb.notLike(cb.upper(pathOfString(fieldPath)), preFormattedValue.toUpperCase());
            }
            return cb.notEqual(fieldPath, transformedValue);
        }

        private static String escapeValueToSQL(final String transformedValue) {
            return transformedValue.replace("%", "\\%").replace(LIKE_WILDCARD, '%');
        }

        @SuppressWarnings("unchecked")
        private static <Y> Path<Y> pathOfString(final Path<?> path) {
            return (Path<Y>) path;
        }

        private List<Predicate> acceptChilds(final LogicalNode node) {
            final List<Node> children = node.getChildren();
            final List<Predicate> childs = new ArrayList<>();
            for (final Node node2 : children) {
                final List<Predicate> accept = node2.accept(this);
                if (!CollectionUtils.isEmpty(accept)) {
                    childs.addAll(accept);
                } else {
                    LOGGER.debug("visit logical node children but could not parse it, ignoring {}", node2);
                }
            }
            return childs;
        }

    }

}
