/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

import java.util.List;

/**
 * Result object for {@link DistributionSetTag} assignments.
 *
 */
public class DistributionSetTagAssignmentResult extends AbstractAssignmentResult<DistributionSet> {

    private final DistributionSetTag distributionSetTag;

    /**
     * Constructor.
     *
     * @param alreadyAssigned
     *            number of already assigned/ignored elements
     * @param assigned
     *            newly assigned elements
     * @param unassigned
     *            unassigned elements
     * @param distributionSetTag
     *            the assigned or unassigned tag
     */
    public DistributionSetTagAssignmentResult(final int alreadyAssigned,
            final List<DistributionSet> assigned, final List<DistributionSet> unassigned,
            final DistributionSetTag distributionSetTag) {
        super(alreadyAssigned, assigned, unassigned);
        this.distributionSetTag = distributionSetTag;
    }

    public DistributionSetTag getDistributionSetTag() {
        return distributionSetTag;
    }

}
