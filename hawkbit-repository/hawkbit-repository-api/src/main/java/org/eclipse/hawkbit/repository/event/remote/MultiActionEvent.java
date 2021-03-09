/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Target;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generic deployment event for the Multi-Assignments feature. The event payload
 * holds a list of controller IDs identifying the targets which are affected by
 * a deployment action (e.g. a software assignment (update) or a cancellation of
 * an update).
 */
public abstract class MultiActionEvent extends RemoteTenantAwareEvent implements Iterable<String> {

    private static final long serialVersionUID = 1L;

    private final List<String> controllerIds = new ArrayList<>();
    private final List<Long> actionIds = new ArrayList<>();

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
     * @param actions
     *            the actions involved
     */
    public MultiActionEvent(String tenant, String applicationId, List<Action> actions) {
        super(applicationId, tenant, applicationId);
        this.controllerIds.addAll(getControllerIdsFromActions(actions));
        this.actionIds.addAll(getIdsFromActions(actions));
    }

    public List<String> getControllerIds() {
        return controllerIds;
    }

    @Override
    public Iterator<String> iterator() {
        return controllerIds.iterator();
    }

    public List<Long> getActionIds() {
        return actionIds;
    }

    private static List<String> getControllerIdsFromActions(final List<Action> actions) {
        return actions.stream().map(Action::getTarget).map(Target::getControllerId).distinct()
                .collect(Collectors.toList());
    }

    private static List<Long> getIdsFromActions(final List<Action> actions) {
        return actions.stream().map(Identifiable::getId).collect(Collectors.toList());
    }

}
