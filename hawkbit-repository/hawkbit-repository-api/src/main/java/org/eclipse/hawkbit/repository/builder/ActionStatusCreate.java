/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.builder;

import java.util.Collection;

import jakarta.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.BaseEntity;

/**
 * Builder to create a new {@link ActionStatus} entry. Defines all fields that
 * can be set at creation time. Other fields are set by the repository
 * automatically, e.g. {@link BaseEntity#getCreatedAt()}.
 */
public interface ActionStatusCreate {

    /**
     * @param status {@link ActionStatus#getStatus()}
     * @return updated {@link ActionStatusCreate} object
     */
    ActionStatusCreate status(@NotNull Status status);

    /**
     * @param occurredAt for {@link ActionStatus#getOccurredAt()}
     * @return updated {@link ActionStatusCreate} object
     */
    ActionStatusCreate occurredAt(long occurredAt);

    ActionStatusCreate code(int code);

    /**
     * @param messages for {@link ActionStatus#getMessages()}
     * @return updated {@link ActionStatusCreate} object
     */
    ActionStatusCreate messages(Collection<String> messages);

    /**
     * @param message for {@link ActionStatus#getMessages()}
     * @return updated {@link ActionStatusCreate} object
     */
    ActionStatusCreate message(String message);

    /**
     * @return peek on current state of {@link ActionStatus} in the builder
     */
    ActionStatus build();
}