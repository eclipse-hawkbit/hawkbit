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
import java.sql.SQLException;

import jakarta.persistence.PersistenceException;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLErrorCodes;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import org.springframework.lang.NonNull;
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
 * 3.b.) the cause is not an {@link SQLException} and as a result cannot be
 * mapped.
 */
class HawkbitEclipseLinkJpaDialect extends EclipseLinkJpaDialect {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final SQLErrorCodeSQLExceptionTranslator SQL_EXCEPTION_TRANSLATOR;

    // providing list/set of codes which are not handled from the sql translator properly
    private static final String[] DATA_INTEGRITY_VIOLATION_CODES = new String[] {
            "1366"
    };

    static {
        SQL_EXCEPTION_TRANSLATOR = new SQLErrorCodeSQLExceptionTranslator();
        SQLErrorCodes codes = new SQLErrorCodes();

        codes.setDataIntegrityViolationCodes(DATA_INTEGRITY_VIOLATION_CODES);
        SQL_EXCEPTION_TRANSLATOR.setSqlErrorCodes(codes);
    }

    @Override
    public DataAccessException translateExceptionIfPossible(@NonNull final RuntimeException ex) {
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

        return SQL_EXCEPTION_TRANSLATOR.translate("", null, sqlException);
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