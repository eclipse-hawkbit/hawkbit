/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Result object for {@link TargetType} assignments.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TargetTypeAssignmentResult extends AbstractAssignmentResult<Target> {

    private final TargetType targetType;

    /**
     * Constructor.
     *
     * @param alreadyAssigned count of already assigned (ignored) elements
     * @param assigned {@link List} of assigned {@link Target}s.
     * @param unassigned {@link List} of unassigned {@link Target}s.
     * @param targetType the assigned or unassigned tag
     */
    public TargetTypeAssignmentResult(final int alreadyAssigned, final List<? extends Target> assigned,
            final List<? extends Target> unassigned, final TargetType targetType) {
        super(alreadyAssigned, assigned, unassigned);
        this.targetType = targetType;
    }
}