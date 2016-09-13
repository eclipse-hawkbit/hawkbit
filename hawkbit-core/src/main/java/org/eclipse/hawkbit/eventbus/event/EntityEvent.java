/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.eventbus.event;

/**
 * An event interface which declares event types that an entity has been
 * changed. {@link EntityEvent}s should not implement {@link DistributedEvent}
 * due all {@link EntityEvent}s will be distributed to other nodes.
 *
 * Retrieving an {@link EntityEvent} on a different node the entity will be load
 * lazy.
 *
 *
 *
 *
 */
public interface EntityEvent<I> extends Event {

    /**
     * A typesafe way to retrieve the entity from the event, which might be
     * loaded lazy in case the event has been distributed from another node.
     * 
     * @param entityClass
     *            the class of the entity to retrieve
     * @return the entity might be lazy loaded. Might be {@code null} in case
     *         the entity e.g. is queried lazy on a different node and has been
     *         already deleted from the database
     * @throws ClassCastException
     *             in case a wrong entity class is given for this event
     */
    <E> E getEntity(Class<E> entityClass);

    /**
     * An unsafe way to retrieve the entity from this event which might be
     * loaded lazy in case the event has been distributed from another node.
     * 
     * @return the entity might be lazy loaded. Might be {@code null} in case
     *         the entity e.g. is queried lazy on a different node and has been
     *         already deleted from the database
     */
    Object getEntity();

    /**
     * TODO:
     */
    I getEntityId();
}
