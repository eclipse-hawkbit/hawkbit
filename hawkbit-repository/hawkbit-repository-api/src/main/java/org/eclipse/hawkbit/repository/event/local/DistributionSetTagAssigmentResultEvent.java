/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.local;

import org.eclipse.hawkbit.repository.model.DistributionSetTagAssignmentResult;

/**
 * An event for assignment target tag.
 */
public class DistributionSetTagAssigmentResultEvent extends LocalTenantAwareEvent {

    private static final long serialVersionUID = 1L;

    private final transient DistributionSetTagAssignmentResult assigmentResult;

    /**
     * Constructor.
     * 
     * @param assigmentResult
     *            the assignment result
     * @param tenant
     *            current
     */
    public DistributionSetTagAssigmentResultEvent(final DistributionSetTagAssignmentResult assigmentResult,
            final String tenant) {
        super(tenant);
        this.assigmentResult = assigmentResult;
    }

    public DistributionSetTagAssignmentResult getAssigmentResult() {
        return assigmentResult;
    }

}
