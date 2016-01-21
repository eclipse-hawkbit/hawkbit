/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator.event;

import java.util.List;

import org.eclipse.hawkbit.simulator.AbstractSimulatedDevice;

/**
 * Event description which indicates an poll time update.
 * 
 * @author Michael Hirsch
 *
 */
public class NextPollCounterUpdate {

    private final List<AbstractSimulatedDevice> devices;

    /**
     * Creates poll timer update event.
     * 
     * @param devices
     *            the devices which progress has been updated
     */
    public NextPollCounterUpdate(final List<AbstractSimulatedDevice> devices) {
        this.devices = devices;
    }

    /**
     * @return the devices of the event
     */
    public List<AbstractSimulatedDevice> getDevices() {
        return devices;
    }

}
