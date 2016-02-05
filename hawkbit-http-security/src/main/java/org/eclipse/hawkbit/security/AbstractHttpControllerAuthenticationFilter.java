/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import java.io.IOException;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.hawkbit.dmf.json.model.TenantSecruityToken;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.util.AntPathMatcher;

import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;

/**
 * An abstraction for all controller based security to parse the e.g. the tenant
 * name from the URL and the controller ID from the URL to do security checks
 * based on these information.
 * 
 *
 *
 *
 */
public abstract class AbstractHttpControllerAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHttpControllerAuthenticationFilter.class);

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
    protected TenantConfigurationManagement tenantConfigurationManagement;
    protected TenantAware tenantAware;
    protected SystemSecurityContext systemSecurityContext;

    private final AntPathMatcher pathExtractor;

    private PreAuthenficationFilter abstractControllerAuthenticationFilter;

    /**
     * Constructor for sub-classes.
     * 
     * @param systemManagement
     *            the system management service
     * @param tenantAware
     *            the tenant aware service
     */
    public AbstractHttpControllerAuthenticationFilter(final TenantConfigurationManagement tenantConfigurationManagement,
            final TenantAware tenantAware, final SystemSecurityContext systemSecurityContext) {
        this.tenantConfigurationManagement = tenantConfigurationManagement;
        this.tenantAware = tenantAware;
        this.systemSecurityContext = systemSecurityContext;
        pathExtractor = new AntPathMatcher();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.web.authentication.preauth.
     * AbstractPreAuthenticatedProcessingFilter
     * #doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
     * javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }

        final TenantSecruityToken secruityToken = createTenantSecruityTokenVariables((HttpServletRequest) request);
        if (secruityToken == null) {
            chain.doFilter(request, response);
            return;
        }
        abstractControllerAuthenticationFilter = createControllerAuthenticationFilter();
        if (abstractControllerAuthenticationFilter.isEnable(secruityToken)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            super.doFilter(request, response, chain);
        } else {
            chain.doFilter(request, response);
        }
    }

    protected abstract PreAuthenficationFilter createControllerAuthenticationFilter();

    /**
     * Extracts tenant and controllerId from the request URI as path variables.
     * 
     * @param request
     *            the Http request to extract the path variables.
     * @return the extracted {@link PathVariables} or {@code null} if the
     *         request does not match the pattern and no variables could be
     *         extracted
     */
    protected TenantSecruityToken createTenantSecruityTokenVariables(final HttpServletRequest request) {
        final String requestURI = request.getRequestURI();

        if (pathExtractor.match(request.getContextPath() + CONTROLLER_REQUEST_ANT_PATTERN, requestURI)) {
            LOGGER.debug("retrieving principal from URI request {}", requestURI);
            final Map<String, String> extractUriTemplateVariables = pathExtractor
                    .extractUriTemplateVariables(request.getContextPath() + CONTROLLER_REQUEST_ANT_PATTERN, requestURI);
            final String controllerId = extractUriTemplateVariables.get(CONTROLLER_ID_PLACE_HOLDER);
            final String tenant = extractUriTemplateVariables.get(TENANT_PLACE_HOLDER);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Parsed tenant {} and controllerId {} from path request {}", tenant, controllerId,
                        requestURI);
            }
            return createTenantSecruityTokenVariables(request, tenant, controllerId);
        } else if (pathExtractor.match(request.getContextPath() + CONTROLLER_DL_REQUEST_ANT_PATTERN, requestURI)) {
            LOGGER.debug("retrieving path variables from URI request {}", requestURI);
            final Map<String, String> extractUriTemplateVariables = pathExtractor.extractUriTemplateVariables(
                    request.getContextPath() + CONTROLLER_DL_REQUEST_ANT_PATTERN, requestURI);
            final String tenant = extractUriTemplateVariables.get(TENANT_PLACE_HOLDER);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Parsed tenant {} from path request {}", tenant, requestURI);
            }
            return createTenantSecruityTokenVariables(request, tenant, "anonymous");
        } else {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("request {} does not match the path pattern {}, request gets ignored", requestURI,
                        CONTROLLER_REQUEST_ANT_PATTERN);
            }
            return null;
        }
    }

    private TenantSecruityToken createTenantSecruityTokenVariables(final HttpServletRequest request,
            final String tenant, final String controllerId) {
        final TenantSecruityToken secruityToken = new TenantSecruityToken(tenant, controllerId, "");
        final UnmodifiableIterator<String> forEnumeration = Iterators.forEnumeration(request.getHeaderNames());
        forEnumeration.forEachRemaining(header -> secruityToken.getHeaders().put(header, request.getHeader(header)));
        return secruityToken;
    }

    @Override
    protected Object getPreAuthenticatedPrincipal(final HttpServletRequest request) {
        final TenantSecruityToken secruityToken = createTenantSecruityTokenVariables(request);
        if (secruityToken == null) {
            return null;
        }
        return abstractControllerAuthenticationFilter.getPreAuthenticatedPrincipal(secruityToken);
    }

    @Override
    protected Object getPreAuthenticatedCredentials(final HttpServletRequest request) {
        final TenantSecruityToken secruityToken = createTenantSecruityTokenVariables(request);
        if (secruityToken == null) {
            return null;
        }
        return abstractControllerAuthenticationFilter.getPreAuthenticatedCredentials(secruityToken);
    }

}
