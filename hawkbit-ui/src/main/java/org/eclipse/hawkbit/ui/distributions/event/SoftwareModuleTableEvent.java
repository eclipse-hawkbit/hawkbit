/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.event;

import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEvent;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;

/**
 * Class which contains the Event when selecting all entries of a table
 */
public class SoftwareModuleTableEvent extends BaseEntityEvent<SoftwareModule> {

    /**
     * SoftwareModule table components events.
     *
     */
    public enum SoftwareModuleComponentEvent {
        SELECT_ALL
    }

    private SoftwareModuleComponentEvent softwareModuleComponentEvent;

    /**
     * Constructor.
     * 
     * @param eventType
     *            the event type.
     * @param entity
     *            the entity
     */
    public SoftwareModuleTableEvent(final BaseEntityEventType eventType, final SoftwareModule entity) {
        super(eventType, entity);
    }

    /**
     * The component event.
     * 
     * @param softwareModuleComponentEvent
     *            the softwareModule component event.
     */
    public SoftwareModuleTableEvent(final SoftwareModuleComponentEvent softwareModuleComponentEvent) {
        super(null, null);
        this.softwareModuleComponentEvent = softwareModuleComponentEvent;
    }

    public SoftwareModuleComponentEvent getSoftwareModuleComponentEvent() {
        return softwareModuleComponentEvent;
    }

}
