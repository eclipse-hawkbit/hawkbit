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
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.text.StrLookup;
import org.eclipse.hawkbit.repository.ActionFields;
import org.eclipse.hawkbit.repository.QueryField;
import org.eclipse.hawkbit.repository.exception.QueryException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.jpa.ql.Node.Comparison;
import org.eclipse.hawkbit.repository.jpa.rsql.RsqlParser;
import org.eclipse.hawkbit.repository.jpa.rsql.legacy.SpecificationBuilderLegacy;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;

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
public class QLSupport {

    private static final QLSupport SINGLETON = new QLSupport();

    public enum SpecBuilder {
        LEGACY_G1, // legacy RSQL visitor
        LEGACY_G2, // G2 RSQL visitor
        G3 // G3 specification builder - still experimental / yet default
    }

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
    @Value("${hawkbit.rsql.case-insensitive-db:false}")
    private boolean caseInsensitiveDB;

    /**
     * @deprecated in favour fixed final visitor / spec builder of G2 RSQL visitor / G3 spec builder. since 0.6.0
     */
    @Setter // for tests only
    @Deprecated(forRemoval = true, since = "0.6.0")
    @Value("${hawkbit.rsql.rsql-to-spec-builder:G3}") //
    private SpecBuilder specBuilder;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private QueryParser parser;
    private Database database;
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

    @Autowired
    void setDatabase(final JpaProperties jpaProperties) {
        database = jpaProperties.getDatabase();
    }

    @Autowired
    void setEntityManager(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Autowired(required = false)
    void setVirtualPropertyResolver(final VirtualPropertyResolver virtualPropertyResolver) {
        this.virtualPropertyResolver = virtualPropertyResolver;
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
        if (specBuilder == SpecBuilder.G3) {
            return new SpecificationBuilder<T>(!caseInsensitiveDB && ignoreCase, database)
                    .specification(parser.parse(caseInsensitiveDB || ignoreCase ? query.toLowerCase() : query, queryFieldType));
        } else {
            return new SpecificationBuilderLegacy<A, T>(queryFieldType, virtualPropertyResolver, database).specification(query);
        }
    }

    public <A extends Enum<A> & QueryField> EntityMatcher entityMatcher(final String query, final Class<A> queryFieldType) {
        return EntityMatcher.of(parser.parse(caseInsensitiveDB || ignoreCase ? query.toLowerCase() : query, queryFieldType));
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

    public interface QueryParser {

        <T extends Enum<T> & QueryField> Node parse(final String query, final Class<T> queryFieldType) throws QueryException;
    }

    public static class DefaultQueryParser implements QueryParser {

        @Override
        public <T extends Enum<T> & QueryField> Node parse(final String query, final Class<T> queryFieldType) throws QueryException {
            return RsqlParser.parse(query, queryFieldType).map(comparison -> map(comparison, queryFieldType));
        }

        protected <T extends Enum<T> & QueryField> Comparison map(final Comparison comparison, final Class<T> queryFieldType) {
            final String key = mapKey(comparison.getKey(), comparison, queryFieldType).toString();
            final Object value = mapValue(comparison.getValue(), comparison, queryFieldType);
            return key.equals(comparison.getKey()) && Objects.equals(value, comparison.getValue())
                    ? comparison : Comparison.builder().key(key).op(comparison.getOp()).value(value).build();
        }

        // just extension points for subclasses
        protected <T extends Enum<T> & QueryField> Object mapKey(final String key, final Comparison comparison, final Class<T> queryFieldType) {
            return key;
        }

        // internal, override only if you really want to replace whole lists
        protected <T extends Enum<T> & QueryField> Object mapValue(
                final Object value, final Comparison comparison, final Class<T> queryFieldType) {
            if (value instanceof List<?> list) {
                final List<Object> mappedList = new ArrayList<>();
                boolean modified = false;
                for (final Object e : list) {
                    final Object mapped = mapSimpleValue(e, comparison, queryFieldType);
                    if (!Objects.equals(mapped, value)) {
                        modified = true;
                    }
                    mappedList.add(mapped);
                }
                return modified ? mappedList : list;
            } else {
                return mapSimpleValue(value, comparison, queryFieldType);
            }
        }

        // just extension points for subclasses
        protected <T extends Enum<T> & QueryField> Object mapSimpleValue(
                final Object value, final Comparison comparison, final Class<T> queryFieldType) {
            return queryFieldType == (Class<?>) ActionFields.class && "active".equalsIgnoreCase(comparison.getKey())
                    ? mapActionStatus(value)
                    : value;
        }

        private static Object mapActionStatus(final Object value) {
            final String strValue = String.valueOf(value);
            if ("true".equalsIgnoreCase(strValue) || "false".equalsIgnoreCase(strValue)) {
                return value;
            } else {
                // handle custom action fields status
                try {
                    return ActionFields.convertStatusValue(strValue);
                } catch (final IllegalArgumentException e) {
                    throw new RSQLParameterUnsupportedFieldException(e.getMessage());
                }
            }
        }
    }
}