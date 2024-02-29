/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote;

import java.io.Serial;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.eclipse.hawkbit.repository.model.Action;

/**
 * TenantAwareEvent that gets sent when a distribution set gets assigned to a target.
 */
@NoArgsConstructor // for serialization libs like jackson
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TargetAssignDistributionSetEvent extends AbstractAssignmentEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    private long distributionSetId;
    private boolean maintenanceWindowAvailable;

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
        super(distributionSetId, tenant,
                a.stream().filter(action -> action.getDistributionSet().getId().longValue() == distributionSetId)
                        .collect(Collectors.toList()),
                applicationId);
        this.distributionSetId = distributionSetId;
        this.maintenanceWindowAvailable = maintenanceWindowAvailable;
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
}
