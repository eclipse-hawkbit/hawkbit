/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import java.io.Serial;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.eclipse.hawkbit.repository.event.entity.EntityUpdatedEvent;
import org.eclipse.hawkbit.repository.model.Action;

/**
 * Defines the remote event of updated a {@link Action}.
 */
@NoArgsConstructor(force = true) // for serialization libs like jackson
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ActionUpdatedEvent extends AbstractActionEvent implements EntityUpdatedEvent {

    @Serial
    private static final long serialVersionUID = 3L;

    private final Action.Status status;
    private final boolean downloadOnly;

    @JsonIgnore
    public ActionUpdatedEvent(final Action action, final Long targetId, final Long rolloutId, final Long rolloutGroupId) {
        super(action, targetId, rolloutId, rolloutGroupId);
        this.status = action.getStatus();
        this.downloadOnly = action.isDownloadOnly();
    }
}