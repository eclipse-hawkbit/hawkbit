/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.repository;

import org.eclipse.hawkbit.repository.jpa.RepositoryApplicationConfiguration;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;

/**
 * Auto-Configuration for enabling JPA repository.
 */
@Configuration
@ConditionalOnClass({ RepositoryApplicationConfiguration.class })
@Import({ RepositoryApplicationConfiguration.class })
public class JpaRepositoryAutoConfiguration {

    /**
     * @return returns a VirtualPropertyReplacer
     */
    @Bean
    @ConditionalOnMissingBean
    public VirtualPropertyReplacer virtualPropertyReplacer() {
        return new VirtualPropertyResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public LockRegistry lockRegistry() {
        return new DefaultLockRegistry();
    }
}