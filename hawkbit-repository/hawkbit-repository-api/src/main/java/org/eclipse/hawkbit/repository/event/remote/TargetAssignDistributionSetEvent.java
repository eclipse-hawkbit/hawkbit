/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import org.eclipse.hawkbit.repository.model.Action;

/**
 * TenantAwareEvent that gets sent when a distribution set gets assigned to a
 * target.
 *
 */
public class TargetAssignDistributionSetEvent extends RemoteTenantAwareEvent {

    private static final long serialVersionUID = 1L;

    private final Long actionId;

    private final Long distributionSetId;

    private final String controllerId;

    /**
     * Constructor.
     * 
     * @param tenant
     *            the tenant
     * @param actionId
     *            the actionId
     * @param distributionSetId
     *            the distributionSetId
     * @param controllerId
     *            the controllerId
     * @param applicationId
     *            the application id.
     */
    protected TargetAssignDistributionSetEvent(final String tenant, final Long actionId, final Long distributionSetId,
            final String controllerId, final String applicationId) {
        super(actionId, tenant, applicationId);
        this.actionId = actionId;
        this.distributionSetId = distributionSetId;
        this.controllerId = controllerId;
    }

    /**
     * Constructor.
     * 
     * @param action
     *            the action
     * @param applicationId
     *            the application id.
     */
    public TargetAssignDistributionSetEvent(final Action action, final String applicationId) {
        this(action.getTenant(), action.getId(), action.getDistributionSet().getId(),
                action.getTarget().getControllerId(), applicationId);
    }

    public Long getActionId() {
        return actionId;
    }

    public String getControllerId() {
        return controllerId;
    }

    public Long getDistributionSetId() {
        return distributionSetId;
    }

}