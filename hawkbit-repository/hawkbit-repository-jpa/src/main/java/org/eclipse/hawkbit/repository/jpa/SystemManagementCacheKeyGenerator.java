/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

import jakarta.validation.constraints.NotNull;

import lombok.NonNull;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.annotation.Bean;

/**
 * Implementation of {@link CurrentTenantCacheKeyGenerator}.
 */
public class SystemManagementCacheKeyGenerator implements CurrentTenantCacheKeyGenerator {

    private final ThreadLocal<String> createInitialTenant = new ThreadLocal<>();

    @Override
    @Bean
    public KeyGenerator currentTenantKeyGenerator() {
        return new CurrentTenantKeyGenerator();
    }

    /**
     * Get the tenant which overwrites the actual tenant used by the
     * {@linkplain #currentTenantKeyGenerator()}.
     *
     * @return A present optional in case that there is a tenant in the progress of
     *         creation.
     */
    public Optional<String> getTenantInCreation() {
        return Optional.ofNullable(createInitialTenant.get());
    }

    /**
     * Overwrite the tenant used by the key generator in case that the tenant is in
     * the process of creation.
     *
     * @param tenant the tenant which should be used instead of the actual one.
     */
    public void setTenantInCreation(@NotNull String tenant) {
        createInitialTenant.set(Objects.requireNonNull(tenant));
    }

    /**
     * Removes the tenant overwriting the standard one used by the
     * {@linkplain #currentTenantKeyGenerator()}.
     */
    public void removeTenantInCreation() {
        createInitialTenant.remove();
    }

    /**
     * An implementation of the {@link KeyGenerator} to generate a key based on
     * either the {@code createInitialTenant} thread local and the
     * {@link TenantAware}, but in case we are in a tenant creation with its default
     * types we need to use as the tenant the current tenant which is currently
     * created and not the one currently in the {@link TenantAware}.
     */
    public class CurrentTenantKeyGenerator implements KeyGenerator {

        @Override
        public Object generate(@NonNull final Object target, @NonNull final Method method, @NonNull final Object... params) {
            final String tenant = Objects.requireNonNull(
                            getTenantInCreation().orElseGet(TenantAware::getCurrentTenant),
                            "CurrentTenantKeyGenerator.generate called not in tenant context")
                    .toUpperCase();
            return SimpleKeyGenerator.generateKey(tenant, tenant);
        }
    }
}