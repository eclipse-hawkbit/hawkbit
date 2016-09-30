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
public class AssignmentResult<T extends BaseEntity> {

    private final int total;
    private final int assigned;
    private final int alreadyAssigned;
    private final int unassigned;
    private final List<T> assignedEntity;
    private final List<T> unassignedEntity;

    /**
     * Constructor.
     *
     * @param assigned
     *            is the number of newly assigned elements.
     * @param alreadyAssigned
     *            number of already assigned/ignored elements
     * @param unassigned
     *            number of newly assigned elements
     * @param assignedEntity
     *            {@link List} of assigned entity.
     * @param unassignedEntity
     *            {@link List} of unassigned entity.
     */
    public AssignmentResult(final int assigned, final int alreadyAssigned, final int unassigned,
            final List<T> assignedEntity, final List<T> unassignedEntity) {
        this.assigned = assigned;
        this.alreadyAssigned = alreadyAssigned;
        total = assigned + alreadyAssigned;
        this.unassigned = unassigned;
        this.assignedEntity = assignedEntity;
        this.unassignedEntity = unassignedEntity;
    }

    /**
     * @return number of newly assigned elements.
     */
    public int getAssigned() {
        return assigned;
    }

    /**
     * @return total number (assigned and already assigned).
     */
    public int getTotal() {
        return total;
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
        return unassigned;
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
