/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.mgmt.json.model.MgmtMaintenanceWindowRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtTargetAssignmentRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtDistributionSetAssignment;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.MaintenanceScheduleHelper;
import org.eclipse.hawkbit.repository.model.DeploymentRequestBuilder;

/**
 * A mapper for assignment requests
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MgmtDeploymentRequestMapper {

    /**
     * Convert assignment information to an {@link DeploymentRequestBuilder}
     *
     * @param dsAssignment DS assignment information
     * @param targetId target to assign the DS to
     * @return resulting {@link DeploymentRequestBuilder}
     */
    public static DeploymentRequestBuilder createAssignmentRequestBuilder(
            final MgmtDistributionSetAssignment dsAssignment, final String targetId) {

        return createAssignmentRequestBuilder(targetId, dsAssignment.getId(), dsAssignment.getType(),
                dsAssignment.getForcetime(), dsAssignment.getWeight(), dsAssignment.getMaintenanceWindow());
    }

    /**
     * Convert assignment information to an {@link DeploymentRequestBuilder}
     *
     * @param targetAssignment target assignment information
     * @param dsId DS to assign the target to
     * @return resulting {@link DeploymentRequestBuilder}
     */
    public static DeploymentRequestBuilder createAssignmentRequestBuilder(
            final MgmtTargetAssignmentRequestBody targetAssignment, final Long dsId) {
        return createAssignmentRequestBuilder(targetAssignment.getId(), dsId, targetAssignment.getType(),
                targetAssignment.getForcetime(), targetAssignment.getWeight(), targetAssignment.getMaintenanceWindow());
    }

    private static DeploymentRequestBuilder createAssignmentRequestBuilder(final String targetId, final Long dsId,
            final MgmtActionType type, final long forcetime, final Integer weight,
            final MgmtMaintenanceWindowRequestBody maintenanceWindow) {
        final DeploymentRequestBuilder request = DeploymentManagement.deploymentRequest(targetId, dsId)
                .setActionType(MgmtRestModelMapper.convertActionType(type)).setForceTime(forcetime).setWeight(weight);
        if (maintenanceWindow != null) {
            final String cronSchedule = maintenanceWindow.getSchedule();
            final String duration = maintenanceWindow.getDuration();
            final String timezone = maintenanceWindow.getTimezone();
            MaintenanceScheduleHelper.validateMaintenanceSchedule(cronSchedule, duration, timezone);
            request.setMaintenance(cronSchedule, duration, timezone);
        }
        return request;
    }
}