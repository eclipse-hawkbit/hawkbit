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

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.text.StrLookup;
import org.eclipse.hawkbit.repository.qfields.QueryField;
import org.eclipse.hawkbit.repository.exception.QueryException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.domain.Specification;

/**
 * A utility class which is able to parse RSQL strings into an spring data
 * {@link Specification} which then can be enhanced sql queries to filter
 * entities. RSQL parser library: https://github.com/jirutka/rsql-parser
 *
 * <ul>
 * <li>{@code Equal to : ==}</li>
 * <li>{@code Not equal to : !=}</li>
 * <li>{@code Less than : =lt= or <}</li>
 * <li>{@code Less than or equal to : =le= or <=}</li>
 * <li>{@code Greater than operator : =gt= or >}</li>
 * <li>{@code Greater than or equal to : =ge= or >=}</li>
 * </ul>
 * <p>
 * Examples of RSQL expressions in both FIQL-like and alternative notation:
 * <ul>
 * <li>{@code version==2.0.0}</li>
 * <li>{@code name==targetId1;description==plugAndPlay}</li>
 * <li>{@code name==targetId1 and description==plugAndPlay}</li>
 * <li>{@code name==targetId1,description==plugAndPlay,updateStatus==UNKNOWN}</li>
 * <li>{@code name==targetId1 or description==plugAndPlay or updateStatus==UNKNOWN}</li>
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
 */
@Slf4j
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("java:S6548") // singleton holder ensures static access to spring resources in some places
public class QLSupport implements ApplicationListener<ContextRefreshedEvent> {

    private static final QLSupport SINGLETON = new QLSupport();

    /**
     * If QL comparison operators shall ignore the case. If ignore case is <code>true</code> "x == ax" will match "x == aX"
     */
    @Value("${hawkbit.ql.ignore-case:true}")
    private boolean ignoreCase;
    /**
     * Declares if the database is case-insensitive, by default assumes <code>false</code>. In case it is case-sensitive and,
     * {@link #ignoreCase} is set to <code>true</code> the SQL queries use upper case comparisons to ignore case.
     * <p/>
     * If the database is declared as case-sensitive and ignoreCase is set to <code>false</code> the RSQL queries shall use strict
     * syntax - i.e. 'and' instead of 'AND' / 'aND'. Otherwise, the queries would be case-insensitive regarding operators.
     */
    @Value("${hawkbit.ql.case-insensitive-db:false}")
    private boolean caseInsensitiveDB;

    private QueryParser parser;
    private List<NodeTransformer> nodeTransformers;
    private EntityManager entityManager;
    private VirtualPropertyResolver virtualPropertyResolver;

    /**
     * @return The holder singleton instance.
     */
    public static QLSupport getInstance() {
        return SINGLETON;
    }

    @Autowired
    void setQueryParser(final QueryParser parser) {
        this.parser = parser;
    }

    @Override
    public void onApplicationEvent(@NonNull final ContextRefreshedEvent event) {
        nodeTransformers = event.getApplicationContext().getBeansOfType(NodeTransformer.class).values().stream().sorted((b1, b2) -> {
            final Order o1 = b1.getClass().getAnnotation(Order.class);
            final Order o2 = b2.getClass().getAnnotation(Order.class);
            return Integer.compare(o1 != null ? o1.value() : Ordered.LOWEST_PRECEDENCE, o2 != null ? o2.value() : Ordered.LOWEST_PRECEDENCE);
        }).toList();
    }

    @Autowired
    void setEntityManager(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Autowired(required = false)
    void setVirtualPropertyResolver(final VirtualPropertyResolver virtualPropertyResolver) {
        this.virtualPropertyResolver = virtualPropertyResolver;
    }

    public Node parse(final String query) {
        return parse(query, null);
    }

    public <A extends Enum<A> & QueryField> Node parse(final String query, final Class<A> queryFieldType) {
        return parser.parse(ignoreCase || caseInsensitiveDB ? query.toLowerCase() : query, queryFieldType);
    }

    /**
     * Builds a JPA {@link Specification} which corresponds with the given RSQL query. The specification can be used to filter for JPA entities
     * with the given RSQL query.
     *
     * @param query the rsql query to be parsed
     * @param queryFieldType the enum class type which implements the {@link QueryField}
     * @return a specification which can be used with JPA
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *         given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     */
    public <A extends Enum<A> & QueryField, T> Specification<T> buildSpec(final String query, final Class<A> queryFieldType) {
        return new SpecificationBuilder<T>(ignoreCase && !caseInsensitiveDB)
                    .specification(transform(parse(query, queryFieldType), queryFieldType));
    }

    public <A extends Enum<A> & QueryField, T> Specification<T> buildSpec(final Node query, final Class<A> queryFieldType) {
        return new SpecificationBuilder<T>(ignoreCase && !caseInsensitiveDB)
                .specification(transform(query, queryFieldType));
    }

    @SuppressWarnings("java:S1117") // it is again ignoreCase
    public <A extends Enum<A> & QueryField> EntityMatcher entityMatcher(final String query, final Class<A> queryFieldType) {
        return EntityMatcher.of(transform(parse(query, queryFieldType), queryFieldType), ignoreCase || caseInsensitiveDB);
    }

    /**
     * Validates the query string
     *
     * @param query query string to validate
     * @param queryFieldType the enum class type which implements the {@link QueryField}
     * @param jpaType the JPA entity type to validate against
     * @throws QueryException if query is invalid
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <A extends Enum<A> & QueryField> void validate(final String query, final Class<A> queryFieldType, final Class<?> jpaType) {
        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<?> criteriaQuery = criteriaBuilder.createQuery(jpaType);
        buildSpec(query, queryFieldType).toPredicate(criteriaQuery.from((Class) jpaType), criteriaQuery, criteriaBuilder);
    }

    private <A extends Enum<A> & QueryField> Node transform(Node node, final Class<A> queryFieldType) {
        for (final NodeTransformer transformer : nodeTransformers) {
            node = transformer.transform(node, queryFieldType);
        }
        return node;
    }

    /**
     * By registering a custom {@link QueryParser} (as a {@link org.springframework.context.annotation.Bean}) the entire parsing of the queries
     * could be replaced / customized, e.g. the default query language (RSQL) could be replaced with a custom.
     */
    public interface QueryParser {

        <T extends Enum<T> & QueryField> Node parse(final String query, final Class<T> queryFieldType) throws QueryException;
    }

    /**
     * By registering a custom {@link NodeTransformer} (as a {@link org.springframework.context.annotation.Bean}) the nodes could be
     * modified after parsing, e.g. to add implicit nodes or to modify values.
     * <p/>
     * By default, all transformers are with {@link Ordered#LOWEST_PRECEDENCE} order. So, if you need a specific order use the {@link Order}
     * annotation of their class (not on the bean registering methods).
     */
    public interface NodeTransformer {

        <T extends Enum<T> & QueryField> Node transform(Node node, final Class<T> queryFieldType);

        /**
         * Base implementation that does no real transformation but allows extenders to easily modify keys and / or values by simply extending
         * the extension points.
         */
        abstract class Abstract implements NodeTransformer {

            public <T extends Enum<T> & QueryField> Node transform(final Node node, final Class<T> queryFieldType) {
                return node.transform(comparison -> transform(comparison, queryFieldType));
            }

            protected <T extends Enum<T> & QueryField> Comparison transform(final Comparison comparison, final Class<T> queryFieldType) {
                final String key = transformKey(comparison.getKey(), comparison, queryFieldType).toString();
                final Object value = transformValue(comparison.getValue(), comparison, queryFieldType);
                return key.equals(comparison.getKey()) && Objects.equals(value, comparison.getValue())
                        ? comparison : Comparison.builder().key(key).op(comparison.getOp()).value(value).build();
            }

            // just extension points for subclasses
            @SuppressWarnings("java:S1172") // comparison and queryFieldType might be useful for subclasses
            protected <T extends Enum<T> & QueryField> Object transformKey(
                    final String key, final Comparison comparison, final Class<T> queryFieldType) {
                return key;
            }

            // internal, override only if you really want to replace whole lists
            protected <T extends Enum<T> & QueryField> Object transformValue(
                    final Object value, final Comparison comparison, final Class<T> queryFieldType) {
                if (value instanceof List<?> list) {
                    final List<Object> mappedList = new ArrayList<>();
                    boolean modified = false;
                    for (final Object e : list) {
                        final Object mapped = transformValueElement(e, comparison, queryFieldType);
                        if (!Objects.equals(mapped, value)) {
                            modified = true;
                        }
                        mappedList.add(mapped);
                    }
                    return modified ? mappedList : list;
                } else {
                    return transformValueElement(value, comparison, queryFieldType);
                }
            }

            // just extension points for subclasses
            @SuppressWarnings("java:S1172") // comparison and queryFieldType might be useful for subclasses
            protected <T extends Enum<T> & QueryField> Object transformValueElement(
                    final Object value, final Comparison comparison, final Class<T> queryFieldType) {
                return value;
            }
        }
    }
}