/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.test.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.util.StringUtils;

/**
 * A {@link TestExecutionListener} for creating and dropping MS SQL Server
 * schemas if tests are setup with MS SQL Server.
 */
public class MsSqlTestDatabase extends AbstractSqlTestDatabase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MsSqlTestDatabase.class);
    private static final String MSSQL_URI_PATTERN = "jdbc:sqlserver://{host}:{port};database={db}*";

    @Override
    protected boolean isApplicable() {
        return "SQL_SERVER".equals(System.getProperty("spring.jpa.database")) //
                && MATCHER.match(MSSQL_URI_PATTERN, System.getProperty(SPRING_DATASOURCE_URL_KEY)) //
                && !StringUtils.isEmpty(getSchemaName());
    }

    @Override
    public void createSchema() {
        final String uri = System.getProperty(SPRING_DATASOURCE_URL_KEY);
        final String schemaName = getSchemaName();
        LOGGER.info("\033[0;33m Creating mssql schema {} if not existing \033[0m", schemaName);

        executeStatement(uri.split(";database=")[0], "CREATE DATABASE IF NOT EXISTS " + schemaName + ";");
    }

    @Override
    protected void dropSchema() {
        final String uri = System.getProperty(SPRING_DATASOURCE_URL_KEY);
        final String schemaName = getSchemaName();
        final String dbServerUri = uri.split(";database=")[0];

        // Needed to avoid the DROP is rejected with "database still in use"
        executeStatement(dbServerUri, "ALTER DATABASE " + schemaName + " SET SINGLE_USER WITH ROLLBACK IMMEDIATE;");
        executeStatement(dbServerUri, "DROP DATABASE " + schemaName + ";");
    }

    @Override
    protected String getRandomSchemaUri() {
        final String schemaName = "HAWKBIT_TEST_" + RandomStringUtils.randomAlphanumeric(10);
        final String uri = System.getProperty(SPRING_DATASOURCE_URL_KEY);
        return uri.substring(0, uri.indexOf(';')) + ";database=" + schemaName;
    }

    private static String getSchemaName() {
        return MATCHER.extractUriTemplateVariables(MSSQL_URI_PATTERN, System.getProperty(SPRING_DATASOURCE_URL_KEY))
                .get("db");
    }
}
