/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.event;

import org.eclipse.hawkbit.repository.model.SoftwareModule;

/**
 * Event to represent software add, update or delete.
 * 
 *
 *
 */
public class SoftwareModuleEvent {

    /**
     * Software module events in the Upload UI.
     * 
     *
     *
     */
    public enum SoftwareModuleEventType {
        NEW_SOFTWARE_MODULE, UPDATED_SOFTWARE_MODULE, DELETE_SOFTWARE_MODULE, SELECTED_SOFTWARE_MODULE, MAXIMIZED, MINIMIZED, ARTIFACTS_CHANGED, ASSIGN_SOFTWARE_MODULE
    }

    private SoftwareModuleEventType softwareModuleEventType;

    private SoftwareModule softwareModule;

    /**
     * Creates software module event.
     * 
     * @param softwareModuleEventType
     *            reference of {@link SoftwareModuleEventType}
     * @param softwareModule
     *            reference of {@link SoftwareModule}
     */
    public SoftwareModuleEvent(final SoftwareModuleEventType softwareModuleEventType,
            final SoftwareModule softwareModule) {
        super();
        this.softwareModuleEventType = softwareModuleEventType;
        this.softwareModule = softwareModule;
    }

    public SoftwareModuleEventType getSoftwareModuleEventType() {
        return softwareModuleEventType;
    }

    public void setSoftwareModuleEventType(final SoftwareModuleEventType softwareModuleEventType) {
        this.softwareModuleEventType = softwareModuleEventType;
    }

    public SoftwareModule getSoftwareModule() {
        return softwareModule;
    }

    public void setSoftwareModule(final SoftwareModule softwareModule) {
        this.softwareModule = softwareModule;
    }

}
