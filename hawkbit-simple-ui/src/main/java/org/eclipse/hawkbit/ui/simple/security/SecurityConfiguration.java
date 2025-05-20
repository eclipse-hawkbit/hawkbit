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

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.eclipse.hawkbit.ui.simple.view.LoginView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@EnableWebSecurity
@Configuration
@EnableConfigurationProperties({ OidcClientProperties.class })
public class SecurityConfiguration extends VaadinWebSecurity {

    private Customizer<OAuth2LoginConfigurer<HttpSecurity>> oAuth2LoginConfigurerCustomizer;

    @Autowired(required = false)
    public void setOAuth2LoginConfigurerCustomizer(
            @Qualifier("hawkbitOAuth2ClientCustomizer") final Customizer<OAuth2LoginConfigurer<HttpSecurity>> oauth2LoginConfigurerCustomizer
    ) {
        this.oAuth2LoginConfigurerCustomizer = oauth2LoginConfigurerCustomizer;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(
                authorize -> authorize.requestMatchers(new AntPathRequestMatcher("/images/*.png")).permitAll());

        super.configure(http);

        if (oAuth2LoginConfigurerCustomizer != null) {
            http.oauth2Login(oAuth2LoginConfigurerCustomizer);
        } else {
            setLoginView(http, LoginView.class);
        }
    }
}
