/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
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
