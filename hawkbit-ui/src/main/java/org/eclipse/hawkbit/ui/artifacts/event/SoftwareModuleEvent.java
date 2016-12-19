/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.event;

import java.util.Collection;

import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.common.table.BaseUIEntityEvent;

/**
 * TenantAwareEvent to represent software add, update or delete.
 *
 */
public class SoftwareModuleEvent extends BaseUIEntityEvent<SoftwareModule> {

    /**
     * Software module events in the Upload UI.
     *
     */
    public enum SoftwareModuleEventType {
        ARTIFACTS_CHANGED, ASSIGN_SOFTWARE_MODULE
    }

    private SoftwareModuleEventType softwareModuleEventType;

    /**
     * Creates software module event.
     * 
     * @param entityEventType
     *            the event type
     */
    public SoftwareModuleEvent(final BaseEntityEventType entityEventType) {
        super(entityEventType, null);
    }

    /**
     * Creates software module event.
     * 
     * @param entityEventType
     *            the event type
     * @param softwareModule
     *            the module
     */
    public SoftwareModuleEvent(final BaseEntityEventType entityEventType, final SoftwareModule softwareModule) {
        super(entityEventType, softwareModule);
    }

    /**
     * Constructor
     * 
     * @param eventType
     *            the event type
     * @param entityIds
     *            the entity ids
     */
    public SoftwareModuleEvent(final BaseEntityEventType eventType, final Collection<Long> entityIds) {
        super(eventType, entityIds, SoftwareModule.class);
    }

    /**
     * Creates software module event.
     * 
     * @param softwareModuleEventType
     *            the event type
     * @param softwareModule
     *            the module
     */
    public SoftwareModuleEvent(final SoftwareModuleEventType softwareModuleEventType,
            final SoftwareModule softwareModule) {
        super(null, softwareModule);
        this.softwareModuleEventType = softwareModuleEventType;
    }

    public SoftwareModuleEventType getSoftwareModuleEventType() {
        return softwareModuleEventType;
    }

}
