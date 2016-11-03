/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator.event;

import java.util.Collection;

import org.eclipse.hawkbit.simulator.AbstractSimulatedDevice;

/**
 * Event description which indicates a poll time update.
 */
public class NextPollCounterUpdate {

    private final Collection<AbstractSimulatedDevice> devices;

    /**
     * Creates poll timer update event.
     * 
     * @param devices
     *            the devices which progress has been updated
     */
    public NextPollCounterUpdate(final Collection<AbstractSimulatedDevice> devices) {
        this.devices = devices;
    }

    /**
     * @return the devices of the event
     */
    public Collection<AbstractSimulatedDevice> getDevices() {
        return devices;
    }

}
