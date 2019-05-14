/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import java.util.List;

/**
 * Deployment event which implies that the Multi-Assignment feature is enabled.
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class MultiActionEvent extends DeploymentEvent {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor for serialization purposes.
     */
    public MultiActionEvent() {
        super();
    }

    /**
     * Constructs a Multi-Action event for the targets with the given controller
     * IDs.
     * 
     * @param tenant
     *            this event is scoped to
     * @param applicationId
     *            of the application
     * @param controllerIds
     *            of the targets this event refers to
     */
    public MultiActionEvent(final String tenant, final String applicationId, final List<String> controllerIds) {
        super(tenant, applicationId, controllerIds);
    }

}
