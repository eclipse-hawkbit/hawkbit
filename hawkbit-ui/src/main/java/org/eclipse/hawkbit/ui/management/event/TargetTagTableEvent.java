/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ui.management.event;

import java.util.Collection;

import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.common.table.BaseUIEntityEvent;

/**
 * Event for distribution set tag table
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
