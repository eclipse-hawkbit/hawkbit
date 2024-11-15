/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.awaitility.Awaitility;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.jupiter.api.Test;

@Feature("Unit Tests - Repository")
@Story("Deployment Management")
class ActionTest extends AbstractJpaIntegrationTest {

    private Target target;
    private DistributionSet distributionSet;

    @Test
    @Description("Ensures that timeforced moded switch from soft to forces after defined timeframe.")
    void timeforcedHitNewHasCodeIsGenerated() {

        // current time + 1 seconds
        final long sleepTime = 1000;
        final long timeForceTimeAt = System.currentTimeMillis() + sleepTime;
        final JpaAction timeforcedAction = new JpaAction();
        timeforcedAction.setActionType(ActionType.TIMEFORCED);
        timeforcedAction.setForcedTime(timeForceTimeAt);
        assertThat(timeforcedAction.isForcedOrTimeForced()).isFalse();

        // wait until timeforce time is hit
        Awaitility.await().atMost(Duration.ofSeconds(2)).pollInterval(Duration.ofMillis(100))
                .until(timeforcedAction::isForcedOrTimeForced);
    }

    @Test
    void testActionTypeConvert() {
        final long id = createAction().getId();
        for (final ActionType actionType : ActionType.values()) {
            final JpaAction action = actionRepository
                    .findById(id).orElseThrow(() -> new IllegalStateException("Action not found"));
            action.setActionType(actionType);
            actionRepository.save(action);
            assertThat(actionRepository.findById(id).orElseThrow(() -> new IllegalStateException("Action not found"))
                    .getActionType()).isEqualTo(actionType);
        }
    }

    @Test
    void testStatusConvert() {
        final long id = createAction().getId();
        for (final Status status : Status.values()) {
            final JpaAction action = actionRepository
                    .findById(id).orElseThrow(() -> new IllegalStateException("Action not found"));
            action.setStatus(status);
            actionRepository.save(action);
            assertThat(actionRepository.findById(id).orElseThrow(() -> new IllegalStateException("Action not found"))
                    .getStatus()).isEqualTo(status);
        }
    }

    @Test
    void testActionsStatusStatusConvert() {
        for (final Status status : Status.values()) {
            final long id = createAction().getId();
            controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(id).status(status));
            final List<ActionStatus> actionStatuses = actionRepository
                    .findById(id).orElseThrow(() -> new IllegalStateException("Action not found"))
                    .getActionStatus();
            assertThat(actionStatuses).hasSize(1);
            assertThat(actionStatuses.stream().filter(as -> as.getStatus() == status).count()).isEqualTo(1);
        }
    }

    private Action createAction() {
        if (target == null) {
            target = testdataFactory.createTarget("testActionTypeMappingTarget");
        }
        if (distributionSet == null) {
            distributionSet = testdataFactory.createDistributionSet("testActionTypeMappingDS");
        }

        final ActionType[] actionTypes = ActionType.values();
        final JpaAction action = new JpaAction();
        action.setTarget(target);
        action.setDistributionSet(distributionSet);
        action.setActionType(actionTypes[0]);
        action.setStatus(Status.SCHEDULED);
        action.setWeight(100);
        action.setInitiatedBy("none");
        action.setActive(true);
        return actionRepository.save(action);
    }
}
