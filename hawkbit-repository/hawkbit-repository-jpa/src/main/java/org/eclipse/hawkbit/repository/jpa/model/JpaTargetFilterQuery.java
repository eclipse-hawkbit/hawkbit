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
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.event.remote.TargetFilterQueryDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetFilterQueryCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetFilterQueryUpdatedEvent;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;

/**
 * Stored target filter.
 */
@NoArgsConstructor // Default constructor for JPA
@Setter
@Getter
@Entity
@Table(name = "sp_target_filter_query")
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for sub entities
@SuppressWarnings("squid:S2160")
public class JpaTargetFilterQuery extends AbstractJpaTenantAwareBaseEntity implements TargetFilterQuery, EventAwareEntity {

    @Column(name = "name", length = NamedEntity.NAME_MAX_SIZE, nullable = false)
    @Size(max = NamedEntity.NAME_MAX_SIZE)
    @NotEmpty
    private String name;

    @Column(name = "query", length = TargetFilterQuery.QUERY_MAX_SIZE, nullable = false)
    @Size(max = TargetFilterQuery.QUERY_MAX_SIZE)
    @NotEmpty
    private String query;

    @Column(name = "access_control_context")
    @Lob
    @Size(max = TargetFilterQuery.ACCESS_CONTROL_CONTEXT_MAX_SIZE)
    private String accessControlContext;

    @OneToOne(fetch = FetchType.LAZY, targetEntity = JpaAutoAssignment.class)
    @JoinColumn(name = "name", referencedColumnName = "name", insertable = false, updatable = false)
    @JoinColumn(name = "query", referencedColumnName = "query", insertable = false, updatable = false)
    private JpaAutoAssignment autoAssignment;

    public JpaTargetFilterQuery(final String name, final String query) {
        this.name = name;
        this.query = query;
    }

    @Override
    public void fireCreateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new TargetFilterQueryCreatedEvent(this));
    }

    @Override
    public void fireUpdateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new TargetFilterQueryUpdatedEvent(this));
    }

    @Override
    public void fireDeleteEvent() {
        EventPublisherHolder.getInstance().getEventPublisher()
                .publishEvent(new TargetFilterQueryDeletedEvent(getTenant(), getId(), getClass()));
    }
}
