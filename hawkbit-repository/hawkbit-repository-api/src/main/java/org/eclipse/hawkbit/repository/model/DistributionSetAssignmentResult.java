/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.TargetManagement;
import org.springframework.util.CollectionUtils;

/**
 * A bean which holds a complex result of an service operation to combine the
 * information of an assignment and how much of the assignment has been done and
 * how much of the assignments had already been existed.
 *
 */
public class DistributionSetAssignmentResult extends AssignmentResult<Target> {

    private final List<String> assignedTargets;
    private final List<Action> actions;

    private final TargetManagement targetManagement;

    /**
     *
     * Constructor.
     *
     * @param assignedTargets
     *            the target objects which have been assigned to the
     *            distribution set
     * @param assigned
     *            count of the assigned targets
     * @param alreadyAssigned
     *            the count of the already assigned targets
     * @param targetManagement
     *            to retrieve the assigned targets
     * @param actions
     *            of the assignment
     *
     */
    public DistributionSetAssignmentResult(final List<String> assignedTargets, final int assigned,
            final int alreadyAssigned, final List<Action> actions, final TargetManagement targetManagement) {
        super(assigned, alreadyAssigned, 0, Collections.emptyList(), Collections.emptyList());
        this.assignedTargets = assignedTargets;
        this.actions = actions;
        this.targetManagement = targetManagement;
    }

    /**
     * @return the actionIds
     */
    public List<Long> getActions() {
        if (actions == null) {
            return Collections.emptyList();
        }

        return actions.stream().map(Action::getId).collect(Collectors.toList());
    }

    @Override
    public List<Target> getAssignedEntity() {
        if (CollectionUtils.isEmpty(assignedTargets)) {
            return Collections.emptyList();
        }

        return targetManagement.getByControllerID(assignedTargets);
    }

}
