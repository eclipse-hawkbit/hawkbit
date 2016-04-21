package org.eclipse.hawkbit.ui.distributions.event;

/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEvent;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;

/**
 *
 *
 *
 */
public class DistributionSetTableEvent extends BaseEntityEvent<DistributionSet> {

    /**
     * DistributionSet table components events.
     *
     */
    public enum DistributionSetComponentEvent {
        SELECT_ALL
    }

    private DistributionSetComponentEvent distributionSetComponentEvent;

    /**
     * Constructor.
     * 
     * @param eventType
     *            the event type.
     * @param entity
     *            the entity
     */
    public DistributionSetTableEvent(final BaseEntityEventType eventType, final DistributionSet entity) {
        super(eventType, entity);
    }

    /**
     * The component event.
     * 
     * @param DistributionSetTableEvent
     *            the distributionSet component event.
     */
    public DistributionSetTableEvent(final DistributionSetComponentEvent distributionSetComponentEvent) {
        super(null, null);
        this.distributionSetComponentEvent = distributionSetComponentEvent;
    }

    public DistributionSetComponentEvent getDistributionSetComponentEvent() {
        return distributionSetComponentEvent;
    }

}
