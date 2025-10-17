/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.simple.security;

import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategyConfiguration;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.ui.simple.view.LoginView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@EnableWebSecurity
@Configuration
@EnableConfigurationProperties(OidcClientProperties.class)
@Slf4j
@Import(VaadinAwareSecurityContextHolderStrategyConfiguration.class)
public class SecurityConfiguration {

    private Customizer<OAuth2LoginConfigurer<HttpSecurity>> oAuth2LoginConfigurerCustomizer;
    @Autowired
    private UserDetailsSetter userDetailsSetter;

    @Autowired(required = false)
    private InMemoryClientRegistrationRepository clientRegistrationRepository;

    @Autowired(required = false)
    public void setOAuth2LoginConfigurerCustomizer(
            @Qualifier("hawkbitOAuth2ClientCustomizer") final Customizer<OAuth2LoginConfigurer<HttpSecurity>> oauth2LoginConfigurerCustomizer) {
        this.oAuth2LoginConfigurerCustomizer = oauth2LoginConfigurerCustomizer;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationFailureHandler customFailureHandler() {
        // Redirect back to login with your message
        return (request, response, exception) -> response.sendRedirect("/login?error=" + URLEncoder.encode(exception.getMessage(),
                StandardCharsets.UTF_8));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize.requestMatchers("/images/*.png").permitAll());
        http.addFilterAfter(userDetailsSetter, AuthorizationFilter.class);
        return http.with(VaadinSecurityConfigurer.vaadin(), configurer -> {
            if (oAuth2LoginConfigurerCustomizer == null) {
                configurer.loginView(LoginView.class);

            } else {
                var defaultClientRegistration = clientRegistrationRepository.iterator().next();
                configurer.oauth2LoginPage(
                        "/oauth2/authorization/" + defaultClientRegistration.getRegistrationId(),
                        "{baseUrl}/"
                );
            }
        }).build();
    }
}
