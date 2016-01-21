/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event;

import java.util.List;

import org.eclipse.hawkbit.eventbus.event.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.eventbus.event.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.eventbus.event.TargetInfoUpdateEvent;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.eventbus.EventBus;

/**
 * Defines all events which are sent from the {@link DeploymentManagement}.
 *
 *
 *
 *
 */
@Service
public class DeploymentManagementEvents {

    @Autowired
    private EventBus eventBus;

    /**
     * Sends the {@link TargetAssignDistributionSetEvent} for a specific target
     * to the {@link EventBus}.
     * 
     * @param target
     *            the Target which has been assigned to a distribution set
     * @param actionId
     *            the action id of the assignment
     * @param softwareModules
     *            the software modules which have been assigned
     */
    public void assignDistributionSet(final Target target, final Long actionId,
            final List<SoftwareModule> softwareModules) {
        target.getTargetInfo().setUpdateStatus(TargetUpdateStatus.PENDING);
        eventBus.post(new TargetInfoUpdateEvent(target.getTargetInfo()));
        eventBus.post(new TargetAssignDistributionSetEvent(target.getControllerId(), actionId, softwareModules,
                target.getTargetInfo().getAddress()));
    }

    /**
     * Sends the {@link CancelTargetAssignmentEvent} for a specific target to
     * the {@link EventBus}.
     * 
     * @param target
     *            the Target which has been assigned to a distribution set
     * @param actionId
     *            the action id of the assignment
     */
    public void cancalAssignDistributionSet(final Target target, final Long actionId) {
        eventBus.post(new CancelTargetAssignmentEvent(target.getControllerId(), actionId,
                target.getTargetInfo().getAddress()));
    }
}
