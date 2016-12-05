/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ui.management.event;

import java.util.Collection;

import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.common.table.BaseUIEntityEvent;

/**
 * Event for distribution set tag table
 */
public class DistributionSetTagTableEvent extends BaseUIEntityEvent<DistributionSetTag> {

    /**
     * Constructor
     * 
     * @param eventType
     *            the event type
     * @param entity
     *            the entity.
     */
    public DistributionSetTagTableEvent(final BaseEntityEventType eventType, final DistributionSetTag entity) {
        super(eventType, entity);
    }

    /**
     * Constructor
     * 
     * @param entity
     *            the created entity.
     */
    public DistributionSetTagTableEvent(final DistributionSetTag entity) {
        super(BaseEntityEventType.ADD_ENTITY, entity);
    }

    /**
     * Delete entity event.
     * 
     * @param entityIds
     *            entities which will be deleted
     */
    public DistributionSetTagTableEvent(final Collection<Long> entityIds) {
        super(entityIds, DistributionSetTag.class);
    }

}
