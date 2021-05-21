/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.test.util;

import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JUnitTestLoggerExtension implements BeforeTestExecutionCallback, TestWatcher {

    private static final Logger LOG = LoggerFactory.getLogger(JUnitTestLoggerExtension.class);

    @Override
    public void testSuccessful(ExtensionContext context) {
        LOG.info("Test {} succeeded.", context.getTestMethod());
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        LOG.error("Test {} failed with {}.", context.getTestMethod());
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        LOG.info("Starting Test {}...", context.getTestMethod());
    }
}
