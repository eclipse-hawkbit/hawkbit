/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.List;

import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;

/**
 * Result object for {@link TargetTag} assigments.
 *
 *
 *
 *
 */
public class TargetTagAssigmentResult extends AssignmentResult {

    private final int unassigned;
    private final List<Target> assignedTargets;
    private final List<Target> unassignedTargets;

    /**
     * Constructor.
     *
     * @param alreadyAssigned
     *            number of already assigned/ignored elements
     * @param assigned
     *            number of newly assigned elements
     * @param unassigned
     *            number of newly assigned elements
     * @param assignedTargets
     *            {@link List} of assigned {@link Target}s.
     * @param unassignedTargets
     *            {@link List} of unassigned {@link Target}s.
     */
    public TargetTagAssigmentResult(final int alreadyAssigned, final int assigned, final int unassigned,
            final List<Target> assignedTargets, final List<Target> unassignedTargets) {
        super(assigned, alreadyAssigned);
        this.unassigned = unassigned;
        this.assignedTargets = assignedTargets;
        this.unassignedTargets = unassignedTargets;
    }

    /**
     * @return the unassigned
     */
    public int getUnassigned() {
        return unassigned;
    }

    /**
     * @return the assignedTargets
     */
    public List<Target> getAssignedTargets() {
        return assignedTargets;
    }

    /**
     * @return the unassignedTargets
     */
    public List<Target> getUnassignedTargets() {
        return unassignedTargets;
    }

}
