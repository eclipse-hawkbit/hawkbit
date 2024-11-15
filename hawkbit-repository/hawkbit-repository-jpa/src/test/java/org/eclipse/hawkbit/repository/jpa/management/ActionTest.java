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

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.awaitility.Awaitility;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.junit.jupiter.api.Test;

@Feature("Unit Tests - Repository")
@Story("Deployment Management")
public class ActionTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Ensures that timeforced moded switch from soft to forces after defined timeframe.")
    public void timeforcedHitNewHasCodeIsGenerated() {

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
    public void testActionTypeConvert() {
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
    public void testStatusConvert() {
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

    private Action createAction() {
        final ActionType[] actionTypes = ActionType.values();
        final JpaAction action = new JpaAction();
        action.setTarget(testdataFactory.createTarget("testActionTypeMappingTarget"));
        action.setDistributionSet(testdataFactory.createDistributionSet("testActionTypeMappingDS"));
        action.setActionType(actionTypes[0]);
        action.setStatus(Status.SCHEDULED);
        action.setWeight(100);
        action.setInitiatedBy("none");
        return actionRepository.save(action);
    }
}
