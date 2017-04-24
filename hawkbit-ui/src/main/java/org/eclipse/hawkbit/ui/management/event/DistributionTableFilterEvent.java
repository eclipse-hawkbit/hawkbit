/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.event;

import org.eclipse.hawkbit.ui.common.table.BaseUIEvent;

/**
 * Distribution Set Filter Event. Is published when there is a filter action on
 * a distribution set table on the Deployment or Distribution View. It is
 * possible to filter by text or tag.
 */
public class DistributionTableFilterEvent extends BaseUIEvent {

    /**
     * Enum for the different types of DistributionTableFilterEvents
     */
    public enum DistributionTableFilterEventType {
        /**
         * filters the distribution sets by tag
         */
        FILTER_BY_TAG,
        /**
         * remove distribution sets' filter by tag
         */
        REMOVE_FILTER_BY_TAG,
        /**
         * filters the distribution sets by text
         */
        FILTER_BY_TEXT,
        /**
         * remove distribution sets' filter by text
         */
        REMOVE_FILTER_BY_TEXT;
    }

    private final DistributionTableFilterEventType eventType;

    /**
     * Constructor for DistributionTableFilterEvent
     * 
     * @param eventType
     *            the type of the event
     */
    public DistributionTableFilterEvent(final DistributionTableFilterEventType eventType) {
        super();
        this.eventType = eventType;
    }

    public DistributionTableFilterEventType getEventType() {
        return eventType;
    }

}
