/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.test.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.util.StringUtils;

/**
 * A {@link TestExecutionListener} for creating and dropping MySql schemas if
 * tests are setup with MySql.
 */
public class MySqlTestDatabase extends AbstractSqlTestDatabase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MySqlTestDatabase.class);
    protected static final String MYSQL_URI_PATTERN = "jdbc:mysql://{host}:{port}/{db}*";

    @Override 
    protected boolean isApplicable() {
        return "MYSQL".equals(System.getProperty("spring.jpa.database")) //
                && MATCHER.match(MYSQL_URI_PATTERN, System.getProperty(SPRING_DATASOURCE_URL_KEY))  //
                && !StringUtils.isEmpty(getSchemaName());
    }

    @Override
    public void createSchema() {
        final String schemaName = getSchemaName();
        final String uri = System.getProperty(SPRING_DATASOURCE_URL_KEY);
        LOGGER.info("\033[0;33m Creating mysql schema {} if not existing \033[0m", schemaName);

        executeStatement(uri.split("/" + schemaName)[0], "CREATE SCHEMA IF NOT EXISTS " + schemaName + ";");
    }

    @Override
    protected void dropSchema() {
        final String schemaName = getSchemaName();
        final String uri = System.getProperty(SPRING_DATASOURCE_URL_KEY);
        executeStatement(uri.split("/" + schemaName)[0], "DROP SCHEMA " + schemaName + ";");
    }

    @Override
    protected String getRandomSchemaUri() {
       final String schemaName = "HAWKBIT_TEST_" + RandomStringUtils.randomAlphanumeric(10);
        final String uri = System.getProperty(SPRING_DATASOURCE_URL_KEY);
        return uri.substring(0, uri.lastIndexOf('/') + 1) + schemaName;
    }

    private static String getSchemaName() {
        return MATCHER.extractUriTemplateVariables(MYSQL_URI_PATTERN, System.getProperty(SPRING_DATASOURCE_URL_KEY))
                .get("db");
    }
}
