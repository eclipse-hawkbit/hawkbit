/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.artifact;

import org.eclipse.hawkbit.artifact.urlresolver.ArtifactUrlResolver;
import org.eclipse.hawkbit.artifact.urlresolver.PropertyBasedArtifactUrlResolver;
import org.eclipse.hawkbit.artifact.urlresolver.PropertyBasedArtifactUrlResolverProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Autoconfiguration for {@link ArtifactUrlResolver} based on a properties.
 */
@Configuration
@EnableConfigurationProperties({ PropertyBasedArtifactUrlResolverProperties.class })
@PropertySource("classpath:/hawkbit-artifactdl-defaults.properties")
public class PropertyBasedArtifactUrlResolverAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ArtifactUrlResolver.class)
    PropertyBasedArtifactUrlResolver propertyBasedArtifactUrlHandler(
            final PropertyBasedArtifactUrlResolverProperties urlHandlerProperties,
            @Value("${server.servlet.context-path:}") final String contextPath) {
        return new PropertyBasedArtifactUrlResolver(urlHandlerProperties, contextPath);
    }
}