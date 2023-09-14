/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.test.util;

import java.util.Map;

import org.junit.jupiter.api.extension.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link Extension} for creating and dropping MySql schemas if
 * tests are set up with MySql.
 */
public class MySqlTestDatabase extends AbstractSqlTestDatabase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MySqlTestDatabase.class);
    protected static final String MYSQL_URI_PATTERN = "jdbc:mariadb://{host}:{port}/{db}*";

    public MySqlTestDatabase(final DatasourceContext context) {
        super(context);
    }

    @Override
    public MySqlTestDatabase createRandomSchema() {
        final String uri = context.getDatasourceUrl();
        final String schemaName = getSchemaName(uri);
        LOGGER.info("\033[0;33mCreating mysql schema {} if not existing \033[0m", context.getRandomSchemaName());

        executeStatement(uri.split("/" + schemaName)[0],
                "CREATE SCHEMA IF NOT EXISTS " + context.getRandomSchemaName() + ";");
        return this;
    }

    @Override
    protected void dropRandomSchema() {
        final String uri = context.getDatasourceUrl();
        final String schemaName = getSchemaName(uri);
        LOGGER.info("\033[0;33mDropping mysql schema {} \033[0m", context.getRandomSchemaName());
        executeStatement(uri.split("/" + schemaName)[0], "DROP SCHEMA " + context.getRandomSchemaName() + ";");
    }

    @Override
    protected String getRandomSchemaUri() {
        final String uri = context.getDatasourceUrl();
        final Map<String, String> databaseProperties = MATCHER.extractUriTemplateVariables(MYSQL_URI_PATTERN, uri);

        return MYSQL_URI_PATTERN.replace("{host}", databaseProperties.get("host"))
                .replace("{port}", databaseProperties.get("port"))
                .replace("{db}*", context.getRandomSchemaName());
    }

    private static String getSchemaName(final String datasourceUrl) {
        return MATCHER.extractUriTemplateVariables(MYSQL_URI_PATTERN, datasourceUrl).get("db");
    }
}
