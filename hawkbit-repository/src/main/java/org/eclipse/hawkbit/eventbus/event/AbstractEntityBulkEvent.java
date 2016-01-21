/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.eventbus.event;

import java.util.Arrays;
import java.util.List;

import org.eclipse.hawkbit.repository.model.BaseEntity;

/**
 * 
 * A abstract typesafe bulkevent which contains all changed base entities.
 * 
 * @param <E>
 */
public abstract class AbstractEntityBulkEvent<E extends BaseEntity> implements EntityBulkEvent<E> {

    private static final long serialVersionUID = 1L;

    private List<E> entities;

    private String tenant;

    /**
     * Constructor.
     * 
     * @param tenant
     *            the tenant
     * @param entities
     *            the changed entities
     */
    public AbstractEntityBulkEvent(final String tenant, final List<E> entities) {
        this.entities = entities;
        this.tenant = tenant;
    }

    /**
     * Constructor.
     * 
     * @param tenant
     *            the tenant
     * @param entitiy
     *            the changed entity
     */
    public AbstractEntityBulkEvent(final String tenant, final E entitiy) {
        this(tenant, Arrays.asList(entitiy));
    }

    @Override
    public List<E> getEntities() {
        return entities;
    }

    @Override
    public long getRevision() {
        return -1;
    }

    @Override
    public String getTenant() {
        return tenant;
    }

}
