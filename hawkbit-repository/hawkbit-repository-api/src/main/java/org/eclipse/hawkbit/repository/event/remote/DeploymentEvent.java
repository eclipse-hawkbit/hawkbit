/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * TenantAwareEvent that gets sent when a distribution set gets assigned to a
 * target.
 */
public class DeploymentEvent extends RemoteTenantAwareEvent implements Iterable<String> {

    private static final long serialVersionUID = 1L;

    private final List<String> controllerIds = new ArrayList<>();

    /**
     * Default constructor.
     */
    public DeploymentEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor.
     * 
     * @param tenant
     *            of the event
     * @param applicationId
     *            the application id.
     * @param controllerIds
     *            the controller IDs
     */
    public DeploymentEvent(final String tenant, final String applicationId,
            final List<String> controllerIds) {
        super(applicationId, tenant, applicationId);
        this.controllerIds.addAll(controllerIds);
    }

    public List<String> getControllerIds() {
        return controllerIds;
    }

    @Override
    public Iterator<String> iterator() {
        return controllerIds.iterator();
    }

}
