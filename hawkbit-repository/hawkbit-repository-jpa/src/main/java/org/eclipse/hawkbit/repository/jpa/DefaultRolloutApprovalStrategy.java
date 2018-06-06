/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.im.authentication.UserPrincipal;
import org.eclipse.hawkbit.repository.RolloutApprovalStrategy;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Default implementation of {@link RolloutApprovalStrategy}. Decides whether
 * approval is needed based on configuration of the tenant as well as the roles
 * of the user who created the Rollout. Provides a no-operation implementation
 * of {@link RolloutApprovalStrategy#onApprovalRequired(Rollout)}.
 */
public class DefaultRolloutApprovalStrategy implements RolloutApprovalStrategy {

    private final UserDetailsService userDetailsService;

    private final TenantConfigurationManagement tenantConfigurationManagement;

    private final SystemSecurityContext systemSecurityContext;

    DefaultRolloutApprovalStrategy(UserDetailsService userDetailsService,
            TenantConfigurationManagement tenantConfigurationManagement,
            SystemSecurityContext systemSecurityContext) {
        this.userDetailsService = userDetailsService;
        this.tenantConfigurationManagement = tenantConfigurationManagement;
        this.systemSecurityContext = systemSecurityContext;
    }

    /**
     * Returns true, if rollout approval is enabled and rollout creator doesn't have
     * approval role.
     */
    @Override
    public boolean isApprovalNeeded(final Rollout rollout) {
        final UserDetails userDetails = this.getActor(rollout);
        final boolean approvalEnabled = this.tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationKey.ROLLOUT_APPROVAL_ENABLED, Boolean.class).getValue();
        return approvalEnabled && userDetails.getAuthorities().stream()
                .noneMatch(authority -> SpPermission.APPROVE_ROLLOUT.equals(authority.getAuthority()));
    }


    private UserDetails getActor(Rollout rollout) {
        final String actor = rollout.getLastModifiedBy() != null ? rollout.getLastModifiedBy() : rollout.getCreatedBy();
        return systemSecurityContext.runAsSystem(() -> {
            UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if(userPrincipal.getUsername().equals(actor)) {
                return userPrincipal;
            } else {
                return this.userDetailsService.loadUserByUsername(actor);
            }
        });
    }

    /***
     * Per default do nothing.
     * 
     * @param rollout
     *            rollout to create approval task for.
     */
    @Override
    public void onApprovalRequired(Rollout rollout) {
        // do nothing per default, can be extended by further implementations.
    }

    @Override
    public String getApprovalUser(Rollout rollout) {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
