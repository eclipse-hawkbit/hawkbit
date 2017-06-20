/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.sql.SQLException;

import javax.persistence.PersistenceException;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaDialect;

/**
 * {@link EclipseLinkJpaDialect} with additional exception translation
 * mechanisms based on {@link SQLStateSQLExceptionTranslator}.
 * 
 * There are multiple variations of exceptions coming out of persistence
 * provider:
 * 
 * <p>
 * 1) {@link PersistenceException}s that can be mapped by
 * {@link EclipseLinkJpaDialect} into corresponding {@link DataAccessException}.
 * <p>
 * 2) {@link PersistenceException}s that could not be mapped by
 * {@link EclipseLinkJpaDialect} directly but instead are wrapped into
 * {@link JpaSystemException}.
 * <p>
 * 2.a) here the wrapped exception's causes might be an {@link SQLException}
 * which might be mappable by {@link SQLStateSQLExceptionTranslator} or
 * <p>
 * 2.b.) the wrapped exception's causes due not contain an {@link SQLException}
 * and as a result cannot be mapped.
 * <p>
 * 3) A {@link RuntimeException} that is no {@link PersistenceException}.
 * <p>
 * 3.a) here a cause might be an {@link SQLException} which might be mappable by
 * {@link SQLStateSQLExceptionTranslator} or
 * <p>
 * 3.b.) the the cause is not an {@link SQLException} and as a result cannot be
 * mapped.
 *
 */
public class HawkBitEclipseLinkJpaDialect extends EclipseLinkJpaDialect {
    private static final long serialVersionUID = 1L;

    private static final SQLStateSQLExceptionTranslator SQLSTATE_EXCEPTION_TRANSLATOR = new SQLStateSQLExceptionTranslator();

    @Override
    public DataAccessException translateExceptionIfPossible(final RuntimeException ex) {
        final DataAccessException dataAccessException = super.translateExceptionIfPossible(ex);

        if (dataAccessException == null) {
            return searchAndTranslateSqlException(ex);
        }

        return translateJpaSystemExceptionIfPossible(dataAccessException);
    }

    private static DataAccessException translateJpaSystemExceptionIfPossible(
            final DataAccessException accessException) {
        if (!(accessException instanceof JpaSystemException)) {
            return accessException;
        }

        final DataAccessException sql = searchAndTranslateSqlException(accessException);
        if (sql == null) {
            return accessException;
        }

        return sql;
    }

    private static DataAccessException searchAndTranslateSqlException(final RuntimeException ex) {
        final SQLException sqlException = findSqlException(ex);

        if (sqlException == null) {
            return null;
        }

        return SQLSTATE_EXCEPTION_TRANSLATOR.translate(null, null, sqlException);
    }

    private static SQLException findSqlException(final RuntimeException jpaSystemException) {
        Throwable exception = jpaSystemException;
        do {
            final Throwable cause = exception.getCause();
            if (cause instanceof SQLException) {
                return (SQLException) cause;
            }
            exception = cause;
        } while (exception != null);

        return null;
    }
}
