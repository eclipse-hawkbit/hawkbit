/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.test.util;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Provides a convenient way to generate a test database that can be used, and disposed of after the test is executed.
 */
public class DisposableSqlTestDatabase extends SharedSqlTestDatabase implements AfterAllCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(DisposableSqlTestDatabase.class);
    private static final String DEFAULT_SCHEMA_URI = System.getProperty(SPRING_DATASOURCE_KEY);

    @Override
    public void beforeAll(final ExtensionContext extensionContext) {
        System.setProperty(SPRING_DATASOURCE_KEY, getRandomSchemaName());
        super.beforeAll(extensionContext);
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        for (final AbstractSqlTestDatabase database : DATABASES) {
            if (database.isApplicable()) {
                LOGGER.info("Dropping Schema at url {} using {}", System.getProperty(SPRING_DATASOURCE_KEY),
                        database.getClass().getSimpleName());
                database.dropSchema();
                break;
            }
        }

        if (!StringUtils.isEmpty(DEFAULT_SCHEMA_URI)) {
            System.setProperty(SPRING_DATASOURCE_KEY, DEFAULT_SCHEMA_URI);
        }
    }
}
