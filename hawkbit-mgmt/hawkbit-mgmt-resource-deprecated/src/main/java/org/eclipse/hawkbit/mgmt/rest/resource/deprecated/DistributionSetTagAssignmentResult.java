/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource.deprecated;

import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.eclipse.hawkbit.repository.model.AbstractAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;

/**
 * Result object for {@link DistributionSetTag} assignments.
 *
 * @deprecated since 0.6.0 with toggle deprecation
 */
@Deprecated(forRemoval = true, since = "0.6.0")
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuppressWarnings("java:S1133") // will be removed at some point
public class DistributionSetTagAssignmentResult extends AbstractAssignmentResult<DistributionSet> {

    private final DistributionSetTag distributionSetTag;

    public DistributionSetTagAssignmentResult(final int alreadyAssigned,
            final List<DistributionSet> assigned, final List<DistributionSet> unassigned,
            final DistributionSetTag distributionSetTag) {
        super(alreadyAssigned, assigned, unassigned);
        this.distributionSetTag = distributionSetTag;
    }
}
