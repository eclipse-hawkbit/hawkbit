/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.rsql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.PluralAttribute;

import org.eclipse.hawkbit.repository.FieldNameProvider;
import org.eclipse.hawkbit.repository.FieldValueConverter;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;

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
 * Examples of RSQL expressions in both FIQL-like and alternative notation:
 * <ul>
 * <li>version==2.0.0</li>
 * <li>name==targetId1;description==plugAndPlay</li>
 * <li>name==targetId1 and description==plugAndPlay</li>
 * <li>name==targetId1;description==plugAndPlay</li>
 * <li>name==targetId1 and description==plugAndPlay</li>
 * <li>name==targetId1,description==plugAndPlay,updateStatus==UNKNOWN</li>
 * <li>name==targetId1 or description==plugAndPlay or updateStatus==UNKNOWN</li>
 * </ul>
 * 
 *
 *
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
     * @param entityManager
     *            {@link EntityManager}
     * @return an specification which can be used with JPA
     * @throws RSQLParameterUnsupportedFieldException
     *             if a field in the RSQL string is used but not provided by the
     *             given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException
     *             if the RSQL syntax is wrong
     */
    public static <A extends Enum<A> & FieldNameProvider, T> Specification<T> parse(final String rsql,
            final Class<A> fieldNameProvider, final EntityManager entityManager) {
        return new RSQLSpecification<>(rsql, fieldNameProvider, entityManager);
    }

    private static final class RSQLSpecification<A extends Enum<A> & FieldNameProvider, T> implements Specification<T> {

        private final String rsql;
        private final Class<A> enumType;
        private final EntityManager entityManager;

        private RSQLSpecification(final String rsql, final Class<A> enumType, final EntityManager entityManager) {
            this.rsql = rsql;
            this.enumType = enumType;
            this.entityManager = entityManager;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.springframework.data.jpa.domain.Specification#toPredicate(javax.
         * persistence.criteria .Root, javax.persistence.criteria.CriteriaQuery,
         * javax.persistence.criteria.CriteriaBuilder)
         */
        @Override
        public Predicate toPredicate(final Root<T> root, final CriteriaQuery<?> query, final CriteriaBuilder cb) {

            final Node rootNode;
            try {
                LOGGER.debug("parsing rsql string {}", rsql);
                final Set<ComparisonOperator> operators = RSQLOperators.defaultOperators();
                operators.add(new ComparisonOperator("=li=", false));
                rootNode = new RSQLParser(operators).parse(rsql);
            } catch (final RSQLParserException e) {
                throw new RSQLParameterSyntaxException(e);
            }

            final JpqQueryRSQLVisitor<A, T> jpqQueryRSQLVisitor = new JpqQueryRSQLVisitor<>(root, cb, enumType,
                    entityManager);
            final List<Predicate> accept = rootNode.<List<Predicate>, String> accept(jpqQueryRSQLVisitor);

            if (accept != null && !accept.isEmpty()) {
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
    private static final class JpqQueryRSQLVisitor<A extends Enum<A> & FieldNameProvider, T> implements
            RSQLVisitor<List<Predicate>, String> {
        public static final Character LIKE_WILDCARD = '*';

        static {
            /**
             * Property mapping are done in FieldNameProvider like
             * TargetFields,SoftwareModuleFields etc.
             * 
             * In addition to this mapping in PropertyMapper are done if we want
             * to drill down on entity .
             * 
             * For example : Drill down on distribution set of target entity are
             * done by adding the mappings in PropertyMapper as below.
             * 
             * User can now use assignedds.name and assignedds.version
             * 
             */
            PropertyMapper.addNewMapping(Target.class, "assignedds", "assignedDistributionSet");
            PropertyMapper.addNewMapping(DistributionSet.class, "name", "name");
            PropertyMapper.addNewMapping(DistributionSet.class, "version", "version");
        }

        private final Root<T> root;
        private final CriteriaBuilder cb;
        private final Class<A> enumType;
        private final EntityManager entityManager;

        private JpqQueryRSQLVisitor(final Root<T> root, final CriteriaBuilder cb, final Class<A> enumType,
                final EntityManager entityManager) {
            this.root = root;
            this.cb = cb;
            this.enumType = enumType;
            this.entityManager = entityManager;
        }

        @Override
        public List<Predicate> visit(final AndNode node, final String param) {
            final List<Predicate> childs = acceptChilds(node);
            if (!childs.isEmpty()) {
                return toSingleList(cb.and(childs.toArray(new Predicate[childs.size()])));
            }
            return toSingleList(cb.conjunction());
        }

        @Override
        public List<Predicate> visit(final OrNode node, final String param) {
            final List<Predicate> childs = acceptChilds(node);
            if (!childs.isEmpty()) {
                return toSingleList(cb.or(childs.toArray(new Predicate[childs.size()])));
            }
            return toSingleList(cb.conjunction());
        }

        private static <T> boolean isItAssociationType(final String property, final ManagedType<T> classMetadata) {
            return classMetadata.getAttribute(property).isAssociation();
        }

        private static <T> Class<?> getPropertyType(final String property, final ManagedType<T> classMetadata) {
            Class<?> propertyType;
            if (classMetadata.getAttribute(property).isCollection()) {
                propertyType = ((PluralAttribute) classMetadata.getAttribute(property)).getBindableJavaType();
            } else {
                propertyType = classMetadata.getAttribute(property).getJavaType();
            }
            return propertyType;
        }

        private static ManagedType<?> getClassMetaData(final String property, final ManagedType classMetadata,
                final Metamodel metaModel) {
            if (isItAssociationType(property, classMetadata)) {
                final Class<?> associationType = getPropertyType(property, classMetadata);
                return metaModel.managedType(associationType);
            } else {
                if (isItEmbeddedType(property, classMetadata)) {
                    final Class<?> embeddedType = getPropertyType(property, classMetadata);
                    return metaModel.managedType(embeddedType);
                }
            }
            return classMetadata;
        }

        private static <T> boolean isItEmbeddedType(final String property, final ManagedType<T> classMetadata) {
            return classMetadata.getAttribute(property).getPersistentAttributeType() == PersistentAttributeType.EMBEDDED;
        }

        private String validatePropertyFieldName(final String propertyFieldName, final boolean isDefinedInEnum,
                ManagedType classMetadata, final Metamodel metaModel, final ComparisonNode node) {
            String finalProperty = propertyFieldName;
            final String[] graph = propertyFieldName.split("\\.");
            for (String property : graph) {
                if (!isDefinedInEnum && PropertyMapper.getAllowedcolmns().containsKey(classMetadata.getJavaType())) {
                    if (PropertyMapper.getAllowedcolmns().get(classMetadata.getJavaType()).get(property) != null) {
                        final String mappedValue = PropertyMapper.getAllowedcolmns().get(classMetadata.getJavaType())
                                .get(property);
                        finalProperty = finalProperty.replace(property, mappedValue);
                        property = mappedValue;
                    } else {
                        throw new RSQLParameterUnsupportedFieldException("The given search parameter field {"
                                + node.getSelector() + "} does not exist, must be one of the following fields {"
                                + getExpectedFieldList() + "}", new Exception());
                    }
                }
                classMetadata = getClassMetaData(property, classMetadata, metaModel);
            }
            return finalProperty;
        }

        private Path<Object> getFieldPath(final String finalProperty) {
            Path<Object> fieldPath = null;
            final String[] split = finalProperty.split("\\.");
            if (split.length == 0) {
                fieldPath = root.get(split[0]);
            } else {
                for (final String fieldNameSplit : split) {
                    // hibernate workaround because cannot get attribute of an
                    // PluralAttributePath, needs
                    // an implicit join.
                    // https://hibernate.atlassian.net/browse/HHH-7892
                    if (fieldPath == null && root.get(fieldNameSplit) != null
                            && Collection.class.isAssignableFrom(root.get(fieldNameSplit).getJavaType())) {
                        fieldPath = root.join(fieldNameSplit);
                    } else {
                        fieldPath = (fieldPath != null) ? fieldPath.get(fieldNameSplit) : root.get(fieldNameSplit);
                    }
                }
            }
            return fieldPath;
        }

        @Override
        public List<Predicate> visit(final ComparisonNode node, final String param) {
            final Metamodel metaModel = entityManager.getMetamodel();
            final ManagedType<?> classMetadata = metaModel.managedType(root.getJavaType());
            String propertyFieldName = null;
            Boolean isDefinedInEnum = Boolean.FALSE;
            A fieldName = null;
            try {
                /**
                 * Get the property mapping from FieldNameProvider .If not
                 * available check in PropertyMapping.If not found throw
                 * RSQLParameterUnsupportedFieldException.
                 */
                fieldName = getFieldIdentifierByName(node);
                propertyFieldName = fieldName.getFieldName();
                isDefinedInEnum = Boolean.TRUE;
            } catch (final IllegalArgumentException e) {
                if (PropertyMapper.getAllowedcolmns().containsKey(classMetadata.getJavaType())) {
                    propertyFieldName = node.getSelector();
                } else {
                    throw new RSQLParameterUnsupportedFieldException("The given search parameter field {"
                            + node.getSelector()
                            + "} does not exist, must be one of the following fields {"
                            + Arrays.stream(enumType.getEnumConstants()).map(v -> v.name().toLowerCase())
                                    .collect(Collectors.toList()) + "}", e);
                }
            }
            final String finalProperty = validatePropertyFieldName(propertyFieldName, isDefinedInEnum, classMetadata,
                    metaModel, node);

            final List<String> values = node.getArguments();
            final List<Object> transformedValue = new ArrayList<>();
            final Path<Object> fieldPath = getFieldPath(finalProperty);
            for (final String value : values) {
                transformedValue.add(convertValueIfNecessary(node, fieldName, value, fieldPath));
            }

            return mapToPredicate(node, fieldPath, node.getArguments(), transformedValue);
        }

        private List<String> getExpectedFieldList() {
            final List<String> expectedFieldList = Arrays.stream(enumType.getEnumConstants())
                    .map(v -> v.name().toLowerCase()).collect(Collectors.toList());
            expectedFieldList.add("assignedds.name");
            expectedFieldList.add("assignedds.version");
            return expectedFieldList;
        }

        private A getFieldIdentifierByName(final ComparisonNode node) {
            final String enumName = node.getSelector().toUpperCase();
            LOGGER.debug("get fieldidentifier by name {} of enum type {}", enumName, enumType);
            return Enum.valueOf(enumType, enumName);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private Object convertValueIfNecessary(final ComparisonNode node, final A fieldName, final String value,
                final Path<Object> fieldPath) {
            // in case the value of an rsql query e.g. type==application is an
            // enum we need to
            // handle it separately because JPA needs the correct java-type to
            // build an
            // expression. So String and numeric values JPA can do it by it's
            // own but not for
            // classes like enums. So we need to transform the given value
            // string into the enum
            // class.
            final Class<? extends Object> javaType = fieldPath.getJavaType();
            if (javaType != null && javaType.isEnum()) {
                return transformEnumValue(node, value, javaType);
            }
            if (fieldName instanceof FieldValueConverter) {
                final Object convertedValue = ((FieldValueConverter) fieldName).convertValue(fieldName, value);
                if (convertedValue == null) {
                    throw new RSQLParameterUnsupportedFieldException("field {" + node.getSelector()
                            + "} must be one of the following values {"
                            + Arrays.toString(((FieldValueConverter) fieldName).possibleValues(fieldName)) + "}", null);
                } else {
                    return convertedValue;
                }
            }
            return value;
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private Object transformEnumValue(final ComparisonNode node, final String value,
                final Class<? extends Object> javaType) {
            final Class<? extends Enum> tmpEnumType = (Class<? extends Enum>) javaType;
            try {
                return Enum.valueOf(tmpEnumType, value.toUpperCase());
            } catch (final IllegalArgumentException e) {
                // we could not transform the given string value into the enum
                // type, so ignore
                // it and return null and do not filter
                LOGGER.info("given value {} cannot be transformed into the correct enum type {}", value.toUpperCase(),
                        javaType);
                LOGGER.debug("value cannot be transformed to an enum", e);

                throw new RSQLParameterUnsupportedFieldException("field {"
                        + node.getSelector()
                        + "} must be one of the following values {"
                        + Arrays.stream(tmpEnumType.getEnumConstants()).map(v -> v.name().toLowerCase())
                                .collect(Collectors.toList()) + "}", e);
            }
        }

        private List<Predicate> mapToPredicate(final ComparisonNode node, final Path<Object> fieldPath,
                final List<String> values, final List<Object> transformedValues) {
            // only 'equal' and 'notEqual' can handle transformed value like
            // enums. The JPA API
            // cannot handle object types for greaterThan etc methods.
            final Object transformedValue = transformedValues.get(0);
            final String value = values.get(0);
            final List<Predicate> singleList;
            switch (node.getOperator().getSymbol()) {
            case "=li=":
                singleList = toSingleList(cb.like(cb.upper(pathOfString(fieldPath)), transformedValue.toString()
                        .toUpperCase()));
                break;
            case "==":
                singleList = getEqualToPredicate(transformedValue, fieldPath);
                break;
            case "!=":
                singleList = toSingleList(cb.notEqual(fieldPath, transformedValue));
                break;
            case "=gt=":
                singleList = toSingleList(cb.greaterThan(pathOfString(fieldPath), value));
                break;
            case "=ge=":
                singleList = toSingleList(cb.greaterThanOrEqualTo(pathOfString(fieldPath), value));
                break;
            case "=lt=":
                singleList = toSingleList(cb.lessThan(pathOfString(fieldPath), value));
                break;
            case "=le=":
                singleList = toSingleList(cb.lessThanOrEqualTo(pathOfString(fieldPath), value));
                break;
            case "=in=":
                singleList = toSingleList(fieldPath.in(transformedValues));
                break;
            case "=out=":
                singleList = toSingleList(cb.not(fieldPath.in(transformedValues)));
                break;
            default:
                LOGGER.info("operator symbol {} is either not supported or not implemented");
                singleList = Collections.emptyList();
            }
            return singleList;
        }

        private List<Predicate> getEqualToPredicate(final Object transformedValue, final Path<Object> fieldPath) {
            if (transformedValue instanceof String) {
                final String preFormattedValue = ((String) transformedValue).replace(LIKE_WILDCARD, '%');
                return toSingleList(cb.like(cb.upper(pathOfString(fieldPath)), preFormattedValue.toString()
                        .toUpperCase()));
            } else {
                return toSingleList(cb.equal(fieldPath, transformedValue));
            }
        }

        @SuppressWarnings("unchecked")
        private <Y> Path<Y> pathOfString(final Path<?> path) {
            return (Path<Y>) path;
        }

        private List<Predicate> acceptChilds(final LogicalNode node) {
            final List<Node> children = node.getChildren();
            final List<Predicate> childs = new ArrayList<>();
            for (final Node node2 : children) {
                final List<Predicate> accept = node2.accept(this);
                if (accept != null && !accept.isEmpty()) {
                    childs.addAll(accept);
                } else {
                    LOGGER.debug("visit logical node children but could not parse it, ignoring {}", node2);
                }
            }
            return childs;
        }

        private static List<Predicate> toSingleList(final Predicate p) {
            return Collections.singletonList(p);
        }
    }
}
