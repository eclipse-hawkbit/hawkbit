/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
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

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestExecutionListener;

/**
 * A {@link TestExecutionListener} for creating and dropping MySql schemas if
 * tests are setup with MySql.
 */
public class MySqlTestDatabase extends AbstractSqlTestDatabase {

    private static final Logger LOG = LoggerFactory.getLogger(MySqlTestDatabase.class);

    @Override
    protected void createSchemaUri() {
        schemaName = "SP" + RandomStringUtils.randomAlphanumeric(10);
        this.uri = this.uri.substring(0, uri.lastIndexOf('/') + 1);

        System.setProperty("spring.datasource.url", uri + schemaName);
    }

    @Override
    protected boolean isRunningWithSql() {
        return "MYSQL".equals(System.getProperty("spring.jpa.database"));
    }

    @Override
    protected void createSchema() {
        try (Connection connection = DriverManager.getConnection(uri, username, password)) {
            try (PreparedStatement statement = connection.prepareStatement("CREATE SCHEMA " + schemaName + ";")) {
                LOG.info("Creating schema {} on uri {}", schemaName, uri);
                statement.execute();
                LOG.info("Created schema {} on uri {}", schemaName, uri);
            }
        } catch (final SQLException e) {
            LOG.error("Schema creation failed!", e);
        }

    }

    @Override
    protected void dropSchema() {
        try (Connection connection = DriverManager.getConnection(uri, username, password)) {
            try (PreparedStatement statement = connection.prepareStatement("DROP SCHEMA " + schemaName + ";")) {
                LOG.info("Dropping schema {} on uri {}", schemaName, uri);
                statement.execute();
                LOG.info("Dropped schema {} on uri {}", schemaName, uri);
            }
        } catch (final SQLException e) {
            LOG.error("Schema drop failed!", e);
        }
    }
}
