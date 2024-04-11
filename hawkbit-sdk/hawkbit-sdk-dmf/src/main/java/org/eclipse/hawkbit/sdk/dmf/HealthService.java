/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk.dmf;

import org.eclipse.hawkbit.sdk.dmf.amqp.DmfSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handle all incoming Messages from hawkBit update server.
 */
class HealthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthService.class);

    private final DeviceManagement deviceManagement;
    private final DmfSenderService dmfSenderService;

    private final Set<String> openPings = Collections.synchronizedSet(new HashSet<>());

    HealthService(final DeviceManagement deviceManagement, final DmfSenderService dmfSenderService) {
        this.deviceManagement = deviceManagement;
        this.dmfSenderService = dmfSenderService;
    }

    @Scheduled(fixedDelay = 5_000, initialDelay = 5_000)
    void checkDmfHealth() {
        if (openPings.size() > 5) {
            LOGGER.error("Currently {} open pings! DMF does not seem to be reachable.", openPings.size());
        } else {
            LOGGER.debug("Currently {} open pings", openPings.size());
        }

        deviceManagement.getTenants().forEach(tenant -> {
            final String correlationId = UUID.randomUUID().toString();
            openPings.add(correlationId);
            LOGGER.debug("Ping tenant {} with correlationId {}", tenant, correlationId);
            dmfSenderService.ping(tenant, correlationId, this::pingReceived);
        });
    }

    void pingReceived(final String correlationId, final Message message) {
        if (!openPings.remove(correlationId)) {
            LOGGER.error("Unknown PING_RESPONSE received for correlationId: {}.", correlationId);
        }
    }
}