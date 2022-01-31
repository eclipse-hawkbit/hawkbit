/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.util.Objects;

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

    protected AbstractJpaMetaData(final String key, final String value) {
        this.key = key;
        this.value = value;
    }

    protected AbstractJpaMetaData() {
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
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        final AbstractJpaMetaData that = (AbstractJpaMetaData) o;
        return Objects.equals(key, that.key) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }
}
