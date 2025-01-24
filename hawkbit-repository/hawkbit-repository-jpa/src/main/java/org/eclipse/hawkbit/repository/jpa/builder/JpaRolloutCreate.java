/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import java.util.Optional;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.ValidString;
import org.eclipse.hawkbit.repository.builder.AbstractNamedEntityBuilder;
import org.eclipse.hawkbit.repository.builder.RolloutCreate;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;

public class JpaRolloutCreate extends AbstractNamedEntityBuilder<RolloutCreate> implements RolloutCreate {

    protected Long distributionSetId;
    @ValidString
    protected String targetFilterQuery;
    protected Action.ActionType actionType;
    protected Long forcedTime;
    protected Long startAt;
    @Min(Action.WEIGHT_MIN)
    @Max(Action.WEIGHT_MAX)
    protected Integer weight;
    private final DistributionSetManagement distributionSetManagement;
    private boolean dynamic;

    JpaRolloutCreate(final DistributionSetManagement distributionSetManagement) {
        this.distributionSetManagement = distributionSetManagement;
    }

    /**
     * {@link DistributionSet} of rollout
     *
     * @param distributionSetId ID of the distributionSetId
     * @return this builder
     */
    public RolloutCreate distributionSetId(final long distributionSetId) {
        this.distributionSetId = distributionSetId;
        return this;
    }

    /**
     * Filter of the rollout
     *
     * @param targetFilterQuery query
     * @return this builder
     */
    public RolloutCreate targetFilterQuery(final String targetFilterQuery) {
        this.targetFilterQuery = targetFilterQuery == null ? null : targetFilterQuery.strip();
        return this;
    }

    /**
     * {@link Action.ActionType} used for {@link Action}s
     *
     * @param actionType type
     * @return this builder
     */
    public RolloutCreate actionType(final Action.ActionType actionType) {
        this.actionType = actionType;
        return this;
    }

    /**
     * forcedTime used for {@link Action}s
     *
     * @param forcedTime time
     * @return this builder
     */
    public RolloutCreate forcedTime(final Long forcedTime) {
        this.forcedTime = forcedTime;
        return this;
    }

    /**
     * weight used for {@link Action}s
     *
     * @param weight weight
     * @return this builder
     */
    public RolloutCreate weight(final Integer weight) {
        this.weight = weight;
        return this;
    }

    public RolloutCreate dynamic(final boolean dynamic) {
        this.dynamic = dynamic;
        return this;
    }

    /**
     * Set start of the Rollout
     *
     * @param startAt start time point
     * @return this builder
     */
    public RolloutCreate startAt(final Long startAt) {
        this.startAt = startAt;
        return this;
    }

    @Override
    public JpaRollout build() {
        final JpaRollout rollout = new JpaRollout();

        rollout.setName(name);
        rollout.setDescription(description);
        rollout.setDistributionSet(distributionSetManagement.getValidAndComplete(distributionSetId));
        rollout.setTargetFilterQuery(targetFilterQuery);
        rollout.setStartAt(startAt);
        rollout.setWeight(weight);
        rollout.setDynamic(dynamic);

        if (actionType != null) {
            rollout.setActionType(actionType);
        }

        if (forcedTime != null) {
            rollout.setForcedTime(forcedTime);
        }

        return rollout;
    }

    public Optional<Long> getDistributionSetId() {
        return Optional.ofNullable(distributionSetId);
    }

    public Optional<Action.ActionType> getActionType() {
        return Optional.ofNullable(actionType);
    }

    public Optional<Long> getForcedTime() {
        return Optional.ofNullable(forcedTime);
    }

    public Optional<Integer> getWeight() {
        return Optional.ofNullable(weight);
    }

    public Optional<Long> getStartAt() {
        return Optional.ofNullable(startAt);
    }
}