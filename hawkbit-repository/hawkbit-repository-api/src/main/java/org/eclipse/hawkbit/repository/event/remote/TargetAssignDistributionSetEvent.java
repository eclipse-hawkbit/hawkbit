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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * TenantAwareEvent that gets sent when a distribution set gets assigned to a
 * target.
 *
 */
public class TargetAssignDistributionSetEvent extends RemoteTenantAwareEvent {

    private static final long serialVersionUID = 1L;

    @JsonProperty(required = true)
    private final Long actionId;

    @JsonProperty(required = true)
    private final Long distributionSetId;

    @JsonProperty
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
    @JsonCreator
    protected TargetAssignDistributionSetEvent(@JsonProperty("tenant") final String tenant,
            @JsonProperty("actionId") final Long actionId,
            @JsonProperty("distributionSetId") final Long distributionSetId,
            @JsonProperty("controllerId") final String controllerId,
            @JsonProperty("originService") final String applicationId) {
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
    @JsonCreator
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