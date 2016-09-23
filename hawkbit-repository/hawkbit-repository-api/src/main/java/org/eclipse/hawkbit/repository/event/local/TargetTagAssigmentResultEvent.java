/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.local;

import org.eclipse.hawkbit.repository.model.TargetTagAssignmentResult;

/**
 * A event for assignment target tag.
 */
public class TargetTagAssigmentResultEvent extends DefaultEvent {

    private static final long serialVersionUID = 1L;
    private final TargetTagAssignmentResult assigmentResult;

    /**
     * Constructor.
     * 
     * @param assigmentResult
     *            the assignment result-
     */
    public TargetTagAssigmentResultEvent(final TargetTagAssignmentResult assigmentResult) {
        super(-1, null);
        this.assigmentResult = assigmentResult;
    }

    public TargetTagAssignmentResult getAssigmentResult() {
        return assigmentResult;
    }

}
