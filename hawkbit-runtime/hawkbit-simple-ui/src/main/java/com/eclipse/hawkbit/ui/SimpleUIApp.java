/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.eclipse.hawkbit.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import feign.Client;
import feign.Contract;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.LinkedList;
import java.util.List;

@Theme(themeClass = Lumo.class)
@PWA(name="hawkBit UI", shortName="hawkBit UI")
@SpringBootApplication
@Import(FeignClientsConfiguration.class)
public class SimpleUIApp implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(SimpleUIApp.class, args);
    }

    @Bean
    HawkbitClient hawkbitClient(
            @Value("${hawkbit.url:http://localhost:8080}")
            final String hawkbitUrl,
            final Client client, final Encoder encoder, final Decoder decoder, final Contract contract) {
        return new HawkbitClient(hawkbitUrl, client, encoder, decoder, contract);
    }

    // accepts all user / pass, just delegating them to the feign client
    @Bean
    AuthenticationManager authenticationManager(final HawkbitClient hawkbitClient) {
        return authentication-> {
                final String username = authentication.getName();
                final String password = authentication.getCredentials().toString();

                final List<String> roles = new LinkedList<>();
                roles.add("ANONYMOUS");
                final SecurityContext unauthorizedContext = SecurityContextHolder.createEmptyContext();
                unauthorizedContext.setAuthentication(
                        new UsernamePasswordAuthenticationToken(username, password));
                final SecurityContext currentContext = SecurityContextHolder.getContext();
                try {
                    SecurityContextHolder.setContext(unauthorizedContext);
                    if (hawkbitClient.hasSoftwareModulesRead()) {
                        roles.add("SOFTWARE_MODULE_READ");
                    }
                    if (hawkbitClient.hasRolloutRead()) {
                        roles.add("ROLLOUT_READ");
                    }
                    if (hawkbitClient.hasDistributionSetRead()) {
                        roles.add("DISTRIBUTION_SET_READ");
                    }
                    if (hawkbitClient.hasTargetRead()) {
                        roles.add("TARGET_READ");
                    }
                } finally {
                    SecurityContextHolder.setContext(currentContext);
                }
                return new UsernamePasswordAuthenticationToken(
                        username, password,
                        roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList()) {
                    public void eraseCredentials() {}
                };
            };
    }
}
