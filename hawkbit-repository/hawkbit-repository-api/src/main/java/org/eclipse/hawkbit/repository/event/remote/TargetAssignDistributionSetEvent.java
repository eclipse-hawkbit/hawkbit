/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.model.Action;

/**
 * TenantAwareEvent that gets sent when a distribution set gets assigned to a
 * target.
 */
public class TargetAssignDistributionSetEvent extends RemoteTenantAwareEvent {

    private static final long serialVersionUID = 1L;

    private long distributionSetId;

    private final Map<String, Long> actions = new HashMap<>();

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
     */
    public TargetAssignDistributionSetEvent(final String tenant, final long distributionSetId, final List<Action> a,
            final String applicationId) {
        super(distributionSetId, tenant, applicationId);
        this.distributionSetId = distributionSetId;
        actions.putAll(a.stream().filter(action -> action.getDistributionSet().getId().longValue() == distributionSetId)
                .collect(Collectors.toMap(action -> action.getTarget().getControllerId(), Action::getId)));

    }

    public TargetAssignDistributionSetEvent(final Action action, final String applicationId) {
        this(action.getTenant(), action.getDistributionSet().getId(), Arrays.asList(action), applicationId);
    }

    public Long getDistributionSetId() {
        return distributionSetId;
    }

    public Map<String, Long> getActions() {
        return actions;
    }

}
