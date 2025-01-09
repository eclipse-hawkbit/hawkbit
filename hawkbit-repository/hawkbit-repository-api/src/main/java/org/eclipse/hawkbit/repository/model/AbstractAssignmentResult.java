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

import java.util.Collections;
import java.util.List;

import lombok.Getter;

/**
 * Generic assignment result bean.
 *
 * @param <T> type of the assigned and unassigned {@link BaseEntity}s.
 */
public abstract class AbstractAssignmentResult<T extends BaseEntity> {

    @Getter
    private final int alreadyAssigned;
    private final List<? extends T> assignedEntity;
    private final List<? extends T> unassignedEntity;

    protected AbstractAssignmentResult(
            final int alreadyAssigned, final List<? extends T> assignedEntity, final List<? extends T> unassignedEntity) {
        this.alreadyAssigned = alreadyAssigned;
        this.assignedEntity = assignedEntity;
        this.unassignedEntity = unassignedEntity;
    }

    public int getAssigned() {
        return getAssignedEntity().size();
    }

    public int getTotal() {
        return getAssigned() + alreadyAssigned;
    }

    public int getUnassigned() {
        return getUnassignedEntity().size();
    }

    public List<T> getAssignedEntity() {
        return assignedEntity == null ? Collections.emptyList() : Collections.unmodifiableList(assignedEntity);
    }

    public List<T> getUnassignedEntity() {
        return unassignedEntity == null ? Collections.emptyList() : Collections.unmodifiableList(unassignedEntity);
    }
}