/**
 * Copyright (c) 2020 Microsoft and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.test.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * A {@link TestExecutionListener} for creating and dropping MySql schemas if
 * tests are setup with MySql.
 */
public abstract class AbstractSqlTestDatabase extends AbstractTestExecutionListener {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractSqlTestDatabase.class);
    protected String schemaName;
    protected String uri;
    protected String username;
    protected String password;

    @Override
    public void beforeTestClass(final TestContext testContext) throws Exception {
        if (isRunningWithSql()) {
            LOG.info("Setting up database for test class {}", testContext.getTestClass().getName());
            this.username = System.getProperty("spring.datasource.username");
            this.password = System.getProperty("spring.datasource.password");
            this.uri = System.getProperty("spring.datasource.url");
            createSchemaUri();
            createSchema();
        }
    }

    @Override
    public void afterTestClass(final TestContext testContext) throws Exception {
        if (isRunningWithSql()) {
            dropSchema();
        }
    }

    protected abstract void createSchemaUri();

    protected abstract boolean isRunningWithSql();

    protected abstract void createSchema();

    protected abstract void dropSchema();
}
