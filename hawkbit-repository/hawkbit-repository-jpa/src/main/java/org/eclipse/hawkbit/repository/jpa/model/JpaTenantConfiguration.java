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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.event.remote.TenantConfigurationDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TenantConfigurationCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TenantConfigurationUpdatedEvent;
import org.eclipse.hawkbit.repository.model.TenantConfiguration;

/**
 * A JPA entity which stores the tenant specific configuration.
 */
@NoArgsConstructor // Default constructor for JPA
@Setter
@Getter
@Entity
@Table(name = "sp_tenant_configuration")
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

    public JpaTenantConfiguration(final String key, final String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public void fireCreateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new TenantConfigurationCreatedEvent(this));
    }

    @Override
    public void fireUpdateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new TenantConfigurationUpdatedEvent(this));
    }

    @Override
    public void fireDeleteEvent() {
        EventPublisherHolder.getInstance().getEventPublisher()
                .publishEvent(new TenantConfigurationDeletedEvent(getTenant(), getId(), getClass(), getKey(), getValue()));
    }
}