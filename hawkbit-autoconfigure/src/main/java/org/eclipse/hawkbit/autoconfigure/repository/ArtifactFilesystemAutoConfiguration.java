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

import org.eclipse.hawkbit.artifact.repository.ArtifactFilesystemConfiguration;
import org.eclipse.hawkbit.artifact.repository.ArtifactFilesystemRepository;
import org.eclipse.hawkbit.artifact.repository.ArtifactRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto configuration for the {@link ArtifactFilesystemRepository}.
 */
@Configuration
@ConditionalOnMissingBean(ArtifactRepository.class)
@ConditionalOnClass({ ArtifactFilesystemConfiguration.class })
@Import(ArtifactFilesystemConfiguration.class)
public class ArtifactFilesystemAutoConfiguration {

}
