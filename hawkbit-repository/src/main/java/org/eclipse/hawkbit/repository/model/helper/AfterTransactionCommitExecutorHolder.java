/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.model.helper;

import org.eclipse.hawkbit.eventbus.EntityPropertyChangeListener;
import org.eclipse.hawkbit.executor.AfterTransactionCommitExecutor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A singleton bean which holds the {@link AfterTransactionCommitExecutor} to
 * have to the cache manager in beans not instantiated by spring e.g. JPA
 * entities or {@link EntityPropertyChangeListener} which cannot be autowired.
 *
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
     * @param afterCommit
     *            the afterCommit to set
     */
    public void setAfterCommit(final AfterTransactionCommitExecutor afterCommit) {
        this.afterCommit = afterCommit;
    }

}
