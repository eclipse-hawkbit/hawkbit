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
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.model.Action;

/**
 * Event that gets sent when the assignment of a distribution set to a target gets canceled.
 */
@NoArgsConstructor(access = AccessLevel.PUBLIC) // for serialization libs like jackson
public class CancelTargetAssignmentEvent extends AbstractAssignmentEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    public CancelTargetAssignmentEvent(final Action a, final String applicationId) {
        super(applicationId, a, applicationId);
    }

    public CancelTargetAssignmentEvent(final String tenant, final List<Action> a, final String applicationId) {
        super(applicationId, tenant, a, applicationId);

    }
}