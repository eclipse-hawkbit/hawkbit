/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.entitiy;

import org.eclipse.hawkbit.repository.event.TenantAwareEvent;

/**
 * Interface to indicate an entity event which contains at least an entity id.
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
