/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.test.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

@Slf4j
public class TestLoggerExtension implements BeforeTestExecutionCallback, TestWatcher {

    @Override
    public void testSuccessful(final ExtensionContext context) {
        log.info("Test {} succeeded.", context.getTestMethod());
    }

    @Override
    public void testFailed(final ExtensionContext context, final Throwable cause) {
        log.error("Test {} failed with {}.", context.getTestMethod(), cause.getMessage());
    }

    @Override
    public void beforeTestExecution(final ExtensionContext context) {
        log.info("Starting Test {}...", context.getTestMethod());
    }
}