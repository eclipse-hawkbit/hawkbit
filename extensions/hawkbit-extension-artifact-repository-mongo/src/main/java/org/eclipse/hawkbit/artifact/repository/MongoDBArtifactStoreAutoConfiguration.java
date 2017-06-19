/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.gridfs.GridFsOperations;

/**
 * Auto configuration for the {@link MongoDBArtifactStore}.
 */
@Configuration
@ConditionalOnProperty(prefix = "org.eclipse.hawkbit.artifact.repository.mongo", name = "enabled", matchIfMissing = true)
@PropertySource("classpath:/hawkbit-mongodb-defaults.properties")
public class MongoDBArtifactStoreAutoConfiguration {

    /**
     * @return Default {@link ArtifactRepository} implementation.
     */
    @Bean
    ArtifactRepository artifactRepository(final GridFsOperations gridFs) {
        return new MongoDBArtifactStore(gridFs);
    }
}
