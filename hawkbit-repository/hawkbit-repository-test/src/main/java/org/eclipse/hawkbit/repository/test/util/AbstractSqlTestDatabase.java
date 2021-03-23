/**
 * Copyright (c) 2020 Microsoft and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.test.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.AntPathMatcher;

/**
 * A {@link TestExecutionListener} for creating and dropping SQL schemas if
 * tests are setup with an SQL schema.
 */
public abstract class AbstractSqlTestDatabase extends AbstractTestExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSqlTestDatabase.class);
    protected static final AntPathMatcher MATCHER = new AntPathMatcher();

    protected static final String SPRING_DATASOURCE_URL_KEY = "spring.datasource.url";
    protected static final String USERNAME = System.getProperty("spring.datasource.username");
    protected static final String PASSWORD = System.getProperty("spring.datasource.password");

    protected abstract boolean isApplicable();

    protected abstract void createSchema();

    protected abstract void dropSchema();

    protected abstract String getRandomSchemaUri();

    protected static void executeStatement(final String uri, final String statement) {
        try (final Connection connection = DriverManager.getConnection(uri, AbstractSqlTestDatabase.USERNAME,
                AbstractSqlTestDatabase.PASSWORD)) {
            try (final PreparedStatement preparedStatement = connection.prepareStatement(statement)) {
                preparedStatement.execute();
            }
        } catch (final SQLException e) {
            LOGGER.error("Execution of statement '{}' on uri {} failed!", statement, uri, e);
        }
    }
}
