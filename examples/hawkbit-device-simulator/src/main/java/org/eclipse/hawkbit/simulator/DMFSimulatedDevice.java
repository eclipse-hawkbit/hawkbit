/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator;

/**
 * An simulated device using the DMF API of the hawkbit update server.
 * 
 * @author Michael Hirsch
 *
 */
public class DMFSimulatedDevice extends AbstractSimulatedDevice {

    /**
     * @param id
     *            the ID of the device
     * @param tenant
     *            the tenant of the simulated device
     */
    public DMFSimulatedDevice(final String id, final String tenant) {
        super(id, tenant, Protocol.DMF_AMQP);
    }

}
