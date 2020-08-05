/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Target;
import org.springframework.hateoas.Identifiable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generic deployment event for the Multi-Assignments feature. The event extends
 * the {@link MultiActionEvent} and holds a list of controller IDs to identify
 * the targets which are affected by a deployment action and a list of
 * actionIds containing the identifiers of the affected actions
 * as payload. This event is only published in case of an assignment.
 */
public class MultiActionAssignEvent extends MultiActionEvent {

    private static final long serialVersionUID = 1L;
    
    private List<Long> actionIds;

    /**
     * Default constructor.
     */
    public MultiActionAssignEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor.
     *
     * @param tenant
     *            tenant the event is scoped to
     * @param applicationId
     *            the application id
     * @param a
     *            the actions of the deployment action
     */
    public MultiActionAssignEvent(String tenant, String applicationId, List<Action> a) {
        super(tenant, applicationId,
                a.stream().map(Action::getTarget).map(Target::getControllerId).distinct().collect(Collectors.toList()));
        actionIds = a.stream().map(Identifiable::getId).collect(Collectors.toList());
    }

    public List<Long> getActionIds() {
        return actionIds;
    }

}
