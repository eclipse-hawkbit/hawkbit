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

import java.io.Serial;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.eclipse.hawkbit.repository.event.remote.TenantConfigurationDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TenantConfigurationCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TenantConfigurationUpdatedEvent;
import org.eclipse.hawkbit.repository.model.TenantConfiguration;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;

/**
 * A JPA entity which stores the tenant specific configuration.
 */
@Entity
@Table(name = "sp_tenant_configuration", uniqueConstraints = @UniqueConstraint(columnNames = { "conf_key",
        "tenant" }, name = "uk_tenant_key"))
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for sub entities
@SuppressWarnings("squid:S2160")
public class JpaTenantConfiguration extends AbstractJpaTenantAwareBaseEntity implements TenantConfiguration, EventAwareEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "conf_key", length = TenantConfiguration.KEY_MAX_SIZE, nullable = false, updatable = false)
    @Size(min = 1, max = TenantConfiguration.KEY_MAX_SIZE)
    @NotNull
    private String key;

    @Column(name = "conf_value", length = TenantConfiguration.VALUE_MAX_SIZE, nullable = false)
    @Basic
    @Size(max = TenantConfiguration.VALUE_MAX_SIZE)
    @NotNull
    private String value;

    /**
     * JPA default constructor.
     */
    public JpaTenantConfiguration() {
        // JPA default constructor.
    }

    /**
     * @param key the key of this configuration
     * @param value the value of this configuration
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

    @Override
    public void fireCreateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(
                new TenantConfigurationCreatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireUpdateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(
                new TenantConfigurationUpdatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireDeleteEvent() {
        EventPublisherHolder.getInstance().getEventPublisher()
                .publishEvent(new TenantConfigurationDeletedEvent(getTenant(), getId(), getKey(), getValue(),
                        getClass(), EventPublisherHolder.getInstance().getApplicationId()));
    }
}
