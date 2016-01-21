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
import org.eclipse.hawkbit.repository.model.TargetIdName;

/**
 *
 *
 *
 */
public class TargetTableEvent {

    /**
     * Target table components events.
     *
     */
    public enum TargetComponentEvent {
        REFRESH_TARGETS, EDIT_TARGET, DELETE_TARGET, SELECTED_TARGET, MAXIMIZED, MINIMIZED, SELLECT_ALL, BULK_TARGET_CREATED, BULK_UPLOAD_COMPLETED, BULK_TARGET_UPLOAD_STARTED, BULK_UPLOAD_PROCESS_STARTED
    }

    private TargetComponentEvent targetComponentEvent;

    private Target target;

    private TargetIdName targetIdName;

    /**
     * @param targetComponentEvent
     */
    public TargetTableEvent(final TargetComponentEvent targetComponentEvent) {
        super();
        this.targetComponentEvent = targetComponentEvent;
    }

    /**
     * @param targetComponentEvent
     * @param target
     */
    public TargetTableEvent(final TargetComponentEvent targetComponentEvent, final Target target) {
        this(targetComponentEvent);
        this.target = target;
    }

    /**
     * @param targetComponentEvent
     * @param targetIdName
     */
    public TargetTableEvent(final TargetComponentEvent targetComponentEvent, final TargetIdName targetIdName) {
        this(targetComponentEvent);
        this.targetIdName = targetIdName;
    }

    public TargetComponentEvent getTargetComponentEvent() {
        return targetComponentEvent;
    }

    public void setTargetComponentEvent(final TargetComponentEvent targetComponentEvent) {
        this.targetComponentEvent = targetComponentEvent;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(final Target target) {
        this.target = target;
    }

    public TargetIdName getTargetIdName() {
        return targetIdName;
    }

    public void setTargetIdName(final TargetIdName targetIdName) {
        this.targetIdName = targetIdName;
    }

}
