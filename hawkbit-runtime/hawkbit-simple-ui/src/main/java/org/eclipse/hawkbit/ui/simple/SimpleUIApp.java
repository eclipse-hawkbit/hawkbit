/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.simple;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import feign.Client;
import feign.Contract;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import org.eclipse.hawkbit.sdk.HawkbitClient;
import org.eclipse.hawkbit.sdk.HawkbitServer;
import org.eclipse.hawkbit.sdk.Tenant;
import org.eclipse.hawkbit.ui.simple.view.util.Utils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static feign.Util.ISO_8859_1;

@Theme(themeClass = Lumo.class)
@PWA(name="hawkBit UI", shortName="hawkBit UI")
@SpringBootApplication
@Import(FeignClientsConfiguration.class)
public class SimpleUIApp implements AppShellConfigurator {

    private static final RequestInterceptor AUTHORIZATION = requestTemplate -> {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        requestTemplate.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                (Objects.requireNonNull(authentication.getPrincipal(), "User is null!") + ":" + Objects.requireNonNull(
                        authentication.getCredentials(), "Password is not available!")).getBytes(ISO_8859_1)));
    };

    private static final ErrorDecoder DEFAULT_ERROR_DECODER = new ErrorDecoder.Default();
    private static final ErrorDecoder ERROR_DECODER = (methodKey, response) -> {
        final Exception e = DEFAULT_ERROR_DECODER.decode(methodKey, response);
        Utils.errorNotification(e);
        return e;
    };

    public static void main(String[] args) {
        SpringApplication.run(SimpleUIApp.class, args);
    }

    @Bean
    HawkbitClient hawkbitClient(
            final HawkbitServer hawkBitServer,
            final Client client, final Encoder encoder, final Decoder decoder, final Contract contract) {
        return new HawkbitClient(
                hawkBitServer, client, encoder, decoder, contract,
                ERROR_DECODER,
                (tenant, controller) ->
                    controller == null ?
                            AUTHORIZATION : HawkbitClient.DEFAULT_REQUEST_INTERCEPTOR_FN.apply(tenant, controller));
    }

    @Bean
    HawkbitMgmtClient hawkbitMgmtClient(final Tenant tenant, final HawkbitClient hawkbitClient) {
        return new HawkbitMgmtClient(tenant, hawkbitClient);
    }

    // accepts all user / pass, just delegating them to the feign client
    @Bean
    AuthenticationManager authenticationManager(final HawkbitMgmtClient hawkbitClient) {
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
                    if (hawkbitClient.hasConfigRead()) {
                        roles.add("CONFIG_READ");
                    }
                } finally {
                    SecurityContextHolder.setContext(currentContext);
                }
                return new UsernamePasswordAuthenticationToken(
                        username, password,
                        roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList()) {
                    @Override
                    public void eraseCredentials() {
                        // don't erase credentials because they will be used
                        // to authenticate to the hawkBit update server / mgmt server
                    }
                };
            };
    }
}
