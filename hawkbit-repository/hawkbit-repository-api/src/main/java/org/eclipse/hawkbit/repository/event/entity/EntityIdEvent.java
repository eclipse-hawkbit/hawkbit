/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.entity;

import org.eclipse.hawkbit.repository.event.TenantAwareEvent;

/**
 * Marker interface to indicate an event which contains at least an entity id.
 */
public interface EntityIdEvent extends TenantAwareEvent {

    /**
     * @return the class of the entity of this event.
     */
    String getEntityClass();

    /**
     * @return the ID of the entity of this event.
     */
    Long getEntityId();
}