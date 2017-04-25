/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.util.Collections;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.eclipse.hawkbit.repository.event.remote.DistributionSetTagDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagUpdatedEvent;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.eclipse.persistence.descriptors.DescriptorEvent;

/**
 * A {@link DistributionSetTag} is used to describe DistributionSet attributes
 * and use them also for filtering the DistributionSet list.
 *
 */
@Entity
@Table(name = "sp_distributionset_tag", indexes = {
        @Index(name = "sp_idx_distribution_set_tag_prim", columnList = "tenant,id") }, uniqueConstraints = @UniqueConstraint(columnNames = {
                "name", "tenant" }, name = "uk_ds_tag"))
public class JpaDistributionSetTag extends JpaTag implements DistributionSetTag, EventAwareEntity {
    private static final long serialVersionUID = 1L;

    @CascadeOnDelete
    @ManyToMany(mappedBy = "tags", targetEntity = JpaDistributionSet.class, fetch = FetchType.LAZY)
    private List<DistributionSet> assignedToDistributionSet;

    /**
     * Public constructor.
     *
     * @param name
     *            of the {@link DistributionSetTag}
     * @param description
     *            of the {@link DistributionSetTag}
     * @param colour
     *            of tag in UI
     */
    public JpaDistributionSetTag(final String name, final String description, final String colour) {
        super(name, description, colour);
    }

    /**
     * Default constructor for JPA.
     */
    public JpaDistributionSetTag() {
        // Default constructor for JPA.
    }

    public List<DistributionSet> getAssignedToDistributionSet() {
        if (assignedToDistributionSet == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(assignedToDistributionSet);
    }

    @Override
    public void fireCreateEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(
                new DistributionSetTagCreatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireUpdateEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(
                new DistributionSetTagUpdatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));

    }

    @Override
    public void fireDeleteEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new DistributionSetTagDeletedEvent(
                getTenant(), getId(), getClass().getName(), EventPublisherHolder.getInstance().getApplicationId()));

    }
}
