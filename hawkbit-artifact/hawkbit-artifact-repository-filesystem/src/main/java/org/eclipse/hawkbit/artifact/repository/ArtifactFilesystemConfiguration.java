/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.artifact.repository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the {@link ArtifactFilesystemRepository}.
 */
@Configuration
@EnableConfigurationProperties(ArtifactFilesystemProperties.class)
public class ArtifactFilesystemConfiguration {

    /**
     * @param artifactFilesystemProperties the artifact file system properties
     * @return Default {@link ArtifactRepository} implementation.
     */
    @Bean
    @ConditionalOnMissingBean
    public ArtifactRepository artifactRepository(final ArtifactFilesystemProperties artifactFilesystemProperties) {
        return new ArtifactFilesystemRepository(artifactFilesystemProperties);
    }
}
