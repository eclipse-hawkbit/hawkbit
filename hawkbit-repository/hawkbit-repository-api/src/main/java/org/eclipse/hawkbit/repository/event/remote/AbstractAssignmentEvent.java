/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Abstract class providing information about an assignment.
 */
public abstract class AbstractAssignmentEvent extends RemoteTenantAwareEvent {

    private static final long serialVersionUID = 1L;

    private final Map<String, ActionProperties> actions = new HashMap<>();

    /**
     * Default constructor.
     */
    protected AbstractAssignmentEvent() {
        // for serialization libs like jackson
    }

    protected AbstractAssignmentEvent(final Object source, final Action a, final String applicationId) {
        super(source, a.getTenant(), applicationId);
        actions.put(a.getTarget().getControllerId(), new ActionProperties(a));
    }

    protected AbstractAssignmentEvent(final Object source, final String tenant, final List<Action> a,
            final String applicationId) {
        super(source, tenant, applicationId);
        actions.putAll(a.stream()
                .collect(Collectors.toMap(action -> action.getTarget().getControllerId(), ActionProperties::new)));
    }

    public Map<String, ActionProperties> getActions() {
        return actions;
    }

    public Optional<ActionProperties> getActionPropertiesForController(final String controllerId) {
        return Optional.ofNullable(actions.get(controllerId));
    }

}
