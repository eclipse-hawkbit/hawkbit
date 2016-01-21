/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto configuration for the {@link ArtifactStore}.
 * 
 *
 *
 */
@Configuration
@ComponentScan
@ConditionalOnMissingBean(value = ArtifactRepository.class)
@Import(value = MongoConfiguration.class)
public class ArtifactStoreAutoConfiguration {

    /**
     * @return Default {@link ArtifactRepository} implementation.
     */
    @Bean
    public ArtifactRepository artifactRepository() {
        return new ArtifactStore();
    }
}
