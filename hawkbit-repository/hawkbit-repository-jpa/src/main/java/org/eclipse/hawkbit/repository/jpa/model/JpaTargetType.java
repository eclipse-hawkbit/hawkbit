/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.io.Serial;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import lombok.NoArgsConstructor;
import lombok.ToString;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.event.remote.TargetTypeDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTypeCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTypeUpdatedEvent;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetType;

/**
 * A target type defines which distribution set types can or have to be {@link Target}.
 */
@NoArgsConstructor // Default constructor for JPA
@ToString(callSuper = true)
@Entity
@Table(name = "sp_target_type")
@SuppressWarnings("java:S2160") // the super class equals/hashcode shall be used
public class JpaTargetType extends AbstractJpaTypeEntity implements TargetType, EventAwareEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToMany(targetEntity = JpaDistributionSetType.class)
    @JoinTable(
            name = "sp_target_type_ds_type",
            joinColumns = { @JoinColumn(name = "target_type", nullable = false) },
            inverseJoinColumns = { @JoinColumn(name = "distribution_set_type", nullable = false) }
    )
    private Set<DistributionSetType> distributionSetTypes = new HashSet<>();

    public JpaTargetType(final String key, final String name, final String description, final String colour) {
        super(name, description, key, colour);
    }

    /**
     * Adds a compatible distribution set type to this target type.
     *
     * @param dsSetType Distribution set type to add
     * @return Target type
     */
    public JpaTargetType addCompatibleDistributionSetType(final DistributionSetType dsSetType) {
        distributionSetTypes.add(dsSetType);
        return this;
    }

    @Override
    public Set<DistributionSetType> getDistributionSetTypes() {
        return Collections.unmodifiableSet(distributionSetTypes);
    }

    /**
     * Remove a compatible distribution set type from this target type.
     *
     * @param dsTypeId Distribution set type ID
     * @return Target type
     */
    public void removeDistributionSetType(final Long dsTypeId) {
        distributionSetTypes.stream()
                .filter(element -> element.getId().equals(dsTypeId))
                .findAny()
                .ifPresent(distributionSetTypes::remove);
    }

    @Override
    public void fireCreateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new TargetTypeCreatedEvent(this));
    }

    @Override
    public void fireUpdateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new TargetTypeUpdatedEvent(this));
    }

    @Override
    public void fireDeleteEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new TargetTypeDeletedEvent(getTenant(), getId(), getClass()));
    }
}