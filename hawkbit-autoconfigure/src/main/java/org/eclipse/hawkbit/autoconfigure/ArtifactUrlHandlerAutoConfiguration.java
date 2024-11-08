/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure;

import org.eclipse.hawkbit.artifact.repository.urlhandler.ArtifactUrlHandler;
import org.eclipse.hawkbit.artifact.repository.urlhandler.ArtifactUrlHandlerProperties;
import org.eclipse.hawkbit.artifact.repository.urlhandler.PropertyBasedArtifactUrlHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Auto-configuration for {@link ArtifactUrlHandler} based on a properties.
 */
@Configuration
@EnableConfigurationProperties({ ArtifactUrlHandlerProperties.class })
@PropertySource("classpath:/hawkbit-artifactdl-defaults.properties")
public class ArtifactUrlHandlerAutoConfiguration {

    /**
     * @param urlHandlerProperties for bean configuration
     * @return PropertyBasedArtifactUrlHandler bean
     */
    @Bean
    @ConditionalOnMissingBean(ArtifactUrlHandler.class)
    PropertyBasedArtifactUrlHandler propertyBasedArtifactUrlHandler(
            final ArtifactUrlHandlerProperties urlHandlerProperties,
            @Value("${server.servlet.context-path:}") final String contextPath) {
        return new PropertyBasedArtifactUrlHandler(urlHandlerProperties, contextPath);
    }
}