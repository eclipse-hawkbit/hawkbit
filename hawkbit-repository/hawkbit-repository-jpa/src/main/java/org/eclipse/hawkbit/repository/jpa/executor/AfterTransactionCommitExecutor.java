/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.executor;

/**
 * A interface to register a runnable, which will be executed after a successful
 * spring transaction.
 */
@FunctionalInterface
public interface AfterTransactionCommitExecutor {

    /**
     * Register a runnable which will be executed after a successful spring
     * transaction.
     *
     * @param runnable the after commit runnable
     */
    void afterCommit(Runnable runnable);
}