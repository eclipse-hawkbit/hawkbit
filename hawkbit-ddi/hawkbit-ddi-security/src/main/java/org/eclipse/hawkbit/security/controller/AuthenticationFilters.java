/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.security.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.security.DdiSecurityProperties;
import org.eclipse.hawkbit.util.UrlUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * An abstraction for all controller based security to parse the e.g. the tenant name from the URL and the controller ID from the URL to do
 * security checks based on this information.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthenticationFilters {

    public static class GatewayTokenAuthenticationFilter extends AbstractAuthenticationFilter {

        public GatewayTokenAuthenticationFilter(final GatewayTokenAuthenticator authenticator, final DdiSecurityProperties ddiSecurityProperties) {
            super(authenticator, ddiSecurityProperties);
        }
    }

    public static class SecurityHeaderAuthenticationFilter extends AbstractAuthenticationFilter {

        public SecurityHeaderAuthenticationFilter(final SecurityHeaderAuthenticator authenticator, final DdiSecurityProperties ddiSecurityProperties) {
            super(authenticator, ddiSecurityProperties);
        }
    }

    public static class SecurityTokenAuthenticationFilter extends AbstractAuthenticationFilter {

        public SecurityTokenAuthenticationFilter(final SecurityTokenAuthenticator authenticator, final DdiSecurityProperties ddiSecurityProperties) {
            super(authenticator, ddiSecurityProperties);
        }
    }

    /**
     * An abstraction for all controller based security to parse the e.g. the tenant name from the URL and the controller ID from the URL to do
     * security checks based on this information.
     */
    public static abstract class AbstractAuthenticationFilter extends OncePerRequestFilter {

        private static final String TENANT_PLACE_HOLDER = "tenant";
        private static final String CONTROLLER_ID_PLACE_HOLDER = "controllerId";
        /**
         * requestURIPathPattern the request URI path pattern in ANT style containing the placeholder key for retrieving the principal from the URI
         * request. e.g."/{tenant}/controller/v1/{controllerId}
         */
        private static final String CONTROLLER_REQUEST_ANT_PATTERN =
                "/{" + TENANT_PLACE_HOLDER + "}/controller/v1/{" + CONTROLLER_ID_PLACE_HOLDER + "}/**";
        private static final String CONTROLLER_DL_REQUEST_ANT_PATTERN =
                "/{" + TENANT_PLACE_HOLDER + "}/controller/artifacts/v1/**";

        private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
        private final AntPathMatcher pathExtractor = new AntPathMatcher();
        private final Authenticator authenticator;
        private final List<String> authorizedSourceIps;

        protected AbstractAuthenticationFilter(final Authenticator authenticator, final DdiSecurityProperties ddiSecurityProperties) {
            this.authenticator = authenticator;
            authorizedSourceIps = ddiSecurityProperties.getRp().getTrustedIPs();
        }

        @Override
        protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
                throws IOException, ServletException {
            if (acceptIPAddress(request)) {
                final Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();
                if (currentAuthentication == null || !currentAuthentication.isAuthenticated()) {
                    final ControllerSecurityToken securityToken = createTenantSecurityTokenVariables(request);
                    if (securityToken != null) {
                        final Authentication authentication = authenticator.authenticate(securityToken);
                        if (authentication != null) {
                            SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();
                            context.setAuthentication(authentication);
                            this.securityContextHolderStrategy.setContext(context);
                        }
                    }
                } else {
                    authenticator.log().trace("Request is already authenticated. Skip filter");
                }
            }

            chain.doFilter(request, response);
        }

        /**
         * Extracts tenant and controllerId from the request URI as path variables.
         *
         * @param request the Http request to extract the path variables.
         * @return the extracted {@link ControllerSecurityToken} or {@code null} if the request does not match the pattern and no variables could be
         *         extracted
         */
        private ControllerSecurityToken createTenantSecurityTokenVariables(final HttpServletRequest request) {
            final String requestURI = request.getRequestURI();
            if (pathExtractor.match(request.getContextPath() + CONTROLLER_REQUEST_ANT_PATTERN, requestURI)) {
                authenticator.log().debug("retrieving principal from URI request {}", requestURI);
                final Map<String, String> extractUriTemplateVariables = pathExtractor
                        .extractUriTemplateVariables(request.getContextPath() + CONTROLLER_REQUEST_ANT_PATTERN, requestURI);
                final String controllerId = UrlUtils.decodeUriValue(extractUriTemplateVariables.get(CONTROLLER_ID_PLACE_HOLDER));
                final String tenant = UrlUtils.decodeUriValue(extractUriTemplateVariables.get(TENANT_PLACE_HOLDER));
                authenticator.log().trace("Parsed tenant {} and controllerId {} from path request {}", tenant, controllerId, requestURI);
                return createTenantSecurityTokenVariables(request, tenant, controllerId);
            } else if (pathExtractor.match(request.getContextPath() + CONTROLLER_DL_REQUEST_ANT_PATTERN, requestURI)) {
                authenticator.log().debug("retrieving path variables from URI request {}", requestURI);
                final Map<String, String> extractUriTemplateVariables = pathExtractor.extractUriTemplateVariables(
                        request.getContextPath() + CONTROLLER_DL_REQUEST_ANT_PATTERN, requestURI);
                final String tenant = UrlUtils.decodeUriValue(extractUriTemplateVariables.get(TENANT_PLACE_HOLDER));
                authenticator.log().trace("Parsed tenant {} from path request {}", tenant, requestURI);
                return createTenantSecurityTokenVariables(request, tenant, "anonymous");
            } else {
                authenticator.log().trace("request {} does not match the path pattern {}, request gets ignored", requestURI, CONTROLLER_REQUEST_ANT_PATTERN);
                return null;
            }
        }

        private ControllerSecurityToken createTenantSecurityTokenVariables(
                final HttpServletRequest request, final String tenant, final String controllerId) {
            final ControllerSecurityToken securityToken = new ControllerSecurityToken(tenant, null, controllerId, null);
            Collections.list(request.getHeaderNames()).forEach(header -> securityToken.putHeader(header, request.getHeader(header)));
            return securityToken;
        }

        private boolean acceptIPAddress(final HttpServletRequest request) {
            if (authorizedSourceIps == null) {
                // no trusted IP check, because no authorizedSourceIPs configuration
                return true;
            }

            final String remoteAddress = request.getRemoteAddr();
            if (authorizedSourceIps.contains(remoteAddress)) {
                // source ip matches the given pattern -> authenticated
                return true;
            } else {
                authenticator.log().debug(
                        "The remote source IP address {} is not in the list of trusted IP addresses {}", remoteAddress, authorizedSourceIps);
                return false;
            }
        }
    }
}