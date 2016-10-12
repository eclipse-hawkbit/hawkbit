/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.eventbus.event;

import org.eclipse.hawkbit.eventbus.event.Event;
import org.eclipse.hawkbit.repository.model.TargetTagAssignmentResult;

/**
 * A event for assignment target tag.
 */
public class TargetTagAssigmentResultEvent implements Event {

    private final TargetTagAssignmentResult assigmentResult;
    private final String tenant;

    /**
     * Constructor.
     * 
     * @param assigmentResult
     *            the assignment result-
     * @param tenant
     *            current
     */
    public TargetTagAssigmentResultEvent(final TargetTagAssignmentResult assigmentResult, final String tenant) {
        this.tenant = tenant;
        this.assigmentResult = assigmentResult;
    }

    public TargetTagAssignmentResult getAssigmentResult() {
        return assigmentResult;
    }

    @Override
    public long getRevision() {
        return -1;
    }

    @Override
    public String getTenant() {
        return tenant;
    }

}
