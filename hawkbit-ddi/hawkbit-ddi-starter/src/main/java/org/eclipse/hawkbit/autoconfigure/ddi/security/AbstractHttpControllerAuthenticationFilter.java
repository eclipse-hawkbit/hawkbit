/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.ddi.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.DmfTenantSecurityToken;
import org.eclipse.hawkbit.security.PreAuthenticationFilter;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.util.UrlUtils;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.util.AntPathMatcher;

/**
 * An abstraction for all controller based security to parse the e.g. the tenant
 * name from the URL and the controller ID from the URL to do security checks
 * based on this information.
 */
public abstract class AbstractHttpControllerAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {

    protected TenantConfigurationManagement tenantConfigurationManagement;
    protected TenantAware tenantAware;
    protected SystemSecurityContext systemSecurityContext;
    private static final String TENANT_PLACE_HOLDER = "tenant";
    private static final String CONTROLLER_ID_PLACE_HOLDER = "controllerId";
    /**
     * requestURIPathPattern the request URI path pattern in ANT style
     * containing the placeholder key for retrieving the principal from the URI
     * request. e.g."/{tenant}/controller/v1/{controllerId}
     */
    private static final String CONTROLLER_REQUEST_ANT_PATTERN = "/{" + TENANT_PLACE_HOLDER + "}/controller/v1" + "/{"
            + CONTROLLER_ID_PLACE_HOLDER + "}/**";
    private static final String CONTROLLER_DL_REQUEST_ANT_PATTERN = "/{" + TENANT_PLACE_HOLDER
            + "}/controller/artifacts/v1/**";
    private final AntPathMatcher pathExtractor;

    private PreAuthenticationFilter abstractControllerAuthenticationFilter;

    /**
     * Constructor for subclasses.
     *
     * @param tenantConfigurationManagement the tenant configuration service
     * @param tenantAware the tenant aware service
     * @param systemSecurityContext the system security context
     */
    protected AbstractHttpControllerAuthenticationFilter(
            final TenantConfigurationManagement tenantConfigurationManagement, final TenantAware tenantAware,
            final SystemSecurityContext systemSecurityContext) {
        this.tenantConfigurationManagement = tenantConfigurationManagement;
        this.tenantAware = tenantAware;
        this.systemSecurityContext = systemSecurityContext;
        pathExtractor = new AntPathMatcher();
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            log().trace("Request is already authenticated. Skip filter");
            chain.doFilter(request, response);
            return;
        }

        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }

        final DmfTenantSecurityToken securityToken = createTenantSecurityTokenVariables((HttpServletRequest) request);
        if (securityToken == null) {
            chain.doFilter(request, response);
            return;
        }

        abstractControllerAuthenticationFilter = createControllerAuthenticationFilter();
        if (abstractControllerAuthenticationFilter.isEnable(securityToken)) {
            super.doFilter(request, response, chain);
        } else {
            log().debug("Filter is disabled for the tenant {}", securityToken.getTenant());
            chain.doFilter(request, response);
        }
    }

    @Override
    protected void successfulAuthentication(final HttpServletRequest request, final HttpServletResponse response,
            final Authentication authResult) throws IOException, ServletException {
        final Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.addAll(authResult.getAuthorities());
        authorities.addAll(abstractControllerAuthenticationFilter.getSuccessfulAuthenticationAuthorities());
        final PreAuthenticatedAuthenticationToken authTokenWithGrantedAuthorities = new PreAuthenticatedAuthenticationToken(
                authResult.getPrincipal(), authResult.getCredentials(), authorities);
        authTokenWithGrantedAuthorities.setDetails(authResult.getDetails());
        super.successfulAuthentication(request, response, authTokenWithGrantedAuthorities);
    }

    @Override
    protected Object getPreAuthenticatedPrincipal(final HttpServletRequest request) {
        final DmfTenantSecurityToken securityToken = createTenantSecurityTokenVariables(request);
        if (securityToken == null) {
            return null;
        }
        return abstractControllerAuthenticationFilter.getPreAuthenticatedPrincipal(securityToken);
    }

    @Override
    protected Object getPreAuthenticatedCredentials(final HttpServletRequest request) {
        final DmfTenantSecurityToken securityToken = createTenantSecurityTokenVariables(request);
        if (securityToken == null) {
            return null;
        }
        return abstractControllerAuthenticationFilter.getPreAuthenticatedCredentials(securityToken);
    }

    protected abstract PreAuthenticationFilter createControllerAuthenticationFilter();

    protected abstract Logger log();

    /**
     * Extracts tenant and controllerId from the request URI as path variables.
     *
     * @param request the Http request to extract the path variables.
     * @return the extracted {@link DmfTenantSecurityToken} or {@code null} if the
     *         request does not match the pattern and no variables could be
     *         extracted
     */
    protected DmfTenantSecurityToken createTenantSecurityTokenVariables(final HttpServletRequest request) {
        final String requestURI = request.getRequestURI();

        if (pathExtractor.match(request.getContextPath() + CONTROLLER_REQUEST_ANT_PATTERN, requestURI)) {
            log().debug("retrieving principal from URI request {}", requestURI);
            final Map<String, String> extractUriTemplateVariables = pathExtractor
                    .extractUriTemplateVariables(request.getContextPath() + CONTROLLER_REQUEST_ANT_PATTERN, requestURI);
            final String controllerId = UrlUtils.decodeUriValue(extractUriTemplateVariables.get(CONTROLLER_ID_PLACE_HOLDER));
            final String tenant = UrlUtils.decodeUriValue(extractUriTemplateVariables.get(TENANT_PLACE_HOLDER));
            log().trace("Parsed tenant {} and controllerId {} from path request {}", tenant, controllerId, requestURI);
            return createTenantSecurityTokenVariables(request, tenant, controllerId);
        } else if (pathExtractor.match(request.getContextPath() + CONTROLLER_DL_REQUEST_ANT_PATTERN, requestURI)) {
            log().debug("retrieving path variables from URI request {}", requestURI);
            final Map<String, String> extractUriTemplateVariables = pathExtractor.extractUriTemplateVariables(
                    request.getContextPath() + CONTROLLER_DL_REQUEST_ANT_PATTERN, requestURI);
            final String tenant = UrlUtils.decodeUriValue(extractUriTemplateVariables.get(TENANT_PLACE_HOLDER));
            log().trace("Parsed tenant {} from path request {}", tenant, requestURI);
            return createTenantSecurityTokenVariables(request, tenant, "anonymous");
        } else {
            log().trace("request {} does not match the path pattern {}, request gets ignored", requestURI,
                    CONTROLLER_REQUEST_ANT_PATTERN);
            return null;
        }
    }

    private DmfTenantSecurityToken createTenantSecurityTokenVariables(final HttpServletRequest request,
            final String tenant, final String controllerId) {
        final DmfTenantSecurityToken securityToken = new DmfTenantSecurityToken(tenant, null, controllerId, null);

        Collections.list(request.getHeaderNames())
                .forEach(header -> securityToken.putHeader(header, request.getHeader(header)));

        return securityToken;
    }

}
