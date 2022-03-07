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
 * Generic assignment result bean.
 * 
 * @param <T>
 *            type of the assigned and unassigned {@link BaseEntity}s.
 *
 */
public abstract class AbstractAssignmentResult<T extends BaseEntity> {

    private final int alreadyAssigned;
    private final List<? extends T> assignedEntity;
    private final List<? extends T> unassignedEntity;

    /**
     * Constructor.
     * @param alreadyAssigned
     *      count of already assigned entities
     * @param assignedEntity
     *            {@link List} of assigned entity.
     * @param unassignedEntity
     *            {@link List} of unassigned entity.
     */
    protected AbstractAssignmentResult(final int alreadyAssigned, final List<? extends T> assignedEntity,
            final List<? extends T> unassignedEntity) {
        this.alreadyAssigned = alreadyAssigned;
        this.assignedEntity = assignedEntity;
        this.unassignedEntity = unassignedEntity;
    }

    /**
     * @return number of newly assigned elements.
     */
    public int getAssigned() {
        return getAssignedEntity().size();
    }

    /**
     * @return total number (assigned and already assigned).
     */
    public int getTotal() {
        return getAssigned() + alreadyAssigned;
    }

    /**
     * @return number of already assigned/ignored elements.
     */
    public int getAlreadyAssigned() {
        return alreadyAssigned;
    }

    /**
     * @return number of unsassigned elements
     */
    public int getUnassigned() {
        return getUnassignedEntity().size();
    }

    /**
     * @return {@link List} of assigned entity.
     */
    public List<T> getAssignedEntity() {
        if (assignedEntity == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(assignedEntity);
    }

    /**
     * @return {@link List} of unassigned entity.
     */
    public List<T> getUnassignedEntity() {
        if (unassignedEntity == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(unassignedEntity);
    }

}
