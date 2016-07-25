/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.executor;

/**
 * 
 * A interface to register a runnable, which will be executed after a successful
 * spring transaction.
 *
 */
@FunctionalInterface
public interface AfterTransactionCommitExecutor {

    /**
     * Register a runnable which will be executed after a successful spring
     * transaction.
     * 
     * @param runnable
     *            the after commit runnable
     */
    void afterCommit(Runnable runnable);
}
