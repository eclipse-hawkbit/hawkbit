/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.context;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.eclipse.hawkbit.audit.MdcHandler;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * {@link ContextAware} provides means for getting the current context (via {@link #getCurrentContext()}) and then
 * to execute a {@link Runnable} or a {@link Function} in the same context using {@link #runInContext(String, Runnable)}
 * or {@link #runInContext(String, Function, Object)}.
 * <p/>
 * This is useful for scheduled background operations like rollouts and auto assignments where they shall
 * be processed in the scope of the creator.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ContextAware {

    private static SecurityContextSerializer securityContextSerializer = SecurityContextSerializer.NOP;

    /**
     * Provides means to set a custom {@link SecurityContextSerializer} implementation.
     * @param serializer the serializer to set. Must not be <code>null</code>.
     */
    public static void setSecurityContextSerializer(@NonNull  final SecurityContextSerializer serializer) {
        securityContextSerializer = Objects.requireNonNull(serializer);
    }

    /**
     * Return the current context encoded as a {@link String}. Depending on the implementation it could,
     * for instance, be a serialized context or a reference to such.
     *
     * @return could be empty if there is nothing to serialize or context aware is not supported.
     */
    public static Optional<String> getCurrentContext() {
        return Optional.ofNullable(SecurityContextHolder.getContext()).map(securityContextSerializer::serialize);
    }

    /**
     * Wrap a specific execution in a known and pre-serialized context.
     *
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     * @param serializedContext created by {@link #getCurrentContext()}. Must be non-<code>null</code>.
     * @param function function to call in the reconstructed context. Must be non-<code>null</code>.
     * @param t the argument that will be passed to the function
     * @return the function result
     */
    public static <T, R> R runInContext(final String serializedContext, final Function<T, R> function, final T t) {
        Objects.requireNonNull(serializedContext);
        Objects.requireNonNull(function);
        final SecurityContext securityContext = securityContextSerializer.deserialize(serializedContext);
        Objects.requireNonNull(securityContext);

        return runInContext(securityContext, () -> function.apply(t));
    }

    /**
     * Wrap a specific execution in a known and pre-serialized context.
     *
     * @param serializedContext created by {@link #getCurrentContext()}. Must be non-<code>null</code>.
     * @param runnable runnable to call in the reconstructed context. Must be non-<code>null</code>.
     */
    public static void runInContext(final String serializedContext, final Runnable runnable) {
        Objects.requireNonNull(runnable);
        runInContext(serializedContext, v -> {
            runnable.run();
            return null;
        }, null);
    }

    public static <T> T runInContext(final SecurityContext securityContext, final Supplier<T> supplier) {
        final SecurityContext originalContext = SecurityContextHolder.getContext();
        if (Objects.equals(securityContext, originalContext)) {
            return supplier.get();
        } else {
            SecurityContextHolder.setContext(securityContext);
            try {
                return MdcHandler.getInstance().callWithAuthRE(supplier::get);
            } finally {
                SecurityContextHolder.setContext(originalContext);
            }
        }
    }
}