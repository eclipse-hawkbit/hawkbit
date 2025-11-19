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
import java.util.concurrent.Callable;

import lombok.EqualsAndHashCode;
import org.eclipse.hawkbit.auth.SpRole;
import org.eclipse.hawkbit.context.SystemSecurityContext;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
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
     * If the auth mechanism is not enabled for the tenant - it just returns null.
     * If the auth mechanism is supported, the filter extracts from the security token the related credentials,
     * validate them (do authenticate the caller).
     * If validation / auth is successful returns an authenticated auth object. Otherwise,
     * throws BadCredentialsException.
     *
     * @param controllerSecurityToken the securityToken
     * @return the extracted tenant and controller id
     */
    Authentication authenticate(ControllerSecurityToken controllerSecurityToken);

    Logger log();

    abstract class AbstractAuthenticator implements Authenticator {

        private final Callable<Boolean> isEnabledGetter;

        protected AbstractAuthenticator() {
            isEnabledGetter = () -> TenantConfigHelper.getInstance().getConfigValue(getTenantConfigurationKey(), Boolean.class);
        }

        protected boolean isEnabled(final ControllerSecurityToken securityToken) {
            return SystemSecurityContext.runAsSystemAsTenant(securityToken.getTenant(), isEnabledGetter);
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
                    List.of(new SimpleGrantedAuthority(SpRole.CONTROLLER_ROLE));
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