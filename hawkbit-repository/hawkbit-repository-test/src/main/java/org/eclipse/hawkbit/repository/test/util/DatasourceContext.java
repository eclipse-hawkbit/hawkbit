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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Holds all database related configuration
 */
@Getter
@Slf4j
class DatasourceContext {

    static final String SPRING_DATASOURCE_URL_KEY = "spring.datasource.url";
    static final String SPRING_DATABASE_KEY = "spring.jpa.database";
    static final String SPRING_DATABASE_USERNAME_KEY = "spring.datasource.username";
    static final String SPRING_DATABASE_PASSWORD_KEY = "spring.datasource.password";
    static final String DATABASE_PREFIX_KEY = "spring.database.random.prefix";

    private static final String RANDOM_DB_PREFIX = System.getProperty(DATABASE_PREFIX_KEY, "HAWKBIT_TEST_");

    private final String database;
    private final String datasourceUrl;
    private final String username;
    private final String password;
    private final String randomSchemaName = RANDOM_DB_PREFIX + TestdataFactory.randomString(10);

    DatasourceContext(final String database, final String datasourceUrl, final String username, final String password) {
        this.database = database;
        this.datasourceUrl = datasourceUrl;
        this.username = username;
        this.password = password;
    }

    DatasourceContext() {
        database = System.getProperty(SPRING_DATABASE_KEY, System.getProperty(upperCaseVariant(SPRING_DATABASE_KEY)));
        datasourceUrl = System.getProperty(SPRING_DATASOURCE_URL_KEY, System.getProperty(upperCaseVariant(SPRING_DATASOURCE_URL_KEY)));
        username = System.getProperty(SPRING_DATABASE_USERNAME_KEY, System.getProperty(upperCaseVariant(SPRING_DATABASE_USERNAME_KEY)));
        password = System.getProperty(SPRING_DATABASE_PASSWORD_KEY, System.getProperty(upperCaseVariant(SPRING_DATABASE_PASSWORD_KEY)));
    }

    public boolean isNotProperlyConfigured() {
        log.debug(
                "Datasource environment variables: [database: {}, username: {}, password: {}, datasourceUrl: {}]",
                database, username, password, datasourceUrl);

        return database == null || datasourceUrl == null || username == null || password == null;
    }

    private static String upperCaseVariant(final String key) {
        return key.toUpperCase().replace('.', '_');
    }
}