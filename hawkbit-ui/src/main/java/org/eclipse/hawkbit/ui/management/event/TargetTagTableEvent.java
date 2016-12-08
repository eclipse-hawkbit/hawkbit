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

import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.common.table.BaseUIEntityEvent;

/**
 * Event for target tag table
 */
public class TargetTagTableEvent extends BaseUIEntityEvent<TargetTag> {

    /**
     * Constructor
     * 
     * @param eventType
     *            the event typ
     * @param entity
     *            the created entity.
     */
    public TargetTagTableEvent(final BaseEntityEventType eventType, final TargetTag entity) {
        super(eventType, entity);
    }

    /**
     * Constructor
     * 
     * @param entity
     *            the created entity.
     */
    public TargetTagTableEvent(final TargetTag entity) {
        super(BaseEntityEventType.ADD_ENTITY, entity);
    }

    /**
     * Delete entity event.
     * 
     * @param entityIds
     *            entities which will be deleted
     */
    public TargetTagTableEvent(final Collection<Long> entityIds) {
        super(entityIds, TargetTag.class);
    }

}
