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
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.jpa.utils.JpaExceptionTranslator;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import org.jspecify.annotations.NonNull;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaDialect;

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
 */
@Slf4j
class HawkbitEclipseLinkJpaDialect extends EclipseLinkJpaDialect {

    @Serial
    private static final long serialVersionUID = 1L;

    // TODO: switch to Spring fix - temporarily workaround of https://github.com/eclipse-hawkbit/hawkbit/issues/2876
    //  (https://github.com/spring-projects/spring-framework/issues/36165)
    private final ReentrantLock supperTransactionIsolationLock;
    @SuppressWarnings("java:S3011") // temporarily - to workaround bug in supper class
    HawkbitEclipseLinkJpaDialect() {
        try {
            final Field field = EclipseLinkJpaDialect.class.getDeclaredField("transactionIsolationLock");
            field.setAccessible(true);
            supperTransactionIsolationLock = (ReentrantLock) field.get(this);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException("Cannot access supper class field transactionIsolationLock", e);
        }
    }
    // hawkbit uses JDBC and no need of lazy connection fetching
    @Override
    public ConnectionHandle getJdbcConnection(final EntityManager entityManager, final boolean readOnly) throws PersistenceException {
        final Connection connection;
        supperTransactionIsolationLock.lock();
        try {
            connection = entityManager.unwrap(Connection.class);
        } finally {
            supperTransactionIsolationLock.unlock();
        }
        return () -> connection;
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