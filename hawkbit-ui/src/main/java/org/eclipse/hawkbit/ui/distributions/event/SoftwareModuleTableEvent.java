/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.event;

/**
 * Class which contains the Event when selecting all entries of the
 * softwareModule table
 */
public class SoftwareModuleTableEvent {

    /**
     * SoftwareModule table components events.
     */
    public enum SoftwareModuleComponentEvent {
        SELECT_ALL
    }

    private final SoftwareModuleComponentEvent softwareModuleComponentEvent;

    /**
     * The component event.
     * 
     * @param softwareModuleComponentEvent
     *            the softwareModule component event.
     */
    public SoftwareModuleTableEvent(final SoftwareModuleComponentEvent softwareModuleComponentEvent) {
        this.softwareModuleComponentEvent = softwareModuleComponentEvent;
    }

    public SoftwareModuleComponentEvent getSoftwareModuleComponentEvent() {
        return softwareModuleComponentEvent;
    }

}
