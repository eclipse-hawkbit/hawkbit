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

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Provides a convenient way to generate a test database that can be used, and disposed of after the test is executed.
 */
@Slf4j
public class DisposableSqlTestDatabaseExtension extends SharedSqlTestDatabaseExtension implements AfterAllCallback {

    private DatasourceContext datasourceContext = null;

    @Override
    public void beforeAll(final ExtensionContext extensionContext) {
        super.beforeAll(extensionContext);
        final DatasourceContext sharedContext = CONTEXT.get();
        if (sharedContext == null || sharedContext.isNotProperlyConfigured()) {
            return;
        }
        datasourceContext = new DatasourceContext(sharedContext.getDatabase(), sharedContext.getDatasourceUrl(),
                sharedContext.getUsername(), sharedContext.getPassword());
        final AbstractSqlTestDatabase database = matchingDatabase(datasourceContext);
        final String randomSchemaUri = database.createRandomSchema().getRandomSchemaUri();
        log.info("\033[0;33mRandom Schema URI is {} \033[0m", randomSchemaUri);
        System.setProperty(SPRING_DATASOURCE_URL_KEY, randomSchemaUri);
    }

    @Override
    public void afterAll(final ExtensionContext extensionContext) {
        if (datasourceContext == null) {
            return;
        }
        matchingDatabase(datasourceContext).dropRandomSchema();
        System.setProperty(SPRING_DATASOURCE_URL_KEY, matchingDatabase(CONTEXT.get()).getRandomSchemaUri());
    }
}
