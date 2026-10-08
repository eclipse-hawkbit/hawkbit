/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import java.io.Serial;
import java.lang.reflect.UndeclaredThrowableException;
import java.sql.Connection;
import java.sql.SQLException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import org.eclipse.hawkbit.repository.jpa.utils.JpaExceptionTranslator;
import org.eclipse.persistence.sessions.UnitOfWork;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.ConnectionProxy;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaDialect;
import org.springframework.transaction.TransactionDefinition;

/**
 * {@link EclipseLinkJpaDialect} with additional exception translation mechanisms based on {@link SQLStateSQLExceptionTranslator}.
 * There are multiple variations of exceptions coming out of persistence provider:
 * <ol>
 *     <li>{@link PersistenceException}s that can be mapped by {@link EclipseLinkJpaDialect} into corresponding {@link DataAccessException}</li>
 *     <li>{@link PersistenceException}s that could not be mapped by {@link EclipseLinkJpaDialect} directly but instead are wrapped into {@link JpaSystemException}.
 *         <ol>
 *             <li>here the wrapped exception's causes might be an {@link SQLException} which might be mappable by {@link SQLStateSQLExceptionTranslator} or </li>
 *             <li>the wrapped exception's causes due not contain an {@link SQLException} and as a result cannot be mapped. </li>
 *         </ol>
 *     </li>
 *     <li>A {@link RuntimeException} that is no {@link PersistenceException}.
 *         <ol>
 *              <li>here a cause might be an {@link SQLException} which might be mappable by {@link SQLStateSQLExceptionTranslator} or </li>
 *              <li>the cause is not an {@link SQLException} and as a result cannot be mapped.</li>
 *         </ol>
 *     </li>
 * </ol>
 * Additionally, it acquires the physical JDBC connection of a transaction outside the dialect wide lock - see {@link #beginTransaction}.
 */
class HawkbitEclipseLinkJpaDialect extends EclipseLinkJpaDialect {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Workaround for the dialect wide lock of {@link EclipseLinkJpaDialect#beginTransaction}.
     * <p/>
     * <b>Problem:</b> for write and custom isolation transactions the super class begins the database transaction early, holding a dialect
     * wide lock while EclipseLink acquires the JDBC connection. So any blocking connection acquisition - waiting for a free pool (Hikari)
     * connection or for a throttle permit - stalls the transaction begin of every other thread, whatever its tenant.
     * <p/>
     * <b>Workaround:</b>
     * <ol>
     * <li>EclipseLink gets a {@link LazyConnectionDataSourceProxy} as data source (see {@link JpaConfiguration}) - so under the lock it
     * acquires just a connection handle, the isolation level and auto-commit are only recorded on it. No blocking under the lock.</li>
     * <li>Right after the lock is released, the physical connection is acquired eagerly here. That keeps the behaviour without the
     * workaround - connection acquired (blocking for pool / throttle) at transaction begin and a refused connection fails the begin
     * (fail fast), before any transactional code runs. Otherwise it would be acquired lazily, on the first statement.</li>
     * </ol>
     */
    @Override
    public Object beginTransaction(final EntityManager entityManager, final TransactionDefinition definition) throws SQLException {
        final Object transactionData = super.beginTransaction(entityManager, definition);
        // early transaction - connection handle acquired (same check as EclipseLinkJpaDialect.EclipseLinkConnectionHandle)
        if (entityManager.unwrap(UnitOfWork.class).getParent().isInTransaction()
                && entityManager.unwrap(Connection.class) instanceof ConnectionProxy connectionProxy) {
            try {
                connectionProxy.getTargetConnection();
            } catch (final UndeclaredThrowableException e) {
                // getTargetConnection doesn't declare the SQLException of the target data source
                if (e.getCause() instanceof SQLException sqlException) {
                    throw sqlException;
                }
                throw e;
            }
        }
        return transactionData;
    }

    @Override
    public DataAccessException translateExceptionIfPossible(@NonNull final RuntimeException ex) {
        final DataAccessException dataAccessException = super.translateExceptionIfPossible(ex);
        if (dataAccessException == null) {
            return searchAndTranslateSqlException(ex);
        }
        return translateJpaSystemExceptionIfPossible(dataAccessException);
    }

    private static DataAccessException translateJpaSystemExceptionIfPossible(final DataAccessException accessException) {
        if (!(accessException instanceof JpaSystemException)) {
            return accessException;
        }

        final DataAccessException sqlException = searchAndTranslateSqlException(accessException);
        if (sqlException == null) {
            return accessException;
        }
        return sqlException;
    }

    private static DataAccessException searchAndTranslateSqlException(final RuntimeException ex) {
        final SQLException sqlException = findSqlException(ex);
        if (sqlException == null) {
            return null;
        }
        return JpaExceptionTranslator.getTranslator().translate("", null, sqlException);
    }

    private static SQLException findSqlException(final RuntimeException jpaSystemException) {
        Throwable exception = jpaSystemException;
        do {
            final Throwable cause = exception.getCause();
            if (cause instanceof SQLException sqlException) {
                return sqlException;
            }
            exception = cause;
        } while (exception != null);
        return null;
    }
}
