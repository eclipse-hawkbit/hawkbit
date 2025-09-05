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
import org.eclipse.hawkbit.sdk.HawkbitClient;
import org.eclipse.hawkbit.sdk.HawkbitServer;
import org.eclipse.hawkbit.sdk.Tenant;
import org.eclipse.hawkbit.ui.simple.security.OAuth2TokenManager;
import org.eclipse.hawkbit.ui.simple.view.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static feign.Util.ISO_8859_1;
import static java.util.Collections.emptyList;

@Theme("hawkbit")
@PWA(name = "hawkBit UI", shortName = "hawkBit UI")
@SpringBootApplication
@Import(FeignClientsConfiguration.class)
public class SimpleUIApp implements AppShellConfigurator {

    private static final Function<OAuth2TokenManager, RequestInterceptor> AUTHORIZATION = oAuth2TokenManager -> requestTemplate -> {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OAuth2AuthenticationToken authenticationToken) {
            String bearerToken = oAuth2TokenManager.getToken(authenticationToken);
            requestTemplate.header("Authorization", "Bearer " + bearerToken);
        } else {
            requestTemplate.header(
                    "Authorization", "Basic " + Base64.getEncoder().encodeToString(
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
            final Contract contract,
            @Autowired(required = false)
            final OAuth2TokenManager oAuth2TokenManager
    ) {
        return new HawkbitClient(
                hawkBitServer, encoder, decoder, contract,
                ERROR_DECODER,
                (tenant, controller) ->
                        controller == null
                                ? AUTHORIZATION.apply(oAuth2TokenManager)
                                : HawkbitClient.DEFAULT_REQUEST_INTERCEPTOR_FN.apply(tenant, controller)
        );
    }

    @Bean
    HawkbitMgmtClient hawkbitMgmtClient(final Tenant tenant, final HawkbitClient hawkbitClient) {
        return new HawkbitMgmtClient(tenant, hawkbitClient);
    }

    @Bean
    OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService(final HawkbitMgmtClient hawkbitClient) {
        final OidcUserService delegate = new OidcUserService();
        return (userRequest) -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);
            final OAuth2AuthenticationToken tempToken = new OAuth2AuthenticationToken(
                    oidcUser,
                    emptyList(),
                    userRequest.getClientRegistration().getRegistrationId()
            );
            final List<SimpleGrantedAuthority> grantedAuthorities = getGrantedAuthorities(hawkbitClient, tempToken);
            return new DefaultOidcUser(
                    grantedAuthorities,
                    oidcUser.getIdToken(),
                    oidcUser.getUserInfo()
            );
        };
    }

    // accepts all user / pass, just delegating them to the feign client
    @Bean
    AuthenticationManager authenticationManager(final HawkbitMgmtClient hawkbitClient) {
        return authentication -> {
            final String username = authentication.getName();
            final String password = authentication.getCredentials().toString();

            final List<SimpleGrantedAuthority> grantedAuthorities = getGrantedAuthorities(
                    hawkbitClient, new UsernamePasswordAuthenticationToken(username, password));
            return new UsernamePasswordAuthenticationToken(username, password, grantedAuthorities) {

                @Override
                public void eraseCredentials() {
                    // don't erase credentials because they will be used
                    // to authenticate to the hawkBit update server / mgmt server
                }
            };
        };
    }

    private List<SimpleGrantedAuthority> getGrantedAuthorities(final HawkbitMgmtClient hawkbitClient, Authentication authentication) {
        final List<String> roles = new LinkedList<>();
        roles.add("ANONYMOUS");
        final SecurityContext unauthorizedContext = SecurityContextHolder.createEmptyContext();
        unauthorizedContext.setAuthentication(authentication);
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
        return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
    }
}
