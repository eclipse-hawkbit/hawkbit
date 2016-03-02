/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.dmf.json.model.TenantSecruityToken;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.security.CoapAnonymousPreAuthenticatedFilter;
import org.eclipse.hawkbit.security.ControllerPreAuthenticateSecurityTokenFilter;
import org.eclipse.hawkbit.security.ControllerPreAuthenticatedGatewaySecurityTokenFilter;
import org.eclipse.hawkbit.security.ControllerPreAuthenticatedSecurityHeaderFilter;
import org.eclipse.hawkbit.security.PreAuthTokenSourceTrustAuthenticationProvider;
import org.eclipse.hawkbit.security.PreAuthenficationFilter;
import org.eclipse.hawkbit.security.SecurityProperties;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 *
 *
 */
@Component
public class AmqpControllerAuthentfication {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmqpControllerAuthentfication.class);

    private final PreAuthTokenSourceTrustAuthenticationProvider preAuthenticatedAuthenticationProvider;

    private final List<PreAuthenficationFilter> filterChain = new ArrayList<>();

    @Autowired
    private ControllerManagement controllerManagement;

    @Autowired
    private SystemManagement systemManagement;

    @Autowired
    private TenantAware tenantAware;

    @Autowired
    private SecurityProperties secruityProperties;

    /**
     * Constructor.
     */
    public AmqpControllerAuthentfication() {
        preAuthenticatedAuthenticationProvider = new PreAuthTokenSourceTrustAuthenticationProvider();
    }

    /**
     * Called by spring when bean instantiated and autowired.
     */
    @PostConstruct
    public void postConstruct() {
        addFilter();
    }

    private void addFilter() {
        final ControllerPreAuthenticatedGatewaySecurityTokenFilter gatewaySecurityTokenFilter = new ControllerPreAuthenticatedGatewaySecurityTokenFilter(
                systemManagement, tenantAware);
        filterChain.add(gatewaySecurityTokenFilter);

        final ControllerPreAuthenticatedSecurityHeaderFilter securityHeaderFilter = new ControllerPreAuthenticatedSecurityHeaderFilter(
                secruityProperties.getRpCnHeader(), secruityProperties.getRpSslIssuerHashHeader(), systemManagement,
                tenantAware);
        filterChain.add(securityHeaderFilter);

        final ControllerPreAuthenticateSecurityTokenFilter securityTokenFilter = new ControllerPreAuthenticateSecurityTokenFilter(
                systemManagement, controllerManagement, tenantAware);
        filterChain.add(securityTokenFilter);

        filterChain.add(new CoapAnonymousPreAuthenticatedFilter());
    }

    /**
     * Performs authentication with the secruity token.
     *
     * @param secruityToken
     *            the authentication request object
     * @return the authentfication object
     */
    public Authentication doAuthenticate(final TenantSecruityToken secruityToken) {
        PreAuthenticatedAuthenticationToken authentication = new PreAuthenticatedAuthenticationToken(null, null);
        for (final PreAuthenficationFilter filter : filterChain) {
            final PreAuthenticatedAuthenticationToken authenticationRest = createAuthentication(filter, secruityToken);
            if (authenticationRest != null) {
                authentication = authenticationRest;
                authentication.setDetails(new TenantAwareAuthenticationDetails(secruityToken.getTenant(), true));
                break;
            }
        }
        return preAuthenticatedAuthenticationProvider.authenticate(authentication);

    }

    private static PreAuthenticatedAuthenticationToken createAuthentication(final PreAuthenficationFilter filter,
            final TenantSecruityToken secruityToken) {

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

        return new PreAuthenticatedAuthenticationToken(principal, credentials);
    }

    public void setControllerManagement(final ControllerManagement controllerManagement) {
        this.controllerManagement = controllerManagement;
    }

    public void setSecruityProperties(final SecurityProperties secruityProperties) {
        this.secruityProperties = secruityProperties;
    }

    public void setSystemManagement(final SystemManagement systemManagement) {
        this.systemManagement = systemManagement;
    }

    public void setTenantAware(final TenantAware tenantAware) {
        this.tenantAware = tenantAware;
    }

}
