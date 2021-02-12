/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import org.eclipse.hawkbit.repository.Identifiable;
import java.io.Serializable;

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
    public ProxyIdentifiableEntity() {
        this.id = null;
    }

    /**
     * Constructor for ProxyIdentifiableEntity
     * @param id
     *          Id of entity
     */
    public ProxyIdentifiableEntity(final Long id) {
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
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ProxyIdentifiableEntity other = (ProxyIdentifiableEntity) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }
}
