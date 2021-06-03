/**
 * Copyright (c) 2020 Microsoft and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.test.util;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestExecutionListener;

/**
 * A {@link TestExecutionListener} for creating and dropping MySql schemas if
 * tests are setup with MySql.
 */
public class PostgreSqlTestDatabase extends AbstractSqlTestDatabase {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgreSqlTestDatabase.class);
    private static final String POSTGRESQL_URI_PATTERN = "jdbc:postgresql://{host}:{port}/{db}*";

    public PostgreSqlTestDatabase(final DatasourceContext context) {
        super(context);
    }

    @Override
    protected PostgreSqlTestDatabase createRandomSchema() {
        final String uri = context.getDatasourceUrl();
        final String schemaName = getSchemaName(uri);
        LOGGER.info("\033[0;33m Creating postgreSql schema {} if not existing \033[0m", context.getRandomSchemaName());

        executeStatement(uri.split("/" + schemaName)[0],
                "CREATE SCHEMA IF NOT EXISTS " + context.getRandomSchemaName() + ";");
        return this;
    }

    @Override
    protected void dropRandomSchema() {
        final String uri = context.getDatasourceUrl();
        final String schemaName = getSchemaName(uri);
        LOGGER.info("\033[0;33m Dropping postgreSql schema {} if not existing \033[0m", context.getRandomSchemaName());
        executeStatement(uri.split("/" + schemaName)[0], "DROP SCHEMA " + context.getRandomSchemaName() + " CASCADE;");
    }

    @Override
    protected String getRandomSchemaUri() {
        final String uri = context.getDatasourceUrl();
        final Map<String, String> databaseProperties = MATCHER.extractUriTemplateVariables(POSTGRESQL_URI_PATTERN, uri);
        final String schemaName = getSchemaName(uri);

        return POSTGRESQL_URI_PATTERN.replace("{host}", databaseProperties.get("host"))
                .replace("{port}", databaseProperties.get("port"))
                .replace("{db}*", schemaName.split("\\?")[0]) + "?currentSchema=" + context.getRandomSchemaName();
    }

    private static String getSchemaName(final String uri) {
        return MATCHER.extractUriTemplateVariables(POSTGRESQL_URI_PATTERN, uri).get("db");
    }
}
