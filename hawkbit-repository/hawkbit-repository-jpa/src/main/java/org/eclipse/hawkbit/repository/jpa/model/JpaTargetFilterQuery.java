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
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
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

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "name", length = NamedEntity.NAME_MAX_SIZE, nullable = false)
    @Size(max = NamedEntity.NAME_MAX_SIZE)
    @NotEmpty
    private String name;

    @Column(name = "query", length = TargetFilterQuery.QUERY_MAX_SIZE, nullable = false)
    @Size(max = TargetFilterQuery.QUERY_MAX_SIZE)
    @NotEmpty
    private String query;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = JpaDistributionSet.class)
    @JoinColumn(name = "auto_assign_distribution_set")
    private DistributionSet autoAssignDistributionSet;

    @Column(name = "auto_assign_action_type")
    @Convert(converter = JpaAction.ActionTypeConverter.class)
    private ActionType autoAssignActionType;

    @Column(name = "auto_assign_weight")
    private Integer autoAssignWeight;

    @Column(name = "auto_assign_initiated_by", length = USERNAME_FIELD_LENGTH)
    private String autoAssignInitiatedBy;

    @Column(name = "confirmation_required")
    private boolean confirmationRequired;

    @Column(name = "access_control_context")
    private String accessControlContext;

    public JpaTargetFilterQuery(final String name, final String query, final DistributionSet autoAssignDistributionSet,
            final ActionType autoAssignActionType, final Integer autoAssignWeight, final boolean confirmationRequired) {
        this.name = name;
        this.query = query;
        this.autoAssignDistributionSet = (JpaDistributionSet) autoAssignDistributionSet;
        this.autoAssignActionType = autoAssignActionType;
        this.autoAssignWeight = autoAssignWeight == null ? 0 : autoAssignWeight;
        this.confirmationRequired = confirmationRequired;
    }

    public void setAutoAssignActionType(final ActionType actionType) {
        if (actionType == ActionType.TIMEFORCED) {
            throw new IllegalArgumentException("TIMEFORCED is not permitted in autoAssignment");
        }
        this.autoAssignActionType = actionType;
    }

    @Override
    public Optional<Integer> getAutoAssignWeight() {
        return Optional.ofNullable(autoAssignWeight);
    }

    @Override
    public Optional<String> getAccessControlContext() {
        return Optional.ofNullable(accessControlContext);
    }

    @Override
    public void fireCreateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new TargetFilterQueryCreatedEvent(this));
    }

    @Override
    public void fireUpdateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent( new TargetFilterQueryUpdatedEvent(this));
    }

    @Override
    public void fireDeleteEvent() {
        EventPublisherHolder.getInstance().getEventPublisher()
                .publishEvent(new TargetFilterQueryDeletedEvent(getTenant(), getId(), getClass()));
    }
}