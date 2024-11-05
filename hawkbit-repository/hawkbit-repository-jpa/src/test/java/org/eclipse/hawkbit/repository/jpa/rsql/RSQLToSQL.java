/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.eclipse.hawkbit.repository.RsqlQueryField;
import org.eclipse.hawkbit.repository.rsql.RsqlConfigHolder;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.jpa.JpaQuery;
import org.eclipse.persistence.queries.DatabaseQuery;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class RSQLToSQL {

    private static final Database DATABASE = Database.H2;
    private final EntityManager entityManager;

    public RSQLToSQL(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public <T, A extends Enum<A> & RsqlQueryField> String toSQL(final Class<T> domainClass, final Class<A> fieldsClass, final String rsql,
            final boolean legacyRsqlVisitor) {
        return createDbQuery(domainClass, fieldsClass, rsql, legacyRsqlVisitor).getSQLString();
    }

    public <T, A extends Enum<A> & RsqlQueryField> DatabaseQuery createDbQuery(final Class<T> domainClass, final Class<A> fieldsClass,
            final String rsql, final boolean legacyRsqlVisitor) {
        final CriteriaQuery<T> query = createQuery(domainClass, fieldsClass, rsql, legacyRsqlVisitor);
        final TypedQuery<?> typedQuery = entityManager.createQuery(query);
        // executes the query - otherwise the SQL string is not generated
        typedQuery.setParameter(PersistenceUnitProperties.MULTITENANT_PROPERTY_DEFAULT, "DEFAULT");
        typedQuery.getResultList();
        return typedQuery.unwrap(JpaQuery.class).getDatabaseQuery();
    }

    private <T, A extends Enum<A> & RsqlQueryField> CriteriaQuery<T> createQuery(final Class<T> domainClass, final Class<A> fieldsClass,
            final String rsql, final boolean legacyRsqlVisitor) {
        final CriteriaQuery<T> query = entityManager.getCriteriaBuilder().createQuery(domainClass);
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        return query.where(
                RsqlConfigHolder.getInstance().isLegacyRsqlVisitor() == legacyRsqlVisitor ?
                        // use directly
                        RSQLUtility.<A, T> buildRsqlSpecification(rsql, fieldsClass, null, DATABASE)
                                .toPredicate(query.from(domainClass), cb.createQuery(domainClass), cb) :
                        toPredicate(rsql, fieldsClass, null,
                                query.from(domainClass), cb.createQuery(domainClass), cb, legacyRsqlVisitor)
        );
    }

    private <T, A extends Enum<A> & RsqlQueryField> Predicate toPredicate(
            final String rsql,
            final Class<A> fieldsClass, final VirtualPropertyReplacer virtualPropertyReplacer,
            final Root<T> root, final CriteriaQuery<?> query, final CriteriaBuilder cb,
            final boolean legacyRsqlVisitor) {
        final Node rootNode = new RSQLParser(RSQLOperators.defaultOperators()).parse(rsql);
        query.distinct(true);

        final RSQLVisitor<List<Predicate>, String> jpqQueryRSQLVisitor =
                legacyRsqlVisitor ?
                        new JpaQueryRsqlVisitor<>(
                                root, cb, fieldsClass,
                                virtualPropertyReplacer, DATABASE, query,
                                !RsqlConfigHolder.getInstance().isCaseInsensitiveDB() && RsqlConfigHolder.getInstance().isIgnoreCase())
                        :
                                new JpaQueryRsqlVisitorG2<>(
                                        fieldsClass, root, query, cb,
                                        DATABASE, virtualPropertyReplacer,
                                        !RsqlConfigHolder.getInstance().isCaseInsensitiveDB() && RsqlConfigHolder.getInstance().isIgnoreCase());
        final List<Predicate> accept = rootNode.accept(jpqQueryRSQLVisitor);

        if (CollectionUtils.isEmpty(accept)) {
            return cb.conjunction();
        } else {
            return cb.and(accept.toArray(new Predicate[0]));
        }
    }
}