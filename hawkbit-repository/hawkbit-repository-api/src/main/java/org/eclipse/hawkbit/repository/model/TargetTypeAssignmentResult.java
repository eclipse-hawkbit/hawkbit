/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.List;

/**
 * Result object for {@link TargetType} assignments.
 *
 */
public class TargetTypeAssignmentResult extends AbstractAssignmentResult<Target> {

    private final TargetType targetType;

    /**
     * Constructor.
     *
     * @param alreadyAssigned
     * count of already assigned (ignored) elements
     * @param assigned
     * {@link List} of assigned {@link Target}s.
     * @param unassigned
     * {@link List} of unassigned {@link Target}s.
     * @param targetType
     * the assigned or unassigned tag
     */
    public TargetTypeAssignmentResult(final int alreadyAssigned, final List<? extends Target> assigned,
                                      final List<? extends Target> unassigned, final TargetType targetType) {
        super(alreadyAssigned, assigned, unassigned);
        this.targetType = targetType;
    }

    public TargetType getTargetType() {
        return targetType;
    }
}
