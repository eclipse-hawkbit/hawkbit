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
public class DistributionSetAssignmentResult extends AbstractAssignmentResult<Action> {

    private final DistributionSet distributionSet;

    /**
     *
     * Constructor.
     *  @param distributionSet
     *            that has been assigned
     * @param alreadyAssigned
     *            the the count of already assigned targets
     * @param assigned
     *            the assigned actions
     */
    public DistributionSetAssignmentResult(final DistributionSet distributionSet, final int alreadyAssigned,
            final List<? extends Action> assigned) {
        super(alreadyAssigned, assigned, Collections.emptyList());
        this.distributionSet = distributionSet;
    }

    /**
     * @return The distribution set that has been assigned
     */
    public DistributionSet getDistributionSet() {
        return distributionSet;
    }

}
