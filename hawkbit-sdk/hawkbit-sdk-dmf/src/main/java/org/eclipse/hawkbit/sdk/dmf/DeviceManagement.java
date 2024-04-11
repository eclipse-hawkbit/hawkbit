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

import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An in-memory simulated device management to hold the multi-tenants and controller twins in
 * memory and be able to retrieve them again.
 */
@Service
public class DeviceManagement {

    private final Set<String> tenants = new HashSet<>();

    private final Map<DeviceKey, DmfController> controllers = new ConcurrentHashMap<>();

    public DmfController add(final DmfController controller) {
        controllers.put(new DeviceKey(controller.getTenantId().toLowerCase(), controller.getControllerId()), controller);
        tenants.add(controller.getTenantId().toLowerCase());
        return controller;
    }

    public Set<String> getTenants() {
        return tenants;
    }

    public Collection<DmfController> getControllers() {
        return controllers.values();
    }

    public Optional<DmfController> getController(final String tenantId, final String controllerId) {
        return Optional.ofNullable(controllers.get(new DeviceKey(tenantId.toLowerCase(), controllerId)));
    }

    public void remove(final String tenant, final String id) {
        final DmfController controller = controllers.remove(new DeviceKey(tenant.toLowerCase(), id));
        if (controller != null) {
            controller.stop();
        }
    }

    /**
     * Clears all stored devices.
     */
    public void destroy() {
        controllers.values().forEach(DmfController::stop);
        controllers.clear();
        tenants.clear();
    }

    private record DeviceKey(String tenant, String id) {}
}