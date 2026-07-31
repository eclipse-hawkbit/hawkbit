/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.hawkbit.repository.jpa.model;

import static org.eclipse.hawkbit.repository.model.Action.ActionType.TIMEFORCED;
import static org.eclipse.hawkbit.repository.model.TargetFilterQuery.QUERY_MAX_SIZE;

import java.util.EnumMap;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.event.remote.AutoAssignmentDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.AutoAssignmentCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.AutoAssignmentUpdatedEvent;
import org.eclipse.hawkbit.repository.jpa.utils.MapAttributeConverter;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.AutoAssignment;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;

/**
 * JPA implementation of an {@link AutoAssignment}.
 */
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "sp_auto_assignment")
@NamedEntityGraphs({ @NamedEntityGraph(name = "AutoAssignment.ds", attributeNodes = { @NamedAttributeNode("distributionSet") }) })
public class JpaAutoAssignment extends AbstractJpaNamedEntity implements AutoAssignment, EventAwareEntity {

    @Column(name = "query", length = QUERY_MAX_SIZE, nullable = false)
    @Size(max = QUERY_MAX_SIZE)
    @NotEmpty
    private String targetFilterQuery;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = JpaDistributionSet.class)
    @JoinColumn(name = "distribution_set")
    @NotNull
    private DistributionSet distributionSet;

    @Column(name = "action_type")
    @Convert(converter = JpaAction.ActionTypeConverter.class)
    private Action.ActionType actionType;

    @Column(name = "start_at")
    private Long startAt;

    @Column(name = "approval_decided_by")
    @Size(min = 1, max = APPROVED_BY_MAX_SIZE)
    private String approvalDecidedBy;

    @Column(name = "approval_remark")
    @Size(max = APPROVAL_REMARK_MAX_SIZE)
    private String approvalRemark;

    @Column(name = "weight")
    private Integer weight;

    // the column is nullable (kept as it was in sp_target_filter_query), so a wrapper is used to avoid
    // read failures on migrated rows with a NULL value; the interface contract stays a primitive boolean
    @Column(name = "confirmation_required")
    @Getter(AccessLevel.NONE)
    private Boolean confirmationRequired;

    @Column(name = "access_control_context")
    @Lob
    @Size(max = TargetFilterQuery.ACCESS_CONTROL_CONTEXT_MAX_SIZE)
    private String accessControlContext;

    @Column(name = "status")
    @Convert(converter = AutoAssignStatusConverter.class)
    private AutoAssignStatus status;

    public void setActionType(final Action.ActionType actionType) {
        if (actionType == TIMEFORCED) {
            throw new IllegalArgumentException("TIMEFORCED is not permitted in autoAssignment");
        }
        this.actionType = actionType;
    }

    @Override
    public boolean isConfirmationRequired() {
        return Boolean.TRUE.equals(confirmationRequired);
    }

    @Override
    public Optional<Integer> getWeight() {
        return Optional.ofNullable(weight);
    }

    @Override
    public Optional<String> getAccessControlContext() {
        return Optional.ofNullable(accessControlContext);
    }

    @Override
    public void fireCreateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new AutoAssignmentCreatedEvent(this));
    }

    @Override
    public void fireUpdateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new AutoAssignmentUpdatedEvent(this));
    }

    @Override
    public void fireDeleteEvent() {
        EventPublisherHolder.getInstance().getEventPublisher()
                .publishEvent(new AutoAssignmentDeletedEvent(getTenant(), getId(), getClass()));
    }

    @Converter
    public static class AutoAssignStatusConverter extends MapAttributeConverter<AutoAssignStatus, Integer> {

        public AutoAssignStatusConverter() {
            super(new EnumMap<>(AutoAssignStatus.class) {

                {
                    put(AutoAssignStatus.WAITING_FOR_APPROVAL, 0);
                    put(AutoAssignStatus.APPROVAL_DENIED, 1);
                    put(AutoAssignStatus.READY, 2);
                    put(AutoAssignStatus.PAUSED, 3);
                    put(AutoAssignStatus.RUNNING, 4);
                }
            }, null);
        }
    }
}
