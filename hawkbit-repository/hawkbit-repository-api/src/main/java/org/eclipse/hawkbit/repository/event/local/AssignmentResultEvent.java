/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.local;

import org.eclipse.hawkbit.repository.model.AssignmentResult;

/**
 ** An event for assignment assignment results.
 *
 * @param <E>
 *            type of the assignment result
 */
public class AssignmentResultEvent<E extends AssignmentResult<?>> extends LocalTenantAwareEvent {

    private static final long serialVersionUID = 1L;
    private final transient E assigmentResult;

    /**
     * Constructor.
     * 
     * @param assigmentResult
     *            the assignment result
     * @param tenant
     *            current
     */
    public AssignmentResultEvent(final E assigmentResult, final String tenant) {
        super(tenant);
        this.assigmentResult = assigmentResult;
    }

    public E getAssigmentResult() {
        return assigmentResult;
    }
}
