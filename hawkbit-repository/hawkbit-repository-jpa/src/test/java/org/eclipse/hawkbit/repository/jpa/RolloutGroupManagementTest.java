/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.hawkbit.repository.event.remote.RolloutDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetWithActionStatus;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.util.CollectionUtils;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Component Tests - Repository")
@Story("Rollout Management")
class RolloutGroupManagementTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Verifies that management get access reacts as specfied on calls for non existing entities by means "
            + "of Optional not present.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    void nonExistingEntityAccessReturnsNotPresent() {
        assertThat(rolloutGroupManagement.get(NOT_EXIST_IDL)).isNotPresent();
        assertThat(rolloutGroupManagement.getWithDetailedStatus(NOT_EXIST_IDL)).isNotPresent();

    }

    @Test
    @Description("Verifies that management queries react as specfied on calls for non existing entities "
            + " by means of throwing EntityNotFoundException.")
    @ExpectEvents({ @Expect(type = RolloutDeletedEvent.class, count = 0),
            @Expect(type = RolloutGroupCreatedEvent.class, count = 5),
            @Expect(type = RolloutGroupUpdatedEvent.class, count = 5),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = RolloutUpdatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 125),
            @Expect(type = RolloutCreatedEvent.class, count = 1) })
    void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        testdataFactory.createRollout();

        verifyThrownExceptionBy(() -> rolloutGroupManagement.countByRollout(NOT_EXIST_IDL), "Rollout");
        verifyThrownExceptionBy(() -> rolloutGroupManagement.countTargetsOfRolloutsGroup(NOT_EXIST_IDL),
                "RolloutGroup");
        verifyThrownExceptionBy(() -> rolloutGroupManagement.findByRolloutWithDetailedStatus(PAGE, NOT_EXIST_IDL),
                "Rollout");
        verifyThrownExceptionBy(
                () -> rolloutGroupManagement.findAllTargetsOfRolloutGroupWithActionStatus(PAGE, NOT_EXIST_IDL),
                "RolloutGroup");
        verifyThrownExceptionBy(() -> rolloutGroupManagement.findByRolloutAndRsql(PAGE, NOT_EXIST_IDL, "name==*"),
                "Rollout");

        verifyThrownExceptionBy(() -> rolloutGroupManagement.findTargetsOfRolloutGroup(PAGE, NOT_EXIST_IDL),
                "RolloutGroup");
        verifyThrownExceptionBy(
                () -> rolloutGroupManagement.findTargetsOfRolloutGroupByRsql(PAGE, NOT_EXIST_IDL, "name==*"),
                "RolloutGroup");
    }

    @Test
    @Description("Verifies that the returned result considers the provided sort parameters.")
    void findAllTargetsOfRolloutGroupWithActionStatusConsidersSorting() {
        final Rollout rollout = testdataFactory.createAndStartRollout();
        final List<RolloutGroup> rolloutGroups = rolloutGroupManagement.findByRollout(PAGE, rollout.getId())
                .getContent();
        final RolloutGroup rolloutGroup = rolloutGroups.get(0);
        rolloutManagement.pauseRollout(rollout.getId());
        rolloutManagement.handleRollouts();
        final List<Target> targets = rolloutGroupManagement.findTargetsOfRolloutGroup(PAGE, rolloutGroup.getId())
                .getContent();
        Target targetCancelled = targets.get(0);
        final Action actionCancelled = deploymentManagement.findActionsByTarget(targetCancelled.getControllerId(), PAGE)
                .getContent().get(0);
        deploymentManagement.cancelAction(actionCancelled.getId());
        deploymentManagement.forceQuitAction(actionCancelled.getId());
        targetCancelled = reloadTarget(targetCancelled);
        Target targetCancelling = targets.get(1);
        final Action actionCancelling = deploymentManagement
                .findActionsByTarget(targetCancelling.getControllerId(), PAGE).getContent().get(0);
        deploymentManagement.cancelAction(actionCancelling.getId());
        targetCancelling = reloadTarget(targetCancelling);

        final List<TargetWithActionStatus> targetsWithActionStatus = rolloutGroupManagement
                .findAllTargetsOfRolloutGroupWithActionStatus(PageRequest.of(0, 500, Sort.by(Direction.DESC, "status")),
                        rolloutGroup.getId())
                .getContent();
        assertThat(targetsWithActionStatus.get(0).getTarget()).isEqualTo(targetCancelling);
        assertThat(targetsWithActionStatus.get(1).getTarget()).isEqualTo(targetCancelled);

        final List<TargetWithActionStatus> targetsWithActionStatusOrderedByNameDesc = rolloutGroupManagement
                .findAllTargetsOfRolloutGroupWithActionStatus(PageRequest.of(0, 500, Sort.by(Direction.DESC, "name")),
                        rolloutGroup.getId())
                .getContent();
        assertThatListIsSortedByTargetName(targetsWithActionStatusOrderedByNameDesc, Direction.DESC);

        final List<TargetWithActionStatus> targetsWithActionStatusOrderedByNameAsc = rolloutGroupManagement
                .findAllTargetsOfRolloutGroupWithActionStatus(PageRequest.of(0, 500, Sort.by(Direction.ASC, "name")),
                        rolloutGroup.getId())
                .getContent();
        assertThatListIsSortedByTargetName(targetsWithActionStatusOrderedByNameAsc, Direction.ASC);
    }

    @Test
    @Description("Verifies that the returned result considers sorting by action status code.")
    void findAllTargetsOfRolloutGroupWithActionStatusConsidersSortingByLastActionStatusCode() {
        final Rollout rollout = testdataFactory.createAndStartRollout();
        final List<RolloutGroup> rolloutGroups = rolloutGroupManagement.findByRollout(PAGE, rollout.getId())
                .getContent();
        final RolloutGroup rolloutGroup = rolloutGroups.get(0);
        final List<Action> runningActions = findActionsByRolloutAndStatus(rollout, Status.RUNNING);
        final Target target0 = runningActions.get(0).getTarget();
        final Target target24 = CollectionUtils.lastElement(runningActions).getTarget();
        int i = 0;
        for (final Action action : runningActions) {
            controllerManagement.addUpdateActionStatus(
                    entityFactory.actionStatus().create(action.getId()).status(Status.RUNNING).code(i++));
        }

        List<TargetWithActionStatus> targetsWithActionStatus = rolloutGroupManagement
                .findAllTargetsOfRolloutGroupWithActionStatus(
                        PageRequest.of(0, 500, Sort.by(Direction.ASC, "lastActionStatusCode")),
                        rolloutGroup.getId())
                .getContent();
        assertSortedListOfActionStatus(targetsWithActionStatus, target0, 0, target24, 24);
        assertThat(targetsWithActionStatus)
                .hasSize((int) rolloutGroupManagement.countTargetsOfRolloutsGroup(rolloutGroup.getId()));

        targetsWithActionStatus = rolloutGroupManagement.findAllTargetsOfRolloutGroupWithActionStatus(
                PageRequest.of(0, 500, Sort.by(Direction.DESC, "lastActionStatusCode")), rolloutGroup.getId())
                .getContent();
        assertSortedListOfActionStatus(targetsWithActionStatus, target24, 24, target0, 0);
    }

    private void assertSortedListOfActionStatus(final List<TargetWithActionStatus> targetsWithActionStatus,
            final Target first, final Integer firstStatusCode, final Target last, final Integer lastStatusCode) {
        assertTargetAndActionStatusCode(CollectionUtils.firstElement(targetsWithActionStatus), first, firstStatusCode);
        assertTargetAndActionStatusCode(CollectionUtils.lastElement(targetsWithActionStatus), last, lastStatusCode);
    }

    private void assertTargetAndActionStatusCode(final TargetWithActionStatus targetWithActionStatus,
            final Target target, final Integer actionStatusCode) {
        assertThat(targetWithActionStatus.getTarget().getControllerId()).isEqualTo(target.getControllerId());
        assertThat(targetWithActionStatus.getLastActionStatusCode()).isEqualTo(actionStatusCode);
    }

    private void assertTargetNotNullAndActionStatusNullAndActionStatusCode(
            final List<TargetWithActionStatus> targetsWithActionStatus, final Integer actionStatusCode) {
        targetsWithActionStatus.forEach(targetWithActionStatus -> {
            assertThat(targetWithActionStatus.getTarget().getControllerId()).isNotNull();
            assertThat(targetWithActionStatus.getStatus()).isNull();
            assertThat(targetWithActionStatus.getLastActionStatusCode()).isEqualTo(actionStatusCode);
        });
    }

    private void assertTargetNotNullAndActionStatusAndActionStatusCode(
            final List<TargetWithActionStatus> targetsWithActionStatus, final Status actionStatus,
            final Integer actionStatusCode) {
        targetsWithActionStatus.forEach(targetWithActionStatus -> {
            assertThat(targetWithActionStatus.getTarget().getControllerId()).isNotNull();
            assertThat(targetWithActionStatus.getStatus()).isEqualTo(actionStatus);
            assertThat(targetWithActionStatus.getLastActionStatusCode()).isEqualTo(actionStatusCode);
        });
    }

    @Test
    @Description("Verifies that Rollouts in different states are handled correctly.")
    void findAllTargetsOfRolloutGroupWithActionStatus() {
        final Rollout rollout = testdataFactory.createRollout();
        final List<RolloutGroup> rolloutGroups = rolloutGroupManagement.findByRollout(PAGE, rollout.getId())
                .getContent();
        rolloutManagement.handleRollouts();

        // check query when no actions exist
        final List<TargetWithActionStatus> targetsWithActionStatus = rolloutGroupManagement
                .findAllTargetsOfRolloutGroupWithActionStatus(
                        PageRequest.of(0, 500, Sort.by(Direction.DESC, "lastActionStatusCode")),
                        rolloutGroups.get(0).getId())
                .getContent();
        assertThat(targetsWithActionStatus)
                .hasSize((int) rolloutGroupManagement.countTargetsOfRolloutsGroup(rolloutGroups.get(0).getId()));
        assertTargetNotNullAndActionStatusNullAndActionStatusCode(targetsWithActionStatus, null);

        rolloutManagement.start(rollout.getId());
        rolloutManagement.handleRollouts();

        // check query when no action status code exist
        final List<Action> scheduledActions = findActionsByRolloutAndStatus(rollout, Status.SCHEDULED);
        final RolloutGroup rolloutGroupScheduled = scheduledActions.get(0).getRolloutGroup();

        final List<TargetWithActionStatus> targetsWithActionStatusForScheduledRG = rolloutGroupManagement
                .findAllTargetsOfRolloutGroupWithActionStatus(
                        PageRequest.of(0, 500, Sort.by(Direction.DESC, "lastActionStatusCode")),
                        rolloutGroupScheduled.getId())
                .getContent();
        assertThat(targetsWithActionStatusForScheduledRG)
                .hasSize((int) rolloutGroupManagement.countTargetsOfRolloutsGroup(rolloutGroupScheduled.getId()));
        assertTargetNotNullAndActionStatusAndActionStatusCode(targetsWithActionStatusForScheduledRG,
                Status.SCHEDULED, null);

        final List<Action> runningActions = findActionsByRolloutAndStatus(rollout, Status.RUNNING);
        final RolloutGroup rolloutGroupRunning = runningActions.get(0).getRolloutGroup();
        for (final Action action : runningActions) {
            controllerManagement.addUpdateActionStatus(
                    entityFactory.actionStatus().create(action.getId()).status(Status.RUNNING).code(100));
        }

        // check query when action status code exists
        final List<TargetWithActionStatus> targetsWithActionStatusForRunningRG = rolloutGroupManagement
                .findAllTargetsOfRolloutGroupWithActionStatus(
                        PageRequest.of(0, 500, Sort.by(Direction.DESC, "lastActionStatusCode")),
                        rolloutGroupRunning.getId())
                .getContent();
        assertThat(targetsWithActionStatusForRunningRG)
                .hasSize((int) rolloutGroupManagement.countTargetsOfRolloutsGroup(rolloutGroupRunning.getId()));
        assertTargetNotNullAndActionStatusAndActionStatusCode(targetsWithActionStatusForRunningRG,
                Status.RUNNING, 100);
    }

    private void assertThatListIsSortedByTargetName(final List<TargetWithActionStatus> targets,
            final Direction sortDirection) {
        String previousName = null;
        for (final TargetWithActionStatus targetWithActionStatus : targets) {
            final String actualName = targetWithActionStatus.getTarget().getName();
            if (previousName != null) {
                if (Direction.ASC == sortDirection) {
                    assertThat(actualName).isGreaterThan(previousName);
                } else {
                    assertThat(actualName).isLessThan(previousName);
                }
            }
            previousName = actualName;
        }
    }

    private Target reloadTarget(final Target targetCancelled) {
        return targetManagement.get(targetCancelled.getId()).orElseThrow();
    }

}
