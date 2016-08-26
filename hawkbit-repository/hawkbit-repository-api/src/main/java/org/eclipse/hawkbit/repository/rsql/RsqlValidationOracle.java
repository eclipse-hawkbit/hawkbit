/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.rsql;

/**
 * An interface declaration which validates an RSQL based query syntax and
 * allows providing suggestions e.g. in case of syntax errors or current cursor
 * position.
 */
@FunctionalInterface
public interface RsqlValidationOracle {

    /**
     * Parses and validates an given RSQL based query syntax and provides
     * suggestion based on syntax error and cursor positioning.
     * 
     * @param rsqlQuery
     *            an RSQL based query string to parse.
     * @param cursorPosition
     *            the position of the cursor to retrieve suggestions at the
     *            position. {@code -1} indicates for no cursor suggestion
     * @return a validation oracle context providing information about syntax
     *         errors and possible suggestions for fixing the syntax error or at
     *         the cursor position to replace tokens
     */
    ValidationOracleContext suggest(final String rsqlQuery, final int cursorPosition);

}
