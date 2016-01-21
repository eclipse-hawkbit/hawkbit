/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.eventbus.event;

import java.io.Serializable;
import java.util.List;

import org.eclipse.hawkbit.repository.model.BaseEntity;

/**
 * An event interface which declares event types that an entities has been
 * changed.
 *
 * @param <E>
 *            the entity type
 */
public interface EntityBulkEvent<E extends BaseEntity> extends Serializable, Event {

    /**
     * A typesafe way to retrieve the the entities from the event, which might
     * be loaded lazy in case the event has been distributed from another node.
     * 
     * @return the entities might be lazy loaded. Might be {@code null} in case
     *         the entity e.g. is queried lazy on a different node and has been
     *         already deleted from the database
     */
    List<E> getEntities();
}
