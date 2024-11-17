/**
 * Copyright (c) 2020 Microsoft and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.test.util;

import static java.sql.DriverManager.getConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.AntPathMatcher;

/**
 * A {@link TestExecutionListener} for creating and dropping SQL schemas if
 * tests are setup with an SQL schema.
 */
@Slf4j
public abstract class AbstractSqlTestDatabase extends AbstractTestExecutionListener {

    protected static final AntPathMatcher MATCHER = new AntPathMatcher();

    protected final DatasourceContext context;

    public AbstractSqlTestDatabase(final DatasourceContext context) {
        this.context = context;
    }

    protected abstract AbstractSqlTestDatabase createRandomSchema();

    protected abstract void dropRandomSchema();

    protected abstract String getRandomSchemaUri();

    protected void executeStatement(final String uri, final String statement) {
        log.trace("\033[0;33mExecuting statement {} on uri {} \033[0m", statement, uri);

        try (final Connection connection = getConnection(uri, context.getUsername(), context.getPassword());
                final PreparedStatement preparedStatement = connection.prepareStatement(statement)) {
            preparedStatement.execute();
        } catch (final SQLException e) {
            log.error("Execution of statement '{}' on uri {} failed!", statement, uri, e);
        }
    }
}