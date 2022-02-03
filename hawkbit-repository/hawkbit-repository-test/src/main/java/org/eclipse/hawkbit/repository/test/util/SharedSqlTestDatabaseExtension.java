/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.test.util;

import static org.eclipse.hawkbit.repository.test.util.DatasourceContext.SPRING_DATASOURCE_URL_KEY;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a test database configuration for a "shared" database instance across all tests annotated with this extension
 */
public class SharedSqlTestDatabaseExtension implements BeforeAllCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(SharedSqlTestDatabaseExtension.class);
    protected static final AtomicReference<DatasourceContext> CONTEXT = new AtomicReference<>();

    @Override
    public void beforeAll(final ExtensionContext extensionContext) {
        final DatasourceContext testDatasourceContext = new DatasourceContext();
        
        if (testDatasourceContext.isNotProperlyConfigured()) {
            LOGGER.info("\033[0;33mSchema generation skipped... No datasource environment variables found!\033[0m");
            return;
        }

        // update CONTEXT only if the current value is null => initialize only
        if (!CONTEXT.compareAndSet(null, testDatasourceContext)) {
            final String randomSchemaUri = matchingDatabase(testDatasourceContext).getRandomSchemaUri();
            LOGGER.info("\033[0;33mReusing Random Schema at URI {} \033[0m", randomSchemaUri);
            return;
        }

        final AbstractSqlTestDatabase database = matchingDatabase(testDatasourceContext);
        final String randomSchemaUri = database.createRandomSchema().getRandomSchemaUri();
        LOGGER.info("\033[0;33mRandom Schema URI is {} \033[0m", randomSchemaUri);
        System.setProperty(SPRING_DATASOURCE_URL_KEY, randomSchemaUri);

        registerDropSchemaOnSystemShutdownHook(database, randomSchemaUri);
    }

    private void registerDropSchemaOnSystemShutdownHook(final AbstractSqlTestDatabase database, final String schemaUri) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.warn("\033[0;33mDropping schema at url {}  \033[0m", schemaUri);
            database.dropRandomSchema();
        }));
    }

    protected AbstractSqlTestDatabase matchingDatabase(final DatasourceContext context) {
        AbstractSqlTestDatabase database;

        switch (context.getDatabase()) {
            case "H2":
                database = new H2TestDatabase(context);
                break;
            case "SQL_SERVER":
                database = new MsSqlTestDatabase(context);
                break;
            case "MYSQL":
                database = new MySqlTestDatabase(context);
                break;
            case "POSTGRESQL":
                database = new PostgreSqlTestDatabase(context);
                break;
            default:
                throw new IllegalStateException("No supported database found for type " + context.getDatabase());
        }

        return database;
    }

}
