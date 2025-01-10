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

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.NoArgsConstructor;
import lombok.ToString;
import org.eclipse.hawkbit.repository.event.remote.TargetTagDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagUpdatedEvent;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;

/**
 * A {@link TargetTag} is used to describe Target attributes and use them also for filtering the target list.
 */
@NoArgsConstructor // Default constructor for JPA
@ToString(callSuper = true)
@Entity
@Table(name = "sp_target_tag", indexes = { @Index(name = "sp_idx_target_tag_prim", columnList = "tenant,id"),
        @Index(name = "sp_idx_target_tag_01", columnList = "tenant,name") }, uniqueConstraints = @UniqueConstraint(columnNames = {
        "name", "tenant" }, name = "uk_targ_tag"))
public class JpaTargetTag extends JpaTag implements TargetTag, EventAwareEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    public JpaTargetTag(final String name, final String description, final String colour) {
        super(name, description, colour);
    }

    @Override
    public void fireCreateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher()
                .publishEvent(new TargetTagCreatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireUpdateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher()
                .publishEvent(new TargetTagUpdatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireDeleteEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new TargetTagDeletedEvent(getTenant(),
                getId(), getClass(), EventPublisherHolder.getInstance().getApplicationId()));
    }
}