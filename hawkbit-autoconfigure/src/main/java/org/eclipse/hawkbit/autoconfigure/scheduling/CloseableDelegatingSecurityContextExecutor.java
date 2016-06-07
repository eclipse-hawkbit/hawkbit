/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.scheduling;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Extension for {@link DelegatingSecurityContextExecutor} to allow proper
 * shutdown at {@link Bean} destruction time.
 *
 */
public class CloseableDelegatingSecurityContextExecutor extends DelegatingSecurityContextExecutor {

    private final ThreadPoolExecutor executor;

    /**
     * Creates a new {@link CloseableDelegatingSecurityContextExecutor} that
     * uses the current {@link SecurityContext} from the
     * {@link SecurityContextHolder} at the time the task is submitted.
     *
     * @param delegate
     *            the {@link Executor} to delegate to. Cannot be null.
     */
    public CloseableDelegatingSecurityContextExecutor(final ThreadPoolExecutor delegate) {
        super(delegate);
        executor = delegate;
    }

    /**
     * Initiates an orderly shutdown in which previously submitted tasks are
     * executed, but no new tasks will be accepted.
     */
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * Initiates an immediate shutdown.
     * 
     * @return a list of the tasks that were awaiting execution
     */
    public List<Runnable> shutdownNow() {
        return executor.shutdownNow();
    }

}
