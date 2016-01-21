/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.hawkbit.simulator.amqp.SpSenderService;
import org.eclipse.hawkbit.simulator.event.InitUpdate;
import org.eclipse.hawkbit.simulator.event.ProgressUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.eventbus.EventBus;

/**
 * @author Michael Hirsch
 *
 */
@Service
public class DeviceSimulatorUpdater {

    private static final ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(4);

    @Autowired
    private SpSenderService spSenderService;

    @Autowired
    private EventBus eventbus;

    @Autowired
    private DeviceSimulatorRepository repository;

    /**
     * Starting an simulated update process of an simulated device.
     * 
     * @param tenant
     *            the tenant of the device
     * @param id
     *            the ID of the simulated device
     * @param actionId
     *            the actionId from the hawkbit update server to start the
     *            update.
     * @param swVersion
     *            the software module version from the hawkbit update server
     * @param callback
     *            the callback which gets called when the simulated update
     *            process has been finished
     */
    public void startUpdate(final String tenant, final String id, final long actionId, final String swVersion,
            final UpdaterCallback callback) {
        final AbstractSimulatedDevice device = repository.get(tenant, id);
        device.setProgress(0.0);
        device.setSwversion(swVersion);
        eventbus.post(new InitUpdate(device));
        threadPool.schedule(new DeviceSimulatorUpdateThread(device, spSenderService, actionId, eventbus, callback),
                2000, TimeUnit.MILLISECONDS);
    }

    private static final class DeviceSimulatorUpdateThread implements Runnable {
        private static final Random rndSleep = new Random();

        private final AbstractSimulatedDevice device;
        private final SpSenderService spSenderService;
        private final long actionId;
        private final EventBus eventbus;
        private final UpdaterCallback callback;

        private DeviceSimulatorUpdateThread(final AbstractSimulatedDevice device,
                final SpSenderService spSenderService, final long actionId, final EventBus eventbus,
                final UpdaterCallback callback) {
            this.device = device;
            this.spSenderService = spSenderService;
            this.actionId = actionId;
            this.eventbus = eventbus;
            this.callback = callback;
        }

        @Override
        public void run() {
            final double newProgress = device.getProgress() + 0.2;
            device.setProgress(newProgress);
            if (newProgress < 1.0) {
                threadPool.schedule(new DeviceSimulatorUpdateThread(device, spSenderService, actionId, eventbus,
                        callback), rndSleep.nextInt(3000), TimeUnit.MILLISECONDS);
            } else {
                callback.updateFinished(device, actionId);
            }
            eventbus.post(new ProgressUpdate(device));
        }
    }

    /**
     * Callback interface which is called when the simulated update process has
     * been finished and the caller of starting the simulated update process can
     * send the result to the hawkbit update server back.
     * 
     * @author Michael Hirsch
     *
     */
    @FunctionalInterface
    public interface UpdaterCallback {
        /**
         * Callback method to indicate that the simulated update process has
         * been finished.
         * 
         * @param device
         *            the device which has been updated
         * @param actionId
         *            the ID of the action from the hawkbit update server
         */
        void updateFinished(AbstractSimulatedDevice device, final Long actionId);
    }
}
