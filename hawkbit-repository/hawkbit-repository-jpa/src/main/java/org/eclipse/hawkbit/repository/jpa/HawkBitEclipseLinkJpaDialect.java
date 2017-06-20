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

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaDialect;

/**
 * {@link EclipseLinkJpaDialect} with additional exception translation
 * mechanisms.
 *
 */
public class HawkBitEclipseLinkJpaDialect extends EclipseLinkJpaDialect {
    private static final long serialVersionUID = 1L;

    private static final SQLStateSQLExceptionTranslator SQLSTATE_EXCEPTION_TRANSLATOR = new SQLStateSQLExceptionTranslator();

    @Override
    public DataAccessException translateExceptionIfPossible(final RuntimeException ex) {
        final DataAccessException dataAccessException = super.translateExceptionIfPossible(ex);

        if (dataAccessException != null) {
            return translateJpaSystemExceptionIfPossible(dataAccessException);
        }

        return searchAndTranslateSqlException(ex);
    }

    private static DataAccessException translateJpaSystemExceptionIfPossible(
            final DataAccessException accessException) {
        if (!(accessException instanceof JpaSystemException)) {
            return accessException;
        }

        final DataAccessException sql = searchAndTranslateSqlException(accessException);
        if (sql != null) {
            return sql;
        }

        return accessException;
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
