/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
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
 * Generic deployment event for the Multi-Assignments feature. The event payload
 * holds a list of controller IDs identifying the targets which are affected by
 * a deployment action (e.g. a software assignment (update) or a cancellation of
 * an update).
 */
public class MultiActionEvent extends RemoteTenantAwareEvent implements Iterable<String> {

    private static final long serialVersionUID = 1L;

    private final List<String> controllerIds = new ArrayList<>();

    /**
     * Default constructor.
     */
    public MultiActionEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor.
     * 
     * @param tenant
     *            tenant the event is scoped to
     * @param applicationId
     *            the application id
     * @param controllerIds
     *            the controller IDs of the affected targets
     */
    public MultiActionEvent(final String tenant, final String applicationId, final List<String> controllerIds) {
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