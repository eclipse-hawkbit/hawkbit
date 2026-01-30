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

import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.exception.NoWeightProvidedInMultiAssignmentModeException;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.Rollout;

/**
 * Utility class to handle weight validation in Rollout, Auto Assignments, and Online Assignment.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class WeightValidationHelper {

    /**
     * Validating weights associated with all the {@link DeploymentRequest}s
     *
     * @param deploymentRequests the {@linkplain List} of {@link DeploymentRequest}s
     */
    public static void validate(final List<DeploymentRequest> deploymentRequests) {
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
    public static void validate(final Rollout rollout) {
        validateWeight(rollout.getWeight().orElse(null));
    }

    /**
     * Validating weight associated with the target filter query
     *
     * @param targetFilterQueryCreate the target filter query
     */
    public static void validate(final TargetFilterQueryManagement.Create targetFilterQueryCreate) {
        validateWeight(targetFilterQueryCreate.getAutoAssignWeight());
    }

    /**
     * Validating weight associated with the auto assignment
     *
     * @param autoAssignDistributionSetUpdate the auto assignment distribution set update
     */
    public static void validate(final TargetFilterQueryManagement.AutoAssignDistributionSetUpdate autoAssignDistributionSetUpdate) {
        validateWeight(autoAssignDistributionSetUpdate.weight());
    }

    /**
     * Checks if the weight is valid
     *
     * @param weight weight tied to the rollout, auto assignment, or online assignment.
     */
    public static void validateWeight(final Integer weight) {
        final boolean hasWeight = weight != null;
        validateWeight(hasWeight, !hasWeight);
    }

    /**
     * Checks if the weight is valid with the multi-assignments being turned off/on.
     *
     * @param hasWeight indicator of the weight if it has numerical value
     * @param hasNoWeight indicator of the weight if it doesn't have a numerical value
     */
    public static void validateWeight(final boolean hasWeight, final boolean hasNoWeight) {
        // remove bypassing the weight enforcement as soon as weight can be set via UI
        final boolean bypassWeightEnforcement = true;
        if (!bypassWeightEnforcement && hasNoWeight) {
            throw new NoWeightProvidedInMultiAssignmentModeException();
        }
    }
}