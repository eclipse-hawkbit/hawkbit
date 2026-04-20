/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui;

import static feign.Util.ISO_8859_1;

import java.io.Serial;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.Collections;
import java.util.Objects;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.sdk.HawkbitClient;
import org.eclipse.hawkbit.sdk.HawkbitServer;
import org.eclipse.hawkbit.sdk.Tenant;
import org.eclipse.hawkbit.ui.view.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

@Slf4j
@Theme("hawkbit")
@PWA(name = "hawkBit UI", shortName = "hawkBit UI")
@EnableCaching
@EnableScheduling
@SpringBootApplication
@Import(FeignClientsConfiguration.class)
public class HawkbitUiApp implements AppShellConfigurator {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static RequestInterceptor authorizationInterceptor(final OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager) {
        return requestTemplate -> {
            final Authentication authentication = Objects.requireNonNull(
                    SecurityContextHolder.getContext().getAuthentication(), "No authentication available in security context!");
            if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
                // line from /org/springframework/security/oauth2/client/web/client/OAuth2ClientHttpRequestInterceptor.java#authorizeClient
                OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(oauth2Token
                        .getAuthorizedClientRegistrationId()).principal(authentication).build();
                OAuth2AuthorizedClient authorizedClient = oAuth2AuthorizedClientManager.authorize(authorizeRequest);
                if (authorizedClient != null) {
                    requestTemplate.header(AUTHORIZATION_HEADER, "Bearer " + authorizedClient.getAccessToken().getTokenValue());
                } else {
                    log.warn("No authorized client found for principal {} — request will be sent without Authorization header", oauth2Token
                            .getName());
                }
            } else {
                final Object principal = Objects.requireNonNull(authentication.getPrincipal(), "User is null!");
                final String user = String.valueOf(principal);
                final Object pass = Objects.requireNonNull(authentication.getCredentials(), "Password is not available!");
                requestTemplate.header(
                        AUTHORIZATION_HEADER,
                        "Basic " + Base64.getEncoder().encodeToString((user + ":" + pass).getBytes(ISO_8859_1)));
            }
        };
    }

    private static final ErrorDecoder DEFAULT_ERROR_DECODER = new ErrorDecoder.Default();
    private static final ErrorDecoder ERROR_DECODER = (methodKey, response) -> {
        final Exception e = DEFAULT_ERROR_DECODER.decode(methodKey, response);
        Utils.errorNotification(e);
        return e;
    };

    public static void main(String[] args) {
        SpringApplication.run(HawkbitUiApp.class, args);
    }

    @Bean
    HawkbitClient hawkbitClient(final HawkbitServer hawkBitServer,
            @Autowired(required = false) final OAuth2AuthorizedClientManager authorizedClientManager) {
        final RequestInterceptor authorization = authorizationInterceptor(authorizedClientManager);
        return new HawkbitClient(
                hawkBitServer, null, null, null,
                ERROR_DECODER,
                (tenant, controller) -> controller == null
                        ? authorization
                        : HawkbitClient.DEFAULT_REQUEST_INTERCEPTOR_FN.apply(tenant, controller));
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
                    // don't erase credentials because they will be used to authenticate to the hawkBit update server / mgmt server
                }
            };
        };
    }

    public static boolean isAuthenticated(String username, String password, String mgmtUrl) {
        try {
            final URL url = new URI(mgmtUrl + "/rest/v1/rollouts").toURL();
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            final String authentication = username + ":" + password;
            final String encodedAuth = Base64.getEncoder().encodeToString(authentication.getBytes());
            conn.setRequestProperty(AUTHORIZATION_HEADER, "Basic " + encodedAuth);

            return conn.getResponseCode() != 401;
        } catch (final Exception ex) {
            log.error("Failed to authenticate user {} .Reason : ", username, ex);
            return false;
        }
    }
}