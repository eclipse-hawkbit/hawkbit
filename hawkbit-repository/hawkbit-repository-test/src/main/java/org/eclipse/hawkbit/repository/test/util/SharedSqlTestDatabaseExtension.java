/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.test.util;

import static org.eclipse.hawkbit.repository.test.util.DatasourceContext.SPRING_DATASOURCE_URL_KEY;

import java.util.concurrent.atomic.AtomicReference;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Represents a test database configuration for a "shared" database instance across all tests annotated with this extension
 */
@Slf4j
public class SharedSqlTestDatabaseExtension implements BeforeAllCallback {
    
    protected static final AtomicReference<DatasourceContext> CONTEXT = new AtomicReference<>();

    @Override
    public void beforeAll(final ExtensionContext extensionContext) {
        final DatasourceContext testDatasourceContext = new DatasourceContext();
        
        if (testDatasourceContext.isNotProperlyConfigured()) {
            log.info("\033[0;33mSchema generation skipped... No datasource environment variables found!\033[0m");
            return;
        }

        // update CONTEXT only if the current value is null => initialize only
        if (!CONTEXT.compareAndSet(null, testDatasourceContext)) {
            final String randomSchemaUri = matchingDatabase(testDatasourceContext).getRandomSchemaUri();
            log.info("\033[0;33mReusing Random Schema at URI {} \033[0m", randomSchemaUri);
            return;
        }

        final AbstractSqlTestDatabase database = matchingDatabase(testDatasourceContext);
        final String randomSchemaUri = database.createRandomSchema().getRandomSchemaUri();
        log.info("\033[0;33mRandom Schema URI is {} \033[0m", randomSchemaUri);
        System.setProperty(SPRING_DATASOURCE_URL_KEY, randomSchemaUri);

        registerDropSchemaOnSystemShutdownHook(database, randomSchemaUri);
    }

    private void registerDropSchemaOnSystemShutdownHook(final AbstractSqlTestDatabase database, final String schemaUri) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.warn("\033[0;33mDropping schema at url {}  \033[0m", schemaUri);
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
