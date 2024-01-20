/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.util.Objects;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
