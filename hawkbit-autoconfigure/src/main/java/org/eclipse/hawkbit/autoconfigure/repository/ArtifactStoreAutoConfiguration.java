/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.repository;

import org.eclipse.hawkbit.artifact.repository.ArtifactFilesystemProperties;
import org.eclipse.hawkbit.artifact.repository.ArtifactFilesystemRepository;
import org.eclipse.hawkbit.artifact.repository.ArtifactRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto configuration for the {@link ArtifactFilesystemRepository}.
 */
@Configuration
@ConditionalOnMissingBean(ArtifactRepository.class)
@EnableConfigurationProperties(ArtifactFilesystemProperties.class)
public class ArtifactStoreAutoConfiguration {

    /**
     * @param artifactFilesystemProperties
     *            the artifact file system properties
     * @return Default {@link ArtifactRepository} implementation.
     */
    @Bean
    public ArtifactRepository artifactRepository(final ArtifactFilesystemProperties artifactFilesystemProperties) {
        return new ArtifactFilesystemRepository(artifactFilesystemProperties);
    }
}
