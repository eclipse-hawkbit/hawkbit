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
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;

/**
 * Result object for {@link TargetTag} assignments.
 *
 * @deprecated since 0.6.0 with deprecation of toggle assignments
 */
@Deprecated(forRemoval = true, since = "0.6.0")
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TargetTagAssignmentResult extends AbstractAssignmentResult<Target> {

    private final TargetTag targetTag;

    public TargetTagAssignmentResult(
            final int alreadyAssigned, final List<? extends Target> assigned, final List<? extends Target> unassigned,
            final TargetTag targetTag) {
        super(alreadyAssigned, assigned, unassigned);
        this.targetTag = targetTag;
    }
}
