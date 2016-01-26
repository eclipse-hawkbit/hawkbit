/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.repository.RolloutTargetsStatusCount.RolloutTargetStatus;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * @author Michael Hirsch
 *
 */
@Features("Component Tests - Repository")
@Stories("Rollout Management")
public class RolloutManagementTest extends AbstractIntegrationTest {

    @Autowired
    private RolloutManagement rolloutManagement;

    @Test
    @Description("Verfiying that the rollout is created correctly, executing the filter and split up the targets in the correct group size.")
    public void creatingRolloutIsCorrectPersisted() {
        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 15;
        final int amountGroups = 5;
        final Rollout createdRollout = createTestRollout(amountTargetsForRollout, amountOtherTargets, amountGroups);

        // verify the split of the target and targetGroup
        final Page<RolloutGroup> rolloutGroups = rolloutManagement.findRolloutGroupsByRollout(createdRollout.getId(),
                pageReq);
        // we have total of #amountTargetsForRollout in rollouts splitted in
        // group size #groupSize
        assertThat(rolloutGroups).hasSize(amountGroups);
    }

    @Test
    @Description("Verfiying that when the rollout is started the actions for all targets in the rollout is created and the state of the first group is running as well as the corresponding actions")
    public void startRolloutSetFirstGroupAndActionsInRunningStateAndOthersInScheduleState() {
        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 15;
        final int amountGroups = 5;
        final Rollout createdRollout = createTestRollout(amountTargetsForRollout, amountOtherTargets, amountGroups);

        // start the rollout
        rolloutManagement.startRollout(createdRollout);

        // verify first group is running
        final RolloutGroup firstGroup = rolloutManagement
                .findRolloutGroupsByRollout(createdRollout.getId(),
                        new OffsetBasedPageRequest(0, 1, new Sort(Direction.ASC, "id"))).getContent().get(0);
        assertThat(firstGroup.getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);

        // verify other groups are scheduled
        final List<RolloutGroup> scheduledGroups = rolloutManagement.findRolloutGroupsByRollout(createdRollout.getId(),
                new OffsetBasedPageRequest(1, 100, new Sort(Direction.ASC, "id"))).getContent();
        scheduledGroups.forEach(group -> assertThat(group.getStatus()).isEqualTo(RolloutGroupStatus.SCHEDULED).as(
                "group which should be in scheduled state is in " + group.getStatus() + " state"));
        // verify that the first group actions has been started and are in state
        // running
        final List<Action> runningActions = deploymentManagement.findActionsByRolloutAndStatus(createdRollout,
                Status.RUNNING);
        assertThat(runningActions).hasSize(amountTargetsForRollout / amountGroups);
        // the rest targets are only scheduled
        assertThat(deploymentManagement.findActionsByRolloutAndStatus(createdRollout, Status.SCHEDULED)).hasSize(
                amountTargetsForRollout - (amountTargetsForRollout / amountGroups));
    }

    @Test
    @Description("Verfiying that a finish condition of a group is hit the next group of the rollout is also started")
    public void checkRunningRolloutsDoesNotStartNextGroupIfFinishConditionIsNotHit() {
        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 15;
        final int amountGroups = 5;
        final Rollout createdRollout = createTestRollout(amountTargetsForRollout, amountOtherTargets, amountGroups);

        // start the rollout
        rolloutManagement.startRollout(createdRollout);

        // set first actions in finish state so finish condition hits
        final List<Action> runningActions = deploymentManagement.findActionsByRolloutAndStatus(createdRollout,
                Status.RUNNING);
        // finish one action should be sufficient due the finish condition is at
        // 50%
        final Action action = runningActions.get(0);
        action.setStatus(Status.FINISHED);
        controllerManagament.addUpdateActionStatus(new ActionStatus(action, Status.FINISHED,
                System.currentTimeMillis(), ""), action);

        // check running rollouts again, now the finish condition should be hit
        // and should start the next group
        rolloutManagement.checkRunningRollouts(0);

        // verify that now the first and the second group are in running state
        final List<RolloutGroup> runningRolloutGroups = rolloutManagement.findRolloutGroupsByRollout(
                createdRollout.getId(), new OffsetBasedPageRequest(0, 2, new Sort(Direction.ASC, "id"))).getContent();
        runningRolloutGroups.forEach(group -> assertThat(group.getStatus()).isEqualTo(RolloutGroupStatus.RUNNING).as(
                "group should be in running state because it should be started but it is in " + group.getStatus()
                        + " state"));

        // verify that the other groups are still in schedule state
        final List<RolloutGroup> scheduledRolloutGroups = rolloutManagement.findRolloutGroupsByRollout(
                createdRollout.getId(), new OffsetBasedPageRequest(2, 10, new Sort(Direction.ASC, "id"))).getContent();
        scheduledRolloutGroups.forEach(group -> assertThat(group.getStatus()).isEqualTo(RolloutGroupStatus.SCHEDULED)
                .as("group should be in scheduled state because it should not be started but it is in "
                        + group.getStatus() + " state"));
    }

    @Test
    @Description("Verfiying that the error handling action of a group is executed to pause the current rollout")
    public void checkErrorHitOfGroupCallsErrorActionToPauseTheRollout() {
        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 15;
        final int amountGroups = 5;
        final Rollout createdRollout = createTestRollout(amountTargetsForRollout, amountOtherTargets, amountGroups);

        // start the rollout
        rolloutManagement.startRollout(createdRollout);

        // set both actions in error state so error condition is hit and error
        // action is executed
        final List<Action> runningActions = deploymentManagement.findActionsByRolloutAndStatus(createdRollout,
                Status.RUNNING);

        // finish actions with error
        for (final Action action : runningActions) {
            action.setStatus(Status.ERROR);
            controllerManagament.addUpdateActionStatus(
                    new ActionStatus(action, Status.ERROR, System.currentTimeMillis(), ""), action);
        }

        // check running rollouts again, now the error condition should be hit
        // and should execute the error action
        rolloutManagement.checkRunningRollouts(0);

        final Rollout rollout = rolloutManagement.findRolloutById(createdRollout.getId());
        // the rollout itself should be in paused based on the error action
        assertThat(rollout.getStatus()).isEqualTo(RolloutStatus.PAUSED);

        // the first rollout group should be in error state
        final List<RolloutGroup> errorGroup = rolloutManagement.findRolloutGroupsByRollout(createdRollout.getId(),
                new OffsetBasedPageRequest(0, 1, new Sort(Direction.ASC, "id"))).getContent();
        assertThat(errorGroup).hasSize(1);
        assertThat(errorGroup.get(0).getStatus()).isEqualTo(RolloutGroupStatus.ERROR);

        // all other groups should still be in scheduled state
        final List<RolloutGroup> scheduleGroups = rolloutManagement.findRolloutGroupsByRollout(createdRollout.getId(),
                new OffsetBasedPageRequest(1, 100, new Sort(Direction.ASC, "id"))).getContent();
        scheduleGroups.forEach(group -> assertThat(group.getStatus()).isEqualTo(RolloutGroupStatus.SCHEDULED));
    }

    @Test
    @Description("Verfiying a paused rollout in case of error action hit can be resumed again")
    public void errorActionPausesRolloutAndRolloutGetsResumedStartsNextScheduledGroup() {
        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 15;
        final int amountGroups = 5;
        final Rollout createdRollout = createTestRollout(amountTargetsForRollout, amountOtherTargets, amountGroups);

        // start the rollout
        rolloutManagement.startRollout(createdRollout);

        // set both actions in error state so error condition is hit and error
        // action is executed
        final List<Action> runningActions = deploymentManagement.findActionsByRolloutAndStatus(createdRollout,
                Status.RUNNING);

        // finish actions with error
        for (final Action action : runningActions) {
            action.setStatus(Status.ERROR);
            controllerManagament.addUpdateActionStatus(
                    new ActionStatus(action, Status.ERROR, System.currentTimeMillis(), ""), action);
        }

        // check running rollouts again, now the error condition should be hit
        // and should execute the error action
        rolloutManagement.checkRunningRollouts(0);

        final Rollout rollout = rolloutManagement.findRolloutById(createdRollout.getId());
        // the rollout itself should be in paused based on the error action
        assertThat(rollout.getStatus()).isEqualTo(RolloutStatus.PAUSED);

        // all other groups should still be in scheduled state
        final List<RolloutGroup> scheduleGroups = rolloutManagement.findRolloutGroupsByRollout(createdRollout.getId(),
                new OffsetBasedPageRequest(1, 100, new Sort(Direction.ASC, "id"))).getContent();
        scheduleGroups.forEach(group -> assertThat(group.getStatus()).isEqualTo(RolloutGroupStatus.SCHEDULED));

        // resume the rollout again after it gets paused by error action
        rolloutManagement.resumeRollout(rolloutManagement.findRolloutById(createdRollout.getId()));

        // the rollout should be running again
        assertThat(rolloutManagement.findRolloutById(createdRollout.getId()).getStatus()).isEqualTo(
                RolloutStatus.RUNNING);

        // checking rollouts again
        rolloutManagement.checkRunningRollouts(0);

        // next group should be running again after resuming the rollout
        final List<RolloutGroup> resumedGroups = rolloutManagement.findRolloutGroupsByRollout(createdRollout.getId(),
                new OffsetBasedPageRequest(1, 1, new Sort(Direction.ASC, "id"))).getContent();
        assertThat(resumedGroups).hasSize(1);
        assertThat(resumedGroups.get(0).getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);
    }

    @Test
    @Description("Verfiying that the rollout is starting group after group and gets finished at the end")
    public void rolloutStartsGroupAfterGroupAndGetsFinished() {
        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 15;
        final int amountGroups = 5;
        final Rollout createdRollout = createTestRollout(amountTargetsForRollout, amountOtherTargets, amountGroups);

        // start the rollout
        rolloutManagement.startRollout(createdRollout);
        // finish running actions, 2 actions should be finished
        assertThat(changeStatusForRunningActions(createdRollout, Status.FINISHED)).isEqualTo(2);

        // calculate the rest of the groups and finish them
        for (int groupsLeft = amountGroups - 1; groupsLeft >= 1; groupsLeft--) {
            // next check and start next group
            rolloutManagement.checkRunningRollouts(0);
            // finish running actions, 2 actions should be finished
            assertThat(changeStatusForRunningActions(createdRollout, Status.FINISHED)).isEqualTo(2);
            assertThat(rolloutManagement.findRolloutById(createdRollout.getId()).getStatus()).isEqualTo(
                    RolloutStatus.RUNNING);

        }
        // check rollout to see that all actions and all groups are finished and
        // so can go to FINISHED state of the rollout
        rolloutManagement.checkRunningRollouts(0);

        // verify all groups are in finished state
        rolloutManagement.findRolloutGroupsByRollout(createdRollout.getId(),
                new OffsetBasedPageRequest(0, 100, new Sort(Direction.ASC, "id"))).forEach(
                group -> assertThat(group.getStatus()).isEqualTo(RolloutGroupStatus.FINISHED));

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
        final Rollout createdRollout = createTestRollout(amountTargetsForRollout, amountOtherTargets, amountGroups);

        // test that the 8 targets have not started
        validateRolloutDetailedStatus(createdRollout.getId(), new Long(8), new Long(0), new Long(0), new Long(0),
                new Long(0));

        // start the rollout
        rolloutManagement.startRollout(createdRollout);

        // test that the 6 targets are ready and 2 are running
        validateRolloutDetailedStatus(createdRollout.getId(), new Long(0), new Long(6), new Long(2), new Long(0),
                new Long(0));

        changeStatusForRunningActions(createdRollout, Status.FINISHED);
        rolloutManagement.checkRunningRollouts(0);

        // test that the 4 targets are ready, 2 are finished and 2 are running
        validateRolloutDetailedStatus(createdRollout.getId(), new Long(0), new Long(4), new Long(2), new Long(2),
                new Long(0));

        changeStatusForRunningActions(createdRollout, Status.FINISHED);
        rolloutManagement.checkRunningRollouts(0);

        // test that the 2 targets are ready, 4 are finished and 2 are running
        validateRolloutDetailedStatus(createdRollout.getId(), new Long(0), new Long(2), new Long(2), new Long(4),
                new Long(0));

        changeStatusForRunningActions(createdRollout, Status.FINISHED);
        rolloutManagement.checkRunningRollouts(0);

        // test that the 0 targets are ready, 6 are finished and 2 are running
        validateRolloutDetailedStatus(createdRollout.getId(), new Long(0), new Long(0), new Long(2), new Long(6),
                new Long(0));

        changeStatusForRunningActions(createdRollout, Status.FINISHED);
        rolloutManagement.checkRunningRollouts(0);

        // test that the 0 targets are ready, 8 are finished and 0 are running
        validateRolloutDetailedStatus(createdRollout.getId(), new Long(0), new Long(0), new Long(0), new Long(8),
                new Long(0));

    }

    @Test
    @Description("Verify that the targets have the right status during the rollout when an error emerges.")
    public void countCorrectStatusForEachTargetDuringRolloutWithError() {

        final int amountTargetsForRollout = 8;
        final int amountOtherTargets = 15;
        final int amountGroups = 4;
        final Rollout createdRollout = createTestRollout(amountTargetsForRollout, amountOtherTargets, amountGroups);

        // test that the 8 targets have not started
        validateRolloutDetailedStatus(createdRollout.getId(), new Long(8), new Long(0), new Long(0), new Long(0),
                new Long(0));

        // start the rollout
        rolloutManagement.startRollout(createdRollout);

        // test that the 6 targets are ready and 2 are running
        validateRolloutDetailedStatus(createdRollout.getId(), new Long(0), new Long(6), new Long(2), new Long(0),
                new Long(0));

        changeStatusForRunningActions(createdRollout, Status.ERROR);
        rolloutManagement.checkRunningRollouts(0);

        // test that the 6 targets are ready and 2 are error
        validateRolloutDetailedStatus(createdRollout.getId(), new Long(0), new Long(6), new Long(0), new Long(0),
                new Long(2));

    }

    @Test
    @Description("Verify that the targets have the right status during the rollout when receiving the status of rollout groups.")
    public void countCorrectStatusForEachTargetGroupDuringRollout() {
        final int amountTargetsForRollout = 9;
        final int amountOtherTargets = 15;
        final int amountGroups = 4;
        Rollout createdRollout = createTestRollout(amountTargetsForRollout, amountOtherTargets, amountGroups);

        rolloutManagement.startRollout(createdRollout);
        changeStatusForRunningActions(createdRollout, Status.FINISHED);
        rolloutManagement.checkRunningRollouts(0);
        changeStatusForRunningActions(createdRollout, Status.FINISHED);
        rolloutManagement.checkRunningRollouts(0);

        // In this stage there should be 4 targets finished (Group 1 and 2), 2
        // targets running (Group 3) and 2 targets ready (Group 4) and one 1
        // target ready (Group 5)

        createdRollout = rolloutManagement.findRolloutById(createdRollout.getId());
        final List<RolloutGroup> rolloutGruops = createdRollout.getRolloutGroups();

        validateRolloutGroupStatus(rolloutGruops.get(0), new Long(0), new Long(0), new Long(0), new Long(2),
                new Long(0));
        validateRolloutGroupStatus(rolloutGruops.get(1), new Long(0), new Long(0), new Long(0), new Long(2),
                new Long(0));
        validateRolloutGroupStatus(rolloutGruops.get(2), new Long(0), new Long(0), new Long(2), new Long(0),
                new Long(0));
        validateRolloutGroupStatus(rolloutGruops.get(3), new Long(0), new Long(2), new Long(0), new Long(0),
                new Long(0));
        validateRolloutGroupStatus(rolloutGruops.get(4), new Long(0), new Long(1), new Long(0), new Long(0),
                new Long(0));

    }

    private void validateRolloutGroupStatus(final RolloutGroup rolloutGroup, final Long expectedCountNotstarted,
            final Long expectedCountReady, final Long expectedCountRunning, final Long expectedCountFinished,
            final Long expectedCountError) {
        final RolloutTargetsStatusCount rolloutTargetsStatusCount0 = rolloutManagement
                .getRolloutGroupDetailedStatus(rolloutGroup.getId());
        final Map<RolloutTargetStatus, Long> countStatus = rolloutTargetsStatusCount0.getStatusCountDetails();
        validateStatus(countStatus, expectedCountNotstarted, expectedCountReady, expectedCountRunning,
                expectedCountFinished, expectedCountError);
    }

    private void validateRolloutDetailedStatus(final Long rolloutId, final Long expectedCountNotstarted,
            final Long expectedCountReady, final Long expectedCountRunning, final Long expectedCountFinished,
            final Long expectedCountError) {
        final RolloutTargetsStatusCount rolloutTargetsStatusCount = rolloutManagement
                .getRolloutDetailedStatus(rolloutId);
        final Map<RolloutTargetStatus, Long> countStatus = rolloutTargetsStatusCount.getStatusCountDetails();

        validateStatus(countStatus, expectedCountNotstarted, expectedCountReady, expectedCountRunning,
                expectedCountFinished, expectedCountError);
    }

    private void validateStatus(final Map<RolloutTargetStatus, Long> countStatus, final Long expectedCountNotstarted,
            final Long expectedCountReady, final Long expectedCountRunning, final Long expectedCountFinished,
            final Long expectedCountError) {
        final Long countNotstarted = countStatus.get(RolloutTargetStatus.NOTSTARTED);
        assertThat(countNotstarted).isEqualTo(expectedCountNotstarted);

        final Long countReady = countStatus.get(RolloutTargetStatus.READY);
        assertThat(countReady).isEqualTo(expectedCountReady);

        final Long countRunning = countStatus.get(RolloutTargetStatus.RUNNING);
        assertThat(countRunning).isEqualTo(expectedCountRunning);

        final Long countFinished = countStatus.get(RolloutTargetStatus.FINISHED);
        assertThat(countFinished).isEqualTo(expectedCountFinished);

        final Long countError = countStatus.get(RolloutTargetStatus.ERROR);
        assertThat(countError).isEqualTo(expectedCountError);
    }

    private Rollout createTestRollout(final int amountTargetsForRollout, final int amountOtherTargets,
            final int groupSize) {
        // setup - distribution set
        final SoftwareModule ah = softwareManagement.createSoftwareModule(new SoftwareModule(appType, "agent-hub",
                "1.0.1", null, ""));
        final SoftwareModule jvm = softwareManagement.createSoftwareModule(new SoftwareModule(runtimeType,
                "oracle-jre", "1.7.2", null, ""));
        final SoftwareModule os = softwareManagement.createSoftwareModule(new SoftwareModule(osType, "poky", "3.0.2",
                null, ""));
        final DistributionSet rolloutDS = distributionSetManagement.createDistributionSet(TestDataUtil
                .buildDistributionSet("rolloutDS", "0.0.0", standardDsType, os, jvm, ah));

        // setup - targets
        targetManagement
                .createTargets(TestDataUtil.buildTargetFixtures(amountTargetsForRollout, "rollout-", "rollout"));
        targetManagement.createTargets(TestDataUtil.buildTargetFixtures(amountOtherTargets, "others-", "rollout"));

        // setup - variables
        final String filterQuery = "controllerId==rollout-*";
        final RolloutGroupConditions conditions = new RolloutGroup.RolloutGroupConditionBuilder()
                .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "50")
                .errorCondition(RolloutGroupErrorCondition.THRESHOLD, "80")
                .errorAction(RolloutGroupErrorAction.PAUSE, null).build();

        // creating rollout
        final Rollout rolloutToCreate = new Rollout();
        rolloutToCreate.setName("test-rollout-name-1");
        rolloutToCreate.setDescription("test-rollout-description-1");
        rolloutToCreate.setTargetFilterQuery(filterQuery);
        rolloutToCreate.setDistributionSet(rolloutDS);
        return rolloutManagement.createRollout(rolloutToCreate, groupSize, conditions);
    }

    private int changeStatusForRunningActions(final Rollout rollout, final Status status) {
        // set both actions in error state so error condition is hit and error
        // action is executed
        final List<Action> runningActions = deploymentManagement.findActionsByRolloutAndStatus(rollout, Status.RUNNING);

        // finish actions with error
        for (final Action action : runningActions) {
            action.setStatus(status);
            controllerManagament.addUpdateActionStatus(
                    new ActionStatus(action, status, System.currentTimeMillis(), ""), action);
        }
        return runningActions.size();
    }
}
