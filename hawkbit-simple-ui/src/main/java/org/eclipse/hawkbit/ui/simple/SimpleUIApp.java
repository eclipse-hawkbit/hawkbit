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
import feign.Contract;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.sdk.HawkbitClient;
import org.eclipse.hawkbit.sdk.HawkbitServer;
import org.eclipse.hawkbit.sdk.Tenant;
import org.eclipse.hawkbit.ui.simple.view.util.Utils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Collections;
import java.util.Objects;

import static feign.Util.ISO_8859_1;

@Slf4j
@Theme("hawkbit")
@PWA(name = "hawkBit UI", shortName = "hawkBit UI")
@EnableCaching
@EnableScheduling
@SpringBootApplication
@Import(FeignClientsConfiguration.class)
public class SimpleUIApp implements AppShellConfigurator {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final RequestInterceptor AUTHORIZATION = requestTemplate -> {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            requestTemplate.header(AUTHORIZATION_HEADER, "Bearer " + oidcUser.getIdToken().getTokenValue());
        } else {
            requestTemplate.header(
                    AUTHORIZATION_HEADER, "Basic " + Base64.getEncoder().encodeToString(
                            (Objects.requireNonNull(authentication.getPrincipal(), "User is null!") + ":" + Objects.requireNonNull(
                                    authentication.getCredentials(), "Password is not available!")).getBytes(ISO_8859_1))
            );
        }
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
            final Encoder encoder,
            final Decoder decoder,
            final Contract contract
    ) {
        return new HawkbitClient(
                hawkBitServer, encoder, decoder, contract,
                ERROR_DECODER,
                (tenant, controller) -> controller == null
                        ? AUTHORIZATION
                        : HawkbitClient.DEFAULT_REQUEST_INTERCEPTOR_FN.apply(tenant, controller)
        );
    }

    @Bean
    HawkbitMgmtClient hawkbitMgmtClient(final Tenant tenant, final HawkbitClient hawkbitClient) {
        return new HawkbitMgmtClient(tenant, hawkbitClient);
    }

    // accepts all user / pass, just delegating them to the feign client
    @Bean
    AuthenticationManager authenticationManager(final HawkbitMgmtClient hawkbitClient, final HawkbitServer server) {
        return authentication -> {
            final String username = authentication.getName();
            final String password = authentication.getCredentials().toString();

            // make simple check in order not to be logged in as not real user.
            if (!isAuthenticated(username, password, server.getMgmtUrl())) {
                throw new BadCredentialsException("Incorrect username or password!");
            }

            return new UsernamePasswordAuthenticationToken(username, password, Collections.emptyList()) {

                @Override
                public void eraseCredentials() {
                    // don't erase credentials because they will be used
                    // to authenticate to the hawkBit update server / mgmt server
                }
            };
        };
    }

    public static boolean isAuthenticated(String username, String password, String mgmtUrl) {
        try {
            final URL url = new URL(mgmtUrl + "/rest/v1/rollouts");
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            final String auth = username + ":" + password;
            final String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            conn.setRequestProperty(AUTHORIZATION_HEADER, "Basic " + encodedAuth);

            return conn.getResponseCode() != 401;
        } catch (final Exception ex) {
            log.error("Failed to authenticate user {} .Reason : ", username, ex);
            return false;
        }
    }
}