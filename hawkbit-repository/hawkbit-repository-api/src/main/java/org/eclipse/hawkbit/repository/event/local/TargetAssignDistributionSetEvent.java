/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.local;

import java.util.Collection;

import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;

/**
 * Event that gets sent when a distribution set gets assigned to a target.
 *
 */
public class TargetAssignDistributionSetEvent extends DefaultEvent {

    private static final long serialVersionUID = 1L;
    private final Collection<SoftwareModule> softwareModules;
    private final Target target;
    private final Long actionId;

    /**
     * Creates a new {@link TargetAssignDistributionSetEvent}.
     *
     * @param revision
     *            the revision of the event
     * @param tenant
     *            the tenant of the event
     * @param target
     *            the assigned {@link Target}
     * @param actionId
     *            the action id of the assignment
     * @param softwareModules
     *            the software modules which have been assigned to the target
     */
    public TargetAssignDistributionSetEvent(final long revision, final String tenant, final Target target,
            final Long actionId, final Collection<SoftwareModule> softwareModules) {
        super(revision, tenant);
        this.target = target;
        this.actionId = actionId;
        this.softwareModules = softwareModules;
    }

    /**
     * @return the action id of the assignment
     */
    public Long getActionId() {
        return actionId;
    }

    /**
     * @return the {@link Target} which has been assigned to the distribution
     *         set
     */
    public Target getTarget() {
        return target;
    }

    /**
     * @return the software modules which have been assigned to the target
     */
    public Collection<SoftwareModule> getSoftwareModules() {
        return softwareModules;
    }
}