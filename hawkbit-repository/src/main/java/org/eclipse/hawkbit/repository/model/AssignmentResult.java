/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.List;

/**
 * Generic assignment result bean.
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
    
    public int getAssigned() {
        return assigned;
    }
    
    public int getTotal() {
        return total;
    }
    
    public int getAlreadyAssigned() {
        return alreadyAssigned;
    }
    

    public int getUnassigned() {
        return unassigned;
    }

    public List<T> getAssignedEntity() {
        return assignedEntity;
    }
    
    public List<T> getUnassignedEntity() {
        return unassignedEntity;
    }

}
