/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.model;

/**
 * Interfaces which can be implemented by entities to be called when the entity should fire an event because the entity has been created,
 * updated or deleted.
 */
public interface EventAwareEntity {

    /**
     * Fired for the Entity creation.
     */
    void fireCreateEvent();

    /**
     * Fired for the Entity update.
     */
    void fireUpdateEvent();

    /**
     * Fired for the Entity deletion.
     */
    void fireDeleteEvent();
}