/**
 * Copyright (c) 2020 Microsoft and others.
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
 * A {@link TestExecutionListener} for creating and dropping MySql schemas if
 * tests are setup with MySql.
 */
public class PostgreSqlTestDatabase extends AbstractSqlTestDatabase {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgreSqlTestDatabase.class);
    protected static final String POSTGRESQL_URI_PATTERN = "jdbc:postgresql://{host}:{port}/{path}?currentSchema={db}*";

    @Override
    protected boolean isApplicable() {
        return "POSTGRESQL".equals(System.getProperty("spring.jpa.database")) //
                && MATCHER.match(POSTGRESQL_URI_PATTERN, System.getProperty(SPRING_DATASOURCE_URL_KEY))  //
                && !StringUtils.isEmpty(getSchemaName());
    }

    @Override
    protected void createSchema() {
        final String schemaName = getSchemaName();
        final String uri = System.getProperty(SPRING_DATASOURCE_URL_KEY);
        LOGGER.info("\033[0;33m Creating postgreSql schema {} if not existing \033[0m", schemaName);

        executeStatement(uri.split("\\?currentSchema=")[0], "CREATE SCHEMA IF NOT EXISTS " + schemaName + ";");
    }

    @Override
    protected void dropSchema() {
        final String schemaName = getSchemaName();
        final String uri = System.getProperty(SPRING_DATASOURCE_URL_KEY);

        executeStatement(uri.split("\\?currentSchema=")[0], "DROP schema " + schemaName + " CASCADE;");
    }

    @Override
    protected String getRandomSchemaUri() {
        final String schemaName = "HAWKBIT_TEST" + RandomStringUtils.randomAlphanumeric(10).toLowerCase();
        final String uri = System.getProperty(SPRING_DATASOURCE_URL_KEY);

        return uri.substring(0, uri.indexOf('?')) + "?currentSchema=" + schemaName;
    }

    private static String getSchemaName() {
        return MATCHER.extractUriTemplateVariables(POSTGRESQL_URI_PATTERN, System.getProperty(SPRING_DATASOURCE_URL_KEY))
                .get("db");
    }
}
