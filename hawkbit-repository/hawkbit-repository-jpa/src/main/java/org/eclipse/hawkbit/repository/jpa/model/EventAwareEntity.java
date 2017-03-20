/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.util.Collections;
import java.util.List;

import org.eclipse.persistence.descriptors.DescriptorEvent;

/**
 * Interfaces which can be implemented by entities to be called when the entity
 * should fire an event because the entity has been created, updated or deleted.
 */
public interface EventAwareEntity {

    /**
     * Fired for the Entity creation.
     * 
     * @param descriptorEvent
     */
    void fireCreateEvent(DescriptorEvent descriptorEvent);

    /**
     * Fired for the Entity updation.
     * 
     * @param descriptorEvent
     */
    void fireUpdateEvent(DescriptorEvent descriptorEvent);

    /**
     * Fired for the Entity deletion.
     * 
     * @param descriptorEvent
     */
    void fireDeleteEvent(DescriptorEvent descriptorEvent);

    /**
     * @return list of entity fields that if the only changed fields prevents
     *         {@link #fireUpdateEvent(DescriptorEvent)} call.
     */
    default List<String> getUpdateIgnoreFields() {
        return Collections.emptyList();
    }
}
