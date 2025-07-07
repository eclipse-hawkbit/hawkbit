/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.ql.utils;

import java.lang.reflect.InvocationTargetException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.eclipse.hawkbit.repository.RsqlQueryField;
import org.eclipse.hawkbit.repository.jpa.rsql.RsqlUtility;
import org.eclipse.hawkbit.repository.jpa.rsql.RsqlUtility.RsqlToSpecBuilder;

public class HawkbitQlToSql {

    private final EntityManager entityManager;
    private final boolean isEclipselink;

    public HawkbitQlToSql(final EntityManager entityManager) {
        this.entityManager = entityManager;
        isEclipselink = entityManager.getProperties().keySet().stream().anyMatch(key -> key.startsWith("eclipselink."));
    }

    public <T, A extends Enum<A> & RsqlQueryField> String toSQL(
            final Class<T> domainClass, final Class<A> fieldsClass, final String rsql, final RsqlToSpecBuilder rsqlToSpecBuilder) {
        final CriteriaQuery<T> query = createQuery(domainClass, fieldsClass, rsql, rsqlToSpecBuilder);
        final TypedQuery<?> typedQuery = entityManager.createQuery(query);
        if (isEclipselink) {
            try {
                return (String)Class.forName("org.eclipse.hawkbit.repository.jpa.EclipselinkUtils")
                        .getMethod("toSql", Query.class)
                        .invoke(null, typedQuery);
            } catch (final IllegalAccessException | NoSuchMethodException | ClassNotFoundException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException re) {
                    throw re;
                } else {
                    throw new IllegalStateException(e.getCause());
                }
            }
        } else {
            return HibernateUtils.toSql(typedQuery);
        }
    }

    private <T, A extends Enum<A> & RsqlQueryField> CriteriaQuery<T> createQuery(
            final Class<T> domainClass, final Class<A> fieldsClass, final String rsql, final RsqlToSpecBuilder rsqlToSpecBuilder) {
        final CriteriaQuery<T> query = entityManager.getCriteriaBuilder().createQuery(domainClass);
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final RsqlToSpecBuilder defaultRsqlToSpecBuilder = RsqlUtility.getInstance().getRsqlToSpecBuilder();
        if (defaultRsqlToSpecBuilder != rsqlToSpecBuilder) {
            RsqlUtility.getInstance().setRsqlToSpecBuilder(rsqlToSpecBuilder);
        }
        try {
            return query.where(RsqlUtility.getInstance().<A, T> buildRsqlSpecification(rsql, fieldsClass)
                    .toPredicate(query.from(domainClass), cb.createQuery(domainClass), cb));
        } finally {
            if (defaultRsqlToSpecBuilder != rsqlToSpecBuilder) {
                RsqlUtility.getInstance().setRsqlToSpecBuilder(defaultRsqlToSpecBuilder);
            }
        }
    }
}