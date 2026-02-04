/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mcp.server.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.mcp.server.client.AuthenticationValidator;
import org.eclipse.hawkbit.mcp.server.client.AuthenticationValidator.ValidationResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Security configuration for the MCP server.
 * <p>
 * This configuration is only active in HTTP/servlet mode. In STDIO mode,
 * authentication is handled via static credentials from properties.
 * </p>
 * <p>
 * When authentication validation is enabled ({@code hawkbit.mcp.validation.enabled=true}),
 * a filter validates credentials against hawkBit before forwarding requests.
 * When disabled, no validation filter is added and requests pass through directly.
 * </p>
 */
@Slf4j
@Configuration
@EnableWebSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "spring.ai.mcp.server.stdio", havingValue = "false", matchIfMissing = true)
public class McpSecurityConfiguration {

    private final Optional<AuthenticationValidator> authenticationValidator;

    public McpSecurityConfiguration(final Optional<AuthenticationValidator> authenticationValidator) {
        this.authenticationValidator = authenticationValidator;
        if (authenticationValidator.isEmpty()) {
            log.info("Authentication validation disabled - requests will be forwarded without validation");
        }
    }

    @Bean
    @SuppressWarnings("java:S4502") // CSRF protection is not needed for stateless REST APIs using Authorization header
    public SecurityFilterChain mcpSecurityFilterChain(final HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        authenticationValidator.ifPresent(validator -> {
            log.info("Authentication validation enabled - adding validation filter");
            http.addFilterBefore(new HawkBitAuthenticationFilter(validator),
                    UsernamePasswordAuthenticationFilter.class);
        });

        return http.build();
    }

    /**
     * Filter that validates authentication against hawkBit.
     * <p>
     * Only added to the filter chain when authentication validation is enabled.
     * </p>
     */
    @Slf4j
    @RequiredArgsConstructor
    public static class HawkBitAuthenticationFilter extends OncePerRequestFilter {

        private final AuthenticationValidator validator;

        @Override
        protected void doFilterInternal(final HttpServletRequest request, final @NonNull HttpServletResponse response,
                                        final @NonNull FilterChain filterChain) throws ServletException, IOException {
            final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            final ValidationResult result = validator.validate(authHeader);

            switch (result) {
                case VALID -> filterChain.doFilter(request, response);
                case MISSING_CREDENTIALS -> {
                    log.debug("Rejecting request: missing credentials");
                    sendErrorResponse(response, HttpStatus.UNAUTHORIZED,
                            "Authentication required. Please provide hawkBit credentials.");
                }
                case INVALID_CREDENTIALS -> {
                    log.debug("Rejecting request: invalid credentials");
                    sendErrorResponse(response, HttpStatus.UNAUTHORIZED,
                            "Invalid hawkBit credentials.");
                }
                case HAWKBIT_ERROR -> {
                    log.warn("Rejecting request: hawkBit unavailable");
                    sendErrorResponse(response, HttpStatus.SERVICE_UNAVAILABLE,
                            "Unable to validate credentials. hawkBit may be unavailable.");
                }
            }
        }

        private void sendErrorResponse(final HttpServletResponse response, final HttpStatus status, final String message)
                throws IOException {
            response.setStatus(status.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(String.format(
                    "{\"error\":\"%s\",\"message\":\"%s\"}",
                    status.getReasonPhrase(),
                    message));
        }
    }
}
