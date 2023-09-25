/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.data.mappers;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.RolloutGroupCreate;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyAdvancedRolloutGroup;

/**
 * Maps {@link ProxyAdvancedRolloutGroup} entities, fetched from advanced group
 * rows, to the {@link RolloutGroupCreate} entities.
 */
public class AdvancedRolloutGroupDefinitionToCreateMapper {
    private final EntityFactory entityFactory;

    /**
     * Constructor for AdvancedRolloutGroupDefinitionToCreateMapper
     *
     * @param entityFactory
     *            the entity factory
     */
    public AdvancedRolloutGroupDefinitionToCreateMapper(final EntityFactory entityFactory) {
        this.entityFactory = entityFactory;
    }

    /**
     * Map rollout group
     *
     * @param advancedRolloutGroupDefinition
     *            ProxyAdvancedRolloutGroup
     *
     * @return ProxyAdvancedRolloutGroup
     */
    public RolloutGroupCreate map(final ProxyAdvancedRolloutGroup advancedRolloutGroupDefinition) {
        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder()
                .successAction(RolloutGroupSuccessAction.NEXTGROUP, null)
                .successCondition(RolloutGroupSuccessCondition.THRESHOLD,
                        advancedRolloutGroupDefinition.getTriggerThresholdPercentage())
                .errorCondition(RolloutGroupErrorCondition.THRESHOLD,
                        advancedRolloutGroupDefinition.getErrorThresholdPercentage())
                .errorAction(RolloutGroupErrorAction.PAUSE, null).build();

        return entityFactory.rolloutGroup().create().name(advancedRolloutGroupDefinition.getGroupName())
                .description(advancedRolloutGroupDefinition.getGroupName())
                .targetFilterQuery(advancedRolloutGroupDefinition.getTargetFilterQuery())
                .targetPercentage(advancedRolloutGroupDefinition.getTargetPercentage()).conditions(conditions)
                .confirmationRequired(advancedRolloutGroupDefinition.isConfirmationRequired());
    }
}
