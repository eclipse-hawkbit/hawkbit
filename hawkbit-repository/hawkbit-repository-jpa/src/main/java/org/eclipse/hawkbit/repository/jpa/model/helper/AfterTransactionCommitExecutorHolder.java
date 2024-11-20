/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.model.helper;

import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.EntityPropertyChangeListener;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A singleton bean which holds the {@link AfterTransactionCommitExecutor} to
 * have to the cache manager in beans not instantiated by spring e.g. JPA
 * entities or {@link EntityPropertyChangeListener} which cannot be autowired.
 */
public final class AfterTransactionCommitExecutorHolder {

    private static final AfterTransactionCommitExecutorHolder SINGLETON = new AfterTransactionCommitExecutorHolder();

    @Autowired
    private AfterTransactionCommitExecutor afterCommit;

    private AfterTransactionCommitExecutorHolder() {

    }

    /**
     * @return the cache manager holder singleton instance
     */
    public static AfterTransactionCommitExecutorHolder getInstance() {
        return SINGLETON;
    }

    /**
     * @return the afterCommit
     */
    public AfterTransactionCommitExecutor getAfterCommit() {
        return afterCommit;
    }

    /**
     * @param afterCommit the afterCommit to set
     */
    public void setAfterCommit(final AfterTransactionCommitExecutor afterCommit) {
        this.afterCommit = afterCommit;
    }

}
