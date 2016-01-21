/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

/**
 * Generic assigment result bean.
 *
 *
 *
 *
 */
public class AssignmentResult {

    private final int total;
    private final int assigned;
    private final int alreadyAssigned;

    /**
     * Constructor.
     *
     * @param assigned
     *            is the number of newly assigned elements.
     * @param alreadyAssigned
     *            is the number of already assigned elements.
     */
    public AssignmentResult(final int assigned, final int alreadyAssigned) {
        super();
        this.assigned = assigned;
        this.alreadyAssigned = alreadyAssigned;
        total = assigned + alreadyAssigned;
    }

    /**
     * @return the assignedTargets
     */
    public int getAssigned() {
        return assigned;
    }

    /**
     * @return the total
     */
    public int getTotal() {
        return total;
    }

    /**
     * @return the alreadyAssigned
     */
    public int getAlreadyAssigned() {
        return alreadyAssigned;
    }

}
