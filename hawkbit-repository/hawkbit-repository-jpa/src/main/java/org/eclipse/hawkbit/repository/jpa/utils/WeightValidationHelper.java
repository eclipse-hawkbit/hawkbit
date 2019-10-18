package org.eclipse.hawkbit.repository.jpa.utils;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.exception.MultiAssignmentIsNotEnabledException;
import org.eclipse.hawkbit.repository.exception.NoWeightProvidedInMultiAssignmentModeException;
import org.eclipse.hawkbit.security.SystemSecurityContext;

/**
 * Utility class to handle weight validation in Rollout, Auto Assignments, and
 * Online Assignment.
 */
public final class WeightValidationHelper {

    private WeightValidationHelper() {
        // utility class
    }

    /**
     * Checks if the weight is valid.
     * 
     * @param weight
     *            weight tied to the rollout, auto assignment, or online
     *            assignment
     * @param systemSecurityContext
     *            security context used to get the tenant and for execution
     * @param tenantConfigurationManagement
     *            to get the value from
     */
    public static void verifyWeight(final Integer weight, final SystemSecurityContext systemSecurityContext,
            final TenantConfigurationManagement tenantConfigurationManagement) {
        final boolean hasWeight = weight != null;
        verifyWeight(hasWeight, !hasWeight, systemSecurityContext, tenantConfigurationManagement);
    }

    /**
     * Checks if the weight is valid with the multi-assignments being turned
     * off/on.
     * 
     * @param hasWeight
     *            indicator of the weight if it has numerical value
     * @param hasNoWeight
     *            indicator of the weight if it doesn't have a numerical value
     * @param systemSecurityContext
     *            security context used to get the tenant and for execution
     * @param tenantConfigurationManagement
     *            to get the value from
     */
    public static void verifyWeight(final boolean hasWeight, final boolean hasNoWeight,
            final SystemSecurityContext systemSecurityContext,
            final TenantConfigurationManagement tenantConfigurationManagement) {
        // remove bypassing the weight enforcement as soon as weight can be set
        // via UI
        final boolean bypassWeightEnforcement = true;
        final boolean multiAssignmentsEnabled = TenantConfigHelper.isMultiAssignmentsEnabled(systemSecurityContext,
                tenantConfigurationManagement);
        if (!multiAssignmentsEnabled && hasWeight) {
            throw new MultiAssignmentIsNotEnabledException();
        } else if (bypassWeightEnforcement) {
            return;
        } else if (multiAssignmentsEnabled && hasNoWeight) {
            throw new NoWeightProvidedInMultiAssignmentModeException();
        }
    }
}
