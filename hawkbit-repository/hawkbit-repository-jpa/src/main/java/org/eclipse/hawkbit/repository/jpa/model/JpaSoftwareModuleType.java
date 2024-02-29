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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;

import org.eclipse.hawkbit.repository.event.remote.SoftwareModuleTypeDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleTypeCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleTypeUpdatedEvent;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.persistence.descriptors.DescriptorEvent;

import java.io.Serial;

/**
 * Type of a software modules.
 *
 */
@Entity
@Table(name = "sp_software_module_type", indexes = {
        @Index(name = "sp_idx_software_module_type_01", columnList = "tenant,deleted"),
        @Index(name = "sp_idx_software_module_type_prim", columnList = "tenant,id") }, uniqueConstraints = {
                @UniqueConstraint(columnNames = { "type_key", "tenant" }, name = "uk_smt_type_key"),
                @UniqueConstraint(columnNames = { "name", "tenant" }, name = "uk_smt_name") })
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaSoftwareModuleType extends AbstractJpaTypeEntity implements SoftwareModuleType, EventAwareEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "max_ds_assignments", nullable = false)
    @Min(1)
    private int maxAssignments;

    @Column(name = "deleted")
    private boolean deleted;

    /**
     * Constructor.
     *
     * @param key
     *            of the type
     * @param name
     *            of the type
     * @param description
     *            of the type
     * @param maxAssignments
     *            assignments to a DS
     */
    public JpaSoftwareModuleType(final String key, final String name, final String description,
            final int maxAssignments) {
        this(key, name, description, maxAssignments, null);
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
     * @param maxAssignments
     *            assignments to a DS
     * @param colour
     *            of the type. It will be null by default
     */
    public JpaSoftwareModuleType(final String key, final String name, final String description,
            final int maxAssignments, final String colour) {
        super(name, description, key, colour);
        this.maxAssignments = maxAssignments;
    }

    /**
     * Default Constructor for JPA.
     */
    public JpaSoftwareModuleType() {
        // Default Constructor for JPA.
    }

    public void setMaxAssignments(final int maxAssignments) {
        this.maxAssignments = maxAssignments;
    }

    @Override
    public int getMaxAssignments() {
        return maxAssignments;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        return "SoftwareModuleType [key=" + getKey() + ", getName()=" + getName() + ", getId()=" + getId() + "]";
    }

    @Override
    public void fireCreateEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(
                new SoftwareModuleTypeCreatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireUpdateEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(
                new SoftwareModuleTypeUpdatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireDeleteEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new SoftwareModuleTypeDeletedEvent(
                getTenant(), getId(), getClass(), EventPublisherHolder.getInstance().getApplicationId()));
    }
}
