/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.security;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;

@Configuration
@ConditionalOnProperty(prefix = "hawkbit.server.security.oauth2.client", name = "enabled")
public class Oauth2ClientConfig {

    @Bean(name = "hawkbitOAuth2ClientCustomizer")
    @ConditionalOnMissingBean(name = "hawkbitOAuth2ClientCustomizer")
    Customizer<OAuth2LoginConfigurer<HttpSecurity>> defaultOAuth2ClientCustomizer() {
        return Customizer.withDefaults();
    }

    @Bean
    @ConditionalOnMissingBean
    public OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository repo,
            OidcClientProperties properties) {

        final Map<String, String> customHeaders = properties.getOauth2().getClient().getCustomHeaders();
        final DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(repo,
                OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
        resolver.setAuthorizationRequestCustomizer(
                customizer -> customizer.additionalParameters(params -> params.putAll(customHeaders)));
        return resolver;
    }
}
