/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.test.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.Extension;

/**
 * An {@link Extension} for creating and dropping MS SQL Server
 * schemas if tests are set up with MS SQL Server.
 */
@Slf4j
public class MsSqlTestDatabase extends AbstractSqlTestDatabase {

    private static final String DATABASE_AND_SURROUNDINGS = ";database=";

    public MsSqlTestDatabase(final DatasourceContext context) {
        super(context);
    }

    @Override
    public MsSqlTestDatabase createRandomSchema() {
        final String uri = context.getDatasourceUrl();
        log.info("\033[0;33mCreating mssql schema {} \033[0m", context.getRandomSchemaName());

        executeStatement(uri.split(DATABASE_AND_SURROUNDINGS)[0], "CREATE DATABASE " + context.getRandomSchemaName() + ";");
        return this;
    }

    @Override
    protected void dropRandomSchema() {
        final String uri = context.getDatasourceUrl();
        final String dbServerUri = uri.split(DATABASE_AND_SURROUNDINGS)[0];
        log.info("\033[0;33mDropping mssql schema {} \033[0m", context.getRandomSchemaName());

        // Needed to avoid the DROP is rejected with "database still in use"
        executeStatement(dbServerUri, "ALTER DATABASE " + context.getRandomSchemaName() + " SET SINGLE_USER WITH ROLLBACK IMMEDIATE;");
        executeStatement(dbServerUri, "DROP DATABASE " + context.getRandomSchemaName() + ";");
    }

    @Override
    protected String getRandomSchemaUri() {
        final String uri = context.getDatasourceUrl();
        return uri.substring(0, uri.indexOf(';')) + DATABASE_AND_SURROUNDINGS + context.getRandomSchemaName();
    }
}