/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk.dmf.health;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.eclipse.hawkbit.sdk.dmf.DmfTenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Handle all incoming Messages from hawkBit update server.
 */
public class HealthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthService.class);

    private final Collection<DmfTenant> dmfTenants;

    private final Set<String> openPings = Collections.synchronizedSet(new HashSet<>());

    HealthService(final Collection<DmfTenant> dmfTenants) {
        this.dmfTenants = dmfTenants;
    }

    @Scheduled(fixedDelay = 5_000, initialDelay = 5_000)
    void checkDmfHealth() {
        if (openPings.size() > 5) {
            LOGGER.error("Currently {} open pings! DMF does not seem to be reachable.", openPings.size());
        } else {
            LOGGER.debug("Currently {} open pings", openPings.size());
        }

        dmfTenants.forEach(tenant -> {
            final String correlationId = UUID.randomUUID().toString();
            openPings.add(correlationId);
            LOGGER.debug("Ping tenant {} with correlationId {}", tenant, correlationId);
            tenant.ping(correlationId, this::pingReceived);
        });
    }

    void pingReceived(final String correlationId, final Message message) {
        if (!openPings.remove(correlationId)) {
            LOGGER.error("Unknown PING_RESPONSE received for correlationId: {}.", correlationId);
        }
    }
}