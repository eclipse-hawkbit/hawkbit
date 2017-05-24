/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.hawkbit.HawkbitServerProperties;
import org.eclipse.hawkbit.api.ArtifactUrlHandler;
import org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties;
import org.eclipse.hawkbit.api.HostnameResolver;
import org.eclipse.hawkbit.api.PropertyBasedArtifactUrlHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.google.common.base.Throwables;

/**
 * Auto configuration for {@link HostnameResolver} and
 * {@link ArtifactUrlHandler} based on a properties.
 */
@Configuration
@EnableConfigurationProperties({ HawkbitServerProperties.class, ArtifactUrlHandlerProperties.class })
@PropertySource("classpath:/hawkbit-artifactdl-defaults.properties")
public class PropertyHostnameResolverAutoConfiguration {

    /**
     * @param serverProperties
     *            to get the servers URL
     * @return the default autoconfigure hostname resolver implementation which
     *         is property based specified by the property {@link #url}
     */
    @Bean
    @ConditionalOnMissingBean(value = HostnameResolver.class)
    public HostnameResolver hostnameResolver(final HawkbitServerProperties serverProperties) {
        return () -> {
            try {
                return new URL(serverProperties.getUrl());
            } catch (final MalformedURLException e) {
                throw Throwables.propagate(e);
            }
        };
    }

    /**
     * @param urlHandlerProperties
     *            for bean configuration
     * @return PropertyBasedArtifactUrlHandler bean
     */
    @Bean
    @ConditionalOnMissingBean(ArtifactUrlHandler.class)
    public PropertyBasedArtifactUrlHandler propertyBasedArtifactUrlHandler(
            final ArtifactUrlHandlerProperties urlHandlerProperties) {
        return new PropertyBasedArtifactUrlHandler(urlHandlerProperties);
    }

}
