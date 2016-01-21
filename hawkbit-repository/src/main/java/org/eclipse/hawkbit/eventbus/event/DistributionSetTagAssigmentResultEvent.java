/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.eventbus.event;

import org.eclipse.hawkbit.repository.model.DistributionSetTagAssigmentResult;

/**
 * A event for assignment target tag.
 */
public class DistributionSetTagAssigmentResultEvent {

    private final DistributionSetTagAssigmentResult assigmentResult;

    /**
     * Constructor.
     * 
     * @param assigmentResult
     *            the assignment result-
     */
    public DistributionSetTagAssigmentResultEvent(final DistributionSetTagAssigmentResult assigmentResult) {
        this.assigmentResult = assigmentResult;
    }

    public DistributionSetTagAssigmentResult getAssigmentResult() {
        return assigmentResult;
    }

}
