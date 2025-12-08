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
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.event.remote.RolloutGroupDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupUpdatedEvent;
import org.eclipse.hawkbit.repository.jpa.utils.MapAttributeConverter;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;

/**
 * JPA entity definition of persisting a group of an rollout.
 */
@NoArgsConstructor // Default constructor needed for JPA entities.
@Entity
@Table(name = "sp_rollout_group")
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for sub entities
@SuppressWarnings("squid:S2160")
public class JpaRolloutGroup extends AbstractJpaNamedEntity implements RolloutGroup, EventAwareEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "rollout", nullable = false, updatable = false)
    private JpaRollout rollout;

    @Setter
    @Getter
    @Column(name = "status", nullable = false)
    @Convert(converter = RolloutGroupStatusConverter.class)
    private RolloutGroupStatus status = RolloutGroupStatus.CREATING;

    @OneToMany(
            mappedBy = "rolloutGroup", fetch = FetchType.LAZY,
            cascade = { CascadeType.PERSIST, CascadeType.REMOVE }, targetEntity = RolloutTargetGroup.class)
    private List<RolloutTargetGroup> rolloutTargetGroup;

    // No foreign key to avoid to many nested cascades on delete which some DBs cannot handle
    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST })
    @JoinColumn(name = "parent")
    private JpaRolloutGroup parent;

    @Setter
    @Getter
    @Column(name = "is_dynamic") // dynamic is reserved keyword in some databases
    private boolean dynamic;

    @Setter
    @Getter
    @Column(name = "success_condition", nullable = false)
    @NotNull
    private RolloutGroupSuccessCondition successCondition = RolloutGroupSuccessCondition.THRESHOLD;

    @Setter
    @Getter
    @Column(name = "success_condition_exp", length = 512, nullable = false)
    @Size(max = 512)
    @NotNull
    private String successConditionExp;

    @Setter
    @Getter
    @Column(name = "success_action", nullable = false)
    @NotNull
    private RolloutGroupSuccessAction successAction = RolloutGroupSuccessAction.NEXTGROUP;

    @Setter
    @Getter
    @Column(name = "success_action_exp", length = 512)
    @Size(max = 512)
    private String successActionExp;

    @Setter
    @Getter
    @Column(name = "error_condition")
    private RolloutGroupErrorCondition errorCondition;

    @Setter
    @Getter
    @Column(name = "error_condition_exp", length = 512)
    @Size(max = 512)
    private String errorConditionExp;

    @Setter
    @Getter
    @Column(name = "error_action")
    private RolloutGroupErrorAction errorAction;

    @Setter
    @Getter
    @Column(name = "error_action_exp", length = 512)
    @Size(max = 512)
    private String errorActionExp;

    @Setter
    @Getter
    @Column(name = "total_targets")
    private int totalTargets;

    @Setter
    @Getter
    @Column(name = "target_filter", length = 1024)
    @Size(max = 1024)
    private String targetFilterQuery = "";

    @Setter
    @Getter
    @Column(name = "target_percentage")
    private float targetPercentage = 100;

    @Setter
    @Getter
    @Column(name = "confirmation_required")
    private boolean confirmationRequired;

    @Setter
    @Transient
    private transient TotalTargetCountStatus totalTargetCountStatus;

    public void setRollout(final Rollout rollout) {
        this.rollout = (JpaRollout) rollout;
    }

    /**
     * @return the totalTargetCountStatus
     */
    @Override
    public TotalTargetCountStatus getTotalTargetCountStatus() {
        if (totalTargetCountStatus == null) {
            totalTargetCountStatus = new TotalTargetCountStatus((long) totalTargets, rollout.getActionType());
        }
        return totalTargetCountStatus;
    }

    @Override
    public String toString() {
        return "RolloutGroup [" +
                "rollout=" + (rollout != null ? rollout.getId() : "") +
                ", name=" + getName() +
                ", status=" + status +
                ", parent=" + parent +
                ", finishCondition=" + successCondition + ", finishExp=" + successConditionExp +
                ", errorCondition=" + errorCondition + ", errorExp=" + errorConditionExp +
                ", isConfirmationRequired()=" + isConfirmationRequired() +
                ", rolloutTargetGroup=" + rolloutTargetGroup +
                ", id()=" + getId() +
                "]";
    }

    @Override
    public void fireCreateEvent() {
        // there is no RolloutGroup created event
    }

    @Override
    public void fireUpdateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new RolloutGroupUpdatedEvent(this, getRollout().getId()));
    }

    @Override
    public void fireDeleteEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new RolloutGroupDeletedEvent(getTenant(), getId(), getClass()));
    }

    @Converter
    public static class RolloutGroupStatusConverter extends MapAttributeConverter<RolloutGroupStatus, Integer> {

        public RolloutGroupStatusConverter() {
            super(Map.of(
                    RolloutGroupStatus.READY, 0,
                    RolloutGroupStatus.SCHEDULED, 1,
                    RolloutGroupStatus.FINISHED, 2,
                    RolloutGroupStatus.ERROR, 3,
                    RolloutGroupStatus.RUNNING, 4,
                    RolloutGroupStatus.CREATING, 5
            ), null);
        }
    }
}