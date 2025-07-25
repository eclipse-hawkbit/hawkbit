/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote;

import java.io.Serial;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.eclipse.hawkbit.repository.event.entity.EntityDeletedEvent;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;

/**
 * Grouped event for {@link TargetDeletedEvent}. Event that needs single processing
 */
public class GroupedTargetDeletedEvent extends AbstractGroupedRemoteEvent<TargetDeletedEvent> {

    @Serial
    private static final long serialVersionUID = 1L;


    @JsonCreator
    public GroupedTargetDeletedEvent(@JsonProperty("payload") final TargetDeletedEvent remoteEvent) {
        super(remoteEvent);
    }
}