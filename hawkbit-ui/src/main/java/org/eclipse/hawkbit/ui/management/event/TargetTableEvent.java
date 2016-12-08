/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.event;

import java.util.Collection;

import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.common.table.BaseUIEntityEvent;

/**
 * Class which contains the TenantAwareEvent when selecting all entries of the
 * target table
 */
public class TargetTableEvent extends BaseUIEntityEvent<Target> {

    /**
     * Target table components events.
     *
     */
    public enum TargetComponentEvent {
        REFRESH_TARGETS, SELECT_ALL, BULK_TARGET_CREATED, BULK_UPLOAD_COMPLETED, BULK_TARGET_UPLOAD_STARTED, BULK_UPLOAD_PROCESS_STARTED
    }

    private TargetComponentEvent targetComponentEvent;

    /**
     * Constrcutor.
     * 
     * @param eventType
     *            the event type.
     */
    public TargetTableEvent(final BaseEntityEventType eventType) {
        super(eventType, null);
    }

    /**
     * Constrcutor .
     * 
     * @param eventType
     *            the event type.
     * @param entity
     *            the entity
     */
    public TargetTableEvent(final BaseEntityEventType eventType, final Target entity) {
        super(eventType, entity);
    }

    /**
     * Constructor
     * 
     * @param eventType
     *            the event type
     * @param entityIds
     *            the entity ids
     */
    public TargetTableEvent(final BaseEntityEventType eventType, final Collection<Long> entityIds) {
        super(eventType, entityIds, Target.class);
    }

    /**
     * The component event.
     * 
     * @param targetComponentEvent
     *            the target component event.
     */
    public TargetTableEvent(final TargetComponentEvent targetComponentEvent) {
        super(null, null);
        this.targetComponentEvent = targetComponentEvent;
    }

    public TargetComponentEvent getTargetComponentEvent() {
        return targetComponentEvent;
    }

}
