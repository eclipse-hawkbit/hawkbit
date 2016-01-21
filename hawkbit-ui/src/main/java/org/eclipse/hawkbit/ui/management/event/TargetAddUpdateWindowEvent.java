/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.event;

import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent.TargetComponentEvent;

/**
 * 
 *
 *
 *
 */
public class TargetAddUpdateWindowEvent {

    private final TargetComponentEvent targetComponentEvent;

    private final Target target;

    /**
     * @param eventType
     *            the event type
     * @param target
     *            the target which has been created or modified
     */
    public TargetAddUpdateWindowEvent(final TargetComponentEvent eventType, final Target target) {
        this.targetComponentEvent = eventType;
        this.target = target;
    }

    /**
     * @return the targetComponentEvent
     */
    public TargetComponentEvent getTargetComponentEvent() {
        return targetComponentEvent;
    }

    /**
     * @return the target
     */
    public Target getTarget() {
        return target;
    }
}
