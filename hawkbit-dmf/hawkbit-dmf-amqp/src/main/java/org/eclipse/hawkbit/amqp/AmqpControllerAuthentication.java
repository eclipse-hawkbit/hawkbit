/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import java.util.List;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.ControllerPreAuthenticateSecurityTokenFilter;
import org.eclipse.hawkbit.security.ControllerPreAuthenticatedAnonymousDownload;
import org.eclipse.hawkbit.security.ControllerPreAuthenticatedAnonymousFilter;
import org.eclipse.hawkbit.security.ControllerPreAuthenticatedGatewaySecurityTokenFilter;
import org.eclipse.hawkbit.security.ControllerPreAuthenticatedSecurityHeaderFilter;
import org.eclipse.hawkbit.security.DdiSecurityProperties;
import org.eclipse.hawkbit.security.DmfTenantSecurityToken;
import org.eclipse.hawkbit.security.PreAuthTokenSourceTrustAuthenticationProvider;
import org.eclipse.hawkbit.security.PreAuthenticationFilter;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.google.common.collect.Lists;

/**
 *
 * A controller which handles the DMF AMQP authentication.
 */
public class AmqpControllerAuthentication {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmqpControllerAuthentication.class);

    private final PreAuthTokenSourceTrustAuthenticationProvider preAuthenticatedAuthenticationProvider = new PreAuthTokenSourceTrustAuthenticationProvider();

    private List<PreAuthenticationFilter> filterChain;

    private final ControllerManagement controllerManagement;

    private final SystemManagement systemManagement;

    private final TenantConfigurationManagement tenantConfigurationManagement;

    private final TenantAware tenantAware;

    private final DdiSecurityProperties ddiSecruityProperties;

    private final SystemSecurityContext systemSecurityContext;

    /**
     * Constructor.
     * 
     * @param systemManagement
     * @param controllerManagement
     * @param tenantConfigurationManagement
     * @param tenantAware
     *            current tenant
     * @param ddiSecruityProperties
     *            security configurations
     * @param systemSecurityContext
     *            security context
     */
    public AmqpControllerAuthentication(final SystemManagement systemManagement,
            final ControllerManagement controllerManagement,
            final TenantConfigurationManagement tenantConfigurationManagement, final TenantAware tenantAware,
            final DdiSecurityProperties ddiSecruityProperties, final SystemSecurityContext systemSecurityContext) {
        this.controllerManagement = controllerManagement;
        this.systemManagement = systemManagement;
        this.tenantConfigurationManagement = tenantConfigurationManagement;
        this.tenantAware = tenantAware;
        this.ddiSecruityProperties = ddiSecruityProperties;
        this.systemSecurityContext = systemSecurityContext;
    }

    /**
     * Called by spring when bean instantiated and autowired.
     */
    @PostConstruct
    public void postConstruct() {
        addFilter();
    }

    private void addFilter() {
        filterChain = Lists.newArrayListWithExpectedSize(5);

        final ControllerPreAuthenticatedGatewaySecurityTokenFilter gatewaySecurityTokenFilter = new ControllerPreAuthenticatedGatewaySecurityTokenFilter(
                tenantConfigurationManagement, tenantAware, systemSecurityContext);
        filterChain.add(gatewaySecurityTokenFilter);

        final ControllerPreAuthenticatedSecurityHeaderFilter securityHeaderFilter = new ControllerPreAuthenticatedSecurityHeaderFilter(
                ddiSecruityProperties.getRp().getCnHeader(), ddiSecruityProperties.getRp().getSslIssuerHashHeader(),
                tenantConfigurationManagement, tenantAware, systemSecurityContext);
        filterChain.add(securityHeaderFilter);

        final ControllerPreAuthenticateSecurityTokenFilter securityTokenFilter = new ControllerPreAuthenticateSecurityTokenFilter(
                tenantConfigurationManagement, controllerManagement, tenantAware, systemSecurityContext);
        filterChain.add(securityTokenFilter);

        final ControllerPreAuthenticatedAnonymousDownload anonymousDownloadFilter = new ControllerPreAuthenticatedAnonymousDownload(
                tenantConfigurationManagement, tenantAware, systemSecurityContext);
        filterChain.add(anonymousDownloadFilter);

        filterChain.add(new ControllerPreAuthenticatedAnonymousFilter(ddiSecruityProperties));
    }

    /**
     * Performs authentication with the security token.
     *
     * @param securityToken
     *            the authentication request object
     * @return the authentication object
     */
    public Authentication doAuthenticate(final DmfTenantSecurityToken securityToken) {
        resolveTenant(securityToken);
        PreAuthenticatedAuthenticationToken authentication = new PreAuthenticatedAuthenticationToken(null, null);
        for (final PreAuthenticationFilter filter : filterChain) {
            final PreAuthenticatedAuthenticationToken authenticationRest = createAuthentication(filter, securityToken);
            if (authenticationRest != null) {
                authentication = authenticationRest;
                authentication.setDetails(new TenantAwareAuthenticationDetails(securityToken.getTenant(), true));
                break;
            }
        }
        return preAuthenticatedAuthenticationProvider.authenticate(authentication);

    }

    private void resolveTenant(final DmfTenantSecurityToken securityToken) {
        if (securityToken.getTenant() == null) {
            securityToken.setTenant(systemSecurityContext
                    .runAsSystem(() -> systemManagement.getTenantMetadata(securityToken.getTenantId()).getTenant()));
        }

    }

    private static PreAuthenticatedAuthenticationToken createAuthentication(final PreAuthenticationFilter filter,
            final DmfTenantSecurityToken secruityToken) {

        if (!filter.isEnable(secruityToken)) {
            return null;
        }

        final Object principal = filter.getPreAuthenticatedPrincipal(secruityToken);
        final Object credentials = filter.getPreAuthenticatedCredentials(secruityToken);

        if (principal == null) {
            LOGGER.debug("No pre-authenticated principal found in message");
            return null;
        }

        LOGGER.debug("preAuthenticatedPrincipal = {} trying to authenticate", principal);

        return new PreAuthenticatedAuthenticationToken(principal, credentials,
                filter.getSuccessfulAuthenticationAuthorities());
    }

}
