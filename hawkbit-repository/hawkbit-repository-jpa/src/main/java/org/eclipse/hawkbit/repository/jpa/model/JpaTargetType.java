/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Size;
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
public class JpaTargetType extends AbstractJpaNamedEntity implements TargetType, EventAwareEntity{

    private static final long serialVersionUID = 1L;

    @Column(name = "colour", nullable = true, length = TargetType.COLOUR_MAX_SIZE)
    @Size(max = TargetType.COLOUR_MAX_SIZE)
    private String colour;

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
     * Constructor
     *
     * @param name
     *          Type name
     * @param description
     *          Description
     * @param colour
     *          Colour
     */
    public JpaTargetType(String name, String description, String colour) {
        super(name,description);
        this.colour = colour;
    }

    /**
     * @param dsSetType
     *          Distribution set type
     * @return Target type
     */
    public JpaTargetType addCompatibleDistributionSetType(final DistributionSetType dsSetType) {
        if (distributionSetTypes == null) {
            distributionSetTypes = new HashSet<>();
        }

        distributionSetTypes.add(dsSetType);
        return this;
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
    public String getColour() {
        return colour;
    }

    public void setColour(final String colour) {
        this.colour = colour;
    }

    @Override
    public void fireCreateEvent(DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(
                new TargetTypeCreatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireUpdateEvent(DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(
                new TargetTypeUpdatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireDeleteEvent(DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new TargetTypeDeletedEvent(
                getTenant(), getId(), getClass(), EventPublisherHolder.getInstance().getApplicationId()));
    }
}
