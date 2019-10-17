/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.repository.event.remote;

/**
 * An event that can be generally used to communicate within the microservices
 * using the internal event bus.
 */
public class GenericRemoteEvent extends RemoteTenantAwareEvent {

    private static final long serialVersionUID = 1L;
    private Class<?> payloadType;
    private Object payload;

    /**
     * Default constructor.
     */
    public GenericRemoteEvent() {
        // for serialization libs like jackson
    }

    /**
     * constructor
     * 
     * @param tenant
     *            the tenant
     * @param payloadType
     *            the class type of the payload
     * @param payload
     *            the content of the payload
     * @param applicationId
     *            the origin application id
     */
    public GenericRemoteEvent(final String tenant, final Class<?> payloadType, final Object payload,
            final String applicationId) {
        super(payload, tenant, applicationId);
        this.payloadType = payloadType;
        this.payload = payload;
    }

    /**
     * Gets the class type of the payload associated with this event
     * 
     * @return type of the payload
     */
    public Class<?> getPayloadType() {
        return payloadType;
    }

    /**
     * Gets the payload associated with this event
     * 
     * @return the payload
     */
    public Object getPayload() {
        return payload;
    }
}
