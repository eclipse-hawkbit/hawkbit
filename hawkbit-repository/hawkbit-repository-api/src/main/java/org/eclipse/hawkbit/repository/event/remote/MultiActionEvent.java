/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Target;

/**
 * Generic deployment event for the Multi-Assignments feature. The event payload holds a list of controller IDs identifying the targets which
 * are affected by a deployment action (e.g. a software assignment (update) or a cancellation of an update).
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for serialization libs like jackson
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public abstract class MultiActionEvent extends RemoteTenantAwareEvent implements Iterable<String> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<String> controllerIds = new ArrayList<>();
    private final List<Long> actionIds = new ArrayList<>();

    protected MultiActionEvent(final String tenant, final List<Action> actions) {
        super(tenant, null);
        this.controllerIds.addAll(getControllerIdsFromActions(actions));
        this.actionIds.addAll(getIdsFromActions(actions));
    }

    @Override
    @NonNull
    public Iterator<String> iterator() {
        return controllerIds.iterator();
    }

    private static List<String> getControllerIdsFromActions(final List<Action> actions) {
        return actions.stream().map(Action::getTarget).map(Target::getControllerId).distinct().toList();
    }

    private static List<Long> getIdsFromActions(final List<Action> actions) {
        return actions.stream().map(Identifiable::getId).toList();
    }
}