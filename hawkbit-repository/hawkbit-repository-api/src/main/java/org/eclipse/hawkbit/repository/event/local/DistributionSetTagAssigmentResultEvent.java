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
import org.springframework.data.annotation.Transient;

/**
 * An event for assignment target tag.
 */
public class DistributionSetTagAssigmentResultEvent extends DefaultEvent {

    private static final long serialVersionUID = 1L;

    @Transient
    private final DistributionSetTagAssignmentResult assigmentResult;

    /**
     * Constructor.
     * 
     * @param assigmentResult
     *            the assignment result-
     */
    public DistributionSetTagAssigmentResultEvent(final DistributionSetTagAssignmentResult assigmentResult) {
        super(-1, null);
        this.assigmentResult = assigmentResult;
    }

    public DistributionSetTagAssignmentResult getAssigmentResult() {
        return assigmentResult;
    }

}
