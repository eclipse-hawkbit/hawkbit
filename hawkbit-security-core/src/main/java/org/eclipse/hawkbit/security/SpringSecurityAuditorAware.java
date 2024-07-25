/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.security;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Auditor class that allows BaseEntitys to insert current logged in user for
 * repository changes.
 *
 */
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    // Sometimes 'system' need to override the auditor when do create/modify actions in context of a tenant and user.
    // Though this could be made using runAsTenantAsUser sometimes (as in transaction) this override is needed
    // after runAsTenantAsUser (because it seems that auditor is got in commit time).
    // So this thread local variable provides option to override explicitly the auditor.
    private static final ThreadLocal<String> AUDITOR_OVERRIDE = new ThreadLocal<>();

    @Override
    public Optional<String> getCurrentAuditor() {
        if (AUDITOR_OVERRIDE.get() != null) {
            return Optional.of(AUDITOR_OVERRIDE.get());
        }

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (isAuthenticationInvalid(authentication)) {
            return Optional.empty();
        }

        return Optional.ofNullable(getCurrentAuditor(authentication));
    }

    // Always shall be followed by {@link #clearAuditorOverride}
    public static void setAuditorOverride(final String auditor) {
        if (auditor == null) {
            AUDITOR_OVERRIDE.remove();
        } else {
            AUDITOR_OVERRIDE.set(auditor);
        }
    }

    public static void clearAuditorOverride() {
        AUDITOR_OVERRIDE.remove();
    }

    protected String getCurrentAuditor(final Authentication authentication) {
        if (authentication.getPrincipal() instanceof UserDetails) {
            return ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        if (authentication.getPrincipal() instanceof OidcUser) {
            return ((OidcUser) authentication.getPrincipal()).getPreferredUsername();
        }
        return authentication.getPrincipal().toString();
    }

    private static boolean isAuthenticationInvalid(final Authentication authentication) {
        return authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null;
    }
}