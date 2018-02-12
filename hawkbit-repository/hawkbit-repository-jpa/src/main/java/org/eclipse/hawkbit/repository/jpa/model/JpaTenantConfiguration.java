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
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.eclipse.hawkbit.repository.model.TenantConfiguration;

/**
 * A JPA entity which stores the tenant specific configuration.
 *
 */
@Entity
@Table(name = "sp_tenant_configuration", uniqueConstraints = @UniqueConstraint(columnNames = { "conf_key",
        "tenant" }, name = "uk_tenant_key"))
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaTenantConfiguration extends AbstractJpaTenantAwareBaseEntity implements TenantConfiguration {
    private static final long serialVersionUID = 1L;

    @Column(name = "conf_key", length = 128, nullable = false, updatable = false)
    @Size(max = 128)
    @NotEmpty
    private String key;

    @Column(name = "conf_value", length = 512, nullable = false)
    @Basic
    @Size(max = 512)
    @NotNull
    private String value;

    /**
     * JPA default constructor.
     */
    public JpaTenantConfiguration() {
        // JPA default constructor.
    }

    /**
     * @param key
     *            the key of this configuration
     * @param value
     *            the value of this configuration
     */
    public JpaTenantConfiguration(final String key, final String value) {
        this.key = key;
        this.value = value;

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

}
