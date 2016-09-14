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

import org.eclipse.hawkbit.dmf.json.model.TenantSecurityToken;
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
import org.eclipse.hawkbit.security.PreAuthTokenSourceTrustAuthenticationProvider;
import org.eclipse.hawkbit.security.PreAuthentificationFilter;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 *
 * A controller which handles the amqp authentfication.
 */
@Component
public class AmqpControllerAuthentfication {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmqpControllerAuthentfication.class);

    private final PreAuthTokenSourceTrustAuthenticationProvider preAuthenticatedAuthenticationProvider;

    private final List<PreAuthentificationFilter> filterChain = new ArrayList<>();

    @Autowired
    private ControllerManagement controllerManagement;

    @Autowired
    private SystemManagement systemManagement;

    @Autowired
    private TenantConfigurationManagement tenantConfigurationManagement;

    @Autowired
    private TenantAware tenantAware;

    @Autowired
    private DdiSecurityProperties ddiSecruityProperties;

    @Autowired
    private SystemSecurityContext systemSecurityContext;

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
     * @param secruityToken
     *            the authentication request object
     * @return the authentication object
     */
    public Authentication doAuthenticate(final TenantSecurityToken secruityToken) {
        PreAuthenticatedAuthenticationToken authentication = new PreAuthenticatedAuthenticationToken(null, null);
        for (final PreAuthentificationFilter filter : filterChain) {
            final PreAuthenticatedAuthenticationToken authenticationRest = createAuthentication(filter, secruityToken);
            if (authenticationRest != null) {
                authentication = authenticationRest;

                String tenant = secruityToken.getTenant();
                if (tenant == null) {
                    tenant = systemSecurityContext.runAsSystem(
                            () -> systemManagement.getTenantMetadata(secruityToken.getTenantId()).getTenant());
                }
                authentication.setDetails(new TenantAwareAuthenticationDetails(tenant, true));

                break;
            }
        }
        return preAuthenticatedAuthenticationProvider.authenticate(authentication);

    }

    private static PreAuthenticatedAuthenticationToken createAuthentication(final PreAuthentificationFilter filter,
            final TenantSecurityToken secruityToken) {

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

    public void setSecruityProperties(final DdiSecurityProperties secruityProperties) {
        this.ddiSecruityProperties = secruityProperties;
    }

    public void setTenantConfigurationManagement(final TenantConfigurationManagement tenantConfigurationManagement) {
        this.tenantConfigurationManagement = tenantConfigurationManagement;
    }

    public void setTenantAware(final TenantAware tenantAware) {
        this.tenantAware = tenantAware;
    }

    void setSystemSecurityContext(final SystemSecurityContext systemSecurityContext) {
        this.systemSecurityContext = systemSecurityContext;
    }
}
