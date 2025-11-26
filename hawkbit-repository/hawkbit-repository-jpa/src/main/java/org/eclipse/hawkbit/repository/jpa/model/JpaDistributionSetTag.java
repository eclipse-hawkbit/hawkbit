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
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.event.remote.DistributionSetTagDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagUpdatedEvent;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;

/**
 * A {@link DistributionSetTag} is used to describe DistributionSet attributes and use them also for filtering the DistributionSet list.
 */
@NoArgsConstructor // Default constructor needed for JPA entities.
@Entity
@Table(name = "sp_distribution_set_tag")
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for sub entities
@SuppressWarnings("squid:S2160")
public class JpaDistributionSetTag extends JpaTag implements DistributionSetTag, EventAwareEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToMany(mappedBy = "tags", targetEntity = JpaDistributionSet.class, fetch = FetchType.LAZY)
    private List<DistributionSet> assignedToDistributionSet;

    /**
     * Public constructor.
     *
     * @param name of the {@link DistributionSetTag}
     * @param description of the {@link DistributionSetTag}
     * @param colour of tag in UI
     */
    public JpaDistributionSetTag(final String name, final String description, final String colour) {
        super(name, description, colour);
    }

    @Override
    public void fireCreateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new DistributionSetTagCreatedEvent(this));
    }

    @Override
    public void fireUpdateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new DistributionSetTagUpdatedEvent(this));
    }

    @Override
    public void fireDeleteEvent() {
        EventPublisherHolder.getInstance().getEventPublisher()
                .publishEvent(new DistributionSetTagDeletedEvent(getTenant(), getId(), getClass()));

    }
}