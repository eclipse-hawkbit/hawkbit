/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit;

import org.eclipse.hawkbit.tenancy.TenantAware;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * {@link ContextAware} provides means for getting the current context (via {@link #getCurrentContext()}) and then
 * to execute a {@link Runnable} or a {@link Function} in the same context using {@link #runInContext(String, Runnable)}
 * or {@link #runInContext(String, Function, Object)}.
 * <p/>
 * This is useful for scheduled background operations like rollouts and auto assignments where they shall
 * be processed in the scope of the creator.
 */
public interface ContextAware extends TenantAware {

    /**
     * Return the current context encoded as a {@link String}. Depending on the implementation it could,
     * for instance, be a serialized context or a reference to such.
     * 
     * @return could be empty if there is nothing to serialize or context aware is not supported.
     */
    Optional<String> getCurrentContext();

    /**
     * Wrap a specific execution in a known and pre-serialized context.
     *
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     *
     * @param serializedContext created by {@link #getCurrentContext()}. Must be non-<code>null</code>.
     * @param function function to call in the reconstructed context. Must be non-<code>null</code>.
     * @param t the argument that will be passed to the function
     * @return the function result
     */
    <T, R> R runInContext(String serializedContext, Function<T, R> function, T t);

    /**
     * Wrap a specific execution in a known and pre-serialized context.
     *
     * @param serializedContext created by {@link #getCurrentContext()}. Must be non-<code>null</code>.
     * @param runnable runnable to call in the reconstructed context. Must be non-<code>null</code>.
     */
    default void runInContext(String serializedContext, Runnable runnable) {
        Objects.requireNonNull(runnable);
        runInContext(serializedContext, v -> {
            runnable.run();
            return null;
        }, null);
    }
}