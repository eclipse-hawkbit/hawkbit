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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.event.remote.SoftwareModuleTypeDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleTypeCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleTypeUpdatedEvent;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * Type of software modules.
 */
@NoArgsConstructor // Default constructor for JPA
@Getter
@Entity
@Table(name = "sp_software_module_type")
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for sub entities
@SuppressWarnings("squid:S2160")
public class JpaSoftwareModuleType extends AbstractJpaTypeEntity implements SoftwareModuleType, EventAwareEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Setter(value = lombok.AccessLevel.PRIVATE) // used via reflection
    @Column(name = "min_artifacts", nullable = false)
    @Min(0)
    private int minArtifacts;

    @Setter(value = lombok.AccessLevel.PRIVATE) // used via reflection
    @Column(name = "max_ds_assignments", nullable = false)
    @Min(1)
    private int maxAssignments;

    @Setter
    @Column(name = "deleted")
    private boolean deleted;

    public JpaSoftwareModuleType(final String key, final String name, final String description, final int maxAssignments) {
        this(key, name, description, maxAssignments, null);
    }

    public JpaSoftwareModuleType(final String key, final String name, final String description, final int maxAssignments, final String colour) {
        super(name, description, key, colour);
        this.maxAssignments = maxAssignments;
    }

    @Override
    public String toString() {
        return "SoftwareModuleType [key=" + getKey() + ", getName()=" + getName() + ", getId()=" + getId() + "]";
    }

    @Override
    public void fireCreateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new SoftwareModuleTypeCreatedEvent(this));
    }

    @Override
    public void fireUpdateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new SoftwareModuleTypeUpdatedEvent(this));
    }

    @Override
    public void fireDeleteEvent() {
        EventPublisherHolder.getInstance().getEventPublisher()
                .publishEvent(new SoftwareModuleTypeDeletedEvent(getTenant(), getId(), getClass()));
    }
}