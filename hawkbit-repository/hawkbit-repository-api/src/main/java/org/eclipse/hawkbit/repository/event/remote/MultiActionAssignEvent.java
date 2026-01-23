/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote;

import java.io.Serial;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.model.Action;

/**
 * Generic deployment event for the Multi-Assignments feature. The event extends
 * the {@link MultiActionEvent} and holds a list of controller IDs to identify
 * the targets which are affected by a deployment action and a list of
 * actionIds containing the identifiers of the affected actions
 * as payload. This event is only published in case of an assignment.
 */
@NoArgsConstructor
public class MultiActionAssignEvent extends MultiActionEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param tenant tenant the event is scoped to
     * @param actions the actions of the deployment action
     */
    @JsonIgnore
    public MultiActionAssignEvent(final String tenant, final List<Action> actions) {
        super(tenant, actions);
    }
}