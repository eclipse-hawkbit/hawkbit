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

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import cz.jirutka.rsql.parser.RSQLParserException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.text.StrLookup;
import org.eclipse.hawkbit.repository.RsqlQueryField;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.jpa.ql.SpecificationBuilder;
import org.eclipse.hawkbit.repository.jpa.rsql.legacy.SpecificationBuilderLegacy;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
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
public class RsqlUtility {

    private static final RsqlUtility SINGLETON = new RsqlUtility();

    public enum RsqlToSpecBuilder {
        LEGACY_G1, // legacy RSQL visitor
        LEGACY_G2, // G2 RSQL visitor
        G3 // G3 RSQL visitor - still experimental / yet default
    }

    /**
     * If RSQL comparison operators shall ignore the case. If ignore case is <code>true</code> "x == ax" will match "x == aX"
     */
    @Value("${hawkbit.rsql.ignore-case:true}")
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
    private RsqlToSpecBuilder rsqlToSpecBuilder;

    private VirtualPropertyReplacer virtualPropertyReplacer;
    private Database database;
    private EntityManager entityManager;

    /**
     * @return The holder singleton instance.
     */
    public static RsqlUtility getInstance() {
        return SINGLETON;
    }

    @Autowired(required = false)
    void setVirtualPropertyReplacer(final VirtualPropertyReplacer virtualPropertyReplacer) {
        this.virtualPropertyReplacer = virtualPropertyReplacer;
    }

    @Autowired
    void setDatabase(final JpaProperties jpaProperties) {
        database = jpaProperties.getDatabase();
    }

    @Autowired
    void setEntityManager(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Builds a JPA {@link Specification} which corresponds with the given RSQL query. The specification can be used to filter for JPA entities
     * with the given RSQL query.
     *
     * @param rsql the rsql query to be parsed
     * @param rsqlQueryFieldType the enum class type which implements the {@link RsqlQueryField}
     * @return a specification which can be used with JPA
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *         given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     */
    public <A extends Enum<A> & RsqlQueryField, T> Specification<T> buildRsqlSpecification(
            final String rsql, final Class<A> rsqlQueryFieldType) {
        if (rsqlToSpecBuilder == RsqlToSpecBuilder.G3) {
            return new SpecificationBuilder<T>(virtualPropertyReplacer, !caseInsensitiveDB && ignoreCase, database)
                    .specification(RsqlParser.parse(caseInsensitiveDB || ignoreCase ? rsql.toLowerCase() : rsql, rsqlQueryFieldType));
        } else {
            return new SpecificationBuilderLegacy<A, T>(rsqlQueryFieldType, virtualPropertyReplacer, database).specification(rsql);
        }
    }

    /**
     * Validates the RSQL string
     *
     * @param rsql RSQL string to validate
     * @param rsqlQueryFieldType the enum class type which implements the {@link RsqlQueryField}
     * @param jpaType the JPA entity type to validate against
     * @throws RSQLParserException if RSQL syntax is invalid
     * @throws RSQLParameterUnsupportedFieldException if RSQL key is not allowed
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <A extends Enum<A> & RsqlQueryField> void validateRsqlFor(
            final String rsql, final Class<A> rsqlQueryFieldType, final Class<?> jpaType) {
        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<?> criteriaQuery = criteriaBuilder.createQuery(jpaType);
        buildRsqlSpecification(rsql, rsqlQueryFieldType).toPredicate(criteriaQuery.from((Class) jpaType), criteriaQuery, criteriaBuilder);
    }
}