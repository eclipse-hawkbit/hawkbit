/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.test.util;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class SharedSqlTestDatabase implements BeforeAllCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(SharedSqlTestDatabase.class);
    protected static final String SPRING_DATASOURCE_KEY = "spring.datasource.url";

    protected static final AbstractSqlTestDatabase[] DATABASES = new AbstractSqlTestDatabase[] { new H2TestDatabase(),
            new MsSqlTestDatabase(), new MySqlTestDatabase(), new PostgreSqlTestDatabase() };

    static {
        if (!StringUtils.isEmpty(System.getProperty("spring.jpa.database")) && !StringUtils.isEmpty(
                System.getProperty(SPRING_DATASOURCE_KEY))) {
            System.setProperty(SPRING_DATASOURCE_KEY, getRandomSchemaName());
        }
    }

    public SharedSqlTestDatabase() {
        registerDropSchemaShutdownHook();
    }

    @Override
    public void beforeAll(final ExtensionContext extensionContext) {
        if (StringUtils.isEmpty(System.getProperty("spring.jpa.database")) ||
                StringUtils.isEmpty(System.getProperty(SPRING_DATASOURCE_KEY))) {
            LOGGER.info("No database uri configured. Skipping...");
            return;
        }
        createSchema();
    }

    protected static void createSchema() {
        for (final AbstractSqlTestDatabase database : DATABASES) {
            if (database.isApplicable()) {
                database.createSchema();
                return;
            }
        }

        throw noSupportedDbFoundException();
    }

    private static void registerDropSchemaShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (final AbstractSqlTestDatabase database : DATABASES) {
                if (database.isApplicable()) {
                    database.dropSchema();
                    return;
                }
            }
        }));
    }

    protected static String getRandomSchemaName() {
        for (final AbstractSqlTestDatabase database : DATABASES) {
            if (database.isApplicable()) {
                return database.getRandomSchemaUri();
            }
        }
        throw noSupportedDbFoundException();
    }

    private static IllegalStateException noSupportedDbFoundException() {
        return new IllegalStateException(
                "Found no supported database matching uri " + System.getProperty(SPRING_DATASOURCE_KEY));
    }

}
