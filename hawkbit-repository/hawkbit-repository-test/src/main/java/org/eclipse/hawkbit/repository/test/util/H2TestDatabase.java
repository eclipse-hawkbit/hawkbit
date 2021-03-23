/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.test.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.util.StringUtils;

/**
 * A {@link TestExecutionListener} for creating and dropping H2 schemas if
 * tests are setup with H2.
 */
public class H2TestDatabase extends AbstractSqlTestDatabase {

    protected static final String H2_URI_PATTERN = "jdbc:h2:mem:{db};MODE=MySQL;";

    @Override 
    protected boolean isApplicable() {
        return "H2".equals(System.getProperty("spring.jpa.database")) //
                && MATCHER.match(H2_URI_PATTERN, System.getProperty("spring.datasource.url")) //
                && !StringUtils.isEmpty(getSchemaName());
    }

    @Override
    public void createSchema() {
        // do nothing, since H2 is in memory
    }

    @Override
    protected void dropSchema() {
        // do nothing, since H2 is in memory
    }

    @Override
    protected String getRandomSchemaUri() {
        return "jdbc:h2:mem:" + RandomStringUtils.randomAlphanumeric(16) +";MODE=MySQL;";
    }

    private static String getSchemaName() {
        return MATCHER.extractUriTemplateVariables(H2_URI_PATTERN, System.getProperty("spring.datasource.url"))
                .get("db");
    }
}
