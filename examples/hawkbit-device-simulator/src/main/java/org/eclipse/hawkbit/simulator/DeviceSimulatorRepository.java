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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * An in-memory simulated device repository to hold the simulated device in
 * memory and be able to retrieve them again.
 * 
 * @author Michael Hirsch
 *
 */
@Service
public class DeviceSimulatorRepository {

    private final Map<DeviceKey, AbstractSimulatedDevice> devices = new ConcurrentHashMap<>();

    private final Set<String> tenants = new HashSet<>();

    /**
     * Adds a simulated device to the repository.
     * 
     * @param simulatedDevice
     *            the device to add
     * @return the device which has been added to the repository
     */
    public AbstractSimulatedDevice add(final AbstractSimulatedDevice simulatedDevice) {
        devices.put(new DeviceKey(simulatedDevice.getTenant().toLowerCase(), simulatedDevice.getId()), simulatedDevice);
        tenants.add(simulatedDevice.getTenant().toLowerCase());
        return simulatedDevice;
    }

    /**
     * @return all simulated devices
     */
    public Collection<AbstractSimulatedDevice> getAll() {
        return devices.values();
    }

    /**
     * Retrieves a single simulated devices or {@code null} if device does not
     * exists.
     * 
     * @param tenant
     *            the tenant of the simulated device
     * @param id
     *            the ID of the device
     * @return a simulated device from the repository or {@code null} if device
     *         does not exixts.
     */
    public AbstractSimulatedDevice get(final String tenant, final String id) {
        return devices.get(new DeviceKey(tenant.toLowerCase(), id));
    }

    /**
     * Removes a device from the simulation.
     * 
     * @param tenant
     *            the tenant of the simulated device
     * @param id
     *            the ID of the device
     * @return the simulated device or <code>null</code> if it was not in the
     *         repository
     */
    public AbstractSimulatedDevice remove(final String tenant, final String id) {
        return devices.remove(new DeviceKey(tenant.toLowerCase(), id));
    }

    public Set<String> getTenants() {
        return tenants;
    }

    /**
     * Clears all stored devices.
     */
    public void clear() {
        devices.values().forEach(AbstractSimulatedDevice::clean);
        devices.clear();
        tenants.clear();
    }

    private static final class DeviceKey {
        private final String tenant;
        private final String id;

        private DeviceKey(final String tenant, final String id) {
            this.tenant = tenant;
            this.id = id;
        }

        @Override
        public int hashCode() {// NOSONAR - as this is generated
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            result = prime * result + ((tenant == null) ? 0 : tenant.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {// NOSONAR - as this is
                                                 // generated
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final DeviceKey other = (DeviceKey) obj;
            if (id == null) {
                if (other.id != null) {
                    return false;
                }
            } else if (!id.equals(other.id)) {
                return false;
            }
            if (tenant == null) {
                if (other.tenant != null) {
                    return false;
                }
            } else if (!tenant.equals(other.tenant)) {
                return false;
            }
            return true;
        }
    }
}
