/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.RolloutIllegalStateException;
import org.eclipse.hawkbit.repository.exception.RolloutVerificationException;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.utils.MultipleInvokeHelper;
import org.eclipse.hawkbit.repository.jpa.utils.SuccessCondition;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import com.google.common.collect.Lists;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

/**
 * Junit tests for RolloutManagment.
 */
@Features("Component Tests - Repository")
@Stories("Rollout Management")
public class RolloutManagementTest extends AbstractJpaIntegrationTest {

    @Autowired
    private RolloutManagement rolloutManagement;

    @Autowired
    private RolloutGroupManagement rolloutGroupManagement;

    @Test
    @Description("Verifying that the rollout is created correctly, executing the filter and split up the targets in the correct group size.")
    public void creatingRolloutIsCorrectPersisted() {
        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 15;
        final int amountGroups = 5;
        final String successCondition = "50";
        final String errorCondition = "80";
        final Rollout createdRollout = createSimpleTestRolloutWithTargetsAndDistributionSet(amountTargetsForRollout,
                amountOtherTargets, amountGroups, successCondition, errorCondition);

        // verify the split of the target and targetGroup
        final Page<RolloutGroup> rolloutGroups = rolloutGroupManagement
                .findRolloutGroupsByRolloutId(createdRollout.getId(), pageReq);
        // we have total of #amountTargetsForRollout in rollouts splitted in
        // group size #groupSize
        assertThat(rolloutGroups).hasSize(amountGroups);
    }

    @Test
    @Description("Verifying that when the rollout is started the actions for all targets in the rollout is created and the state of the first group is running as well as the corresponding actions")
    public void startRolloutSetFirstGroupAndActionsInRunningStateAndOthersInScheduleState() {
        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 15;
        final int amountGroups = 5;
        final String successCondition = "50";
        final String errorCondition = "80";
        final Rollout createdRollout = createAndStartRollout(amountTargetsForRollout, amountOtherTargets, amountGroups,
                successCondition, errorCondition);

        // verify first group is running
        final RolloutGroup firstGroup = rolloutGroupManagement.findRolloutGroupsByRolloutId(createdRollout.getId(),
                new OffsetBasedPageRequest(0, 1, new Sort(Direction.ASC, "id"))).getContent().get(0);
        assertThat(firstGroup.getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);

        // verify other groups are scheduled
        final List<RolloutGroup> scheduledGroups = rolloutGroupManagement.findRolloutGroupsByRolloutId(
                createdRollout.getId(), new OffsetBasedPageRequest(1, 100, new Sort(Direction.ASC, "id"))).getContent();
        scheduledGroups.forEach(group -> assertThat(group.getStatus()).isEqualTo(RolloutGroupStatus.SCHEDULED)
                .as("group which should be in scheduled state is in " + group.getStatus() + " state"));
        // verify that the first group actions has been started and are in state
        // running
        final List<Action> runningActions = deploymentManagement.findActionsByRolloutAndStatus(createdRollout,
                Status.RUNNING);
        assertThat(runningActions).hasSize(amountTargetsForRollout / amountGroups);
        // the rest targets are only scheduled
        assertThat(deploymentManagement.findActionsByRolloutAndStatus(createdRollout, Status.SCHEDULED))
                .hasSize(amountTargetsForRollout - (amountTargetsForRollout / amountGroups));
    }

    @Test
    @Description("Verifying that a finish condition of a group is hit the next group of the rollout is also started")
    public void checkRunningRolloutsDoesNotStartNextGroupIfFinishConditionIsNotHit() {
        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 15;
        final int amountGroups = 5;
        final String successCondition = "50";
        final String errorCondition = "80";
        final Rollout createdRollout = createAndStartRollout(amountTargetsForRollout, amountOtherTargets, amountGroups,
                successCondition, errorCondition);

        final List<Action> runningActions = deploymentManagement.findActionsByRolloutAndStatus(createdRollout,
                Status.RUNNING);
        // finish one action should be sufficient due the finish condition is at
        // 50%
        final JpaAction action = (JpaAction) runningActions.get(0);
        action.setStatus(Status.FINISHED);
        controllerManagament
                .addUpdateActionStatus(new JpaActionStatus(action, Status.FINISHED, System.currentTimeMillis(), ""));

        // check running rollouts again, now the finish condition should be hit
        // and should start the next group
        rolloutManagement.checkRunningRollouts(0);

        // verify that now the first and the second group are in running state
        final List<RolloutGroup> runningRolloutGroups = rolloutGroupManagement.findRolloutGroupsByRolloutId(
                createdRollout.getId(), new OffsetBasedPageRequest(0, 2, new Sort(Direction.ASC, "id"))).getContent();
        runningRolloutGroups.forEach(group -> assertThat(group.getStatus()).isEqualTo(RolloutGroupStatus.RUNNING)
                .as("group should be in running state because it should be started but it is in " + group.getStatus()
                        + " state"));

        // verify that the other groups are still in schedule state
        final List<RolloutGroup> scheduledRolloutGroups = rolloutGroupManagement.findRolloutGroupsByRolloutId(
                createdRollout.getId(), new OffsetBasedPageRequest(2, 10, new Sort(Direction.ASC, "id"))).getContent();
        scheduledRolloutGroups.forEach(group -> assertThat(group.getStatus()).isEqualTo(RolloutGroupStatus.SCHEDULED)
                .as("group should be in scheduled state because it should not be started but it is in "
                        + group.getStatus() + " state"));
    }

    @Test
    @Title("Deleting targets of a rollout")
    @Description("Verfiying that next group is started when targets of the group have been deleted.")
    public void checkRunningRolloutsStartsNextGroupIfTargetsDeleted() {

        final int amountTargetsForRollout = 15;
        final int amountOtherTargets = 0;
        final int amountGroups = 3;
        final String successCondition = "100";
        final String errorCondition = "80";
        final Rollout createdRollout = createAndStartRollout(amountTargetsForRollout, amountOtherTargets, amountGroups,
                successCondition, errorCondition);

        finishActionAndDeleteTargetsOfFirstRunningGroup(createdRollout);

        checkSecondGroupStatusIsRunning(createdRollout);

        finishActionAndDeleteTargetsOfSecondRunningGroup(createdRollout);

        deleteAllTargetsFromThirdGroup(createdRollout);

        verifyRolloutAndAllGroupsAreFinished(createdRollout);

    }

    @Step("Create a rollout by the given parameter and start it.")
    private Rollout createAndStartRollout(final int amountTargetsForRollout, final int amountOtherTargets,
            final int amountGroups, final String successCondition, final String errorCondition) {

        final Rollout createdRollout = createSimpleTestRolloutWithTargetsAndDistributionSet(amountTargetsForRollout,
                amountOtherTargets, amountGroups, successCondition, errorCondition);
        rolloutManagement.startRollout(createdRollout);

        // Run here, because scheduler is disabled during tests
        rolloutManagement.checkStartingRollouts(0);

        return rolloutManagement.findRolloutById(createdRollout.getId());
    }

    @Step("Finish three actions of the rollout group and delete two targets")
    private void finishActionAndDeleteTargetsOfFirstRunningGroup(final Rollout createdRollout) {
        // finish group one by finishing targets and deleting targets
        final List<Action> runningActions = deploymentManagement.findActionsByRolloutAndStatus(createdRollout,
                Status.RUNNING);
        finishAction(runningActions.get(0));
        finishAction(runningActions.get(1));
        finishAction(runningActions.get(2));
        targetManagement.deleteTargets(runningActions.get(3).getTarget().getId(),
                runningActions.get(4).getTarget().getId());
    }

    @Step("Check the status of the rollout groups, second group should be in running status")
    private void checkSecondGroupStatusIsRunning(final Rollout createdRollout) {
        rolloutManagement.checkRunningRollouts(0);
        final List<RolloutGroup> runningRolloutGroups = rolloutGroupManagement.findRolloutGroupsByRolloutId(
                createdRollout.getId(), new OffsetBasedPageRequest(0, 10, new Sort(Direction.ASC, "id"))).getContent();
        assertThat(runningRolloutGroups.get(0).getStatus()).isEqualTo(RolloutGroupStatus.FINISHED);
        assertThat(runningRolloutGroups.get(1).getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);
        assertThat(runningRolloutGroups.get(2).getStatus()).isEqualTo(RolloutGroupStatus.SCHEDULED);
    }

    @Step("Finish one action of the rollout group and delete four targets")
    private void finishActionAndDeleteTargetsOfSecondRunningGroup(final Rollout createdRollout) {
        final List<Action> runningActions = deploymentManagement.findActionsByRolloutAndStatus(createdRollout,
                Status.RUNNING);
        finishAction(runningActions.get(0));
        targetManagement.deleteTargets(runningActions.get(1).getTarget().getId(),
                runningActions.get(2).getTarget().getId(), runningActions.get(3).getTarget().getId(),
                runningActions.get(4).getTarget().getId());

    }

    @Step("Delete all targets of the rollout group")
    private void deleteAllTargetsFromThirdGroup(final Rollout createdRollout) {
        final List<Action> runningActions = deploymentManagement.findActionsByRolloutAndStatus(createdRollout,
                Status.SCHEDULED);
        targetManagement.deleteTargets(runningActions.get(0).getTarget().getId(),
                runningActions.get(1).getTarget().getId(), runningActions.get(2).getTarget().getId(),
                runningActions.get(3).getTarget().getId(), runningActions.get(4).getTarget().getId());
    }

    @Step("Check the status of the rollout groups and the rollout")
    private void verifyRolloutAndAllGroupsAreFinished(final Rollout createdRollout) {
        rolloutManagement.checkRunningRollouts(0);
        final List<RolloutGroup> runningRolloutGroups = rolloutGroupManagement.findRolloutGroupsByRolloutId(
                createdRollout.getId(), new OffsetBasedPageRequest(0, 10, new Sort(Direction.ASC, "id"))).getContent();
        assertThat(runningRolloutGroups.get(0).getStatus()).isEqualTo(RolloutGroupStatus.FINISHED);
        assertThat(runningRolloutGroups.get(1).getStatus()).isEqualTo(RolloutGroupStatus.FINISHED);
        assertThat(runningRolloutGroups.get(2).getStatus()).isEqualTo(RolloutGroupStatus.FINISHED);
        assertThat(rolloutManagement.findRolloutById(createdRollout.getId()).getStatus())
                .isEqualTo(RolloutStatus.FINISHED);

    }

    private void finishAction(final Action action) {
        final JpaAction jpaAction = (JpaAction) action;
        action.setStatus(Status.FINISHED);
        controllerManagament
                .addUpdateActionStatus(new JpaActionStatus(jpaAction, Status.FINISHED, System.currentTimeMillis(), ""));
    }

    @Test
    @Description("Verfiying that the error handling action of a group is executed to pause the current rollout")
    public void checkErrorHitOfGroupCallsErrorActionToPauseTheRollout() {
        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 15;
        final int amountGroups = 5;
        final String successCondition = "50";
        final String errorCondition = "80";
        final Rollout createdRollout = createAndStartRollout(amountTargetsForRollout, amountOtherTargets, amountGroups,
                successCondition, errorCondition);

        // set both actions in error state so error condition is hit and error
        // action is executed
        final List<Action> runningActions = deploymentManagement.findActionsByRolloutAndStatus(createdRollout,
                Status.RUNNING);

        // finish actions with error
        for (final Action action : runningActions) {
            action.setStatus(Status.ERROR);
            controllerManagament.addUpdateActionStatus(
                    new JpaActionStatus((JpaAction) action, Status.ERROR, System.currentTimeMillis(), ""));
        }

        // check running rollouts again, now the error condition should be hit
        // and should execute the error action
        rolloutManagement.checkRunningRollouts(0);

        final Rollout rollout = rolloutManagement.findRolloutById(createdRollout.getId());
        // the rollout itself should be in paused based on the error action
        assertThat(rollout.getStatus()).isEqualTo(RolloutStatus.PAUSED);

        // the first rollout group should be in error state
        final List<RolloutGroup> errorGroup = rolloutGroupManagement.findRolloutGroupsByRolloutId(
                createdRollout.getId(), new OffsetBasedPageRequest(0, 1, new Sort(Direction.ASC, "id"))).getContent();
        assertThat(errorGroup).hasSize(1);
        assertThat(errorGroup.get(0).getStatus()).isEqualTo(RolloutGroupStatus.ERROR);

        // all other groups should still be in scheduled state
        final List<RolloutGroup> scheduleGroups = rolloutGroupManagement.findRolloutGroupsByRolloutId(
                createdRollout.getId(), new OffsetBasedPageRequest(1, 100, new Sort(Direction.ASC, "id"))).getContent();
        scheduleGroups.forEach(group -> assertThat(group.getStatus()).isEqualTo(RolloutGroupStatus.SCHEDULED));
    }

    @Test
    @Description("Verfiying a paused rollout in case of error action hit can be resumed again")
    public void errorActionPausesRolloutAndRolloutGetsResumedStartsNextScheduledGroup() {
        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 15;
        final int amountGroups = 5;
        final String successCondition = "50";
        final String errorCondition = "80";
        final Rollout createdRollout = createAndStartRollout(amountTargetsForRollout, amountOtherTargets, amountGroups,
                successCondition, errorCondition);

        // set both actions in error state so error condition is hit and error
        // action is executed
        final List<Action> runningActions = deploymentManagement.findActionsByRolloutAndStatus(createdRollout,
                Status.RUNNING);
        // finish actions with error
        for (final Action action : runningActions) {
            action.setStatus(Status.ERROR);
            controllerManagament.addUpdateActionStatus(
                    new JpaActionStatus((JpaAction) action, Status.ERROR, System.currentTimeMillis(), ""));
        }

        // check running rollouts again, now the error condition should be hit
        // and should execute the error action
        rolloutManagement.checkRunningRollouts(0);

        final Rollout rollout = rolloutManagement.findRolloutById(createdRollout.getId());
        // the rollout itself should be in paused based on the error action
        assertThat(rollout.getStatus()).isEqualTo(RolloutStatus.PAUSED);

        // all other groups should still be in scheduled state
        final List<RolloutGroup> scheduleGroups = rolloutGroupManagement.findRolloutGroupsByRolloutId(
                createdRollout.getId(), new OffsetBasedPageRequest(1, 100, new Sort(Direction.ASC, "id"))).getContent();
        scheduleGroups.forEach(group -> assertThat(group.getStatus()).isEqualTo(RolloutGroupStatus.SCHEDULED));

        // resume the rollout again after it gets paused by error action
        rolloutManagement.resumeRollout(rolloutManagement.findRolloutById(createdRollout.getId()));

        // the rollout should be running again
        assertThat(rolloutManagement.findRolloutById(createdRollout.getId()).getStatus())
                .isEqualTo(RolloutStatus.RUNNING);

        // checking rollouts again
        rolloutManagement.checkRunningRollouts(0);

        // next group should be running again after resuming the rollout
        final List<RolloutGroup> resumedGroups = rolloutGroupManagement.findRolloutGroupsByRolloutId(
                createdRollout.getId(), new OffsetBasedPageRequest(1, 1, new Sort(Direction.ASC, "id"))).getContent();
        assertThat(resumedGroups).hasSize(1);
        assertThat(resumedGroups.get(0).getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);
    }

    @Test
    @Description("Verfiying that the rollout is starting group after group and gets finished at the end")
    public void rolloutStartsGroupAfterGroupAndGetsFinished() {
        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 15;
        final int amountGroups = 5;
        final String successCondition = "50";
        final String errorCondition = "80";
        final Rollout createdRollout = createAndStartRollout(amountTargetsForRollout, amountOtherTargets, amountGroups,
                successCondition, errorCondition);

        // finish running actions, 2 actions should be finished
        assertThat(changeStatusForAllRunningActions(createdRollout, Status.FINISHED)).isEqualTo(2);

        // calculate the rest of the groups and finish them
        for (int groupsLeft = amountGroups - 1; groupsLeft >= 1; groupsLeft--) {
            // next check and start next group
            rolloutManagement.checkRunningRollouts(0);
            // finish running actions, 2 actions should be finished
            assertThat(changeStatusForAllRunningActions(createdRollout, Status.FINISHED)).isEqualTo(2);
            assertThat(rolloutManagement.findRolloutById(createdRollout.getId()).getStatus())
                    .isEqualTo(RolloutStatus.RUNNING);

        }
        // check rollout to see that all actions and all groups are finished and
        // so can go to FINISHED state of the rollout
        rolloutManagement.checkRunningRollouts(0);

        // verify all groups are in finished state
        rolloutGroupManagement
                .findRolloutGroupsByRolloutId(createdRollout.getId(),
                        new OffsetBasedPageRequest(0, 100, new Sort(Direction.ASC, "id")))
                .forEach(group -> assertThat(group.getStatus()).isEqualTo(RolloutGroupStatus.FINISHED));

        // verify that rollout itself is in finished state
        final Rollout findRolloutById = rolloutManagement.findRolloutById(createdRollout.getId());
        assertThat(findRolloutById.getStatus()).isEqualTo(RolloutStatus.FINISHED);
    }

    @Test
    @Description("Verify that the targets have the right status during the rollout.")
    public void countCorrectStatusForEachTargetDuringRollout() {

        final int amountTargetsForRollout = 8;
        final int amountOtherTargets = 15;
        final int amountGroups = 4;
        final String successCondition = "50";
        final String errorCondition = "80";
        final Rollout createdRollout = createSimpleTestRolloutWithTargetsAndDistributionSet(amountTargetsForRollout,
                amountOtherTargets, amountGroups, successCondition, errorCondition);

        // targets have not started
        Map<TotalTargetCountStatus.Status, Long> validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.NOTSTARTED, 8L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        rolloutManagement.startRollout(createdRollout);

        // Run here, because scheduler is disabled during tests
        rolloutManagement.checkStartingRollouts(0);

        // 6 targets are ready and 2 are running
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.SCHEDULED, 6L);
        validationMap.put(TotalTargetCountStatus.Status.RUNNING, 2L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        changeStatusForAllRunningActions(createdRollout, Status.FINISHED);
        rolloutManagement.checkRunningRollouts(0);
        // 4 targets are ready, 2 are finished and 2 are running
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.SCHEDULED, 4L);
        validationMap.put(TotalTargetCountStatus.Status.FINISHED, 2L);
        validationMap.put(TotalTargetCountStatus.Status.RUNNING, 2L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        changeStatusForAllRunningActions(createdRollout, Status.FINISHED);
        rolloutManagement.checkRunningRollouts(0);
        // 2 targets are ready, 4 are finished and 2 are running
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.SCHEDULED, 2L);
        validationMap.put(TotalTargetCountStatus.Status.FINISHED, 4L);
        validationMap.put(TotalTargetCountStatus.Status.RUNNING, 2L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        changeStatusForAllRunningActions(createdRollout, Status.FINISHED);
        rolloutManagement.checkRunningRollouts(0);
        // 0 targets are ready, 6 are finished and 2 are running
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.FINISHED, 6L);
        validationMap.put(TotalTargetCountStatus.Status.RUNNING, 2L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        changeStatusForAllRunningActions(createdRollout, Status.FINISHED);
        rolloutManagement.checkRunningRollouts(0);
        // 0 targets are ready, 8 are finished and 0 are running
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.FINISHED, 8L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

    }

    @Test
    @Description("Verify that the targets have the right status during the rollout when an error emerges.")
    public void countCorrectStatusForEachTargetDuringRolloutWithError() {

        final int amountTargetsForRollout = 8;
        final int amountOtherTargets = 15;
        final int amountGroups = 4;
        final String successCondition = "50";
        final String errorCondition = "80";
        final Rollout createdRollout = createSimpleTestRolloutWithTargetsAndDistributionSet(amountTargetsForRollout,
                amountOtherTargets, amountGroups, successCondition, errorCondition);

        // 8 targets have not started
        Map<TotalTargetCountStatus.Status, Long> validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.NOTSTARTED, 8L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        rolloutManagement.startRollout(createdRollout);

        // Run here, because scheduler is disabled during tests
        rolloutManagement.checkStartingRollouts(0);

        // 6 targets are ready and 2 are running
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.SCHEDULED, 6L);
        validationMap.put(TotalTargetCountStatus.Status.RUNNING, 2L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        changeStatusForAllRunningActions(createdRollout, Status.ERROR);
        rolloutManagement.checkRunningRollouts(0);
        // 6 targets are ready and 2 are error
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.SCHEDULED, 6L);
        validationMap.put(TotalTargetCountStatus.Status.ERROR, 2L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);
    }

    @Test
    @Description("Verify that the targets have the right status during the rollout when receiving the status of rollout groups.")
    public void countCorrectStatusForEachTargetGroupDuringRollout() {

        final int amountTargetsForRollout = 9;
        final int amountOtherTargets = 15;
        final int amountGroups = 4;
        final String successCondition = "50";
        final String errorCondition = "80";
        Rollout createdRollout = createAndStartRollout(amountTargetsForRollout, amountOtherTargets, amountGroups,
                successCondition, errorCondition);

        changeStatusForAllRunningActions(createdRollout, Status.FINISHED);
        rolloutManagement.checkRunningRollouts(0);

        // round(9/4)=2 targets finished (Group 1)
        // round(7/3)=2 targets running (Group 3)
        // round(5/2)=3 targets SCHEDULED (Group 3)
        // round(2/1)=2 targets SCHEDULED (Group 4)
        createdRollout = rolloutManagement.findRolloutById(createdRollout.getId());
        final List<RolloutGroup> rolloutGroups = createdRollout.getRolloutGroups();

        Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.FINISHED, 2L);
        validateRolloutGroupActionStatus(rolloutGroups.get(0), expectedTargetCountStatus);

        expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.RUNNING, 2L);
        validateRolloutGroupActionStatus(rolloutGroups.get(1), expectedTargetCountStatus);

        expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.SCHEDULED, 3L);
        validateRolloutGroupActionStatus(rolloutGroups.get(2), expectedTargetCountStatus);

        expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.SCHEDULED, 2L);
        validateRolloutGroupActionStatus(rolloutGroups.get(3), expectedTargetCountStatus);

    }

    @Test
    @Description("Verify that target actions of rollout get canceled when a manuel distribution sets assignment is done.")
    public void targetsOfRolloutGetsManuelDsAssignment() {

        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 0;
        final int amountGroups = 2;
        final String successCondition = "50";
        final String errorCondition = "80";
        Rollout createdRollout = createAndStartRollout(amountTargetsForRollout, amountOtherTargets, amountGroups,
                successCondition, errorCondition);
        final DistributionSet ds = createdRollout.getDistributionSet();

        createdRollout = rolloutManagement.findRolloutById(createdRollout.getId());
        // 5 targets are running
        final List<Action> runningActions = deploymentManagement.findActionsByRolloutAndStatus(createdRollout,
                Status.RUNNING);
        assertThat(runningActions.size()).isEqualTo(5);

        // 5 targets are in the group and the DS has been assigned
        final List<RolloutGroup> rolloutGroups = createdRollout.getRolloutGroups();
        final Page<Target> targets = rolloutGroupManagement.findRolloutGroupTargets(rolloutGroups.get(0),
                new OffsetBasedPageRequest(0, 20, new Sort(Direction.ASC, "id")));
        final List<Target> targetList = targets.getContent();
        assertThat(targetList.size()).isEqualTo(5);
        for (final Target t : targetList) {
            final DistributionSet assignedDs = t.getAssignedDistributionSet();
            assertThat(assignedDs.getId()).isEqualTo(ds.getId());
        }

        final List<Target> targetToCancel = new ArrayList<Target>();
        targetToCancel.add(targetList.get(0));
        targetToCancel.add(targetList.get(1));
        targetToCancel.add(targetList.get(2));
        final DistributionSet dsForCancelTest = testdataFactory.createDistributionSet("dsForTest");
        deploymentManagement.assignDistributionSet(dsForCancelTest, targetToCancel);
        // 5 targets are canceling but still have the status running and 5 are
        // still in SCHEDULED
        final Map<TotalTargetCountStatus.Status, Long> validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.RUNNING, 5L);
        validationMap.put(TotalTargetCountStatus.Status.SCHEDULED, 5L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);
    }

    @Test
    @Description("Verify that target actions of a rollout get cancelled when another rollout with same targets gets started.")
    public void targetsOfRolloutGetDistributionSetAssignmentByOtherRollout() {

        final int amountTargetsForRollout = 15;
        final int amountOtherTargets = 5;
        final int amountGroups = 3;
        final String successCondition = "50";
        final String errorCondition = "80";
        Rollout rolloutOne = createAndStartRollout(amountTargetsForRollout, amountOtherTargets, amountGroups,
                successCondition, errorCondition);

        rolloutOne = rolloutManagement.findRolloutById(rolloutOne.getId());

        final DistributionSet dsForRolloutTwo = testdataFactory.createDistributionSet("dsForRolloutTwo");

        final Rollout rolloutTwo = createRolloutByVariables("rolloutTwo", "This is the description for rollout two", 1,
                "controllerId==rollout-*", dsForRolloutTwo, "50", "80");
        changeStatusForAllRunningActions(rolloutOne, Status.FINISHED);
        rolloutManagement.checkRunningRollouts(0);
        // Verify that 5 targets are finished, 5 are running and 5 are ready.
        Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.RUNNING, 5L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.FINISHED, 5L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.SCHEDULED, 5L);
        validateRolloutActionStatus(rolloutOne.getId(), expectedTargetCountStatus);

        rolloutManagement.startRollout(rolloutTwo);

        // Run here, because scheduler is disabled during tests
        rolloutManagement.checkStartingRollouts(0);

        // Verify that 5 targets are finished, 5 are still running and 5 are
        // cancelled.
        expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.FINISHED, 5L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.RUNNING, 5L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.CANCELLED, 5L);
        validateRolloutActionStatus(rolloutOne.getId(), expectedTargetCountStatus);
    }

    @Test
    @Description("Verify that error status of DistributionSet installation during rollout can get rerun with second rollout so that all targets have some DistributionSet installed at the end.")
    public void startSecondRolloutAfterFristRolloutEndenWithErrors() {

        final int amountTargetsForRollout = 15;
        final int amountOtherTargets = 0;
        final int amountGroups = 3;
        final String successCondition = "50";
        final String errorCondition = "80";
        Rollout rolloutOne = createAndStartRollout(amountTargetsForRollout, amountOtherTargets, amountGroups,
                successCondition, errorCondition);
        final DistributionSet distributionSet = rolloutOne.getDistributionSet();

        rolloutOne = rolloutManagement.findRolloutById(rolloutOne.getId());
        changeStatusForRunningActions(rolloutOne, Status.ERROR, 2);
        changeStatusForRunningActions(rolloutOne, Status.FINISHED, 3);
        rolloutManagement.checkRunningRollouts(0);
        changeStatusForRunningActions(rolloutOne, Status.ERROR, 2);
        changeStatusForRunningActions(rolloutOne, Status.FINISHED, 3);
        rolloutManagement.checkRunningRollouts(0);
        changeStatusForRunningActions(rolloutOne, Status.ERROR, 2);
        changeStatusForRunningActions(rolloutOne, Status.FINISHED, 3);
        rolloutManagement.checkRunningRollouts(0);

        // 9 targets are finished and 6 have error
        Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.FINISHED, 9L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.ERROR, 6L);
        validateRolloutActionStatus(rolloutOne.getId(), expectedTargetCountStatus);
        // rollout is finished
        rolloutOne = rolloutManagement.findRolloutById(rolloutOne.getId());
        assertThat(rolloutOne.getStatus()).isEqualTo(RolloutStatus.FINISHED);

        final int amountGroupsForRolloutTwo = 1;
        Rollout rolloutTwo = createRolloutByVariables("rolloutTwo", "This is the description for rollout two",
                amountGroupsForRolloutTwo, "controllerId==rollout-*", distributionSet, "50", "80");

        rolloutManagement.startRollout(rolloutTwo);

        // Run here, because scheduler is disabled during tests
        rolloutManagement.checkStartingRollouts(0);

        rolloutTwo = rolloutManagement.findRolloutById(rolloutTwo.getId());
        // 6 error targets are know running
        expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.RUNNING, 6L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.FINISHED, 9L);
        validateRolloutActionStatus(rolloutTwo.getId(), expectedTargetCountStatus);
        changeStatusForAllRunningActions(rolloutTwo, Status.FINISHED);
        final Page<Target> targetPage = targetManagement.findTargetByUpdateStatus(pageReq, TargetUpdateStatus.IN_SYNC);
        final List<Target> targetList = targetPage.getContent();
        // 15 targets in finished/IN_SYNC status and same DS assigned
        assertThat(targetList.size()).isEqualTo(amountTargetsForRollout);
        for (final Target t : targetList) {
            final DistributionSet ds = t.getAssignedDistributionSet();
            assertThat(ds).isEqualTo(distributionSet);
        }
    }

    @Test
    @Description("Verify that the rollout moves to the next group when the success condition was achieved and the error condition was not exceeded.")
    public void successConditionAchievedAndErrorConditionNotExceeded() {

        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 0;
        final int amountGroups = 2;
        final String successCondition = "50";
        final String errorCondition = "80";
        Rollout rolloutOne = createAndStartRollout(amountTargetsForRollout, amountOtherTargets, amountGroups,
                successCondition, errorCondition);

        rolloutOne = rolloutManagement.findRolloutById(rolloutOne.getId());
        changeStatusForRunningActions(rolloutOne, Status.ERROR, 2);
        changeStatusForRunningActions(rolloutOne, Status.FINISHED, 3);
        rolloutManagement.checkRunningRollouts(0);
        // verify: 40% error but 60% finished -> should move to next group
        final List<RolloutGroup> rolloutGruops = rolloutOne.getRolloutGroups();
        final Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.RUNNING, 5L);
        validateRolloutGroupActionStatus(rolloutGruops.get(1), expectedTargetCountStatus);

    }

    @Test
    @Description("Verify that the rollout does not move to the next group when the sucess condition was not achieved.")
    public void successConditionNotAchieved() {

        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 0;
        final int amountGroups = 2;
        final String successCondition = "80";
        final String errorCondition = "90";
        Rollout rolloutOne = createAndStartRollout(amountTargetsForRollout, amountOtherTargets, amountGroups,
                successCondition, errorCondition);

        rolloutOne = rolloutManagement.findRolloutById(rolloutOne.getId());
        changeStatusForRunningActions(rolloutOne, Status.ERROR, 2);
        changeStatusForRunningActions(rolloutOne, Status.FINISHED, 3);
        rolloutManagement.checkRunningRollouts(0);
        // verify: 40% error and 60% finished -> should not move to next group
        // because successCondition 80%
        rolloutOne = rolloutManagement.findRolloutById(rolloutOne.getId());
        final List<RolloutGroup> rolloutGruops = rolloutOne.getRolloutGroups();
        final Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.SCHEDULED, 5L);
        validateRolloutGroupActionStatus(rolloutGruops.get(1), expectedTargetCountStatus);
    }

    @Test
    @Description("Verify that the rollout pauses when the error condition was exceeded.")
    public void errorConditionExceeded() {

        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 0;
        final int amountGroups = 2;
        final String successCondition = "50";
        final String errorCondition = "20";
        Rollout rolloutOne = createAndStartRollout(amountTargetsForRollout, amountOtherTargets, amountGroups,
                successCondition, errorCondition);

        rolloutOne = rolloutManagement.findRolloutById(rolloutOne.getId());
        changeStatusForRunningActions(rolloutOne, Status.ERROR, 2);
        changeStatusForRunningActions(rolloutOne, Status.FINISHED, 3);
        rolloutManagement.checkRunningRollouts(0);
        // verify: 40% error -> should pause because errorCondition is 20%
        rolloutOne = rolloutManagement.findRolloutById(rolloutOne.getId());
        assertThat(RolloutStatus.PAUSED).isEqualTo(rolloutOne.getStatus());
    }

    @Test
    @Description("Verify that all rollouts are return with expected target statuses.")
    public void findAllRolloutsWithDetailedStatus() {

        final int amountTargetsForRollout = 12;
        final int amountGroups = 2;
        final String successCondition = "50";
        final String errorCondition = "20";
        final Rollout rolloutA = createTestRolloutWithTargetsAndDistributionSet(amountTargetsForRollout, amountGroups,
                successCondition, errorCondition, "RolloutA", "RolloutA");
        rolloutManagement.startRollout(rolloutA);
        rolloutManagement.checkStartingRollouts(0);

        final int amountTargetsForRollout2 = 10;
        final int amountGroups2 = 2;
        final Rollout rolloutB = createTestRolloutWithTargetsAndDistributionSet(amountTargetsForRollout2, amountGroups2,
                successCondition, errorCondition, "RolloutB", "RolloutB");
        rolloutManagement.startRollout(rolloutB);
        rolloutManagement.checkStartingRollouts(0);

        changeStatusForAllRunningActions(rolloutB, Status.FINISHED);
        rolloutManagement.checkRunningRollouts(0);

        final int amountTargetsForRollout3 = 10;
        final int amountGroups3 = 2;
        final Rollout rolloutC = createTestRolloutWithTargetsAndDistributionSet(amountTargetsForRollout3, amountGroups3,
                successCondition, errorCondition, "RolloutC", "RolloutC");
        rolloutManagement.startRollout(rolloutC);
        rolloutManagement.checkStartingRollouts(0);

        changeStatusForAllRunningActions(rolloutC, Status.ERROR);
        rolloutManagement.checkRunningRollouts(0);

        final int amountTargetsForRollout4 = 15;
        final int amountGroups4 = 3;
        final Rollout rolloutD = createTestRolloutWithTargetsAndDistributionSet(amountTargetsForRollout4, amountGroups4,
                successCondition, errorCondition, "RolloutD", "RolloutD");
        rolloutManagement.startRollout(rolloutD);
        rolloutManagement.checkStartingRollouts(0);

        changeStatusForRunningActions(rolloutD, Status.ERROR, 1);
        rolloutManagement.checkRunningRollouts(0);
        changeStatusForAllRunningActions(rolloutD, Status.FINISHED);
        rolloutManagement.checkRunningRollouts(0);

        final Page<Rollout> rolloutPage = rolloutManagement
                .findAllRolloutsWithDetailedStatus(new OffsetBasedPageRequest(0, 100, new Sort(Direction.ASC, "name")));
        final List<Rollout> rolloutList = rolloutPage.getContent();

        // validate rolloutA -> 6 running and 6 ready
        Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.RUNNING, 6L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.SCHEDULED, 6L);
        validateRolloutActionStatus(rolloutList.get(0).getId(), expectedTargetCountStatus);

        // validate rolloutB -> 5 running and 5 finished
        expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.FINISHED, 5L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.RUNNING, 5L);
        validateRolloutActionStatus(rolloutList.get(1).getId(), expectedTargetCountStatus);

        // validate rolloutC -> 5 running and 5 error
        expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.ERROR, 5L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.SCHEDULED, 5L);
        validateRolloutActionStatus(rolloutList.get(2).getId(), expectedTargetCountStatus);

        // validate rolloutD -> 1, error, 4 finished, 5 running and 5 ready
        expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.ERROR, 1L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.FINISHED, 4L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.RUNNING, 5L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.SCHEDULED, 5L);
        validateRolloutActionStatus(rolloutList.get(3).getId(), expectedTargetCountStatus);
    }

    @Test
    @Description("Verify the count of existing rollouts.")
    public void rightCountForAllRollouts() {

        final int amountTargetsForRollout = 6;
        final int amountGroups = 2;
        final String successCondition = "50";
        final String errorCondition = "80";
        for (int i = 1; i <= 10; i++) {
            createTestRolloutWithTargetsAndDistributionSet(amountTargetsForRollout, amountGroups, successCondition,
                    errorCondition, "Rollout" + i, "Rollout" + i);
        }
        final Long count = rolloutManagement.countRolloutsAll();
        assertThat(count).isEqualTo(10L);
    }

    @Test
    @Description("Verify the count of filtered existing rollouts.")
    public void countRolloutsAllByFilters() {

        final int amountTargetsForRollout = 6;
        final int amountGroups = 2;
        final String successCondition = "50";
        final String errorCondition = "80";
        for (int i = 1; i <= 5; i++) {
            createTestRolloutWithTargetsAndDistributionSet(amountTargetsForRollout, amountGroups, successCondition,
                    errorCondition, "Rollout" + i, "Rollout" + i);
        }
        for (int i = 1; i <= 5; i++) {
            createTestRolloutWithTargetsAndDistributionSet(amountTargetsForRollout, amountGroups, successCondition,
                    errorCondition, "SomethingElse" + i, "SomethingElse" + i);
        }

        final Long count = rolloutManagement.countRolloutsAllByFilters("Rollout%");
        assertThat(count).isEqualTo(5L);

    }

    @Test
    @Description("Verify that the filtering and sorting ascending for rollout is working correctly.")
    public void findRolloutByFilters() {

        final int amountTargetsForRollout = 6;
        final int amountGroups = 2;
        final String successCondition = "50";
        final String errorCondition = "80";
        for (int i = 1; i <= 5; i++) {
            createTestRolloutWithTargetsAndDistributionSet(amountTargetsForRollout, amountGroups, successCondition,
                    errorCondition, "Rollout" + i, "Rollout" + i);
        }
        for (int i = 1; i <= 8; i++) {
            createTestRolloutWithTargetsAndDistributionSet(amountTargetsForRollout, amountGroups, successCondition,
                    errorCondition, "SomethingElse" + i, "SomethingElse" + i);
        }

        final Slice<Rollout> rollout = rolloutManagement
                .findRolloutByFilters(new OffsetBasedPageRequest(0, 100, new Sort(Direction.ASC, "name")), "Rollout%");
        final List<Rollout> rolloutList = rollout.getContent();
        assertThat(rolloutList.size()).isEqualTo(5);
        int i = 1;
        for (final Rollout r : rolloutList) {
            assertThat(r.getName()).isEqualTo("Rollout" + i);
            i++;
        }
    }

    @Test
    @Description("Verify that the expected rollout is found by name.")
    public void findRolloutByName() {

        final int amountTargetsForRollout = 12;
        final int amountGroups = 2;
        final String successCondition = "50";
        final String errorCondition = "80";
        final String rolloutName = "Rollout137";
        final Rollout rolloutCreated = createTestRolloutWithTargetsAndDistributionSet(amountTargetsForRollout,
                amountGroups, successCondition, errorCondition, rolloutName, "RolloutA");

        final Rollout rolloutFound = rolloutManagement.findRolloutByName(rolloutName);
        assertThat(rolloutCreated).isEqualTo(rolloutFound);

    }

    @Test
    @Description("Verify that the percent count is acting like aspected when targets move to the status finished or error.")
    public void getFinishedPercentForRunningGroup() {

        final int amountTargetsForRollout = 10;
        final int amountGroups = 2;
        final String successCondition = "50";
        final String errorCondition = "80";
        final String rolloutName = "MyRollout";
        Rollout myRollout = createTestRolloutWithTargetsAndDistributionSet(amountTargetsForRollout, amountGroups,
                successCondition, errorCondition, rolloutName, rolloutName);
        rolloutManagement.startRollout(myRollout);

        // Run here, because scheduler is disabled during tests
        rolloutManagement.checkStartingRollouts(0);

        changeStatusForRunningActions(myRollout, Status.FINISHED, 2);
        rolloutManagement.checkRunningRollouts(0);
        myRollout = rolloutManagement.findRolloutById(myRollout.getId());

        float percent = rolloutManagement.getFinishedPercentForRunningGroup(myRollout.getId(),
                myRollout.getRolloutGroups().get(0));
        assertThat(percent).isEqualTo(40);

        changeStatusForRunningActions(myRollout, Status.FINISHED, 3);
        rolloutManagement.checkRunningRollouts(0);

        percent = rolloutManagement.getFinishedPercentForRunningGroup(myRollout.getId(),
                myRollout.getRolloutGroups().get(0));
        assertThat(percent).isEqualTo(100);

        changeStatusForRunningActions(myRollout, Status.FINISHED, 4);
        changeStatusForAllRunningActions(myRollout, Status.ERROR);
        rolloutManagement.checkRunningRollouts(0);

        percent = rolloutManagement.getFinishedPercentForRunningGroup(myRollout.getId(),
                myRollout.getRolloutGroups().get(1));
        assertThat(percent).isEqualTo(80);
    }

    @Test
    @Description("Verify that the expected targets in the expected order are returned for the rollout groups.")
    public void findRolloutGroupTargetsWithRsqlParam() {

        final int amountTargetsForRollout = 15;
        final int amountGroups = 3;
        final int amountOtherTargets = 15;
        final String successCondition = "50";
        final String errorCondition = "80";
        final String rolloutName = "MyRollout";
        Rollout myRollout = createTestRolloutWithTargetsAndDistributionSet(amountTargetsForRollout, amountGroups,
                successCondition, errorCondition, rolloutName, rolloutName);

        targetManagement.createTargets(testdataFactory.generateTargets(amountOtherTargets, "others-", "rollout"));

        final String rsqlParam = "controllerId==*MyRoll*";

        rolloutManagement.startRollout(myRollout);

        // Run here, because scheduler is disabled during tests
        rolloutManagement.checkStartingRollouts(0);

        myRollout = rolloutManagement.findRolloutById(myRollout.getId());
        final List<RolloutGroup> rolloutGroups = myRollout.getRolloutGroups();

        Page<Target> targetPage = rolloutGroupManagement.findRolloutGroupTargets(rolloutGroups.get(0), rsqlParam,
                new OffsetBasedPageRequest(0, 100, new Sort(Direction.ASC, "controllerId")));
        final List<Target> targetlistGroup1 = targetPage.getContent();
        assertThat(targetlistGroup1.size()).isEqualTo(5);
        assertThat(targetlistGroup1.get(0).getControllerId()).isEqualTo("MyRollout--00000");

        targetPage = rolloutGroupManagement.findRolloutGroupTargets(rolloutGroups.get(1), rsqlParam,
                new OffsetBasedPageRequest(0, 100, new Sort(Direction.DESC, "controllerId")));
        final List<Target> targetlistGroup2 = targetPage.getContent();
        assertThat(targetlistGroup2.size()).isEqualTo(5);
        assertThat(targetlistGroup2.get(0).getControllerId()).isEqualTo("MyRollout--00009");

        targetPage = rolloutGroupManagement.findRolloutGroupTargets(rolloutGroups.get(2), rsqlParam,
                new OffsetBasedPageRequest(0, 100, new Sort(Direction.ASC, "controllerId")));
        final List<Target> targetlistGroup3 = targetPage.getContent();
        assertThat(targetlistGroup3.size()).isEqualTo(5);
        assertThat(targetlistGroup3.get(0).getControllerId()).isEqualTo("MyRollout--00010");

    }

    @Test
    @Description("Verify the creation of a Rollout without targets throws an Exception.")
    public void createRolloutNotMatchingTargets() {
        final int amountGroups = 5;
        final String successCondition = "50";
        final String errorCondition = "80";
        final String rolloutName = "rolloutTest3";

        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsFor" + rolloutName);

        try {
            createRolloutByVariables(rolloutName, "desc", amountGroups, "id==notExisting", distributionSet,
                    successCondition, errorCondition);
            fail("Was able to create a Rollout without targets.");
        } catch(RolloutVerificationException e) {
            // OK
        }

    }

    @Test
    @Description("Verify the creation of a Rollout with the same name throws an Exception.")
    public void createDuplicateRollout() {
        final int amountGroups = 5;
        final int amountTargetsForRollout = 10;
        final String successCondition = "50";
        final String errorCondition = "80";
        final String rolloutName = "rolloutTest4";

        targetManagement.createTargets(testdataFactory.generateTargets(amountTargetsForRollout, "dup-ro-", "rollout"));

        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsFor" + rolloutName);
        createRolloutByVariables(rolloutName, "desc", amountGroups, "id==dup-ro-*", distributionSet,
                successCondition, errorCondition);

        try {
            createRolloutByVariables(rolloutName, "desc", amountGroups, "id==dup-ro-*", distributionSet,
                    successCondition, errorCondition);
            fail("Was able to create a duplicate Rollout.");
        } catch(EntityAlreadyExistsException e) {
            // OK
        }

    }

    @Test
    @Description("Verify the creation and the start of a Rollout with more groups than targets.")
    public void createAndStartRolloutWithEmptyGroups() throws Exception {
        final int amountTargetsForRollout = 3;
        final int amountGroups = 5;
        final String successCondition = "50";
        final String errorCondition = "80";
        final String rolloutName = "rolloutTestG";
        final String targetPrefixName = rolloutName;
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsFor" + rolloutName);
        targetManagement.createTargets(
                testdataFactory.generateTargets(amountTargetsForRollout, targetPrefixName + "-", targetPrefixName));

        Rollout myRollout = createRolloutByVariables(rolloutName, "desc", amountGroups,
                "controllerId==" + targetPrefixName + "-*", distributionSet, successCondition, errorCondition);

        assertThat(myRollout.getStatus()).isEqualTo(RolloutStatus.READY);

        List<RolloutGroup> groups = myRollout.getRolloutGroups();
        assertThat(groups.get(0).getStatus()).isEqualTo(RolloutGroupStatus.READY);
        assertThat(groups.get(0).getTotalTargets()).isEqualTo(1);
        assertThat(groups.get(1).getStatus()).isEqualTo(RolloutGroupStatus.READY);
        assertThat(groups.get(1).getTotalTargets()).isEqualTo(1);
        assertThat(groups.get(2).getStatus()).isEqualTo(RolloutGroupStatus.READY);
        assertThat(groups.get(2).getTotalTargets()).isEqualTo(0);
        assertThat(groups.get(3).getStatus()).isEqualTo(RolloutGroupStatus.READY);
        assertThat(groups.get(3).getTotalTargets()).isEqualTo(1);
        assertThat(groups.get(4).getStatus()).isEqualTo(RolloutGroupStatus.READY);
        assertThat(groups.get(4).getTotalTargets()).isEqualTo(0);

        rolloutManagement.startRollout(myRollout);

        // Run here, because scheduler is disabled during tests
        rolloutManagement.checkStartingRollouts(0);

        SuccessConditionRolloutStatus conditionRolloutStatus = new SuccessConditionRolloutStatus(RolloutStatus.RUNNING);
        assertThat(MultipleInvokeHelper.doWithTimeout(new RolloutStatusCallable(myRollout.getId()),
                conditionRolloutStatus, 15000, 500)).as("Rollout status").isNotNull();

        myRollout = rolloutManagement.findRolloutById(myRollout.getId());
        assertThat(myRollout.getStatus()).isEqualTo(RolloutStatus.RUNNING);
        final Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.RUNNING, 1L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.SCHEDULED, 2L);
        validateRolloutActionStatus(myRollout.getId(), expectedTargetCountStatus);

    }

    @Test
    @Description("Verify the creation and the start of a rollout.")
    public void createAndStartRollout() throws Exception {

        final int amountTargetsForRollout = 500;
        final int amountGroups = 5;
        final String successCondition = "50";
        final String errorCondition = "80";
        final String rolloutName = "rolloutTest";
        final String targetPrefixName = rolloutName;
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsFor" + rolloutName);
        targetManagement.createTargets(
                testdataFactory.generateTargets(amountTargetsForRollout, targetPrefixName + "-", targetPrefixName));

        Rollout myRollout = createRolloutByVariables(rolloutName, "desc", amountGroups,
                "controllerId==" + targetPrefixName + "-*", distributionSet, successCondition, errorCondition);

        assertThat(myRollout.getStatus()).isEqualTo(RolloutStatus.READY);
        rolloutManagement.startRollout(myRollout);

        // Run here, because scheduler is disabled during tests
        rolloutManagement.checkStartingRollouts(0);

        SuccessConditionRolloutStatus conditionRolloutTargetCount = new SuccessConditionRolloutStatus(
                RolloutStatus.RUNNING);
        assertThat(MultipleInvokeHelper.doWithTimeout(new RolloutStatusCallable(myRollout.getId()),
                conditionRolloutTargetCount, 15000, 500)).as("Rollout status").isNotNull();

        myRollout = rolloutManagement.findRolloutById(myRollout.getId());
        assertThat(myRollout.getStatus()).isEqualTo(RolloutStatus.RUNNING);
        final Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.RUNNING, 100L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.SCHEDULED, 400L);
        validateRolloutActionStatus(myRollout.getId(), expectedTargetCountStatus);
    }

    @Test
    @Description("Verify the creation of a Rollout with a groups definition.")
    public void createRolloutWithGroupDefinition() throws Exception {
        final String rolloutName = "rolloutTest3";

        final int amountTargetsInGroup1 = 100;
        final int percentTargetsInGroup1 = 100;

        final int amountTargetsInGroup1and2 = 500;
        final int percentTargetsInGroup2 = 20;
        final int percentTargetsInGroup3 = 100;

        final int countTargetsInGroup2 = (int) Math
                .ceil((double) percentTargetsInGroup2 / 100 * (double) amountTargetsInGroup1and2);
        final int countTargetsInGroup3 = amountTargetsInGroup1and2 - countTargetsInGroup2;

        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder().build();
        // Generate Targets for group 2 and 3 and generate the Rollout
        Rollout myRollout = generateTargetsAndRollout(rolloutName, amountTargetsInGroup1and2);

        // Generate Targets for group 1
        targetManagement.createTargets(
                testdataFactory.generateTargets(amountTargetsInGroup1, rolloutName + "-gr1-", rolloutName));

        List<RolloutGroup> rolloutGroups = new ArrayList<>(3);
        rolloutGroups.add(generateRolloutGroup(0, percentTargetsInGroup1, "id==" + rolloutName + "-gr1-*"));
        rolloutGroups.add(generateRolloutGroup(1, percentTargetsInGroup2, null));
        rolloutGroups.add(generateRolloutGroup(2, percentTargetsInGroup3, null));

        myRollout = rolloutManagement.createRollout(myRollout, rolloutGroups, conditions);
        myRollout = rolloutManagement.findRolloutById(myRollout.getId());

        assertThat(myRollout.getStatus()).isEqualTo(RolloutStatus.CREATING);
        for (RolloutGroup group : myRollout.getRolloutGroups()) {
            assertThat(group.getStatus()).isEqualTo(RolloutGroupStatus.CREATING);
        }

        // Generate Targets that must not be addressed by the rollout, because
        // they were added after the rollout was created
        TimeUnit.SECONDS.sleep(1);
        targetManagement.createTargets(testdataFactory.generateTargets(10, rolloutName + "-notIn-", rolloutName));

        rolloutManagement.fillRolloutGroupsWithTargets(myRollout);

        myRollout = rolloutManagement.findRolloutById(myRollout.getId());
        assertThat(myRollout.getStatus()).isEqualTo(RolloutStatus.READY);
        assertThat(myRollout.getTotalTargets()).isEqualTo(amountTargetsInGroup1and2 + amountTargetsInGroup1);

        List<RolloutGroup> groups = myRollout.getRolloutGroups();
        assertThat(groups.get(0).getStatus()).isEqualTo(RolloutGroupStatus.READY);
        assertThat(groups.get(0).getTotalTargets()).isEqualTo(amountTargetsInGroup1);

        assertThat(groups.get(1).getStatus()).isEqualTo(RolloutGroupStatus.READY);
        assertThat(groups.get(1).getTotalTargets()).isEqualTo(countTargetsInGroup2);

        assertThat(groups.get(2).getStatus()).isEqualTo(RolloutGroupStatus.READY);
        assertThat(groups.get(2).getTotalTargets()).isEqualTo(countTargetsInGroup3);

    }

    @Test
    @Description("Verify Exception when a Rollout with Group definition is created that does not address all targets")
    public void createRolloutWithGroupsNotMatchingTargets() throws Exception {
        final String rolloutName = "rolloutTest4";
        final int amountTargetsForRollout = 500;
        final int percentTargetsInGroup1 = 20;
        final int percentTargetsInGroup2 = 50;

        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder().build();
        Rollout myRollout = generateTargetsAndRollout(rolloutName, amountTargetsForRollout);

        List<RolloutGroup> rolloutGroups = new ArrayList<>(2);
        rolloutGroups.add(generateRolloutGroup(0, percentTargetsInGroup1, null));
        rolloutGroups.add(generateRolloutGroup(1, percentTargetsInGroup2, null));

        try {
            rolloutManagement.createRollout(myRollout, rolloutGroups, conditions);
            fail("Was able to create a Rollout with groups that are not addressing all targets");
        } catch(RolloutVerificationException e) {
            // OK
        }

    }

    @Test
    @Description("Verify Exception when a Rollout with Group definition is created that contains an illegal percentage")
    public void createRolloutWithIllegalPercentage() throws Exception {
        final String rolloutName = "rolloutTest6";
        final int amountTargetsForRollout = 10;
        final int percentTargetsInGroup1 = 101;
        final int percentTargetsInGroup2 = 50;

        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder().build();
        Rollout myRollout = generateTargetsAndRollout(rolloutName, amountTargetsForRollout);

        List<RolloutGroup> rolloutGroups = new ArrayList<>(2);
        rolloutGroups.add(generateRolloutGroup(0, percentTargetsInGroup1, null));
        rolloutGroups.add(generateRolloutGroup(1, percentTargetsInGroup2, null));

        try {
            rolloutManagement.createRollout(myRollout, rolloutGroups, conditions);
            fail("Was able to create a Rollout with groups that have illegal percentages");
        } catch(RolloutVerificationException e) {
            // OK
        }

    }

    @Test
    @Description("Verify Exception when a Rollout is created with too much groups")
    public void createRolloutWithIllegalAmountOfGroups() throws Exception {
        final String rolloutName = "rolloutTest5";
        final int amountTargetsForRollout = 10;
        final int illegalGroupAmount = 501;

        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder().build();
        Rollout myRollout = generateTargetsAndRollout(rolloutName, amountTargetsForRollout);

        try {
            rolloutManagement.createRollout(myRollout, illegalGroupAmount, conditions);
            fail("Was able to create a Rollout with too many groups");
        } catch(RolloutVerificationException e) {
            // OK
        }

    }

    @Test
    @Description("Verify the start of a Rollout does not work during creation phase.")
    public void createAndStartRolloutDuringCreationFails() throws Exception {
        final int amountTargetsForRollout = 3;
        final int amountGroups = 5;
        final String successCondition = "50";
        final String errorCondition = "80";
        final String rolloutName = "rolloutTestGC";
        final String targetPrefixName = rolloutName;
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsFor" + rolloutName);
        targetManagement.createTargets(
                testdataFactory.generateTargets(amountTargetsForRollout, targetPrefixName + "-", targetPrefixName));

        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder()
                .successCondition(RolloutGroupSuccessCondition.THRESHOLD, successCondition)
                .errorCondition(RolloutGroupErrorCondition.THRESHOLD, errorCondition)
                .errorAction(RolloutGroupErrorAction.PAUSE, null).build();
        final Rollout rolloutToCreate = new JpaRollout();
        rolloutToCreate.setName(rolloutName);
        rolloutToCreate.setDescription("some description");
        rolloutToCreate.setTargetFilterQuery("id==" + targetPrefixName + "-*");
        rolloutToCreate.setDistributionSet(distributionSet);

        Rollout myRollout = rolloutManagement.createRollout(rolloutToCreate, amountGroups, conditions);
        myRollout = rolloutManagement.findRolloutById(myRollout.getId());

        assertThat(myRollout.getStatus()).isEqualTo(RolloutStatus.CREATING);

        try {
            rolloutManagement.startRollout(myRollout);
            fail("Was able to start a Rollout in CREATING status");
        } catch(RolloutIllegalStateException e) {
            // OK
        }

    }

    private RolloutGroup generateRolloutGroup(final int index, Integer percentage, String targetFilter) {
        RolloutGroup group = entityFactory.generateRolloutGroup();
        group.setName("Group" + index);
        group.setDescription("Group" + index + "desc");
        if (percentage != null) {
            group.setTargetPercentage(percentage);
        }
        if (targetFilter != null) {
            group.setTargetFilterQuery(targetFilter);
        }
        return group;
    }

    private Rollout generateTargetsAndRollout(final String rolloutName, final int amountTargetsForRollout) {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsFor" + rolloutName);

        targetManagement.createTargets(
                testdataFactory.generateTargets(amountTargetsForRollout, rolloutName + "-", rolloutName));

        Rollout myRollout = entityFactory.generateRollout();
        myRollout.setName(rolloutName);
        myRollout.setDescription("This is a test description for the rollout");
        myRollout.setTargetFilterQuery("controllerId==" + rolloutName + "-*");
        myRollout.setDistributionSet(distributionSet);

        return myRollout;
    }

    private void validateRolloutGroupActionStatus(final RolloutGroup rolloutGroup,
            final Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus) {
        final RolloutGroup rolloutGroupWithDetail = rolloutGroupManagement
                .findRolloutGroupWithDetailedStatus(rolloutGroup.getId());
        validateStatus(rolloutGroupWithDetail.getTotalTargetCountStatus(), expectedTargetCountStatus);
    }

    private void validateRolloutActionStatus(final Long rolloutId,
            final Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus) {
        final Rollout rolloutWithDetail = rolloutManagement.findRolloutWithDetailedStatus(rolloutId);
        validateStatus(rolloutWithDetail.getTotalTargetCountStatus(), expectedTargetCountStatus);
    }

    private void validateStatus(final TotalTargetCountStatus totalTargetCountStatus,
            final Map<TotalTargetCountStatus.Status, Long> expectedTotalCountStates) {
        for (final Map.Entry<TotalTargetCountStatus.Status, Long> entry : expectedTotalCountStates.entrySet()) {
            final Long countReady = totalTargetCountStatus.getTotalTargetCountByStatus(entry.getKey());
            assertThat(countReady).isEqualTo(entry.getValue());
        }
    }

    private Rollout createSimpleTestRolloutWithTargetsAndDistributionSet(final int amountTargetsForRollout,
            final int amountOtherTargets, final int groupSize, final String successCondition,
            final String errorCondition) {
        final SoftwareModule ah = testdataFactory.createSoftwareModule(TestdataFactory.SM_TYPE_APP);
        final SoftwareModule jvm = testdataFactory.createSoftwareModule(TestdataFactory.SM_TYPE_RT);
        final SoftwareModule os = testdataFactory.createSoftwareModule(TestdataFactory.SM_TYPE_OS);

        final DistributionSet rolloutDS = distributionSetManagement.createDistributionSet(testdataFactory
                .generateDistributionSet("rolloutDS", "0.0.0", standardDsType, Lists.newArrayList(os, jvm, ah)));
        targetManagement.createTargets(testdataFactory.generateTargets(amountTargetsForRollout, "rollout-", "rollout"));
        targetManagement.createTargets(testdataFactory.generateTargets(amountOtherTargets, "others-", "rollout"));
        final String filterQuery = "controllerId==rollout-*";
        return createRolloutByVariables("test-rollout-name-1", "test-rollout-description-1", groupSize, filterQuery,
                rolloutDS, successCondition, errorCondition);
    }

    private Rollout createTestRolloutWithTargetsAndDistributionSet(final int amountTargetsForRollout,
            final int groupSize, final String successCondition, final String errorCondition, final String rolloutName,
            final String targetPrefixName) {
        final DistributionSet dsForRolloutTwo = testdataFactory.createDistributionSet("dsFor" + rolloutName);
        targetManagement.createTargets(
                testdataFactory.generateTargets(amountTargetsForRollout, targetPrefixName + "-", targetPrefixName));
        return createRolloutByVariables(rolloutName, rolloutName + "description", groupSize,
                "controllerId==" + targetPrefixName + "-*", dsForRolloutTwo, successCondition, errorCondition);
    }

    private Rollout createRolloutByVariables(final String rolloutName, final String rolloutDescription,
            final int groupSize, final String filterQuery, final DistributionSet distributionSet,
            final String successCondition, final String errorCondition) {
        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder()
                .successCondition(RolloutGroupSuccessCondition.THRESHOLD, successCondition)
                .errorCondition(RolloutGroupErrorCondition.THRESHOLD, errorCondition)
                .errorAction(RolloutGroupErrorAction.PAUSE, null).build();
        final Rollout rolloutToCreate = new JpaRollout();
        rolloutToCreate.setName(rolloutName);
        rolloutToCreate.setDescription(rolloutDescription);
        rolloutToCreate.setTargetFilterQuery(filterQuery);
        rolloutToCreate.setDistributionSet(distributionSet);
        final Rollout rollout =  rolloutManagement.createRollout(rolloutToCreate, groupSize, conditions);

        // Run here, because Scheduler is disabled during tests
        rolloutManagement.fillRolloutGroupsWithTargets(rolloutManagement.findRolloutById(rollout.getId()));

        return rolloutManagement.findRolloutById(rollout.getId());
    }

    private int changeStatusForAllRunningActions(final Rollout rollout, final Status status) {
        final List<Action> runningActions = deploymentManagement.findActionsByRolloutAndStatus(rollout, Status.RUNNING);
        for (final Action action : runningActions) {
            action.setStatus(status);
            controllerManagament.addUpdateActionStatus(
                    new JpaActionStatus((JpaAction) action, status, System.currentTimeMillis(), ""));
        }
        return runningActions.size();
    }

    private int changeStatusForRunningActions(final Rollout rollout, final Status status,
            final int amountOfTargetsToGetChanged) {
        final List<Action> runningActions = deploymentManagement.findActionsByRolloutAndStatus(rollout, Status.RUNNING);
        assertThat(runningActions.size()).isGreaterThanOrEqualTo(amountOfTargetsToGetChanged);
        for (int i = 0; i < amountOfTargetsToGetChanged; i++) {
            controllerManagament.addUpdateActionStatus(
                    new JpaActionStatus((JpaAction) runningActions.get(i), status, System.currentTimeMillis(), ""));
        }
        return runningActions.size();
    }

    private static Map<TotalTargetCountStatus.Status, Long> createInitStatusMap() {
        final Map<TotalTargetCountStatus.Status, Long> map = new HashMap<>();
        for (final TotalTargetCountStatus.Status status : TotalTargetCountStatus.Status.values()) {
            map.put(status, 0L);
        }
        return map;
    }

    private static class SuccessConditionRolloutStatus implements SuccessCondition<RolloutStatus> {

        private final RolloutStatus rolloutStatus;

        public SuccessConditionRolloutStatus(final RolloutStatus rolloutStatus) {
            this.rolloutStatus = rolloutStatus;
        }

        @Override
        public boolean success(final RolloutStatus result) {
            return result.equals(rolloutStatus);
        }
    }

    private class RolloutStatusCallable implements Callable<RolloutStatus> {

        final Long rolloutId;

        RolloutStatusCallable(final Long rolloutId) {
            this.rolloutId = rolloutId;
        }

        @Override
        public RolloutStatus call() throws Exception {

            final Rollout myRollout = rolloutManagement.findRolloutById(rolloutId);
            return myRollout.getStatus();

        }
    }

}
