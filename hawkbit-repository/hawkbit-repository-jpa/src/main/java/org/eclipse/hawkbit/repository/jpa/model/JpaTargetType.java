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

import org.eclipse.hawkbit.repository.event.remote.TargetTypeDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTypeCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTypeUpdatedEvent;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.eclipse.persistence.descriptors.DescriptorEvent;

import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.io.Serial;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A target type defines which distribution set types can or have to be
 * {@link Target}.
 *
 */
@Entity
@Table(name = "sp_target_type", indexes = {
        @Index(name = "sp_idx_target_type_prim", columnList = "tenant,id") }, uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "tenant" }, name = "uk_target_type_name")})
public class JpaTargetType extends AbstractJpaTypeEntity implements TargetType, EventAwareEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @CascadeOnDelete
    @ManyToMany(targetEntity = JpaDistributionSetType.class)
    @JoinTable(name = "sp_target_type_ds_type_relation", joinColumns = {
            @JoinColumn(name = "target_type", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_target_type_relation_target_type"))}, inverseJoinColumns = {
            @JoinColumn(name = "distribution_set_type", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_target_type_relation_ds_type"))})
    private Set<DistributionSetType> distributionSetTypes;

    @OneToMany(targetEntity = JpaTarget.class, mappedBy = "targetType", fetch = FetchType.LAZY)
    private Set<Target> targets;

    /**
     * Constructor
     */
    public JpaTargetType() {
        // default public constructor for JPA
    }

    /**
     * Constructor, legacy support where <code>key</code> is set to passed <code>name</code>.
     *
     * @deprecated will be removed
     *
     * @param name
     *            of the type
     * @param description
     *            of the type
     * @param colour
     *            of the type
     */
    @Deprecated(forRemoval = true)
    public JpaTargetType(final String name, final String description, final String colour) {
        this(name, name, description, colour);
    }

    /**
     * Constructor.
     *
     * @param key
     *            of the type
     * @param name
     *            of the type
     * @param description
     *            of the type
     * @param colour
     *            of the type. It will be null by default
     */
    public JpaTargetType(final String key, final String name, final String description, final String colour) {
        super(name, description, key, colour);
    }

    /**
     * @param dsSetType
     *          Distribution set type
     * @return Target type
     */
    public void addCompatibleDistributionSetType(final DistributionSetType dsSetType) {
        if (distributionSetTypes == null) {
            distributionSetTypes = new HashSet<>();
        }

        distributionSetTypes.add(dsSetType);
    }

    /**
     * @param dsTypeId
     *          Distribution set type ID
     * @return Target type
     */
    public JpaTargetType removeDistributionSetType(final Long dsTypeId) {
        if (distributionSetTypes == null) {
            return this;
        }
        distributionSetTypes.stream().filter(element -> element.getId().equals(dsTypeId)).findAny()
                .ifPresent(distributionSetTypes::remove);
        return this;
    }

    @Override
    public Set<DistributionSetType> getCompatibleDistributionSetTypes() {

        if (distributionSetTypes == null) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(distributionSetTypes);
    }

    @Override
    public Set<Target> getTargets() {
        return targets;
    }

    @Override
    public String toString() {
        return "TargetType [key=" + getKey() + ", isDeleted()=" + isDeleted() + ", getId()=" + getId() + "]";
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
