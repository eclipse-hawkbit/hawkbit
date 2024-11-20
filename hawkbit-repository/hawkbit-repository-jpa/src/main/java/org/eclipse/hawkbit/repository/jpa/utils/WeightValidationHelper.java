/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.utils;

import java.util.List;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.builder.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.exception.NoWeightProvidedInMultiAssignmentModeException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetFilterQueryCreate;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.utils.TenantConfigHelper;

/**
 * Utility class to handle weight validation in Rollout, Auto Assignments, and
 * Online Assignment.
 */
public final class WeightValidationHelper {

    private final TenantConfigurationManagement tenantConfigurationManagement;
    private final SystemSecurityContext systemSecurityContext;

    private WeightValidationHelper(final SystemSecurityContext systemSecurityContext,
            final TenantConfigurationManagement tenantConfigurationManagement) {
        this.systemSecurityContext = systemSecurityContext;
        this.tenantConfigurationManagement = tenantConfigurationManagement;
    }

    /**
     * Setting the context of the tenant
     *
     * @param systemSecurityContext security context used to get the tenant and for execution
     * @param tenantConfigurationManagement to get the value from
     */
    public static WeightValidationHelper usingContext(final SystemSecurityContext systemSecurityContext,
            final TenantConfigurationManagement tenantConfigurationManagement) {
        return new WeightValidationHelper(systemSecurityContext, tenantConfigurationManagement);
    }

    /**
     * Validating weights associated with all the {@link DeploymentRequest}s
     *
     * @param deploymentRequests the {@linkplain List} of {@link DeploymentRequest}s
     */
    public void validate(final List<DeploymentRequest> deploymentRequests) {
        final long assignmentsWithWeight = deploymentRequests.stream()
                .filter(request -> request.getTargetWithActionType().getWeight() != null).count();
        final boolean containsAssignmentWithWeight = assignmentsWithWeight > 0;
        final boolean containsAssignmentWithoutWeight = assignmentsWithWeight < deploymentRequests.size();

        validateWeight(containsAssignmentWithWeight, containsAssignmentWithoutWeight);
    }

    /**
     * Validating weight associated with the {@link Rollout}
     *
     * @param rollout the {@linkplain Rollout}
     */
    public void validate(final Rollout rollout) {
        validateWeight(rollout.getWeight().orElse(null));
    }

    /**
     * Validating weight associated with the target filter query
     *
     * @param targetFilterQueryCreate the target filter query
     */
    public void validate(final JpaTargetFilterQueryCreate targetFilterQueryCreate) {
        validateWeight(targetFilterQueryCreate.getAutoAssignWeight().orElse(null));

    }

    /**
     * Validating weight associated with the auto assignment
     *
     * @param autoAssignDistributionSetUpdate the auto assignment distribution set update
     */
    public void validate(final AutoAssignDistributionSetUpdate autoAssignDistributionSetUpdate) {
        validateWeight(autoAssignDistributionSetUpdate.getWeight());

    }

    /**
     * Checks if the weight is valid
     *
     * @param weight weight tied to the rollout, auto assignment, or online
     *         assignment.
     */
    public void validateWeight(final Integer weight) {
        final boolean hasWeight = weight != null;
        validateWeight(hasWeight, !hasWeight);
    }

    /**
     * Checks if the weight is valid with the multi-assignments being turned
     * off/on.
     *
     * @param hasWeight indicator of the weight if it has numerical value
     * @param hasNoWeight indicator of the weight if it doesn't have a numerical value
     */
    public void validateWeight(final boolean hasWeight, final boolean hasNoWeight) {
        // remove bypassing the weight enforcement as soon as weight can be set
        // via UI
        final boolean bypassWeightEnforcement = true;
        final boolean multiAssignmentsEnabled = TenantConfigHelper
                .usingContext(systemSecurityContext, tenantConfigurationManagement).isMultiAssignmentsEnabled();
        if (bypassWeightEnforcement) {
            return;
        } else if (multiAssignmentsEnabled && hasNoWeight) {
            throw new NoWeightProvidedInMultiAssignmentModeException();
        }
    }
}
