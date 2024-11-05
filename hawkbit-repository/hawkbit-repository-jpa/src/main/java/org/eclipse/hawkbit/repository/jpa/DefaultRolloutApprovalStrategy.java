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

import java.util.Collection;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.RolloutApprovalStrategy;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.UserAuthoritiesResolver;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link RolloutApprovalStrategy}. Decides whether
 * approval is needed based on configuration of the tenant as well as the roles
 * of the user who created the Rollout. Provides a no-operation implementation
 * of {@link RolloutApprovalStrategy#onApprovalRequired(Rollout)}.
 */
public class DefaultRolloutApprovalStrategy implements RolloutApprovalStrategy {

    private final UserAuthoritiesResolver userAuthoritiesResolver;

    private final TenantConfigurationManagement tenantConfigurationManagement;

    private final SystemSecurityContext systemSecurityContext;

    DefaultRolloutApprovalStrategy(
            final UserAuthoritiesResolver userAuthoritiesResolver,
            final TenantConfigurationManagement tenantConfigurationManagement,
            final SystemSecurityContext systemSecurityContext) {
        this.userAuthoritiesResolver = userAuthoritiesResolver;
        this.tenantConfigurationManagement = tenantConfigurationManagement;
        this.systemSecurityContext = systemSecurityContext;
    }

    /**
     * Returns true, if rollout approval is enabled and rollout creator doesn't
     * have approval role.
     */
    @Override
    public boolean isApprovalNeeded(final Rollout rollout) {
        return isApprovalEnabled() && hasNoApproveRolloutPermission(getActorAuthorities(rollout));
    }

    /***
     * Per default do nothing.
     *
     * @param rollout
     *            rollout to create approval task for.
     */
    @Override
    public void onApprovalRequired(final Rollout rollout) {
        // do nothing per default, can be extended by further implementations.
    }

    @Override
    public String getApprovalUser(final Rollout rollout) {
        return getCurrentAuthentication().getName();
    }

    private static Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private static boolean hasNoApproveRolloutPermission(final Collection<String> authorities) {
        return authorities.stream().noneMatch(SpPermission.APPROVE_ROLLOUT::equals);
    }

    private boolean isApprovalEnabled() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationKey.ROLLOUT_APPROVAL_ENABLED, Boolean.class).getValue());
    }

    private Collection<String> getActorAuthorities(final Rollout rollout) {
        // rollout state transition from CREATING to CREATED is managed by
        // scheduler under SYSTEM user context, thus we get the
        // user based on the properties of initially created rollout entity
        if (RolloutStatus.CREATING == rollout.getStatus()) {
            final String actor = rollout.getLastModifiedBy() != null ? rollout.getLastModifiedBy()
                    : rollout.getCreatedBy();
            if (!StringUtils.isEmpty(actor)) {
                return systemSecurityContext.runAsSystem(
                        () -> userAuthoritiesResolver.getUserAuthorities(rollout.getTenant(), actor));
            }
        }

        return ((User) getCurrentAuthentication().getPrincipal()).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList();
    }
}
