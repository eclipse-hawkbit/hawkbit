/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.url;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.hawkbit.HawkbitServerProperties;
import org.eclipse.hawkbit.api.HostnameResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Throwables;

/**
 * Autoconfiguration of the {@link HostnameResolver} based on a property.
 * 
 *
 *
 */
@Configuration
@EnableConfigurationProperties(HawkbitServerProperties.class)
public class PropertyHostnameResolverAutoConfiguration {

    @Autowired
    private HawkbitServerProperties serverProperties;

    /**
     * @return the default autoconfigure hostname resolver implementation which
     *         is property based specified by the property {@link #url}
     */
    @Bean
    @ConditionalOnMissingBean(value = HostnameResolver.class)
    public HostnameResolver hostnameResolver() {
        return () -> {
            try {
                return new URL(serverProperties.getUrl());
            } catch (final MalformedURLException e) {
                throw Throwables.propagate(e);
            }
        };
    }

}
