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

import org.eclipse.hawkbit.repository.jpa.management.JpaSystemManagement;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * Defines the interfaces to register the {@link KeyGenerator} as bean which is
 * used by spring caching framework to resolve the key-generator. The
 * key-generator must registered as bean so spring can resolve the key-generator
 * by its name.
 *
 * When using the {@link Service} annotation e.g. by {@link JpaSystemManagement}
 * the bean registration must be declared by the interface due spring registers
 * the bean by the implemented interfaces. So introduce a single interface for
 * the {@link JpaSystemManagement} implementation to allow it to register the
 * key-generator bean.
 */
@FunctionalInterface
public interface CurrentTenantCacheKeyGenerator {

    /**
     * Bean declaration to register a {@code currentTenantKeyGenerator} bean
     * which is used by the caching framework.
     *
     * @return the {@link KeyGenerator} to be used to cache the values of the
     *         current used tenant in the {@link JpaSystemManagement}
     */
    @Bean
    KeyGenerator currentTenantKeyGenerator();
}
