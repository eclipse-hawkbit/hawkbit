/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import org.eclipse.hawkbit.mgmt.json.model.MgmtMaintenanceWindowRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtTargetAssignmentRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtDistributionSetAssignment;
import org.eclipse.hawkbit.repository.MaintenanceScheduleHelper;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;

/**
 * A mapper for assignment requests
 */
public final class MgmtDeploymentRequestMapper {
    private MgmtDeploymentRequestMapper() {
        // Utility class
    }

    /**
     * Convert assignment information to an {@link DeploymentRequest}
     * 
     * @param dsAssignment
     *            DS assignment information
     * @param targetId
     *            target to assign the DS to
     * @return resulting {@link DeploymentRequest}
     */
    public static DeploymentRequest createAssignmentRequest(final MgmtDistributionSetAssignment dsAssignment,
            final String targetId) {

        return createAssignmentRequest(targetId, dsAssignment.getId(), dsAssignment.getType(),
                dsAssignment.getForcetime(), dsAssignment.getMaintenanceWindow());
    }

    /**
     * Convert assignment information to an {@link DeploymentRequest}
     * 
     * @param targetAssignment
     *            target assignment information
     * @param dsId
     *            DS to assign the target to
     * @return resulting {@link DeploymentRequest}
     */
    public static DeploymentRequest createAssignmentRequest(final MgmtTargetAssignmentRequestBody targetAssignment,
            final Long dsId) {

        return createAssignmentRequest(targetAssignment.getId(), dsId, targetAssignment.getType(),
                targetAssignment.getForcetime(), targetAssignment.getMaintenanceWindow());
    }

    private static DeploymentRequest createAssignmentRequest(final String targetId, final Long dsId,
            final MgmtActionType type, final long forcetime, final MgmtMaintenanceWindowRequestBody maintenanceWindow) {
        if (maintenanceWindow == null) {
            return new DeploymentRequest(targetId, dsId, MgmtRestModelMapper.convertActionType(type), forcetime);
        }

        final String cronSchedule = maintenanceWindow.getSchedule();
        final String duration = maintenanceWindow.getDuration();
        final String timezone = maintenanceWindow.getTimezone();

        MaintenanceScheduleHelper.validateMaintenanceSchedule(cronSchedule, duration, timezone);

        return new DeploymentRequest(targetId, dsId, MgmtRestModelMapper.convertActionType(type), forcetime,
                cronSchedule, duration, timezone);
    }

}
