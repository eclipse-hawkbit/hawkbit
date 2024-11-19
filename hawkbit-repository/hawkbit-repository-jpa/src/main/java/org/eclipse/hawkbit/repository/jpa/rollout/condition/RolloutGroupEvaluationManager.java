/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rollout.condition;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.model.RolloutGroup;

/**
 * Manager class to collect all instances of
 * {@link RolloutGroupConditionEvaluator} and
 * {@link RolloutGroupActionEvaluator} for specific conditions and actions. The
 * corresponding instance can be fetched by providing the action/condition.
 */
@Slf4j
public class RolloutGroupEvaluationManager {

    private final List<RolloutGroupConditionEvaluator<RolloutGroup.RolloutGroupErrorCondition>> errorConditionEvaluators;
    private final List<RolloutGroupConditionEvaluator<RolloutGroup.RolloutGroupSuccessCondition>> successConditionEvaluators;
    private final List<RolloutGroupActionEvaluator<RolloutGroup.RolloutGroupErrorAction>> errorActionEvaluators;
    private final List<RolloutGroupActionEvaluator<RolloutGroup.RolloutGroupSuccessAction>> successActionEvaluators;

    /**
     * Constructor
     *
     * @param errorConditionEvaluators evaluators for instances of {@link RolloutGroupConditionEvaluator}
     *         handling the {@link RolloutGroup.RolloutGroupErrorCondition}
     * @param successConditionEvaluators evaluators for instances of {@link RolloutGroupConditionEvaluator}
     *         handling the {@link RolloutGroup.RolloutGroupSuccessCondition}
     * @param errorActionEvaluators evaluators for instances of {@link RolloutGroupActionEvaluator}
     *         handling the {@link RolloutGroup.RolloutGroupErrorAction}
     * @param successActionEvaluators evaluators for instances of {@link RolloutGroupActionEvaluator}
     *         handling the {@link RolloutGroup.RolloutGroupSuccessAction}
     */
    public RolloutGroupEvaluationManager(
            final List<RolloutGroupConditionEvaluator<RolloutGroup.RolloutGroupErrorCondition>> errorConditionEvaluators,
            final List<RolloutGroupConditionEvaluator<RolloutGroup.RolloutGroupSuccessCondition>> successConditionEvaluators,
            final List<RolloutGroupActionEvaluator<RolloutGroup.RolloutGroupErrorAction>> errorActionEvaluators,
            final List<RolloutGroupActionEvaluator<RolloutGroup.RolloutGroupSuccessAction>> successActionEvaluators) {
        this.errorConditionEvaluators = errorConditionEvaluators;
        this.successConditionEvaluators = successConditionEvaluators;
        this.errorActionEvaluators = errorActionEvaluators;
        this.successActionEvaluators = successActionEvaluators;
    }

    public RolloutGroupActionEvaluator<RolloutGroup.RolloutGroupErrorAction> getErrorActionEvaluator(
            final RolloutGroup.RolloutGroupErrorAction errorAction) {
        return findFirstActionEvaluator(errorActionEvaluators, errorAction);

    }

    public RolloutGroupActionEvaluator<RolloutGroup.RolloutGroupSuccessAction> getSuccessActionEvaluator(
            final RolloutGroup.RolloutGroupSuccessAction successAction) {
        return findFirstActionEvaluator(successActionEvaluators, successAction);
    }

    public RolloutGroupConditionEvaluator<RolloutGroup.RolloutGroupErrorCondition> getErrorConditionEvaluator(
            final RolloutGroup.RolloutGroupErrorCondition errorCondition) {
        return findFirstConditionEvaluator(errorConditionEvaluators, errorCondition);

    }

    public RolloutGroupConditionEvaluator<RolloutGroup.RolloutGroupSuccessCondition> getSuccessConditionEvaluator(
            final RolloutGroup.RolloutGroupSuccessCondition successCondition) {
        return findFirstConditionEvaluator(successConditionEvaluators, successCondition);
    }

    private static <T extends Enum<T>> RolloutGroupActionEvaluator<T> findFirstActionEvaluator(
            final List<RolloutGroupActionEvaluator<T>> evaluators, final T action) {
        return evaluators.stream().filter(evaluator -> evaluator.getAction() == action).findFirst().orElseThrow(() -> {
            log.warn("Could not find suitable evaluator for the '{}' action.", action.name());
            return new EvaluatorNotConfiguredException(action.name());
        });
    }

    private static <T extends Enum<T>> RolloutGroupConditionEvaluator<T> findFirstConditionEvaluator(
            final List<RolloutGroupConditionEvaluator<T>> evaluators, final T condition) {
        return evaluators.stream().filter(evaluator -> evaluator.getCondition() == condition).findFirst()
                .orElseThrow(() -> {
                    log.warn("Could not find suitable evaluator for the '{}' condition.", condition.name());
                    return new EvaluatorNotConfiguredException(condition.name());
                });
    }
}