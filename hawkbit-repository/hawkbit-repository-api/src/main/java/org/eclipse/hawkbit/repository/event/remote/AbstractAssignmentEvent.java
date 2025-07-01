/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote;

import java.io.Serial;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionProperties;

/**
 * Abstract class providing information about an assignment.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for serialization libs like jackson
public abstract class AbstractAssignmentEvent extends RemoteTenantAwareEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Map<String, ActionProperties> actions = new HashMap<>();

    protected AbstractAssignmentEvent(final Action a) {
        super(a.getTenant(), null);
        actions.put(a.getTarget().getControllerId(), new ActionProperties(a));
    }

    protected AbstractAssignmentEvent(final String tenant, final Object source, final List<Action> a) {
        super(tenant, source);
        actions.putAll(a.stream()
                .collect(Collectors.toMap(action -> action.getTarget().getControllerId(), ActionProperties::new)));
    }

    public Optional<ActionProperties> getActionPropertiesForController(final String controllerId) {
        return Optional.ofNullable(actions.get(controllerId));
    }
}