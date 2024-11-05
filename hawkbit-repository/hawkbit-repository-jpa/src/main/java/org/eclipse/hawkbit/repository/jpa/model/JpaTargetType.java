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

import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.NoArgsConstructor;
import lombok.ToString;
import org.eclipse.hawkbit.repository.event.remote.TargetTypeDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTypeCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTypeUpdatedEvent;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.eclipse.persistence.descriptors.DescriptorEvent;

/**
 * A target type defines which distribution set types can or have to be
 * {@link Target}.
 */
@NoArgsConstructor // default public constructor for JPA
@ToString(callSuper = true)
@Entity
@Table(name = "sp_target_type", indexes = {
        @Index(name = "sp_idx_target_type_prim", columnList = "tenant,id") }, uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "tenant" }, name = "uk_target_type_name") })
public class JpaTargetType extends AbstractJpaTypeEntity implements TargetType, EventAwareEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @CascadeOnDelete
    @ManyToMany(targetEntity = JpaDistributionSetType.class)
    @JoinTable(name = "sp_target_type_ds_type_relation", joinColumns = {
            @JoinColumn(name = "target_type", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_target_type_relation_target_type")) }, inverseJoinColumns = {
            @JoinColumn(name = "distribution_set_type", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_target_type_relation_ds_type")) })
    private Set<DistributionSetType> distributionSetTypes;

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
        if (distributionSetTypes == null) {
            distributionSetTypes = new HashSet<>();
        }

        distributionSetTypes.add(dsSetType);

        return this;
    }

    @Override
    public Set<DistributionSetType> getCompatibleDistributionSetTypes() {
        if (distributionSetTypes == null) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(distributionSetTypes);
    }

    /**
     * Remove a compatible distribution set type from this target type.
     *
     * @param dsTypeId Distribution set type ID
     * @return Target type
     */
    public JpaTargetType removeDistributionSetType(final Long dsTypeId) {
        if (distributionSetTypes == null) {
            return this;
        }

        distributionSetTypes.stream()
                .filter(element -> element.getId().equals(dsTypeId))
                .findAny()
                .ifPresent(distributionSetTypes::remove);

        return this;
    }

    @Override
    public void fireCreateEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(
                new TargetTypeCreatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireUpdateEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(
                new TargetTypeUpdatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireDeleteEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new TargetTypeDeletedEvent(
                getTenant(), getId(), getClass(), EventPublisherHolder.getInstance().getApplicationId()));
    }
}