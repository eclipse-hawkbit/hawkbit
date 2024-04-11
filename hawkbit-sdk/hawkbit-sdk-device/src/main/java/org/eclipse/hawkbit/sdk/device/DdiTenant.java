/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk.device;

import lombok.Getter;
import org.eclipse.hawkbit.sdk.Controller;
import org.eclipse.hawkbit.sdk.HawkbitClient;
import org.eclipse.hawkbit.sdk.Tenant;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An in-memory simulated DMF Tenant to hold the controller twins in
 * memory and be able to retrieve them again.
 */
public class DdiTenant {

    @Getter
    private final Tenant tenant;

    private final Map<String, DdiController> controllers = new ConcurrentHashMap<>();
    private final HawkbitClient hawkbitClient;

    public DdiTenant(final Tenant tenant, final HawkbitClient hawkbitClient) {
        this.tenant = tenant;
        this.hawkbitClient = hawkbitClient;
    }

    public void destroy() {
        controllers.values().forEach(DdiController::stop);
        controllers.clear();
    }

    public DdiController create(final Controller controller, final UpdateHandler updateHandler) {
        final DdiController ddiController = new DdiController(tenant, controller, updateHandler, hawkbitClient);
        controllers.put(controller.getControllerId(), ddiController);
        return ddiController;
    }

    public void remove(final String controllerId) {
        Optional.ofNullable(controllers.remove(controllerId)).ifPresent(DdiController::stop);
    }

    public Optional<DdiController> getController(final String controllerId) {
        return Optional.ofNullable(controllers.get(controllerId));
    }
}