/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import java.io.Serializable;
import java.util.Objects;

import org.eclipse.hawkbit.repository.Identifiable;

/**
 * Proxy entity representing the {@link Identifiable} entity, fetched from
 * backend.
 */
public abstract class ProxyIdentifiableEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * Constructor to initialize the id with null
     */
    protected ProxyIdentifiableEntity() {
        this.id = null;
    }

    /**
     * Constructor for ProxyIdentifiableEntity
     * @param id
     *          Id of entity
     */
    protected ProxyIdentifiableEntity(final Long id) {
        this.id = id;
    }

    /**
     * Gets the id
     *
     * @return id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the id
     *
     * @param id
     *         Id of entity
     */
    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        final ProxyIdentifiableEntity that = (ProxyIdentifiableEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }
}
