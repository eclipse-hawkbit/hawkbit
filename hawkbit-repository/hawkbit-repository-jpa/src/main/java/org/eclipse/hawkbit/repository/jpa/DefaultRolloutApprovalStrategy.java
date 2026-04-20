/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Optional;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.repository.RolloutApprovalStrategy;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Default implementation of {@link RolloutApprovalStrategy}. Decides whether approval is needed based on configuration of the tenant as well
 * as the roles of the user who created the Rollout. Provides a no-operation implementation of
 * {@link RolloutApprovalStrategy#onApprovalRequired(Rollout)}.
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DefaultRolloutApprovalStrategy implements RolloutApprovalStrategy {

    /**
     * Returns true, if rollout approval is enabled and rollout creator doesn't have approval role. It has to be called in the user context
     */
    @Override
    public boolean isApprovalNeeded(final Rollout rollout) {
        return TenantConfigHelper.getAsSystem(TenantConfigurationKey.ROLLOUT_APPROVAL_ENABLED, Boolean.class) // if approval enabled for tenant
                && !getCurrentAuthentication().map(Authentication::getAuthorities).map(grantedAuthorities -> grantedAuthorities.stream()
                .map(GrantedAuthority::getAuthority).anyMatch(SpPermission.APPROVE_ROLLOUT::equals)).orElse(false);
    }

    @Override
    public void onApprovalRequired(final Rollout rollout) {
        // do nothing per default, can be extended by further implementations.
    }

    @Override
    public String getApprovalUser(final Rollout rollout) {
        return getCurrentAuthentication().map(Authentication::getName).orElse(null);
    }

    private static Optional<Authentication> getCurrentAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }
}