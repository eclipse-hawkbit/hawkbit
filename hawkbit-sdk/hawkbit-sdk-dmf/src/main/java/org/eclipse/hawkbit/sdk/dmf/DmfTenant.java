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

import lombok.Getter;
import org.eclipse.hawkbit.sdk.Controller;
import org.eclipse.hawkbit.sdk.Tenant;
import org.eclipse.hawkbit.sdk.dmf.amqp.Amqp;
import org.eclipse.hawkbit.sdk.dmf.amqp.VHost;
import org.springframework.amqp.core.Message;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * An in-memory simulated DMF Tenant to hold the controller twins in
 * memory and be able to retrieve them again.
 */
public class DmfTenant {

    @Getter
    private final Tenant tenant;

    private final Map<String, DmfController> controllers = new ConcurrentHashMap<>();
    private final Amqp amqp;
    private final VHost vHost;

    public DmfTenant(final Tenant tenant, final Amqp amqp) {
        this(tenant, amqp, true);
    }

    public DmfTenant(final Tenant tenant, final Amqp amqp, final boolean isEnvLocal) {
        this.tenant = tenant;
        this.amqp = amqp;
        this.vHost = amqp.getVhost(tenant.getDmf(), isEnvLocal);
        this.vHost.register(this);
    }

    public void destroy() {
        controllers.values().forEach(DmfController::stop);
        controllers.clear();
    }

    public DmfController create(final Controller controller, final UpdateHandler updateHandler) {
        final DmfController dmfController = new DmfController(tenant, controller, updateHandler, vHost);
        controllers.put(controller.getControllerId(), dmfController);
        return dmfController;
    }

    public void remove(final String controllerId) {
        Optional.ofNullable(controllers.remove(controllerId)).ifPresent(DmfController::stop);
    }

    public Optional<DmfController> getController(final String controllerId) {
        return Optional.ofNullable(controllers.get(controllerId));
    }

    public void ping(final String correlationId, final BiConsumer<String, Message> listener) {
        vHost.ping(tenant.getTenantId(), correlationId, listener);
    }
}