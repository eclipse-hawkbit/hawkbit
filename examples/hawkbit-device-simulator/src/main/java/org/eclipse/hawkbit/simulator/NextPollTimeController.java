/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.hawkbit.simulator.event.NextPollCounterUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

/**
 * Poll time trigger which executes the {@link DDISimulatedDevice#poll()} every
 * second.
 */
@Component
public class NextPollTimeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(NextPollTimeController.class);

    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private static final ExecutorService pollService = Executors.newFixedThreadPool(1);

    @Autowired
    private DeviceSimulatorRepository repository;

    @Autowired
    private EventBus eventBus;

    /**
     * Constructor which schedules the poll trigger runnable every second.
     */
    public NextPollTimeController() {
        executorService.scheduleWithFixedDelay(new NextPollUpdaterRunnable(), 1, 1, TimeUnit.SECONDS);
    }

    private class NextPollUpdaterRunnable implements Runnable {
        @Override
        public void run() {
            final Collection<AbstractSimulatedDevice> devices = repository.getAll();

            devices.forEach(device -> {
                int nextCounter = device.getNextPollCounterSec() - 1;
                if (nextCounter < 0) {
                    try {
                        pollService.submit(() -> device.poll());
                    } catch (final IllegalStateException e) {
                        LOGGER.trace("Device could not be polled", e);
                    }
                    nextCounter = device.getPollDelaySec();
                }

                device.setNextPollCounterSec(nextCounter);
            });
            eventBus.post(new NextPollCounterUpdate(devices));
        }
    }
}
