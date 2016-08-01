/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import org.eclipse.persistence.descriptors.DescriptorEvent;

public interface EventAwareEntity {

    /**
     * Fired for the Entity creation.
     * @param descriptorEvent
     */
    void fireCreateEvent(DescriptorEvent descriptorEvent);

    /**
     * Fired for the Entity updation.
     * @param descriptorEvent
     */
    void fireUpdateEvent(DescriptorEvent descriptorEvent);

    /**
     * Fired for the Entity deletion.
     * @param descriptorEvent
     */
    void fireDeleteEvent(DescriptorEvent descriptorEvent);
}
