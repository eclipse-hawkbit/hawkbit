/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.List;

/**
 * Result object for {@link DistributionSetTag} assigments.
 *
 */
public class DistributionSetTagAssigmentResult extends AssignmentResult {

    private final int unassigned;
    private final List<DistributionSet> assignedDs;
    private final List<DistributionSet> unassignedDs;
    private final DistributionSetTag distributionSetTag;

    /**
     * Constructor.
     *
     * @param alreadyAssigned
     *            number of already assigned/ignored elements
     * @param assigned
     *            number of newly assigned elements
     * @param unassigned
     *            number of newly assigned elements
     * @param assignedDs
     *            {@link List} of assigned {@link DistributionSet}s.
     * @param unassignedDs
     *            {@link List} of unassigned {@link DistributionSet}s.
     * @param distributionSetTag
     *            the assigned or unassigned tag
     */
    public DistributionSetTagAssigmentResult(final int alreadyAssigned, final int assigned, final int unassigned,
            final List<DistributionSet> assignedDs, final List<DistributionSet> unassignedDs,
            final DistributionSetTag distributionSetTag) {
        super(assigned, alreadyAssigned);
        this.unassigned = unassigned;
        this.assignedDs = assignedDs;
        this.unassignedDs = unassignedDs;
        this.distributionSetTag = distributionSetTag;
    }

    /**
     * @return the unassigned
     */
    public int getUnassigned() {
        return unassigned;
    }

    /**
     * @return the distributionSetTag
     */
    public DistributionSetTag getDistributionSetTag() {
        return distributionSetTag;
    }

    /**
     * @return the assignedDs
     */
    public List<DistributionSet> getAssignedDs() {
        return assignedDs;
    }

    /**
     * @return the unassignedDs
     */
    public List<DistributionSet> getUnassignedDs() {
        return unassignedDs;
    }

}
