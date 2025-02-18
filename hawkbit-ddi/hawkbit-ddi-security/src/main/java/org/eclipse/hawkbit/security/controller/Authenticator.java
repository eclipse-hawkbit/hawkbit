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

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import lombok.EqualsAndHashCode;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.tenancy.TenantAwareAuthenticationDetails;
import org.slf4j.Logger;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Interface for Authentication mechanism.
 */
public interface Authenticator {

    /**
     * If the authentication mechanism is not enabled for the tenant - it just returns null.
     * If the authentication mechanism is supported, the filter extracts from the security token the related credentials,
     * validate them (do authenticate the caller).
     * If validation / authentication is successful returns an authenticated authentication object. Otherwise,
     * throws BadCredentialsException.
     *
     * @param controllerSecurityToken the securityToken
     * @return the extracted tenant and controller id
     */
    Authentication authenticate(ControllerSecurityToken controllerSecurityToken);

    Logger log();

    abstract class AbstractAuthenticator implements Authenticator {

        protected final TenantConfigurationManagement tenantConfigurationManagement;
        protected final TenantAware tenantAware;
        protected final SystemSecurityContext systemSecurityContext;
        private final TenantAware.TenantRunner<Boolean> isEnabledTenantRunner;

        protected AbstractAuthenticator(
                final TenantConfigurationManagement tenantConfigurationManagement,
                final TenantAware tenantAware, final SystemSecurityContext systemSecurityContext) {
            this.tenantConfigurationManagement = tenantConfigurationManagement;
            this.tenantAware = tenantAware;
            this.systemSecurityContext = systemSecurityContext;
            isEnabledTenantRunner = () -> systemSecurityContext.runAsSystem(
                    () -> tenantConfigurationManagement.getConfigurationValue(getTenantConfigurationKey(), Boolean.class).getValue());
        }

        protected boolean isEnabled(final ControllerSecurityToken securityToken) {
            return tenantAware.runAsTenant(securityToken.getTenant(), isEnabledTenantRunner);
        }

        protected abstract String getTenantConfigurationKey();

        protected Authentication authenticatedController(final String tenant, final String controllerId) {
            Objects.requireNonNull(tenant, "tenant must not be null");
            Objects.requireNonNull(controllerId, "controllerId must not be null");
            return new AuthenticatedController(tenant, controllerId);
        }

        @EqualsAndHashCode(callSuper = true)
        private static class AuthenticatedController extends AbstractAuthenticationToken {

            private static final Collection<GrantedAuthority> CONTROLLER_AUTHORITY =
                    List.of(new SimpleGrantedAuthority(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
            private final String controllerId;

            AuthenticatedController(final String tenant, final String controllerId) {
                super(CONTROLLER_AUTHORITY);
                super.setDetails(new TenantAwareAuthenticationDetails(tenant, true));
                this.controllerId = controllerId;
                setAuthenticated(true);
            }

            @Override
            public Object getCredentials() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return controllerId;
            }
        }
    }
}