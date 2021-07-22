/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import java.util.List;
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.text.StrLookup;
import org.eclipse.hawkbit.repository.FieldNameProvider;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.rsql.RsqlVisitorFactoryHolder;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.util.CollectionUtils;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;

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
     * Builds a JPA {@link Specification} which corresponds with the given RSQL
     * query. The specification can be used to filter for JPA entities with the
     * given RSQL query.
     *
     * @param rsql
     *            the rsql query to be parsed
     * @param fieldNameProvider
     *            the enum class type which implements the
     *            {@link FieldNameProvider}
     * @param virtualPropertyReplacer
     *            holds the logic how the known macros have to be resolved; may
     *            be <code>null</code>
     * @param database
     *            in use
     *
     * @return an specification which can be used with JPA
     * @throws RSQLParameterUnsupportedFieldException
     *             if a field in the RSQL string is used but not provided by the
     *             given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException
     *             if the RSQL syntax is wrong
     *
     */
    public static <A extends Enum<A> & FieldNameProvider, T> Specification<T> buildRsqlSpecification(final String rsql,
            final Class<A> fieldNameProvider, final VirtualPropertyReplacer virtualPropertyReplacer,
            final Database database) {
        return new RSQLSpecification<>(rsql, fieldNameProvider, virtualPropertyReplacer, database);
    }

    /**
     * Validates the RSQL string
     * 
     * @param rsql
     *            RSQL string to validate
     * @param fieldNameProvider
     * 
     * @throws RSQLParserException
     *             if RSQL syntax is invalid
     * @throws RSQLParameterUnsupportedFieldException
     *             if RSQL key is not allowed
     */
    public static <A extends Enum<A> & FieldNameProvider> void validateRsqlFor(final String rsql,
            final Class<A> fieldNameProvider) {
        final RSQLVisitor<Void, String> visitor = getValidationRsqlVisitor(fieldNameProvider);
        final Node rootNode = parseRsql(rsql);
        rootNode.accept(visitor);
    }

    private static <A extends Enum<A> & FieldNameProvider> RSQLVisitor<Void, String> getValidationRsqlVisitor(
            final Class<A> fieldNameProvider) {
        return RsqlVisitorFactoryHolder.getInstance().getRsqlVisitorFactory().validationRsqlVisitor(fieldNameProvider);
    }

    private static Node parseRsql(final String rsql) {
        try {
            LOGGER.debug("Parsing rsql string {}", rsql);
            final Set<ComparisonOperator> operators = RSQLOperators.defaultOperators();
            return new RSQLParser(operators).parse(rsql.toLowerCase());
        } catch (final IllegalArgumentException e) {
            throw new RSQLParameterSyntaxException("rsql filter must not be null", e);
        } catch (final RSQLParserException e) {
            throw new RSQLParameterSyntaxException(e);
        }
    }

    private static final class RSQLSpecification<A extends Enum<A> & FieldNameProvider, T> implements Specification<T> {

        private static final long serialVersionUID = 1L;

        private final String rsql;
        private final Class<A> enumType;
        private final VirtualPropertyReplacer virtualPropertyReplacer;
        private final Database database;

        private RSQLSpecification(final String rsql, final Class<A> enumType,
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

            final JpaQueryRsqlVisitor<A, T> jpqQueryRSQLVisitor = new JpaQueryRsqlVisitor<>(root, cb, enumType,
                    virtualPropertyReplacer, database, query);
            final List<Predicate> accept = rootNode.<List<Predicate>, String> accept(jpqQueryRSQLVisitor);

            if (!CollectionUtils.isEmpty(accept)) {
                return cb.and(accept.toArray(new Predicate[accept.size()]));
            }
            return cb.conjunction();

        }
    }

}
