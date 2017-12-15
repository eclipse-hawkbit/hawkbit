/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.eclipse.hawkbit.repository.model.MetaData;

/**
 * Meta data for entities.
 *
 */
@MappedSuperclass
public abstract class AbstractJpaMetaData implements MetaData {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "meta_key", nullable = false, length = MetaData.KEY_MAX_SIZE, updatable = false)
    @Size(min = 1, max = MetaData.KEY_MAX_SIZE)
    @NotNull
    private String key;

    @Column(name = "meta_value", length = MetaData.VALUE_MAX_SIZE)
    @Size(max = MetaData.VALUE_MAX_SIZE)
    @Basic
    private String value;

    public AbstractJpaMetaData(final String key, final String value) {
        this.key = key;
        this.value = value;
    }

    public AbstractJpaMetaData() {
        // Default constructor needed for JPA entities
    }

    @Override
    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    @Override
    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
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
        if (!(this.getClass().isInstance(obj))) {
            return false;
        }
        final AbstractJpaMetaData other = (AbstractJpaMetaData) obj;
        if (key == null) {
            if (other.key != null) {
                return false;
            }
        } else if (!key.equals(other.key)) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }

}
