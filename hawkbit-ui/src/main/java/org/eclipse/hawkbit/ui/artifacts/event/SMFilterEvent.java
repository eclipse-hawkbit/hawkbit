/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.event;

import org.eclipse.hawkbit.ui.common.ManagementEntityState;

/**
 * Software module filter event. Is thrown when there is a filter action on a
 * software module table on the Distribution or Upload View. It is possible to
 * filter by text or type.
 */
public class SMFilterEvent {

    /**
     * Enum for the different types of SMFilterEvent
     */
    public enum SMFilterEventType {
        /**
         * filters the software modules by type
         */
        REMOVE_FILTER_BY_TYPE,
        /**
         * remove software modules' filter by type
         */
        FILTER_BY_TYPE,
        /**
         * filters the software modules by text
         */
        FILTER_BY_TEXT,
        /**
         * remove software modules' filter by text
         */
        REMOVE_FILTER_BY_TEXT;
    }

    private final SMFilterEventType eventType;

    private final ManagementEntityState<Long> origin;

    /**
     * Constructor for SMFilterEvent
     * 
     * @param eventType
     *            the type of the event
     * @param origin
     *            the origin of the event
     */
    public SMFilterEvent(final SMFilterEventType eventType, final ManagementEntityState<Long> origin) {
        this.eventType = eventType;
        this.origin = origin;
    }

    public SMFilterEventType getEventType() {
        return eventType;
    }

    public ManagementEntityState<Long> getOrigin() {
        return origin;
    }

}
