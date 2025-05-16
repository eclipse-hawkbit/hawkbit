/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import java.io.Serial;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.text.StrLookup;
import org.eclipse.hawkbit.repository.RsqlQueryField;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.rsql.RsqlConfigHolder;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyResolver;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.util.CollectionUtils;

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
public final class RSQLUtility {

    /**
     * Builds a JPA {@link Specification} which corresponds with the given RSQL
     * query. The specification can be used to filter for JPA entities with the
     * given RSQL query.
     *
     * @param rsql the rsql query to be parsed
     * @param fieldNameProvider the enum class type which implements the {@link RsqlQueryField}
     * @param virtualPropertyReplacer holds the logic how the known macros have to be resolved; may be <code>null</code>
     * @param database database in use
     * @return a specification which can be used with JPA
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *         given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     */
    public static <A extends Enum<A> & RsqlQueryField, T> Specification<T> buildRsqlSpecification(
            final String rsql, final Class<A> fieldNameProvider,
            final VirtualPropertyReplacer virtualPropertyReplacer, final Database database) {
        return new RSQLSpecification<>(rsql, fieldNameProvider, virtualPropertyReplacer, database);
    }

    /**
     * Validates the RSQL string
     *
     * @param rsql RSQL string to validate
     * @param fieldNameProvider
     * @throws RSQLParserException if RSQL syntax is invalid
     * @throws RSQLParameterUnsupportedFieldException if RSQL key is not allowed
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <A extends Enum<A> & RsqlQueryField> void validateRsqlFor(
            final String rsql, final Class<A> fieldNameProvider,
            final Class<?> jpaType,
            final VirtualPropertyReplacer virtualPropertyReplacer, final EntityManager entityManager) {
        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<?> criteriaQuery = criteriaBuilder.createQuery(jpaType);
        new RSQLSpecification<>(rsql, fieldNameProvider, virtualPropertyReplacer, null)
                .toPredicate(criteriaQuery.from((Class)jpaType), criteriaQuery, criteriaBuilder);
    }

    static final ComparisonOperator IS = new ComparisonOperator("=is=", "=eq=");
    static final ComparisonOperator NOT = new ComparisonOperator("=not=", "=ne=");
    private static final Set<ComparisonOperator> OPERATORS;

    static {
        final Set<ComparisonOperator> operators = new HashSet<>(RSQLOperators.defaultOperators());
        // == and != alternatives just treating "null" string as null not as a "null"
        operators.add(IS);
        operators.add(NOT);
        OPERATORS = Collections.unmodifiableSet(operators);
    }

    private static final class RSQLSpecification<A extends Enum<A> & RsqlQueryField, T> implements Specification<T> {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String rsql;
        private final Class<A> enumType;
        private final VirtualPropertyReplacer virtualPropertyReplacer;
        private final Database database;

        private RSQLSpecification(
                final String rsql, final Class<A> enumType,
                final VirtualPropertyReplacer virtualPropertyReplacer, final Database database) {
            this.rsql = rsql;
            this.enumType = enumType;
            this.virtualPropertyReplacer = virtualPropertyReplacer;
            this.database = database;
        }

        @Override
        public Predicate toPredicate(final Root<T> root, final CriteriaQuery<?> query, final CriteriaBuilder cb) {
            final Node rootNode = parseRsql(rsql);
            query.distinct(true);

            final RSQLVisitor<List<Predicate>, String> jpqQueryRSQLVisitor =
                    RsqlConfigHolder.getInstance().isLegacyRsqlVisitor()
                            ? new JpaQueryRsqlVisitor<>(
                                    root, cb, enumType,
                                    virtualPropertyReplacer, database, query,
                                    !RsqlConfigHolder.getInstance().isCaseInsensitiveDB() && RsqlConfigHolder.getInstance().isIgnoreCase())
                            : new JpaQueryRsqlVisitorG2<>(
                                    enumType, root, query, cb,
                                    database, virtualPropertyReplacer,
                                    !RsqlConfigHolder.getInstance().isCaseInsensitiveDB() && RsqlConfigHolder.getInstance()
                                            .isIgnoreCase());
            final List<Predicate> accept = rootNode.accept(jpqQueryRSQLVisitor);

            if (CollectionUtils.isEmpty(accept)) {
                return cb.conjunction();
            } else {
                return cb.and(accept.toArray(new Predicate[0]));
            }
        }
    }

    private static Node parseRsql(final String rsql) {
        log.debug("Parsing rsql string {}", rsql);
        try {
            return new RSQLParser(OPERATORS).parse(
                    RsqlConfigHolder.getInstance().isCaseInsensitiveDB() || RsqlConfigHolder.getInstance().isIgnoreCase()
                            ? rsql.toLowerCase()
                            : rsql);
        } catch (final IllegalArgumentException e) {
            throw new RSQLParameterSyntaxException("RSQL filter must not be null", e);
        } catch (final RSQLParserException e) {
            throw new RSQLParameterSyntaxException(e);
        }
    }
}