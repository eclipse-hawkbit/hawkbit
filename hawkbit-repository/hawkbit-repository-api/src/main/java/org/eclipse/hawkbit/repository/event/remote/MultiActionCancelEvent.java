/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionProperties;
import org.eclipse.hawkbit.repository.model.Target;

/**
 * Generic deployment event for the Multi-Assignments feature. The event extends
 * the {@link MultiActionEvent} and holds a list of controller IDs to identify
 * the targets which are affected by a deployment action and a list of
 * {@link ActionProperties} containing information's about the affected actions
 * as payload. This event is only published in case of an cancellation.
 */
public class MultiActionCancelEvent extends MultiActionEvent {

    private static final long serialVersionUID = 1L;

    private List<ActionProperties> actionProperties;

    /**
     * Default constructor.
     */
    public MultiActionCancelEvent() {
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
     *            the actions to be canceled
     */
    public MultiActionCancelEvent(String tenant, String applicationId, List<Action> a) {
        super(tenant, applicationId,
                a.stream().map(Action::getTarget).map(Target::getControllerId).distinct().collect(Collectors.toList()));
        actionProperties = a.stream().map(ActionProperties::new).collect(Collectors.toList());
    }

    public List<ActionProperties> getActionProperties() {
        return actionProperties;
    }

}
