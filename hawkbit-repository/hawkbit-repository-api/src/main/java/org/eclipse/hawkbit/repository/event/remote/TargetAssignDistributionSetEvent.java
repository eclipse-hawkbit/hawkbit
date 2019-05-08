/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionProperties;

/**
 * TenantAwareEvent that gets sent when a distribution set gets assigned to a
 * target.
 */
public class TargetAssignDistributionSetEvent extends RemoteTenantAwareEvent {

    private static final long serialVersionUID = 1L;

    private long distributionSetId;

    private boolean maintenanceWindowAvailable;

    private final Map<String, ActionProperties> actions = new HashMap<>();

    /**
     * Default constructor.
     */
    public TargetAssignDistributionSetEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor.
     * 
     * @param tenant
     *            of the event
     * @param distributionSetId
     *            of the set that was assigned
     * @param a
     *            the actions and the targets
     * @param applicationId
     *            the application id.
     * @param maintenanceWindowAvailable
     *            see {@link Action#isMaintenanceWindowAvailable()}
     */
    public TargetAssignDistributionSetEvent(final String tenant, final long distributionSetId, final List<Action> a,
            final String applicationId, final boolean maintenanceWindowAvailable) {
        super(distributionSetId, tenant, applicationId);
        this.distributionSetId = distributionSetId;
        this.maintenanceWindowAvailable = maintenanceWindowAvailable;
        actions.putAll(a.stream().filter(action -> action.getDistributionSet().getId().longValue() == distributionSetId)
                .collect(Collectors.toMap(action -> action.getTarget().getControllerId(), ActionProperties::new)));

    }

    /**
     * Constructor.
     *
     * @param action
     *            the action created for this assignment
     * @param applicationId
     *            the application id
     */
    public TargetAssignDistributionSetEvent(final Action action, final String applicationId) {
        this(action.getTenant(), action.getDistributionSet().getId(), Collections.singletonList(action), applicationId,
                action.isMaintenanceWindowAvailable());
    }

    public Long getDistributionSetId() {
        return distributionSetId;
    }

    public boolean isMaintenanceWindowAvailable() {
        return maintenanceWindowAvailable;
    }

    public Map<String, ActionProperties> getActions() {
        return actions;
    }

}
