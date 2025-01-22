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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A singleton bean which holds the {@link AfterTransactionCommitExecutor} to provide it to in beans not instantiated by spring e.g. JPA
 * entities which cannot be autowired.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("java:S6548") // java:S6548 - singleton holder ensures static access to spring resources in some places
public final class AfterTransactionCommitExecutorHolder {

    private static final AfterTransactionCommitExecutorHolder SINGLETON = new AfterTransactionCommitExecutorHolder();

    @Getter
    private AfterTransactionCommitExecutor afterCommit;

    /**
     * @return the cache manager holder singleton instance
     */
    public static AfterTransactionCommitExecutorHolder getInstance() {
        return SINGLETON;
    }

    @Autowired // spring setter injection
    public void setAfterTransactionCommitExecutor(final AfterTransactionCommitExecutor afterCommit) {
        this.afterCommit = afterCommit;
    }
}