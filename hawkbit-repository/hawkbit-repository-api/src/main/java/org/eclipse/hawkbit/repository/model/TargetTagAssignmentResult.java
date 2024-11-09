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

import lombok.Data;

/**
 * Result object for {@link TargetTag} assignments.
 *
 * @deprecated since 0.6.0 with deprecation of toggle assignments
 */
@Deprecated(forRemoval = true, since = "0.6.0")
@Data
public class TargetTagAssignmentResult extends AbstractAssignmentResult<Target> {

    private final TargetTag targetTag;

    /**
     * Constructor.
     *
     * @param alreadyAssigned count of already assigned (ignored) elements
     * @param assigned {@link List} of assigned {@link Target}s.
     * @param unassigned {@link List} of unassigned {@link Target}s.
     * @param targetTag the assigned or unassigned tag
     */
    public TargetTagAssignmentResult(final int alreadyAssigned, final List<? extends Target> assigned,
            final List<? extends Target> unassigned, final TargetTag targetTag) {
        super(alreadyAssigned, assigned, unassigned);
        this.targetTag = targetTag;
    }
}
