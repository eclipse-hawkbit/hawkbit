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

/**
 * A bean which holds a complex result of an service operation to combine the
 * information of an assignment and how much of the assignment has been done and
 * how much of the assignments had already been existed.
 *
 */
public class DistributionSetAssignmentResult extends AssignmentResult<Target> {

    private final List<? extends Action> assignedActions;
    private final DistributionSet distributionSet;

    /**
     *
     * Constructor.
     *  @param distributionSet
     *            that has been assigned
     * @param alreadyAssigned
     *            the the count of already assigned targets
     * @param assigned
 *            the assigned targets
     * @param unassigned
*            the unassigned targets
     * @param assignedActions
     *       the created Actions as a result of this assignment
     */
    public DistributionSetAssignmentResult(final DistributionSet distributionSet, final int alreadyAssigned,
            final List<? extends Target> assigned, final List<? extends Target> unassigned,
            final List<? extends Action> assignedActions) {
        super(alreadyAssigned, assigned, unassigned);
        this.distributionSet = distributionSet;
        this.assignedActions = assignedActions;
    }

    /**
     * @return The distribution set that has been assigned
     */
    public DistributionSet getDistributionSet() {
        return distributionSet;
    }

    /**
     * @return the assigned actions
     */
    public List<Action> getAssignedActions() {
        return Collections.unmodifiableList(assignedActions);
    }

}
