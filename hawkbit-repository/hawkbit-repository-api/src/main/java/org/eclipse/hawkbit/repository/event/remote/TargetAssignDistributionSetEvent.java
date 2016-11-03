/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.SoftwareModule;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * TenantAwareEvent that gets sent when a distribution set gets assigned to a
 * target.
 */
public class TargetAssignDistributionSetEvent extends RemoteTenantAwareEvent {

    private static final long serialVersionUID = 1L;

    private Long actionId;

    private Long distributionSetId;

    private String controllerId;

    private transient Collection<SoftwareModule> modules;

    /**
     * Default constructor.
     */
    public TargetAssignDistributionSetEvent() {
        // for serialization libs like jackson
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
        this.modules = action.getDistributionSet().getModules();

    }

    private TargetAssignDistributionSetEvent(final String tenant, final Long actionId, final Long distributionSetId,
            final String controllerId, final String applicationId) {
        super(actionId, tenant, applicationId);
        this.actionId = actionId;
        this.distributionSetId = distributionSetId;
        this.controllerId = controllerId;
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

    /**
     * @return modules if Event has been published by same node otherwise empty.
     */
    @JsonIgnore
    public Collection<SoftwareModule> getModules() {
        if (modules == null) {
            return Collections.emptyList();
        }

        return modules;
    }
}
