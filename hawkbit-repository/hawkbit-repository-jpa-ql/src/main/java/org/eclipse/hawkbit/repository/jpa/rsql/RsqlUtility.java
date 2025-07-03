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

import static org.eclipse.hawkbit.repository.jpa.rsql.RsqlConfigHolder.RsqlToSpecBuilder.G3;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import cz.jirutka.rsql.parser.RSQLParserException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.text.StrLookup;
import org.eclipse.hawkbit.repository.RsqlQueryField;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.jpa.ql.SpecificationBuilder;
import org.eclipse.hawkbit.repository.jpa.rsql.legacy.SpecificationBuilderLegacy;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyResolver;
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
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RsqlUtility {

    /**
     * Builds a JPA {@link Specification} which corresponds with the given RSQL query. The specification can be used to filter for JPA entities
     * with the given RSQL query.
     *
     * @param rsql the rsql query to be parsed
     * @param rsqlQueryFieldType the enum class type which implements the {@link RsqlQueryField}
     * @param virtualPropertyReplacer holds the logic how the known macros have to be resolved; may be <code>null</code>
     * @param database database in use
     * @return a specification which can be used with JPA
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *         given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     */
    public static <A extends Enum<A> & RsqlQueryField, T> Specification<T> buildRsqlSpecification(
            final String rsql, final Class<A> rsqlQueryFieldType,
            final VirtualPropertyReplacer virtualPropertyReplacer, final Database database) {
        if (RsqlConfigHolder.getInstance().getRsqlToSpecBuilder() == G3) {
            return new SpecificationBuilder<T>(
                    virtualPropertyReplacer,
                    !RsqlConfigHolder.getInstance().isCaseInsensitiveDB() && RsqlConfigHolder.getInstance().isIgnoreCase(),
                    database)
                    .specification(RsqlParser.parse(
                            RsqlConfigHolder.getInstance().isCaseInsensitiveDB() || RsqlConfigHolder.getInstance().isIgnoreCase()
                                    ? rsql.toLowerCase() : rsql,
                            rsqlQueryFieldType));
        } else {
            return new SpecificationBuilderLegacy<A, T>(rsqlQueryFieldType, virtualPropertyReplacer, database).specification(rsql);
        }
    }

    /**
     * Validates the RSQL string
     *
     * @param rsql RSQL string to validate
     * @param rsqlQueryFieldType
     * @throws RSQLParserException if RSQL syntax is invalid
     * @throws RSQLParameterUnsupportedFieldException if RSQL key is not allowed
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <A extends Enum<A> & RsqlQueryField> void validateRsqlFor(
            final String rsql, final Class<A> rsqlQueryFieldType,
            final Class<?> jpaType,
            final VirtualPropertyReplacer virtualPropertyReplacer, final EntityManager entityManager) {
        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<?> criteriaQuery = criteriaBuilder.createQuery(jpaType);
        buildRsqlSpecification(rsql, rsqlQueryFieldType, virtualPropertyReplacer, null)
                .toPredicate(criteriaQuery.from((Class) jpaType), criteriaQuery, criteriaBuilder);
    }
}