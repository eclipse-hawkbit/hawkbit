/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.local;

import java.net.URI;
import java.util.Collection;

import org.eclipse.hawkbit.repository.model.SoftwareModule;

/**
 * Event that gets sent when a distribution set gets assigned to a target.
 *
 */
public class TargetAssignDistributionSetEvent extends DefaultEvent {

    private static final long serialVersionUID = 1L;
    private final Collection<SoftwareModule> softwareModules;
    private final String controllerId;
    private final Long actionId;
    private final URI targetAdress;
    private final String targetToken;

    /**
     * Creates a new {@link TargetAssignDistributionSetEvent}.
     *
     * @param revision
     *            the revision of the event
     * @param tenant
     *            the tenant of the event
     * @param controllerId
     *            the ID of the controller
     * @param actionId
     *            the action id of the assignment
     * @param softwareModules
     *            the software modules which have been assigned to the target
     * @param targetAdress
     *            the targetAdress of the target
     * @param targetToken
     *            the authentication token of the target
     */
    public TargetAssignDistributionSetEvent(final long revision, final String tenant, final String controllerId,
            final Long actionId, final Collection<SoftwareModule> softwareModules, final URI targetAdress,
            final String targetToken) {
        super(revision, tenant);
        this.controllerId = controllerId;
        this.actionId = actionId;
        this.softwareModules = softwareModules;
        this.targetAdress = targetAdress;
        this.targetToken = targetToken;
    }

    /**
     * @return the action id of the assignment
     */
    public Long getActionId() {
        return actionId;
    }

    /**
     * @return the controllerId of the Target which has been assigned to the
     *         distribution set
     */
    public String getControllerId() {
        return controllerId;
    }

    /**
     * @return the software modules which have been assigned to the target
     */
    public Collection<SoftwareModule> getSoftwareModules() {
        return softwareModules;
    }

    public URI getTargetAdress() {
        return targetAdress;
    }

    public String getTargetToken() {
        return targetToken;
    }
}
