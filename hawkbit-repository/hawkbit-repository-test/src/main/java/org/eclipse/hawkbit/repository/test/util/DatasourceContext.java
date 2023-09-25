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

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds all database related configuration
 */
public class DatasourceContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasourceContext.class);

    public static final String SPRING_DATASOURCE_URL_KEY = "spring.datasource.url";
    public static final String SPRING_DATABASE_KEY = "spring.jpa.database";
    public static final String SPRING_DATABASE_USERNAME_KEY = "spring.datasource.username";
    public static final String SPRING_DATABASE_PASSWORD_KEY = "spring.datasource.password";
    public static final String DATABASE_PREFIX_KEY = "spring.database.random.prefix";
    private static final String RANDOM_DB_PREFIX = System.getProperty(DATABASE_PREFIX_KEY, "HAWKBIT_TEST_");

    private final String database;
    private final String datasourceUrl;
    private final String username;
    private final String password;
    private final String randomSchemaName = RANDOM_DB_PREFIX + RandomStringUtils.randomAlphanumeric(10);

    /**
     * Constructor
     */
    public DatasourceContext(final String database, final String datasourceUrl, final String username,
            final String password) {
        this.database = database;
        this.datasourceUrl = datasourceUrl;
        this.username = username;
        this.password = password;
    }

    /**
     * Constructor
     */
    public DatasourceContext() {
        database = System.getProperty(SPRING_DATABASE_KEY, System.getProperty(upperCaseVariant(SPRING_DATABASE_KEY)));
        datasourceUrl = System.getProperty(SPRING_DATASOURCE_URL_KEY,
                System.getProperty(upperCaseVariant(SPRING_DATASOURCE_URL_KEY)));
        username = System.getProperty(SPRING_DATABASE_USERNAME_KEY,
                System.getProperty(upperCaseVariant(SPRING_DATABASE_USERNAME_KEY)));
        password = System.getProperty(SPRING_DATABASE_PASSWORD_KEY,
                System.getProperty(upperCaseVariant(SPRING_DATABASE_PASSWORD_KEY)));
    }

    private static String upperCaseVariant(final String key) {
        return key.toUpperCase().replace('.', '_');
    }
    
    public String getDatabase() {
        return database;
    }

    public String getDatasourceUrl() {
        return datasourceUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRandomSchemaName() {
        return randomSchemaName;
    }

    public boolean isNotProperlyConfigured() {
        LOGGER.debug("Datasource environment variables: [database: {}, username: {}, password: {}, datasourceUrl: {}]",
                database, username, password, datasourceUrl);
        
        return database == null || datasourceUrl == null || username == null || password == null;
    }
}
