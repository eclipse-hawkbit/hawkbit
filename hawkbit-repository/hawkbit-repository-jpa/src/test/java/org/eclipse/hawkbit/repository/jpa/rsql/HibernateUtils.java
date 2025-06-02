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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Query;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.internal.QuerySqmImpl;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.sql.SqmTranslation;
import org.hibernate.query.sqm.sql.SqmTranslator;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.query.sqm.tree.SqmDmlStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@Slf4j
public class HibernateUtils {

    private static final Method getSqmTranslatorFactory;

    static {
        Method method = null;
        try {
            method = QueryEngine.class.getMethod("getSqmTranslatorFactory");
        } catch (final NoSuchMethodException e) {
            log.warn("Can't resolve getSqmTranslatorFactory method (Utils.toString won't work)", e);
        }
        getSqmTranslatorFactory = method;
    }

    public static String toSql(final Query query) {
        if (getSqmTranslatorFactory == null) {
            throw new UnsupportedOperationException("SqmTranslatorFactory resolver is not available");
        }

        final QuerySqmImpl<?> hqlQuery = query.unwrap(QuerySqmImpl.class);
        final SessionFactoryImplementor factory = hqlQuery.getSessionFactory();
        final SharedSessionContractImplementor session = hqlQuery.getSession();
        final SessionFactoryImplementor sessionFactory = session.getFactory();

        final SqmTranslatorFactory sqmTranslatorFactory;
        try {
            sqmTranslatorFactory = (SqmTranslatorFactory) getSqmTranslatorFactory.invoke(factory.getQueryEngine());
        } catch (final IllegalAccessException | InvocationTargetException e) {
            throw new UnsupportedOperationException("Can't create SqmTranslatorFactory", e);
        }

        final SqmTranslator<? extends Statement> sqmSelectTranslator =
                hqlQuery.getSqmStatement() instanceof SqmSelectStatement<?> selectStatement
                        ? sqmTranslatorFactory.createSelectTranslator(selectStatement,
                                hqlQuery.getQueryOptions(), hqlQuery.getDomainParameterXref(), hqlQuery.getQueryParameterBindings(),
                                hqlQuery.getLoadQueryInfluencers(), sessionFactory, false)
                        : sqmTranslatorFactory.createMutationTranslator((SqmDmlStatement<?>) hqlQuery.getSqmStatement(),
                                hqlQuery.getQueryOptions(), hqlQuery.getDomainParameterXref(), hqlQuery.getQueryParameterBindings(),
                                hqlQuery.getLoadQueryInfluencers(), sessionFactory);

        final SqmTranslation<? extends Statement> sqmTranslation = sqmSelectTranslator.translate();
        final SqlAstTranslatorFactory sqlAstTranslatorFactory = factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory();
        final Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> jdbcParamsXref = SqmUtil.generateJdbcParamsXref(
                hqlQuery.getDomainParameterXref(), sqmTranslation::getJdbcParamsBySqmParam);

        final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(hqlQuery.getQueryParameterBindings(),
                hqlQuery.getDomainParameterXref(), jdbcParamsXref, factory.getRuntimeMetamodels().getMappingMetamodel(),
                sqmSelectTranslator.getFromClauseAccess()::findTableGroup, new SqmParameterMappingModelResolutionAccess() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public <T> MappingModelExpressible<T> getResolvedMappingModelType(final SqmParameter<T> parameter) {
                        return (MappingModelExpressible<T>) sqmTranslation.getSqmParameterMappingModelTypeResolutions().get(parameter);
                    }
                }, hqlQuery.getSession());
        return (sqmTranslation.getSqlAst() instanceof SelectStatement selectStatement
                ? sqlAstTranslatorFactory.buildSelectTranslator(factory, selectStatement)
                        .translate(jdbcParameterBindings, hqlQuery.getQueryOptions())
                : sqlAstTranslatorFactory.buildMutationTranslator(factory, (MutationStatement) sqmTranslation.getSqlAst())
                        .translate(jdbcParameterBindings, hqlQuery.getQueryOptions()))
                .getSqlString();
    }
}