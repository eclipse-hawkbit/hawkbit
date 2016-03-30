/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * A JPA entity which stores the tenant specific configuration.
 *
 */
@Entity
@Table(name = "sp_tenant_configuration", uniqueConstraints = @UniqueConstraint(columnNames = { "conf_key",
        "tenant" }, name = "uk_tenant_key"))
public class TenantConfiguration extends TenantAwareBaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "conf_key", length = 128)
    private String key;

    @Column(name = "conf_value", length = 512)
    @Basic
    private String value;

    /**
     * JPA default constructor.
     */
    public TenantConfiguration() {
        // JPA default constructor.
    }

    /**
     * @param key
     *            the key of this configuration
     * @param value
     *            the value of this configuration
     */
    public TenantConfiguration(final String key, final String value) {
        this.key = key;
        this.value = value;

    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key
     *            the key to set
     */
    public void setKey(final String key) {
        this.key = key;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value
     *            the value to set
     */
    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + this.getClass().getName().hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof TenantConfiguration)) {
            return false;
        }

        return true;
    }

}
