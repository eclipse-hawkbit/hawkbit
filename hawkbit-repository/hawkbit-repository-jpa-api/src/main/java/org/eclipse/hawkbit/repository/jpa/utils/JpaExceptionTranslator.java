/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.utils;

import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLErrorCodes;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;

/**
 * A single point of exception translators in hawkBit
 * in order to be used in Hibernate and EclipseLink implementation
 * and unify jpa exception translations behaviour in the project
 */
public class JpaExceptionTranslator {

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
        // explicitly set old translator as a fallback (uses Subclass translator by default)
        SQL_EXCEPTION_TRANSLATOR.setFallbackTranslator(new SQLStateSQLExceptionTranslator());
    }

    private JpaExceptionTranslator() {}

    public static SQLExceptionTranslator getTranslator() {
        return SQL_EXCEPTION_TRANSLATOR;
    }
}
