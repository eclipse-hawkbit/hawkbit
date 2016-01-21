/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.eventbus.event;

import org.eclipse.hawkbit.repository.model.TargetTagAssigmentResult;

/**
 * A event for assignment target tag.
 */
public class TargetTagAssigmentResultEvent {

    private final TargetTagAssigmentResult assigmentResult;

    /**
     * Constructor.
     * 
     * @param assigmentResult
     *            the assignment result-
     */
    public TargetTagAssigmentResultEvent(final TargetTagAssigmentResult assigmentResult) {
        this.assigmentResult = assigmentResult;
    }

    public TargetTagAssigmentResult getAssigmentResult() {
        return assigmentResult;
    }

}
