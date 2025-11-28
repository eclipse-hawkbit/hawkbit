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
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.event.remote.RolloutDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutUpdatedEvent;
import org.eclipse.hawkbit.repository.jpa.utils.MapAttributeConverter;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;

/**
 * JPA implementation of a {@link Rollout}.
 */
@NoArgsConstructor // Default constructor needed for JPA entities.
@Entity
@Table(name = "sp_rollout")
@NamedEntityGraphs({ @NamedEntityGraph(name = "Rollout.ds", attributeNodes = { @NamedAttributeNode("distributionSet") }) })
// squid:S2160 - BaseEntity equals/hashcode is handling correctly for sub entities
// java:S1710 - not possible to use without group annotation
@SuppressWarnings({ "squid:S2160", "java:S1710", "java:S1171", "java:S3599" })
public class JpaRollout extends AbstractJpaNamedEntity implements Rollout, EventAwareEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @OneToMany(targetEntity = JpaRolloutGroup.class, fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE }, mappedBy = "rollout")
    private List<JpaRolloutGroup> rolloutGroups = new ArrayList<>();

    @Setter
    @Getter
    @Column(name = "target_filter", length = TargetFilterQuery.QUERY_MAX_SIZE, nullable = false)
    @Size(min = 1, max = TargetFilterQuery.QUERY_MAX_SIZE)
    @NotNull
    private String targetFilterQuery;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "distribution_set", nullable = false, updatable = false)
    @NotNull
    private JpaDistributionSet distributionSet;

    @Setter
    @Getter
    @Column(name = "status", nullable = false)
    @Convert(converter = RolloutStatusConverter.class)
    @NotNull
    private RolloutStatus status = RolloutStatus.CREATING;

    @Setter
    @Getter
    @Column(name = "last_check")
    private long lastCheck;

    @Setter
    @Getter
    @Column(name = "action_type", nullable = false)
    @Convert(converter = JpaAction.ActionTypeConverter.class)
    @NotNull
    private ActionType actionType = ActionType.FORCED;

    @Setter
    @Getter
    @Column(name = "forced_time")
    private long forcedTime;

    @Setter
    @Getter
    @Column(name = "total_targets")
    private long totalTargets;

    @Setter
    @Getter
    @Column(name = "rollout_groups_created")
    private int rolloutGroupsCreated;

    @Setter
    @Getter
    @Column(name = "deleted")
    private boolean deleted;

    @Setter
    @Getter
    @Column(name = "start_at")
    private Long startAt;

    @Setter
    @Getter
    @Column(name = "approval_decided_by")
    @Size(min = 1, max = Rollout.APPROVED_BY_MAX_SIZE)
    private String approvalDecidedBy;

    @Setter
    @Getter
    @Column(name = "approval_remark")
    @Size(max = Rollout.APPROVAL_REMARK_MAX_SIZE)
    private String approvalRemark;

    @Setter
    @Column(name = "weight")
    @Min(Action.WEIGHT_MIN)
    @Max(Action.WEIGHT_MAX)
    private Integer weight;

    @Setter
    @Column(name = "is_dynamic") // dynamic is reserved keyword in some databases
    private Boolean dynamic;

    @Setter
    @Column(name = "access_control_context")
    private String accessControlContext;

    @Setter
    @Transient
    private transient TotalTargetCountStatus totalTargetCountStatus;

    public List<RolloutGroup> getRolloutGroups() {
        return Collections.unmodifiableList(rolloutGroups);
    }

    // dynamic is null only for old rollouts - could be used for distinguishing old once from the other
    public boolean isNewStyleTargetPercent() {
        return dynamic != null;
    }

    public void setDistributionSet(final DistributionSet distributionSet) {
        this.distributionSet = (JpaDistributionSet) distributionSet;
    }

    @Override
    public TotalTargetCountStatus getTotalTargetCountStatus() {
        if (totalTargetCountStatus == null) {
            totalTargetCountStatus = new TotalTargetCountStatus(totalTargets, actionType);
        }
        return totalTargetCountStatus;
    }

    @Override
    public Optional<Integer> getWeight() {
        return Optional.ofNullable(weight);
    }

    @Override
    public boolean isDynamic() {
        return Boolean.TRUE.equals(dynamic);
    }

    public Optional<String> getAccessControlContext() {
        return Optional.ofNullable(accessControlContext);
    }

    @Override
    public String toString() {
        return "Rollout [ targetFilterQuery=" + targetFilterQuery + ", distributionSet=" + distributionSet + ", status=" + status +
                ", lastCheck=" + lastCheck + ", getName()=" + getName() + ", getId()=" + getId() + "]";
    }

    @Override
    public void fireCreateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new RolloutCreatedEvent(this));
    }

    @Override
    public void fireUpdateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new RolloutUpdatedEvent(this));

        if (deleted) {
            EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new RolloutDeletedEvent(getTenant(), getId(), getClass()));
        }
    }

    @Override
    public void fireDeleteEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new RolloutDeletedEvent(getTenant(), getId(), getClass()));
    }

    @Converter
    public static class RolloutStatusConverter extends MapAttributeConverter<RolloutStatus, Integer> {

        public RolloutStatusConverter() {
            super(new EnumMap<>(RolloutStatus.class) {{
                put(RolloutStatus.CREATING, 0);
                put(RolloutStatus.READY, 1);
                put(RolloutStatus.PAUSED, 2);
                put(RolloutStatus.STARTING, 3);
                put(RolloutStatus.STOPPED, 4);
                put(RolloutStatus.RUNNING, 5);
                put(RolloutStatus.FINISHED, 6);
                put(RolloutStatus.DELETING, 9);
                put(RolloutStatus.DELETED, 10);
                put(RolloutStatus.WAITING_FOR_APPROVAL, 11);
                put(RolloutStatus.APPROVAL_DENIED, 12);
                put(RolloutStatus.STOPPING, 13);
            }}, null);
        }
    }
}