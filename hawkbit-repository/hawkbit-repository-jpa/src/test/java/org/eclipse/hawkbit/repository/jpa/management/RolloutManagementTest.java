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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.auth.SpRole;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.RolloutManagement.Create;
import org.eclipse.hawkbit.repository.RolloutManagement.GroupCreate;
import org.eclipse.hawkbit.repository.RolloutManagement.Update;
import org.eclipse.hawkbit.repository.event.remote.RolloutDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.RolloutGroupDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.exception.InvalidDistributionSetException;
import org.eclipse.hawkbit.repository.exception.RolloutIllegalStateException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionStatusCreate;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

/**
 * Junit tests for RolloutManagement.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Rollout Management
 */
class RolloutManagementTest extends AbstractJpaIntegrationTest {

    /**
     * Tests static assignment aspects of the dynamic group assignment filters.
     * Dynamic group doesn't override newer static group assignments
     */
    @Test
    void dynamicGroupDoesntOverrideItsOrNewerStaticGroups() {
        final int amountGroups = 1; // static only
        final String targetPrefix = "controller-dynamic-rollout-";
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("ds");

        testdataFactory.createTargets(targetPrefix, 0, amountGroups * 2);
        final Rollout dynamicRollout = testdataFactory.createRolloutByVariables("dynamic", "static rollout", amountGroups,
                "controllerid==" + targetPrefix + "*", distributionSet, "0", "30", ActionType.FORCED, 1000, false, true);
        rolloutManagement.start(dynamicRollout.getId());
        rolloutHandler.handleAll();
        assertRollout(dynamicRollout, true, RolloutStatus.RUNNING, amountGroups + 1, amountGroups * 2);
        final List<RolloutGroup> dynamicGroups = rolloutGroupManagement.findByRollout(
                dynamicRollout.getId(), new OffsetBasedPageRequest(0, amountGroups + 10, Sort.by(Direction.ASC, "id"))
        ).getContent();
        for (int i = 0; i < dynamicGroups.size(); i++) {
            final RolloutGroup group = dynamicGroups.get(i);
            if (i + 1 == dynamicGroups.size()) {
                assertGroup(group, true, RolloutGroupStatus.SCHEDULED, 0);
            } else {
                assertGroup(group, false, RolloutGroupStatus.RUNNING, 2);
            }
        }
        assertAndGetRunning(dynamicRollout, 2).forEach(this::finishAction);
        rolloutHandler.handleAll();
        for (int i = 0; i < dynamicGroups.size(); i++) {
            final RolloutGroup group = dynamicGroups.get(i);
            if (i + 1 == dynamicGroups.size()) {
                assertGroup(group, true, RolloutGroupStatus.RUNNING, 0);
            } else {
                assertGroup(group, false, RolloutGroupStatus.FINISHED, 2);
            }
        }
        assertAndGetRunning(dynamicRollout, 0);
        rolloutHandler.handleAll();
        // NB: asserts that dynamic group doesn't get from its static groups (already finished action targets)
        assertGroup(dynamicGroups.get(dynamicGroups.size() - 1), true, RolloutGroupStatus.RUNNING, 0);
        assertAndGetRunning(dynamicRollout, 0);
        rolloutManagement.pauseRollout(dynamicRollout.getId());
        rolloutHandler.handleAll();

        testdataFactory.createTargets(targetPrefix, amountGroups * 2, amountGroups);
        final Rollout staticRollout = testdataFactory.createRolloutByVariables("static", "static rollout", amountGroups,
                "controllerid==" + targetPrefix + "*", distributionSet, "0", "30", ActionType.FORCED, 0, false, false);
        rolloutManagement.start(staticRollout.getId());
        rolloutHandler.handleAll();
        assertRollout(staticRollout, false, RolloutStatus.RUNNING, amountGroups, amountGroups * 3);
        final List<RolloutGroup> staticGroups = rolloutGroupManagement.findByRollout(
                staticRollout.getId(), new OffsetBasedPageRequest(0, amountGroups + 10, Sort.by(Direction.ASC, "id"))
        ).getContent();
        staticGroups.forEach(group -> assertGroup(group, false, RolloutGroupStatus.RUNNING, 3));

        rolloutManagement.resumeRollout(dynamicRollout.getId());
        rolloutHandler.handleAll(); // resume, do not get last devices (they are assigned to a newer group, nevertheless newer is with bigger weight
        assertGroup(dynamicGroups.get(dynamicGroups.size() - 1), true, RolloutGroupStatus.RUNNING, 0);
        assertAndGetRunning(dynamicRollout, 0);
    }

    @BeforeEach
    void reset() {
        this.approvalStrategy.setApprovalNeeded(false);
    }

    /**
     * Verifies that a running action with distribution-set (A) is not canceled by a rollout which tries to also assign a distribution-set (A)
     */
    @Test
    void rolloutShouldNotCancelRunningActionWithTheSameDistributionSet() {
        // manually assign distribution set to target
        final String knownControllerId = "controller12345";
        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSetLocked("");
        testdataFactory.createTarget(knownControllerId);
        final DistributionSetAssignmentResult assignmentResult = assignDistributionSet(knownDistributionSet.getId(), knownControllerId);
        final Long manuallyAssignedActionId = getFirstAssignedActionId(assignmentResult);

        // create rollout with the same distribution set already assigned
        // start rollout
        final Rollout rollout = testdataFactory.createRolloutByVariables(
                "rolloutNotCancelRunningAction", "description", 1, "name==*", knownDistributionSet, "50", "5");
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        // verify that manually created action is still running and action
        // created from rollout is finished
        final List<Action> actionsByKnownTarget = deploymentManagement.findActionsByTarget(knownControllerId, PAGE)
                .getContent();
        // should be 2 actions, one manually and one from the rollout
        assertThat(actionsByKnownTarget).hasSize(2);
        // verify that manually assigned action is still running
        assertThat(deploymentManagement.findAction(manuallyAssignedActionId).get().getStatus())
                .isEqualTo(Status.RUNNING);
        // verify that rollout management created action is finished because is
        // duplicate assignment
        final Action rolloutCreatedAction = actionsByKnownTarget.stream()
                .filter(action -> !action.getId().equals(manuallyAssignedActionId)).findAny().get();
        assertThat(rolloutCreatedAction.getStatus()).isEqualTo(Status.FINISHED);
    }

    /**
     * Verifies that action states are correctly initialized after starting a rollout with different options in regard to the confirmation.
     */
    @ParameterizedTest
    @MethodSource("simpleRolloutsPossibilities")
    void runRolloutWithConfirmationFlagAndCoonfirmationFlowOptions(final boolean confirmationFlowActive,
            final boolean confirmationRequired, final Status expectedStatus) {
        // manually assign distribution set to target
        final String knownControllerId = "controller12345";
        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();
        testdataFactory.createTarget(knownControllerId);

        if (confirmationFlowActive) {
            enableConfirmationFlow();
        }

        // create rollout with the same distribution set already assigned
        // start rollout
        final Rollout rollout = testdataFactory.createRolloutByVariables("rolloutNotCancelRunningAction", "description",
                1, "name==*", knownDistributionSet, "50", "5", confirmationRequired);
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        // verify that manually created action is still running and action
        // created from rollout is finished
        final List<Action> actionsByKnownTarget = deploymentManagement.findActionsByTarget(knownControllerId, PAGE)
                .getContent();
        assertThat(actionsByKnownTarget).hasSize(1);
        assertThat(actionsByKnownTarget.get(0).getStatus()).isEqualTo(expectedStatus);
    }

    /**
     * Verifies that a running action is auto canceled by a rollout which assigns another distribution-set.
     */
    @Test
    void rolloutAssignsNewDistributionSetAndAutoCloseActiveActions() {
        tenantConfigurationManagement().addOrUpdateConfiguration(TenantConfigurationKey.REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED, true);

        try {
            // manually assign distribution set to target
            final String knownControllerId = "controller12345";
            final DistributionSet firstDistributionSet = testdataFactory.createDistributionSet();
            final DistributionSet secondDistributionSet = testdataFactory.createDistributionSet("second");
            testdataFactory.createTarget(knownControllerId);
            final DistributionSetAssignmentResult assignmentResult = assignDistributionSet(firstDistributionSet.getId(),
                    knownControllerId);
            final Long manuallyAssignedActionId = getFirstAssignedActionId(assignmentResult);

            // create rollout with the same distribution set already assigned
            // start rollout
            final Rollout rollout = testdataFactory.createRolloutByVariables("rolloutNotCancelRunningAction",
                    "description", 1, "name==*", secondDistributionSet, "50", "5");
            rolloutManagement.start(rollout.getId());
            rolloutHandler.handleAll();

            // verify that manually created action is canceled and action
            // created from rollout is running
            final List<Action> actionsByKnownTarget = deploymentManagement.findActionsByTarget(knownControllerId, PAGE)
                    .getContent();
            // should be 2 actions, one manually and one from the rollout
            assertThat(actionsByKnownTarget).hasSize(2);
            // verify that manually assigned action is still running
            assertThat(deploymentManagement.findAction(manuallyAssignedActionId).get().getStatus())
                    .isEqualTo(Status.CANCELED);
            // verify that rollout management created action is running
            final Action rolloutCreatedAction = actionsByKnownTarget.stream()
                    .filter(action -> !action.getId().equals(manuallyAssignedActionId)).findAny().get();
            assertThat(rolloutCreatedAction.getStatus()).isEqualTo(Status.RUNNING);
        } finally {
            tenantConfigurationManagement().addOrUpdateConfiguration(TenantConfigurationKey.REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED, false);
        }

    }

    /**
     * Verifies that management get access reacts as specified on calls for non existing entities by means
     * of Optional not present.
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class) })
    void nonExistingEntityAccessReturnsNotPresent() {
        assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> rolloutManagement.get(NOT_EXIST_IDL));
        assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> rolloutManagement.getWithDetailedStatus(NOT_EXIST_IDL));
    }

    /**
     * Verifies that management queries react as specified on calls for non existing entities
     * by means of throwing EntityNotFoundException.
     */
    @Test
    @ExpectEvents({
            @Expect(type = RolloutDeletedEvent.class, count = 0),
            @Expect(type = RolloutGroupCreatedEvent.class, count = 5),
            @Expect(type = RolloutGroupUpdatedEvent.class, count = 5),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = RolloutCreatedEvent.class, count = 1),
            @Expect(type = RolloutUpdatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 125) })
    void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        testdataFactory.createRollout("xxx");

        verifyThrownExceptionBy(() -> rolloutManagement.delete(NOT_EXIST_IDL), "Rollout");

        verifyThrownExceptionBy(() -> rolloutManagement.pauseRollout(NOT_EXIST_IDL), "Rollout");
        verifyThrownExceptionBy(() -> rolloutManagement.resumeRollout(NOT_EXIST_IDL), "Rollout");
        verifyThrownExceptionBy(() -> rolloutManagement.start(NOT_EXIST_IDL), "Rollout");

        verifyThrownExceptionBy(() -> rolloutManagement.update(Update.builder().id(NOT_EXIST_IDL).build()), "Rollout");
        verifyThrownExceptionBy(() -> rolloutManagement.triggerNextGroup(NOT_EXIST_IDL), "Rollout");
    }

    /**
     * Verifying that the rollout is created correctly, executing the filter and split up the targets in the correct group size.
     */
    @Test
    void creatingRolloutIsCorrectPersisted() {
        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 15;
        final int amountGroups = 5;
        final String successCondition = "50";
        final String errorCondition = "80";
        final Rollout createdRollout = testdataFactory.createSimpleTestRolloutWithTargetsAndDistributionSet(
                amountTargetsForRollout,
                amountOtherTargets, amountGroups, successCondition, errorCondition);

        // verify the split of the target and targetGroup
        final Page<RolloutGroup> rolloutGroups = rolloutGroupManagement.findByRollout(createdRollout.getId(), PAGE);
        // we have total of #amountTargetsForRollout in rollouts split in
        // group size #groupSize
        assertThat(rolloutGroups).hasSize(amountGroups);
    }

    /**
     * Verifying that when the rollout is started the actions for all targets in the rollout is created and the state of the first group is running as well as the corresponding actions
     */
    @Test
    void startRolloutSetFirstGroupAndActionsInRunningStateAndOthersInScheduleState() {
        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 15;
        final int amountGroups = 5;
        final String successCondition = "50";
        final String errorCondition = "80";
        final Rollout createdRollout = testdataFactory.createAndStartRollout(amountTargetsForRollout,
                amountOtherTargets, amountGroups,
                successCondition, errorCondition);

        // verify first group is running
        final RolloutGroup firstGroup = rolloutGroupManagement
                .findByRollout(createdRollout.getId(), new OffsetBasedPageRequest(0, 1, Sort.by(Direction.ASC, "id")))
                .getContent().get(0);
        assertThat(firstGroup.getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);

        // verify other groups are scheduled
        final List<RolloutGroup> scheduledGroups = rolloutGroupManagement
                .findByRollout(createdRollout.getId(), new OffsetBasedPageRequest(1, 100, Sort.by(Direction.ASC, "id")))
                .getContent();
        scheduledGroups.forEach(group -> assertThat(group.getStatus())
                .as("group which should be in scheduled state is in " + group.getStatus() + " state")
                .isEqualTo(RolloutGroupStatus.SCHEDULED));
        // verify that the first group actions has been started and are in state running
        final List<Action> runningActions = findActionsByRolloutAndStatus(createdRollout, Status.RUNNING);
        assertThat(runningActions).hasSize(amountTargetsForRollout / amountGroups)
                .as("Created actions are initiated by rollout creator")
                .allMatch(a -> a.getInitiatedBy().equals(createdRollout.getCreatedBy()));
        // the rest targets are only scheduled
        assertThat(findActionsByRolloutAndStatus(createdRollout, Status.SCHEDULED))
                .hasSize(amountTargetsForRollout - (amountTargetsForRollout / amountGroups));
    }

    /**
     * Verifying that a finish condition of a group is hit the next group of the rollout is also started
     */
    @Test
    void checkRunningRolloutsDoesNotStartNextGroupIfFinishConditionIsNotHit() {
        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 15;
        final int amountGroups = 5;
        final String successCondition = "50";
        final String errorCondition = "80";
        final Rollout createdRollout = testdataFactory.createAndStartRollout(amountTargetsForRollout,
                amountOtherTargets, amountGroups,
                successCondition, errorCondition);

        final List<Action> runningActions = findActionsByRolloutAndStatus(createdRollout, Status.RUNNING);
        // finish one action should be sufficient due the finish condition is at
        // 50%
        final JpaAction action = (JpaAction) runningActions.get(0);
        controllerManagement
                .addUpdateActionStatus(ActionStatusCreate.builder().actionId(action.getId()).status(Status.FINISHED).build());

        // check running rollouts again, now the finish condition should be hit
        // and should start the next group
        rolloutHandler.handleAll();

        // verify that now the first and the second group are in running state
        final List<RolloutGroup> runningRolloutGroups = rolloutGroupManagement
                .findByRollout(createdRollout.getId(), new OffsetBasedPageRequest(0, 2, Sort.by(Direction.ASC, "id")))
                .getContent();
        runningRolloutGroups.forEach(group -> assertThat(group.getStatus())
                .as("group should be in running state because it should be started but it is in " + group.getStatus()
                        + " state")
                .isEqualTo(RolloutGroupStatus.RUNNING));

        // verify that the other groups are still in schedule state
        final List<RolloutGroup> scheduledRolloutGroups = rolloutGroupManagement
                .findByRollout(createdRollout.getId(), new OffsetBasedPageRequest(2, 10, Sort.by(Direction.ASC, "id")))
                .getContent();
        scheduledRolloutGroups.forEach(group -> assertThat(group.getStatus())
                .as("group should be in scheduled state because it should not be started but it is in "
                        + group.getStatus() + " state")
                .isEqualTo(RolloutGroupStatus.SCHEDULED));
    }

    /**
     * Verifying that next group is started when targets of the group have been deleted.
     */
    @Test
    void checkRunningRolloutsStartsNextGroupIfTargetsDeleted() {
        final int amountTargetsForRollout = 15;
        final int amountOtherTargets = 0;
        final int amountGroups = 3;
        final String successCondition = "100";
        final String errorCondition = "80";
        final Rollout createdRollout = testdataFactory.createAndStartRollout(
                amountTargetsForRollout, amountOtherTargets, amountGroups, successCondition, errorCondition);

        finishActionAndDeleteTargetsOfFirstRunningGroup(createdRollout);
        checkSecondGroupStatusIsRunning(createdRollout);

        finishActionAndDeleteTargetsOfSecondRunningGroup(createdRollout);
        deleteAllTargetsFromThirdGroup(createdRollout);
        rolloutHandler.handleAll(); // one more time to finish the second group
        verifyRolloutAndAllGroupsAreFinished(createdRollout);
    }

    /**
     * Verifying that the error handling action of a group is executed to pause the current rollout
     */
    @Test
    void checkErrorHitOfGroupCallsErrorActionToPauseTheRollout() {
        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 15;
        final int amountGroups = 5;
        final String successCondition = "50";
        final String errorCondition = "80";
        final Rollout createdRollout = testdataFactory.createAndStartRollout(amountTargetsForRollout,
                amountOtherTargets, amountGroups,
                successCondition, errorCondition);

        // set both actions in error state so error condition is hit and error
        // action is executed
        final List<Action> runningActions = findActionsByRolloutAndStatus(createdRollout, Status.RUNNING);

        // finish actions with error
        for (final Action action : runningActions) {
            controllerManagement
                    .addUpdateActionStatus(ActionStatusCreate.builder().actionId(action.getId()).status(Status.ERROR).build());
        }

        // check running rollouts again, now the error condition should be hit
        // and should execute the error action
        rolloutHandler.handleAll();

        final Rollout rollout = reloadRollout(createdRollout);
        // the rollout itself should be in paused based on the error action
        assertThat(rollout.getStatus()).isEqualTo(RolloutStatus.PAUSED);

        // the first rollout group should be in error state
        final List<RolloutGroup> errorGroup = rolloutGroupManagement
                .findByRollout(createdRollout.getId(), new OffsetBasedPageRequest(0, 1, Sort.by(Direction.ASC, "id")))
                .getContent();
        assertThat(errorGroup).hasSize(1);
        assertThat(errorGroup.get(0).getStatus()).isEqualTo(RolloutGroupStatus.ERROR);

        // all other groups should still be in scheduled state
        final List<RolloutGroup> scheduleGroups = rolloutGroupManagement
                .findByRollout(createdRollout.getId(), new OffsetBasedPageRequest(1, 100, Sort.by(Direction.ASC, "id")))
                .getContent();
        scheduleGroups.forEach(group -> assertThat(group.getStatus()).isEqualTo(RolloutGroupStatus.SCHEDULED));
    }

    /**
     * Verifying a paused rollout in case of error action hit can be resumed again
     */
    @Test
    void errorActionPausesRolloutAndRolloutGetsResumedStartsNextScheduledGroup() {
        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 15;
        final int amountGroups = 5;
        final String successCondition = "50";
        final String errorCondition = "80";
        final Rollout createdRollout = testdataFactory.createAndStartRollout(amountTargetsForRollout,
                amountOtherTargets, amountGroups,
                successCondition, errorCondition);

        // set both actions in error state so error condition is hit and error
        // action is executed
        final List<Action> runningActions = findActionsByRolloutAndStatus(createdRollout, Status.RUNNING);
        // finish actions with error
        for (final Action action : runningActions) {
            controllerManagement
                    .addUpdateActionStatus(ActionStatusCreate.builder().actionId(action.getId()).status(Status.ERROR).build());
        }

        // check running rollouts again, now the error condition should be hit
        // and should execute the error action
        rolloutHandler.handleAll();

        final Rollout rollout = reloadRollout(createdRollout);
        // the rollout itself should be in paused based on the error action
        assertThat(rollout.getStatus()).isEqualTo(RolloutStatus.PAUSED);

        // all other groups should still be in scheduled state
        final List<RolloutGroup> scheduleGroups = rolloutGroupManagement
                .findByRollout(createdRollout.getId(), new OffsetBasedPageRequest(1, 100, Sort.by(Direction.ASC, "id")))
                .getContent();
        scheduleGroups.forEach(group -> assertThat(group.getStatus()).isEqualTo(RolloutGroupStatus.SCHEDULED));

        // resume the rollout again after it gets paused by error action
        rolloutManagement.resumeRollout(createdRollout.getId());

        // the rollout should be running again
        assertThat(reloadRollout(createdRollout).getStatus()).isEqualTo(RolloutStatus.RUNNING);

        // checking rollouts again
        rolloutHandler.handleAll();

        // next group should be running again after resuming the rollout
        final List<RolloutGroup> resumedGroups = rolloutGroupManagement
                .findByRollout(createdRollout.getId(), new OffsetBasedPageRequest(1, 1, Sort.by(Direction.ASC, "id")))
                .getContent();
        assertThat(resumedGroups).hasSize(1);
        assertThat(resumedGroups.get(0).getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);
    }

    /**
     * Verifying that the rollout is starting group after group and gets finished at the end
     */
    @Test
    void rolloutStartsGroupAfterGroupAndGetsFinished() {
        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 15;
        final int amountGroups = 5;
        final String successCondition = "50";
        final String errorCondition = "80";
        final Rollout createdRollout = testdataFactory.createAndStartRollout(amountTargetsForRollout,
                amountOtherTargets, amountGroups,
                successCondition, errorCondition);

        // finish running actions, 2 actions should be finished
        assertThat(changeStatusForAllRunningActions(createdRollout, Status.FINISHED)).isEqualTo(2);

        // calculate the rest of the groups and finish them
        for (int groupsLeft = amountGroups - 1; groupsLeft >= 1; groupsLeft--) {
            // next check and start next group
            rolloutHandler.handleAll();
            // finish running actions, 2 actions should be finished
            assertThat(changeStatusForAllRunningActions(createdRollout, Status.FINISHED)).isEqualTo(2);
            assertThat(rolloutManagement.get(createdRollout.getId()).getStatus()).isEqualTo(RolloutStatus.RUNNING);

        }
        // check rollout to see that all actions and all groups are finished and
        // so can go to FINISHED state of the rollout
        rolloutHandler.handleAll();

        // verify all groups are in finished state
        rolloutGroupManagement
                .findByRollout(createdRollout.getId(), new OffsetBasedPageRequest(0, 100, Sort.by(Direction.ASC, "id")))
                .forEach(group -> assertThat(group.getStatus()).isEqualTo(RolloutGroupStatus.FINISHED));

        // verify that rollout itself is in finished state
        final Rollout findRolloutById = reloadRollout(createdRollout);
        assertThat(findRolloutById.getStatus()).isEqualTo(RolloutStatus.FINISHED);
    }

    /**
     * Verify that the targets have the right status during the rollout.
     */
    @Test
    void countCorrectStatusForEachTargetDuringRollout() {

        final int amountTargetsForRollout = 8;
        final int amountOtherTargets = 15;
        final int amountGroups = 4;
        final String successCondition = "50";
        final String errorCondition = "80";
        final Rollout createdRollout = testdataFactory.createSimpleTestRolloutWithTargetsAndDistributionSet(
                amountTargetsForRollout,
                amountOtherTargets, amountGroups, successCondition, errorCondition);

        // targets have not started
        Map<TotalTargetCountStatus.Status, Long> validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.NOTSTARTED, 8L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        rolloutManagement.start(createdRollout.getId());

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();

        // 6 targets are ready and 2 are running
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.SCHEDULED, 6L);
        validationMap.put(TotalTargetCountStatus.Status.RUNNING, 2L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        changeStatusForAllRunningActions(createdRollout, Status.FINISHED);
        rolloutHandler.handleAll();
        // 4 targets are ready, 2 are finished and 2 are running
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.SCHEDULED, 4L);
        validationMap.put(TotalTargetCountStatus.Status.FINISHED, 2L);
        validationMap.put(TotalTargetCountStatus.Status.RUNNING, 2L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        changeStatusForAllRunningActions(createdRollout, Status.FINISHED);
        rolloutHandler.handleAll();
        // 2 targets are ready, 4 are finished and 2 are running
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.SCHEDULED, 2L);
        validationMap.put(TotalTargetCountStatus.Status.FINISHED, 4L);
        validationMap.put(TotalTargetCountStatus.Status.RUNNING, 2L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        changeStatusForAllRunningActions(createdRollout, Status.FINISHED);
        rolloutHandler.handleAll();
        // 0 targets are ready, 6 are finished and 2 are running
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.FINISHED, 6L);
        validationMap.put(TotalTargetCountStatus.Status.RUNNING, 2L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        changeStatusForAllRunningActions(createdRollout, Status.FINISHED);
        rolloutHandler.handleAll();
        // 0 targets are ready, 8 are finished and 0 are running
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.FINISHED, 8L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

    }

    /**
     * Verify that the targets have the right status during a download_only rollout.
     */
    @Test
    void countCorrectStatusForEachTargetDuringDownloadOnlyRollout() {

        final int amountTargetsForRollout = 8;
        final int amountOtherTargets = 15;
        final int amountGroups = 4;
        final String successCondition = "50";
        final String errorCondition = "80";
        final Rollout createdRollout = testdataFactory.createSimpleTestRolloutWithTargetsAndDistributionSet(
                amountTargetsForRollout,
                amountOtherTargets, amountGroups, successCondition, errorCondition, ActionType.DOWNLOAD_ONLY, null);

        // targets have not started
        Map<TotalTargetCountStatus.Status, Long> validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.NOTSTARTED, 8L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        rolloutManagement.start(createdRollout.getId());

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();

        // 6 targets are ready and 2 are running
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.SCHEDULED, 6L);
        validationMap.put(TotalTargetCountStatus.Status.RUNNING, 2L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        changeStatusForAllRunningActions(createdRollout, Status.DOWNLOADED);
        rolloutHandler.handleAll();
        // 4 targets are ready, 2 are finished(with DOWNLOADED action status)
        // and 2 are running
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.SCHEDULED, 4L);
        validationMap.put(TotalTargetCountStatus.Status.FINISHED, 2L);
        validationMap.put(TotalTargetCountStatus.Status.RUNNING, 2L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        changeStatusForAllRunningActions(createdRollout, Status.DOWNLOADED);
        rolloutHandler.handleAll();
        // 2 targets are ready, 4 are finished(with DOWNLOADED action status)
        // and 2 are running
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.SCHEDULED, 2L);
        validationMap.put(TotalTargetCountStatus.Status.FINISHED, 4L);
        validationMap.put(TotalTargetCountStatus.Status.RUNNING, 2L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        changeStatusForAllRunningActions(createdRollout, Status.DOWNLOADED);
        rolloutHandler.handleAll();
        // 0 targets are ready, 6 are finished(with DOWNLOADED action status)
        // and 2 are running
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.FINISHED, 6L);
        validationMap.put(TotalTargetCountStatus.Status.RUNNING, 2L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        changeStatusForAllRunningActions(createdRollout, Status.FINISHED);
        rolloutHandler.handleAll();
        // 0 targets are ready, 6 are finished(with DOWNLOADED action status), 2
        // are finished and 0 are running
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.FINISHED, 8L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

    }

    /**
     * Verify that the targets have the right status during the rollout when an error emerges.
     */
    @Test
    void countCorrectStatusForEachTargetDuringRolloutWithError() {

        final int amountTargetsForRollout = 8;
        final int amountOtherTargets = 15;
        final int amountGroups = 4;
        final String successCondition = "50";
        final String errorCondition = "80";
        final Rollout createdRollout = testdataFactory.createSimpleTestRolloutWithTargetsAndDistributionSet(
                amountTargetsForRollout,
                amountOtherTargets, amountGroups, successCondition, errorCondition);

        // 8 targets have not started
        Map<TotalTargetCountStatus.Status, Long> validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.NOTSTARTED, 8L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        rolloutManagement.start(createdRollout.getId());

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();

        // 6 targets are ready and 2 are running
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.SCHEDULED, 6L);
        validationMap.put(TotalTargetCountStatus.Status.RUNNING, 2L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        changeStatusForAllRunningActions(createdRollout, Status.ERROR);
        rolloutHandler.handleAll();
        // 6 targets are ready and 2 are error
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.SCHEDULED, 6L);
        validationMap.put(TotalTargetCountStatus.Status.ERROR, 2L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);
    }

    /**
     * Verify that the targets have the right status during the rollout when receiving the status of rollout groups.
     */
    @Test
    void countCorrectStatusForEachTargetGroupDuringRollout() {

        final int amountTargetsForRollout = 9;
        final int amountOtherTargets = 15;
        final int amountGroups = 4;
        final String successCondition = "50";
        final String errorCondition = "80";
        Rollout createdRollout = testdataFactory.createAndStartRollout(amountTargetsForRollout, amountOtherTargets,
                amountGroups,
                successCondition, errorCondition);

        changeStatusForAllRunningActions(createdRollout, Status.FINISHED);
        rolloutHandler.handleAll();

        // round(9/4)=2 targets finished (Group 1)
        // round(7/3)=2 targets running (Group 3)
        // round(5/2)=3 targets SCHEDULED (Group 3)
        // round(2/1)=2 targets SCHEDULED (Group 4)
        createdRollout = reloadRollout(createdRollout);
        final List<RolloutGroup> rolloutGroups = rolloutGroupManagement.findByRollout(createdRollout.getId(), PAGE)
                .getContent();

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

    /**
     * Verify that target actions of rollout get canceled when a manuel distribution sets assignment is done.
     */
    @Test
    void targetsOfRolloutGetsManuelDsAssignment() {

        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 0;
        final int amountGroups = 2;
        final String successCondition = "50";
        final String errorCondition = "80";
        Rollout createdRollout = testdataFactory.createAndStartRollout(amountTargetsForRollout, amountOtherTargets,
                amountGroups,
                successCondition, errorCondition);
        final DistributionSet ds = createdRollout.getDistributionSet();

        createdRollout = reloadRollout(createdRollout);
        // 5 targets are running
        final List<Action> runningActions = findActionsByRolloutAndStatus(createdRollout, Status.RUNNING);
        assertThat(runningActions).hasSize(5);

        // 5 targets are in the group and the DS has been assigned
        final List<RolloutGroup> rolloutGroups = rolloutGroupManagement.findByRollout(createdRollout.getId(), PAGE)
                .getContent();
        final Page<Target> targets = rolloutGroupManagement.findTargetsOfRolloutGroup(rolloutGroups.get(0).getId(), PAGE
        );
        final List<Target> targetList = targets.getContent();
        assertThat(targetList).hasSize(5);

        targets.getContent().stream().map(Target::getControllerId).map(deploymentManagement::findAssignedDistributionSet)
                .forEach(d -> assertThat(d.get().getId()).isEqualTo(ds.getId()));

        final List<Target> targetToCancel = new ArrayList<>();
        targetToCancel.add(targetList.get(0));
        targetToCancel.add(targetList.get(1));
        targetToCancel.add(targetList.get(2));
        final DistributionSet dsForCancelTest = testdataFactory.createDistributionSet("dsForTest");
        assignDistributionSet(dsForCancelTest, targetToCancel);
        // 5 targets are canceling but still have the status running and 5 are
        // still in SCHEDULED
        final Map<TotalTargetCountStatus.Status, Long> validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.RUNNING, 5L);
        validationMap.put(TotalTargetCountStatus.Status.SCHEDULED, 5L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);
    }

    /**
     * Verify that target actions of a rollout get cancelled when another rollout with same targets gets started.
     */
    @Test
    void targetsOfRolloutFindDistributionSetAssignmentByOtherRollout() {

        final int amountTargetsForRollout = 15;
        final int amountOtherTargets = 5;
        final int amountGroups = 3;
        final String successCondition = "50";
        final String errorCondition = "80";
        Rollout rolloutOne = testdataFactory.createAndStartRollout(amountTargetsForRollout, amountOtherTargets,
                amountGroups,
                successCondition, errorCondition);

        rolloutOne = reloadRollout(rolloutOne);

        final DistributionSet dsForRolloutTwo = testdataFactory.createDistributionSet("dsForRolloutTwo");

        final Rollout rolloutTwo = testdataFactory.createRolloutByVariables("rolloutTwo",
                "This is the description for rollout two", 1, "controllerId==rollout-*", dsForRolloutTwo, "50", "80");
        changeStatusForAllRunningActions(rolloutOne, Status.FINISHED);
        rolloutHandler.handleAll();
        // Verify that 5 targets are finished, 5 are running and 5 are ready.
        Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.RUNNING, 5L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.FINISHED, 5L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.SCHEDULED, 5L);
        validateRolloutActionStatus(rolloutOne.getId(), expectedTargetCountStatus);

        rolloutManagement.start(rolloutTwo.getId());

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();

        // Verify that 5 targets are finished, 5 are still running and 5 are
        // cancelled.
        expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.FINISHED, 5L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.RUNNING, 5L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.CANCELLED, 5L);
        validateRolloutActionStatus(rolloutOne.getId(), expectedTargetCountStatus);
    }

    /**
     * Verify that error status of DistributionSet installation during rollout can get rerun with second rollout so that all targets have some DistributionSet installed at the end.
     */
    @Test
    void startSecondRolloutAfterFirstRolloutEndsWithErrors() {

        final int amountTargetsForRollout = 15;
        final int amountOtherTargets = 0;
        final int amountGroups = 3;
        final String successCondition = "50";
        final String errorCondition = "80";
        Rollout rolloutOne = testdataFactory.createAndStartRollout(amountTargetsForRollout, amountOtherTargets,
                amountGroups,
                successCondition, errorCondition);
        final DistributionSet distributionSet = rolloutOne.getDistributionSet();

        rolloutOne = reloadRollout(rolloutOne);
        changeStatusForRunningActions(rolloutOne, Status.ERROR, 2);
        changeStatusForRunningActions(rolloutOne, Status.FINISHED, 3);
        rolloutHandler.handleAll();
        changeStatusForRunningActions(rolloutOne, Status.ERROR, 2);
        changeStatusForRunningActions(rolloutOne, Status.FINISHED, 3);
        rolloutHandler.handleAll();
        changeStatusForRunningActions(rolloutOne, Status.ERROR, 2);
        changeStatusForRunningActions(rolloutOne, Status.FINISHED, 3);
        rolloutHandler.handleAll();

        // 9 targets are finished and 6 have error
        Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.FINISHED, 9L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.ERROR, 6L);
        validateRolloutActionStatus(rolloutOne.getId(), expectedTargetCountStatus);
        // rollout is finished
        rolloutOne = reloadRollout(rolloutOne);
        assertThat(rolloutOne.getStatus()).isEqualTo(RolloutStatus.FINISHED);

        final int amountGroupsForRolloutTwo = 1;
        Rollout rolloutTwo = testdataFactory.createRolloutByVariables("rolloutTwo",
                "This is the description for rollout two", amountGroupsForRolloutTwo, "controllerId==rollout-*",
                distributionSet, "50", "80");

        rolloutManagement.start(rolloutTwo.getId());

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();

        rolloutTwo = reloadRollout(rolloutTwo);
        // 6 error targets are now running
        expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.RUNNING, 6L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.FINISHED, 9L);
        validateRolloutActionStatus(rolloutTwo.getId(), expectedTargetCountStatus);
        changeStatusForAllRunningActions(rolloutTwo, Status.FINISHED);
        final List<? extends Target> targetList = findByUpdateStatus(TargetUpdateStatus.IN_SYNC, PAGE);
        // 15 targets in finished/IN_SYNC status and same DS assigned
        assertThat(targetList).hasSize(amountTargetsForRollout);
        targetList.stream()
                .map(Target::getControllerId)
                .map(deploymentManagement::findAssignedDistributionSet)
                .forEach(d -> assertThat(d).contains(distributionSet));
    }

    /**
     * Verify that the rollout moves to the next group when the success condition was achieved and the error condition was not exceeded.
     */
    @Test
    void successConditionAchievedAndErrorConditionNotExceeded() {

        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 0;
        final int amountGroups = 2;
        final String successCondition = "50";
        final String errorCondition = "80";
        Rollout rolloutOne = testdataFactory.createAndStartRollout(amountTargetsForRollout, amountOtherTargets,
                amountGroups,
                successCondition, errorCondition);

        rolloutOne = reloadRollout(rolloutOne);
        changeStatusForRunningActions(rolloutOne, Status.ERROR, 2);
        changeStatusForRunningActions(rolloutOne, Status.FINISHED, 3);
        rolloutHandler.handleAll();
        // verify: 40% error but 60% finished -> should move to next group
        final List<RolloutGroup> rolloutGroups = rolloutGroupManagement.findByRollout(rolloutOne.getId(), PAGE)
                .getContent();
        final Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.RUNNING, 5L);
        validateRolloutGroupActionStatus(rolloutGroups.get(1), expectedTargetCountStatus);

    }

    /**
     * Verify that the rollout does not move to the next group when the success condition was not achieved.
     */
    @Test
    void successConditionNotAchieved() {

        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 0;
        final int amountGroups = 2;
        final String successCondition = "80";
        final String errorCondition = "90";
        Rollout rolloutOne = testdataFactory.createAndStartRollout(amountTargetsForRollout, amountOtherTargets,
                amountGroups,
                successCondition, errorCondition);

        rolloutOne = reloadRollout(rolloutOne);
        changeStatusForRunningActions(rolloutOne, Status.ERROR, 2);
        changeStatusForRunningActions(rolloutOne, Status.FINISHED, 3);
        rolloutHandler.handleAll();
        // verify: 40% error and 60% finished -> should not move to next group
        // because successCondition 80%
        final List<RolloutGroup> rolloutGruops = rolloutGroupManagement.findByRollout(rolloutOne.getId(), PAGE)
                .getContent();
        final Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.SCHEDULED, 5L);
        validateRolloutGroupActionStatus(rolloutGruops.get(1), expectedTargetCountStatus);
    }

    /**
     * Verify that the rollout pauses when the error condition was exceeded.
     */
    @Test
    void errorConditionExceeded() {

        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 0;
        final int amountGroups = 2;
        final String successCondition = "50";
        final String errorCondition = "20";
        Rollout rolloutOne = testdataFactory.createAndStartRollout(amountTargetsForRollout, amountOtherTargets,
                amountGroups,
                successCondition, errorCondition);

        rolloutOne = reloadRollout(rolloutOne);
        changeStatusForRunningActions(rolloutOne, Status.ERROR, 2);
        changeStatusForRunningActions(rolloutOne, Status.FINISHED, 3);
        rolloutHandler.handleAll();
        // verify: 40% error -> should pause because errorCondition is 20%
        rolloutOne = reloadRollout(rolloutOne);
        assertThat(rolloutOne.getStatus()).isEqualTo(RolloutStatus.PAUSED);
    }

    /**
     * Verify that all rollouts are return with expected target statuses.
     */
    @Test
    void findAllRolloutsWithDetailedStatus() {

        final int amountTargetsForRollout = 12;
        final int amountGroups = 2;
        final String successCondition = "50";
        final String errorCondition = "20";
        final Rollout rolloutA = createTestRolloutWithTargetsAndDistributionSet(amountTargetsForRollout, amountGroups,
                successCondition, errorCondition, "RolloutA", "RolloutA");
        rolloutManagement.start(rolloutA.getId());
        rolloutHandler.handleAll();

        final int amountTargetsForRollout2 = 10;
        final int amountGroups2 = 2;
        final Rollout rolloutB = createTestRolloutWithTargetsAndDistributionSet(amountTargetsForRollout2, amountGroups2,
                successCondition, errorCondition, "RolloutB", "RolloutB");
        rolloutManagement.start(rolloutB.getId());
        rolloutHandler.handleAll();

        changeStatusForAllRunningActions(rolloutB, Status.FINISHED);
        rolloutHandler.handleAll();

        final int amountTargetsForRollout3 = 10;
        final int amountGroups3 = 2;
        final Rollout rolloutC = createTestRolloutWithTargetsAndDistributionSet(amountTargetsForRollout3, amountGroups3,
                successCondition, errorCondition, "RolloutC", "RolloutC");
        rolloutManagement.start(rolloutC.getId());
        rolloutHandler.handleAll();

        changeStatusForAllRunningActions(rolloutC, Status.ERROR);
        rolloutHandler.handleAll();

        final int amountTargetsForRollout4 = 15;
        final int amountGroups4 = 3;
        final Rollout rolloutD = createTestRolloutWithTargetsAndDistributionSet(amountTargetsForRollout4, amountGroups4,
                successCondition, errorCondition, "RolloutD", "RolloutD");
        rolloutManagement.start(rolloutD.getId());
        rolloutHandler.handleAll();

        changeStatusForRunningActions(rolloutD, Status.ERROR, 1);
        rolloutHandler.handleAll();
        changeStatusForAllRunningActions(rolloutD, Status.FINISHED);
        rolloutHandler.handleAll();

        final Slice<Rollout> rolloutPage = rolloutManagement
                .findAllWithDetailedStatus(false, new OffsetBasedPageRequest(0, 100, Sort.by(Direction.ASC, "name")));
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

    /**
     * Verify the count of existing rollouts.
     */
    @Test
    void rightCountForAllRollouts() {

        final int amountTargetsForRollout = 6;
        final int amountGroups = 2;
        final String successCondition = "50";
        final String errorCondition = "80";
        for (int i = 1; i <= 10; i++) {
            createTestRolloutWithTargetsAndDistributionSet(amountTargetsForRollout, amountGroups, successCondition,
                    errorCondition, "Rollout" + i, "Rollout" + i);
        }
        final Long count = rolloutManagement.count();
        assertThat(count).isEqualTo(10L);
    }

    /**
     * Verify that the filtering and sorting ascending for rollout is working correctly.
     */
    @Test
    void findRolloutByFilters() {
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

        final Slice<Rollout> rollout = rolloutManagement.findByRsqlWithDetailedStatus(
                "name==Rollout*", false, new OffsetBasedPageRequest(0, 100, Sort.by(Direction.ASC, "name")));
        final List<Rollout> rolloutList = rollout.getContent();
        assertThat(rolloutList).hasSize(5);
        int i = 1;
        for (final Rollout r : rolloutList) {
            assertThat(r.getName()).isEqualTo("Rollout" + i);
            i++;
        }
    }

    /**
     * Verify that the percent count is acting like aspected when targets move to the status finished or error.
     */
    @Test
    void findFinishedPercentForRunningGroup() {

        final int amountTargetsForRollout = 10;
        final int amountGroups = 2;
        final String successCondition = "50";
        final String errorCondition = "80";
        final String rolloutName = "MyRollout";
        Rollout myRollout = createTestRolloutWithTargetsAndDistributionSet(
                amountTargetsForRollout, amountGroups, successCondition, errorCondition, rolloutName, rolloutName);
        rolloutManagement.start(myRollout.getId());

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();

        changeStatusForRunningActions(myRollout, Status.FINISHED, 2);
        rolloutHandler.handleAll();
        myRollout = reloadRollout(myRollout);

        float percent = rolloutGroupManagement
                .getWithDetailedStatus(rolloutGroupManagement.findByRollout(myRollout.getId(), PAGE).getContent().get(0).getId())
                .getTotalTargetCountStatus().getFinishedPercent();
        assertThat(percent).isEqualTo(40);

        changeStatusForRunningActions(myRollout, Status.FINISHED, 3);
        rolloutHandler.handleAll();

        percent = rolloutGroupManagement
                .getWithDetailedStatus(rolloutGroupManagement.findByRollout(myRollout.getId(), PAGE).getContent().get(0).getId())
                .getTotalTargetCountStatus().getFinishedPercent();
        assertThat(percent).isEqualTo(100);

        changeStatusForRunningActions(myRollout, Status.FINISHED, 4);
        changeStatusForAllRunningActions(myRollout, Status.ERROR);
        rolloutHandler.handleAll();

        percent = rolloutGroupManagement
                .getWithDetailedStatus(rolloutGroupManagement.findByRollout(myRollout.getId(), PAGE).getContent().get(1).getId())
                .getTotalTargetCountStatus().getFinishedPercent();
        assertThat(percent).isEqualTo(80);
    }

    /**
     * Verify that the expected targets are returned for the rollout groups.
     */
    @Test
    void findRolloutGroupTargetsWithRsqlParam() {

        final int amountTargetsForRollout = 15;
        final int amountGroups = 3;
        final int amountOtherTargets = 15;
        final String successCondition = "50";
        final String errorCondition = "80";
        final String rolloutName = "MyRollout";
        Rollout myRollout = createTestRolloutWithTargetsAndDistributionSet(
                amountTargetsForRollout, amountGroups, successCondition, errorCondition, rolloutName, rolloutName);

        testdataFactory.createTargets(amountOtherTargets, "others-", "rollout");

        final String rsql = "controllerId==*MyRoll*";

        rolloutManagement.start(myRollout.getId());

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();

        final Condition<String> targetBelongsInRollout = new Condition<>(s -> s.startsWith(rolloutName), "Target belongs into rollout");

        myRollout = reloadRollout(myRollout);
        final List<RolloutGroup> rolloutGroups = rolloutGroupManagement.findByRollout(myRollout.getId(), PAGE).getContent();

        Page<Target> targetPage = rolloutGroupManagement.findTargetsOfRolloutGroupByRsql(
                rolloutGroups.get(0).getId(), rsql, new OffsetBasedPageRequest(0, 100));
        final List<Target> targetlistGroup1 = targetPage.getContent();
        assertThat(targetlistGroup1).hasSize(5);
        assertThat(targetlistGroup1.stream().map(Target::getControllerId).toList())
                .are(targetBelongsInRollout);

        targetPage = rolloutGroupManagement.findTargetsOfRolloutGroupByRsql(rolloutGroups.get(1).getId(), rsql,
                new OffsetBasedPageRequest(0, 100)
        );
        final List<Target> targetlistGroup2 = targetPage.getContent();
        assertThat(targetlistGroup2).hasSize(5);
        assertThat(targetlistGroup2.stream().map(Target::getControllerId).toList())
                .are(targetBelongsInRollout);

        targetPage = rolloutGroupManagement.findTargetsOfRolloutGroupByRsql(rolloutGroups.get(2).getId(), rsql,
                new OffsetBasedPageRequest(0, 100)
        );
        final List<Target> targetlistGroup3 = targetPage.getContent();
        assertThat(targetlistGroup3).hasSize(5);
        assertThat(targetlistGroup3.stream().map(Target::getControllerId).toList()).are(targetBelongsInRollout);

    }

    /**
     * Verify the creation of a Rollout without targets throws an Exception.
     */
    @Test
    void createRolloutNotMatchingTargets() {
        final int amountGroups = 5;
        final String successCondition = "50";
        final String errorCondition = "80";
        final String rolloutName = "rolloutTest3";

        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsFor" + rolloutName);

        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> testdataFactory.createRolloutByVariables(rolloutName, "desc", amountGroups,
                        "id==notExisting", distributionSet, successCondition, errorCondition))
                .withMessageContaining("does not match any existing");

    }

    /**
     * Verify the creation of a Rollout with the same name throws an Exception.
     */
    @Test
    void createDuplicateRollout() {
        final int amountGroups = 5;
        final int amountTargetsForRollout = 10;
        final String successCondition = "50";
        final String errorCondition = "80";
        final String rolloutName = "rolloutTest4";

        testdataFactory.createTargets(amountTargetsForRollout, "dup-ro-", "rollout");

        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsFor" + rolloutName);
        testdataFactory.createRolloutByVariables(rolloutName, "desc", amountGroups, "id==dup-ro-*", distributionSet,
                successCondition, errorCondition);

        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .isThrownBy(() -> testdataFactory.createRolloutByVariables(rolloutName, "desc", amountGroups,
                        "id==dup-ro-*", distributionSet, successCondition, errorCondition))
                .withMessageContaining("already exists in database");
    }

    /**
     * Verify the creation and the start of a Rollout with more groups than targets.
     */
    @Test
    void createAndStartRolloutWithEmptyGroups() {
        final int amountTargetsForRollout = 3;
        final int amountGroups = 5;
        final String successCondition = "50";
        final String errorCondition = "80";
        final String rolloutName = "rolloutTestG";
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsFor" + rolloutName);
        testdataFactory.createTargets(amountTargetsForRollout, rolloutName + "-", rolloutName);

        Rollout myRollout = testdataFactory.createRolloutByVariables(rolloutName, "desc", amountGroups,
                "controllerId==" + rolloutName + "-*", distributionSet, successCondition, errorCondition);

        assertThat(myRollout.getStatus()).isEqualTo(RolloutStatus.READY);

        final Long myRolloutId = myRollout.getId();
        final List<RolloutGroup> groups = rolloutGroupManagement.findByRollout(myRolloutId, PAGE).getContent();

        assertThat(groups.get(0).getStatus()).isEqualTo(RolloutGroupStatus.READY);
        assertThat(groups.get(0).getTotalTargets()).isEqualTo(1);
        assertThat(groups.get(1).getStatus()).isEqualTo(RolloutGroupStatus.READY);
        assertThat(groups.get(1).getTotalTargets()).isEqualTo(1);
        assertThat(groups.get(2).getStatus()).isEqualTo(RolloutGroupStatus.READY);
        assertThat(groups.get(2).getTotalTargets()).isZero();
        assertThat(groups.get(3).getStatus()).isEqualTo(RolloutGroupStatus.READY);
        assertThat(groups.get(3).getTotalTargets()).isEqualTo(1);
        assertThat(groups.get(4).getStatus()).isEqualTo(RolloutGroupStatus.READY);
        assertThat(groups.get(4).getTotalTargets()).isZero();

        rolloutManagement.start(myRolloutId);

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();

        myRollout = rolloutManagement.get(myRolloutId);
        assertThat(myRollout.getStatus()).isEqualTo(RolloutStatus.RUNNING);
        final Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.RUNNING, 1L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.SCHEDULED, 2L);
        validateRolloutActionStatus(myRolloutId, expectedTargetCountStatus);
    }

    /**
     * Verify the creation and the start of a rollout.
     */
    @Test
    void createAndStartRollout() {
        final int amountTargetsForRollout = 50;
        final int amountGroups = 5;
        final String successCondition = "50";
        final String errorCondition = "80";
        final String rolloutName = "rolloutTest";
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsFor" + rolloutName);
        testdataFactory.createTargets(amountTargetsForRollout, rolloutName + "-", rolloutName);

        Rollout myRollout = testdataFactory.createRolloutByVariables(
                rolloutName, "desc", amountGroups, "controllerId==" + rolloutName + "-*", distributionSet, successCondition, errorCondition);
        assertThat(myRollout.getStatus()).isEqualTo(RolloutStatus.READY);

        rolloutHandler.handleAll();
        myRollout = rolloutManagement.get(myRollout.getId());
        assertThat(myRollout.getStatus()).isEqualTo(RolloutStatus.READY);

        rolloutManagement.start(myRollout.getId());
        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();
        myRollout = reloadRollout(myRollout);
        assertThat(myRollout.getStatus()).isEqualTo(RolloutStatus.RUNNING);

        final Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.RUNNING, 10L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.SCHEDULED, 40L);
        validateRolloutActionStatus(myRollout.getId(), expectedTargetCountStatus);
    }

    /**
     * Verify the creation and the start of a rollout.
     */
    @Test
    void createScheduledRollout() {
        final String rolloutName = "scheduledRolloutTest";
        testdataFactory.createTargets(50, rolloutName + "-", rolloutName);
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsFor" + rolloutName);
        distributionSetManagement.lock(distributionSet);

        final WithUser userWithoutHandleRollout = SecurityContextSwitch.withUser(
                "user_without_handle_rollout",
                SpPermission.READ_DISTRIBUTION_SET, SpPermission.READ_TARGET, SpPermission.CREATE_ROLLOUT);
        final WithUser userWithHandleRollout = SecurityContextSwitch.withUser(
                "user_with_handle_rollout",
                SpPermission.READ_DISTRIBUTION_SET, SpPermission.READ_TARGET, SpPermission.CREATE_ROLLOUT, SpPermission.HANDLE_ROLLOUT);
        final WithUser userWithSystemRole = SecurityContextSwitch.withUser(
                "user_with_system_role",
                SpRole.SYSTEM_ROLE);

        final String filter = "controllerId==" + rolloutName + "-*";
        // create scheduled rollout fails without handle rollout permission
        assertThatExceptionOfType(InsufficientPermissionException.class)
                .as("Insufficient permission exception when startAt and no handle rollout permission")
                .isThrownBy(() -> SecurityContextSwitch.getAs(
                        userWithoutHandleRollout, () -> createRolloutWithStartAt(rolloutName, filter, distributionSet, 1L)));
        // same action succeeds with handle rollout permission
        SecurityContextSwitch.getAs(
                userWithHandleRollout,
                () -> createRolloutWithStartAt(rolloutName + "_withStartTime", filter, distributionSet, 1L));
        // same action succeeds with system role permission
        SecurityContextSwitch.getAs(
                userWithSystemRole,
                () -> createRolloutWithStartAt(rolloutName + "_withStartTimeSystemRole", filter, distributionSet, 1L));
        // same action succeeds without handle rollout permission but with null start at
        SecurityContextSwitch.getAs(
                userWithoutHandleRollout,
                () -> createRolloutWithStartAt(rolloutName + "_withoutStartTime", filter, distributionSet, null));
        // same action succeeds without handle rollout permission but with Long.MAX_VALUE start at
        SecurityContextSwitch.getAs(
                userWithoutHandleRollout,
                () -> createRolloutWithStartAt(rolloutName + "_withLongMax", filter, distributionSet, Long.MAX_VALUE));
    }

    private Rollout createRolloutWithStartAt(
            final String rolloutName, final String filter, final DistributionSet distributionSet, final Long startAt) {
        return rolloutManagement.create(
                Create.builder()
                        .name(rolloutName)
                        .description("desc")
                        .targetFilterQuery(filter)
                        .distributionSet(distributionSet)
                        .weight(1000)
                        .startAt(startAt)
                        .dynamic(false)
                        .build(),
                5, false, new RolloutGroupConditionBuilder().withDefaults()
                        .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "80")
                        .errorCondition(RolloutGroupErrorCondition.THRESHOLD, "50")
                        .errorAction(RolloutGroupErrorAction.PAUSE, null).build(), null);
    }

    /**
     * Verify that a rollout cannot be created if the 'max targets per rollout group' quota is violated.
     */
    @Test
    void createRolloutFailsIfQuotaGroupQuotaIsViolated() {

        final int maxTargets = quotaManagement.getMaxTargetsPerRolloutGroup();

        final int amountTargetsForRollout = maxTargets + 1;
        final int amountGroups = 1;
        final String successCondition = "50";
        final String errorCondition = "80";
        final String rolloutName = "rolloutTest";
        final String targetPrefixName = rolloutName;
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsFor" + rolloutName);
        testdataFactory.createTargets(amountTargetsForRollout, targetPrefixName + "-", targetPrefixName);

        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder().withDefaults()
                .successCondition(RolloutGroupSuccessCondition.THRESHOLD, successCondition)
                .errorCondition(RolloutGroupErrorCondition.THRESHOLD, errorCondition)
                .errorAction(RolloutGroupErrorAction.PAUSE, null).build();

        final Create rollout = Create.builder()
                .name(rolloutName).description(rolloutName)
                .targetFilterQuery("controllerId==" + targetPrefixName + "-*").distributionSet(distributionSet)
                .build();

        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> rolloutManagement.create(rollout, amountGroups, false, conditions));
    }

    /**
     * Verify that a rollout cannot be created based on group definitions if the 'max targets per rollout group' quota is violated for one of the groups.
     */
    @Test
    void createRolloutWithGroupDefinitionsFailsIfQuotaGroupQuotaIsViolated() {
        final int maxTargets = quotaManagement.getMaxTargetsPerRolloutGroup();

        final int amountTargetsForRollout = maxTargets * 2 + 2;
        final String rolloutName = "rolloutTest";
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsFor" + rolloutName);
        testdataFactory.createTargets(amountTargetsForRollout, rolloutName + "-", rolloutName);

        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder().withDefaults().build();

        // create group definitions
        final GroupCreate group1 = GroupCreate.builder().conditions(conditions).name("group1").targetPercentage(50.0F).build();
        final GroupCreate group2 = GroupCreate.builder().conditions(conditions).name("group2").targetPercentage(50.0F).build();

        // group1 exceeds the quota
        final Create rolloutCreate = Create.builder()
                .name(rolloutName)
                .description(rolloutName)
                .targetFilterQuery("controllerId==" + rolloutName + "-*")
                .distributionSet(distributionSet)
                .build();
        final List<GroupCreate> groups = List.of(group1, group2);
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> rolloutManagement.create(rolloutCreate, groups, conditions));

        // create group definitions
        final GroupCreate group3 = GroupCreate.builder().conditions(conditions).name("group3").targetPercentage(1.0F).build();
        final GroupCreate group4 = GroupCreate.builder().conditions(conditions).name("group4").targetPercentage(100.0F).build();

        // group4 exceeds the quota
        final List<GroupCreate> groups2 = List.of(group3, group4);
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> rolloutManagement.create(rolloutCreate, groups2, conditions));

        // create group definitions
        final GroupCreate group5 = GroupCreate.builder().conditions(conditions).name("group5").targetPercentage(33.3F).build();
        final GroupCreate group6 = GroupCreate.builder().conditions(conditions).name("group6").targetPercentage(33.3F).build();
        final GroupCreate group7 = GroupCreate.builder().conditions(conditions).name("group7").targetPercentage(33.3F).build();

        // should work fine
        assertThat(rolloutManagement.create(rolloutCreate, Arrays.asList(group5, group6, group7), conditions)).isNotNull();
    }

    /**
     * Verify the update of a rollout
     */
    @Test
    void updateRollout() {
        final int amountTargetsForRollout = 50;
        final int amountGroups = 5;
        final String successCondition = "50";
        final String errorCondition = "80";
        final String rolloutName = "rolloutTest8";
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsFor" + rolloutName);
        testdataFactory.createTargets(amountTargetsForRollout, rolloutName + "-", rolloutName);

        Rollout myRollout = testdataFactory.createRolloutByVariables(rolloutName, "desc", amountGroups,
                "controllerId==" + rolloutName + "-*", distributionSet, successCondition, errorCondition);

        assertThat(myRollout.getStatus()).isEqualTo(RolloutStatus.READY);

        // schedule rollout auto start into the future
        final Long myRolloutId = myRollout.getId();
        rolloutManagement.update(Update.builder().id(myRolloutId).name("newName").description("newDesc").build());
        rolloutHandler.handleAll();

        // rollout should not have been started
        myRollout = rolloutManagement.get(myRolloutId);
        assertThat(myRollout.getName()).isEqualTo("newName");
        assertThat(myRollout.getDescription()).isEqualTo("newDesc");
    }

    /**
     * Verify the creation of a rollout with a groups definition.
     */
    @Test
    void createRolloutWithGroupDefinition() {
        final String rolloutName = "rolloutTest3";

        final int amountTargetsInGroup1 = 10;
        final int percentTargetsInGroup1 = 100;

        final int amountTargetsInGroup2and3 = 20;
        final int percentTargetsInGroup2 = 20;
        final int percentTargetsInGroup3 = 80;

        final int countTargetsInGroup2 = (int) Math
                .ceil((double) percentTargetsInGroup2 / 100 * amountTargetsInGroup2and3);
        final int countTargetsInGroup3 = amountTargetsInGroup2and3 - countTargetsInGroup2;

        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder().withDefaults().build();
        // Generate Targets for group 2 and 3 and generate the Rollout
        final Create rolloutcreate = generateTargetsAndRollout(rolloutName, amountTargetsInGroup2and3);

        // Generate Targets for group 1
        testdataFactory.createTargets(amountTargetsInGroup1, rolloutName + "-gr1-", rolloutName);

        final List<GroupCreate> rolloutGroups = new ArrayList<>(3);
        rolloutGroups.add(generateRolloutGroup(0, percentTargetsInGroup1, "id==" + rolloutName + "-gr1-*"));
        rolloutGroups.add(generateRolloutGroup(1, percentTargetsInGroup2, null));
        rolloutGroups.add(generateRolloutGroup(2, percentTargetsInGroup3, null));

        Rollout myRollout = rolloutManagement.create(rolloutcreate, rolloutGroups, conditions);
        myRollout = rolloutManagement.get(myRollout.getId());

        assertThat(myRollout.getStatus()).isEqualTo(RolloutStatus.CREATING);
        for (final RolloutGroup group : rolloutGroupManagement.findByRollout(myRollout.getId(), PAGE).getContent()) {
            assertThat(group.getStatus()).isEqualTo(RolloutGroupStatus.CREATING);
        }

        // Generate Targets that must not be addressed by the rollout, because they were added after the rollout was created
        testdataFactory.createTargets(10, rolloutName + "-notIn-", rolloutName);

        rolloutHandler.handleAll();

        myRollout = rolloutManagement.get(myRollout.getId());
        assertThat(myRollout.getStatus()).isEqualTo(RolloutStatus.READY);
        assertThat(myRollout.getTotalTargets()).isEqualTo(amountTargetsInGroup2and3 + amountTargetsInGroup1);

        final List<RolloutGroup> groups = rolloutGroupManagement.findByRollout(myRollout.getId(), PAGE).getContent();

        assertThat(groups.get(0).getStatus()).isEqualTo(RolloutGroupStatus.READY);
        assertThat(groups.get(0).getTotalTargets()).isEqualTo(amountTargetsInGroup1);

        assertThat(groups.get(1).getStatus()).isEqualTo(RolloutGroupStatus.READY);
        assertThat(groups.get(1).getTotalTargets()).isEqualTo(countTargetsInGroup2);

        assertThat(groups.get(2).getStatus()).isEqualTo(RolloutGroupStatus.READY);
        assertThat(groups.get(2).getTotalTargets()).isEqualTo(countTargetsInGroup3);
    }

    /**
     * Verify rollout execution with advanced group definition and confirmation flow active.
     */
    @Test
    void createRolloutWithGroupDefinitionAndConfirmationFlowActive() {
        final String rolloutName = "rolloutTest4";

        final int amountTargetsInGroup1 = 10;
        final int amountTargetsInGroup2 = 20;

        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder().withDefaults().build();
        // Generate Targets for group 2
        final Create rolloutcreate = generateTargetsAndRollout(rolloutName, amountTargetsInGroup2);

        // Generate Targets for group 1
        testdataFactory.createTargets(amountTargetsInGroup1, rolloutName + "-gr1-", rolloutName);

        final List<GroupCreate> rolloutGroups = new ArrayList<>(3);
        rolloutGroups.add(generateRolloutGroup(0, 100, "id==" + rolloutName + "-gr1-*", true));
        rolloutGroups.add(generateRolloutGroup(1, 100, null, false));

        // enable confirmation flow
        enableConfirmationFlow();

        final Long rolloutId = rolloutManagement.create(rolloutcreate, rolloutGroups, conditions).getId();

        assertThat(rolloutManagement.get(rolloutId)).satisfies(rollout -> {
            assertThat(rollout.getStatus()).isEqualTo(RolloutStatus.CREATING);
            for (final RolloutGroup group : rolloutGroupManagement.findByRollout(rollout.getId(), PAGE).getContent()) {
                assertThat(group.getStatus()).isEqualTo(RolloutGroupStatus.CREATING);
            }
        });

        // first handle iteration will put rollout in ready state
        rolloutHandler.handleAll();

        assertThat(rolloutManagement.get(rolloutId)).satisfies(rollout -> {
            assertThat(rollout.getStatus()).isEqualTo(RolloutStatus.READY);
            assertThat(rollout.getTotalTargets()).isEqualTo(amountTargetsInGroup1 + amountTargetsInGroup2);
        });

        // verify created rollout groups
        final List<Long> rolloutGroupIds = rolloutGroupManagement.findByRollout(rolloutId, PAGE).getContent().stream()
                .map(Identifiable::getId).toList();
        assertThat(rolloutGroupIds).hasSize(2);
        assertRolloutGroup(rolloutGroupIds.get(0), RolloutGroupStatus.READY, true, amountTargetsInGroup1, null);
        assertRolloutGroup(rolloutGroupIds.get(1), RolloutGroupStatus.READY, false, amountTargetsInGroup2, null);

        // start rollout
        rolloutManagement.start(rolloutId);
        rolloutHandler.handleAll();

        // verify rollout started. Check groups are in right state.
        // Group 1 should be in WFC state, since confirmation is required here.
        assertRolloutGroup(rolloutGroupIds.get(0), RolloutGroupStatus.RUNNING, true, amountTargetsInGroup1,
                Status.WAIT_FOR_CONFIRMATION);
        assertRolloutGroup(rolloutGroupIds.get(1), RolloutGroupStatus.SCHEDULED, false, amountTargetsInGroup2,
                Status.SCHEDULED);

        // cancel execution of all action of group 1 to trigger second group
        forceQuitAllActionsOfRolloutGroup(rolloutGroupIds.get(0));

        rolloutHandler.handleAll();
        assertRolloutGroup(rolloutGroupIds.get(0), RolloutGroupStatus.FINISHED, true, amountTargetsInGroup1,
                Status.CANCELED);
        assertRolloutGroup(rolloutGroupIds.get(1), RolloutGroupStatus.SCHEDULED, false, amountTargetsInGroup2,
                Status.SCHEDULED);

        // verify actions of second rule are directly in RUNNING state, since
        // confirmation is not required for this group
        rolloutHandler.handleAll();
        assertRolloutGroup(rolloutGroupIds.get(0), RolloutGroupStatus.FINISHED, true, amountTargetsInGroup1,
                Status.CANCELED);
        assertRolloutGroup(rolloutGroupIds.get(1), RolloutGroupStatus.RUNNING, false, amountTargetsInGroup2,
                Status.RUNNING);

    }

    /**
     * Verify rollout creation fails if group definition does not address all targets
     */
    @Test
    void createRolloutWithGroupsNotMatchingTargets() {
        final String rolloutName = "rolloutTest4";
        final int amountTargetsForRollout = 500;
        final int percentTargetsInGroup1 = 20;
        final int percentTargetsInGroup2 = 50;

        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder().withDefaults().build();
        final Create myRollout = generateTargetsAndRollout(rolloutName, amountTargetsForRollout);

        final List<GroupCreate> rolloutGroups = new ArrayList<>(2);
        rolloutGroups.add(generateRolloutGroup(0, percentTargetsInGroup1, null));
        rolloutGroups.add(generateRolloutGroup(1, percentTargetsInGroup2, null));

        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> rolloutManagement.create(myRollout, rolloutGroups, conditions))
                .withMessageContaining("groups don't match");

    }

    /**
     * Verify rollout creation fails if group definition specifies illegal target percentage
     */
    @Test
    void createRolloutWithIllegalPercentage() {
        final String rolloutName = "rolloutTest6";
        final int amountTargetsForRollout = 10;
        final int percentTargetsInGroup1 = 101;
        final int percentTargetsInGroup2 = 50;

        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder().withDefaults().build();
        final Create myRollout = generateTargetsAndRollout(rolloutName, amountTargetsForRollout);

        final List<GroupCreate> rolloutGroups = Arrays.asList(
                generateRolloutGroup(0, percentTargetsInGroup1, null),
                generateRolloutGroup(1, percentTargetsInGroup2, null));

        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> rolloutManagement.create(myRollout, rolloutGroups, conditions))
                .withMessageContaining("percentage has to be between 1 and 100");

    }

    /**
     * Verify rollout creation fails if the 'max rollout groups per rollout' quota is violated.
     */
    @Test
    void createRolloutWithIllegalAmountOfGroups() {
        final String rolloutName = "rolloutTest5";
        final int targets = 10;
        final int maxGroups = quotaManagement.getMaxRolloutGroupsPerRollout();

        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder().withDefaults().build();
        final Create rollout = generateTargetsAndRollout(rolloutName, targets);

        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> rolloutManagement.create(rollout, maxGroups + 1, false, conditions))
                .withMessageContaining("not be greater than " + maxGroups);
    }

    /**
     * Verify the start of a Rollout does not work during creation phase.
     */
    @Test
    void createAndStartRolloutDuringCreationFails() {
        final int amountTargetsForRollout = 3;
        final int amountGroups = 5;
        final String successCondition = "50";
        final String errorCondition = "80";
        final String rolloutName = "rolloutTestGC";
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsFor" + rolloutName);
        testdataFactory.createTargets(amountTargetsForRollout, rolloutName + "-", rolloutName);

        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder().withDefaults()
                .successCondition(RolloutGroupSuccessCondition.THRESHOLD, successCondition)
                .errorCondition(RolloutGroupErrorCondition.THRESHOLD, errorCondition)
                .errorAction(RolloutGroupErrorAction.PAUSE, null).build();
        final Create rolloutToCreate = Create.builder()
                .name(rolloutName).description("some description")
                .targetFilterQuery("id==" + rolloutName + "-*").distributionSet(distributionSet)
                .build();

        Rollout myRollout = rolloutManagement.create(rolloutToCreate, amountGroups, false, conditions);
        myRollout = rolloutManagement.get(myRollout.getId());

        assertThat(myRollout.getStatus()).isEqualTo(RolloutStatus.CREATING);

        final Long rolloutId = myRollout.getId();
        assertThatExceptionOfType(RolloutIllegalStateException.class)
                .isThrownBy(() -> rolloutManagement.start(rolloutId))
                .withMessageContaining("can only be started in state ready");
    }

    /**
     * Creating a rollout with approval role or approval engine disabled results in the rollout being in
     * READY state.
     */
    @Test
    void createdRolloutWithApprovalRoleOrApprovalDisabledTransitionsToReadyState() {
        approvalStrategy.setApprovalNeeded(false);
        final String successCondition = "50";
        final String errorCondition = "80";
        final Rollout rollout = testdataFactory.createSimpleTestRolloutWithTargetsAndDistributionSet(10, 10, 5,
                successCondition,
                errorCondition);
        assertThat(rollout.getStatus()).isEqualTo(Rollout.RolloutStatus.READY);
    }

    /**
     * Creating a rollout without approve role and approval enabled leads to transition to
     * WAITING_FOR_APPROVAL state.
     */
    @Test
    void createdRolloutWithoutApprovalRoleTransitionsToWaitingForApprovalState() {
        approvalStrategy.setApprovalNeeded(true);
        final String successCondition = "50";
        final String errorCondition = "80";
        final Rollout rollout = testdataFactory.createSimpleTestRolloutWithTargetsAndDistributionSet(10, 10, 5,
                successCondition,
                errorCondition);
        assertThat(rollout.getStatus()).isEqualTo(Rollout.RolloutStatus.WAITING_FOR_APPROVAL);
    }

    /**
     * Approving a rollout leads to transition to READY state.
     */
    @Test
    void approvedRolloutTransitionsToReadyState() {
        approvalStrategy.setApprovalNeeded(true);
        final String successCondition = "50";
        final String errorCondition = "80";
        final Rollout rollout = testdataFactory.createSimpleTestRolloutWithTargetsAndDistributionSet(10, 10, 5,
                successCondition,
                errorCondition);
        assertThat(rollout.getStatus()).isEqualTo(Rollout.RolloutStatus.WAITING_FOR_APPROVAL);
        rolloutManagement.approveOrDeny(rollout.getId(), Rollout.ApprovalDecision.APPROVED);
        final Rollout resultingRollout = rolloutRepository.findById(rollout.getId()).get();
        assertThat(resultingRollout.getStatus()).isEqualTo(Rollout.RolloutStatus.READY);
    }

    /**
     * Denying approval for a rollout leads to transition to APPROVAL_DENIED state.
     */
    @Test
    void deniedRolloutTransitionsToApprovalDeniedState() {
        approvalStrategy.setApprovalNeeded(true);
        final String successCondition = "50";
        final String errorCondition = "80";
        final Rollout rollout = testdataFactory.createSimpleTestRolloutWithTargetsAndDistributionSet(10, 10, 5,
                successCondition,
                errorCondition);
        assertThat(rollout.getStatus()).isEqualTo(Rollout.RolloutStatus.WAITING_FOR_APPROVAL);
        rolloutManagement.approveOrDeny(rollout.getId(), Rollout.ApprovalDecision.DENIED);
        final Rollout resultingRollout = rolloutRepository.findById(rollout.getId()).get();
        assertThat(resultingRollout.getStatus()).isEqualTo(RolloutStatus.APPROVAL_DENIED);
    }

    @Test
    @ExpectEvents({
            @Expect(type = RolloutDeletedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 25),
            @Expect(type = RolloutUpdatedEvent.class, count = 2),
            @Expect(type = RolloutGroupCreatedEvent.class, count = 5),
            @Expect(type = RolloutGroupDeletedEvent.class, count = 5),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = RolloutGroupUpdatedEvent.class, count = 5),
            @Expect(type = RolloutCreatedEvent.class, count = 1) })
    void deleteRolloutWhichHasNeverStartedIsHardDeleted() {
        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 15;
        final int amountGroups = 5;
        final int amountTargetsPerGroup = amountTargetsForRollout / amountGroups;
        final String successCondition = "50";
        final String errorCondition = "80";
        final Rollout createdRollout = testdataFactory.createSimpleTestRolloutWithTargetsAndDistributionSet(
                amountTargetsForRollout,
                amountOtherTargets, amountGroups, successCondition, errorCondition);
        final Page<JpaRolloutGroup> rolloutGroups = rolloutGroupRepository.findByRolloutId(createdRollout.getId(), PAGE);
        assertThat(rolloutGroups.getTotalElements()).isEqualTo(amountGroups);

        for (JpaRolloutGroup group : rolloutGroups) {
            assertThat(rolloutTargetGroupRepository.countByRolloutGroup(group)).isEqualTo(amountTargetsPerGroup);
        }

        // test
        rolloutManagement.delete(createdRollout.getId());
        rolloutHandler.handleAll();

        // verify
        final Optional<JpaRollout> deletedRollout = rolloutRepository.findById(createdRollout.getId());
        assertThat(deletedRollout).isNotPresent();
        assertThat(rolloutGroupRepository.countByRolloutId(createdRollout.getId())).isZero();

        for (JpaRolloutGroup group : rolloutGroups) {
            assertThat(rolloutTargetGroupRepository.countByRolloutGroup(group)).isZero();
        }
    }

    @Test
    @ExpectEvents({
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = TargetCreatedEvent.class, count = 25),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 10),
            @Expect(type = ActionUpdatedEvent.class, count = 4),
            @Expect(type = RolloutCreatedEvent.class, count = 1),
            @Expect(type = RolloutUpdatedEvent.class, count = 6),
            @Expect(type = RolloutDeletedEvent.class, count = 1),
            @Expect(type = RolloutGroupUpdatedEvent.class, count = 16),
            @Expect(type = RolloutGroupCreatedEvent.class, count = 5) })
    void deleteRolloutWhichHasBeenStartedBeforeIsSoftDeleted() {
        final int amountTargetsForRollout = 10;
        final int amountOtherTargets = 15;
        final int amountGroups = 5;
        final String successCondition = "50";
        final String errorCondition = "80";
        final Rollout createdRollout = testdataFactory.createSimpleTestRolloutWithTargetsAndDistributionSet(
                amountTargetsForRollout, amountOtherTargets, amountGroups, successCondition, errorCondition);

        // start the rollout, so it has active running actions and a group which has been started
        rolloutManagement.start(createdRollout.getId());
        rolloutHandler.handleAll();

        // verify we have running actions
        assertThat(actionRepository.findByRolloutIdAndStatus(PAGE, createdRollout.getId(), Status.RUNNING).getNumberOfElements())
                .isEqualTo(2);

        // test
        rolloutManagement.delete(createdRollout.getId());
        rolloutHandler.handleAll();

        // verify
        final JpaRollout deletedRollout = rolloutRepository.findById(createdRollout.getId()).get();
        assertThat(deletedRollout).isNotNull();

        assertThat(deletedRollout.getStatus()).isEqualTo(RolloutStatus.DELETED);

        final Update rolloutUpdate = Update.builder().id(createdRollout.getId()).description("test").build();
        assertThatExceptionOfType(EntityReadOnlyException.class)
                .isThrownBy(() -> rolloutManagement.update(rolloutUpdate))
                .withMessageContaining("" + createdRollout.getId());

        assertThat(rolloutManagement.findAll(true, PAGE).getContent()).hasSize(1);
        assertThat(rolloutManagement.findAll(false, PAGE).getContent()).isEmpty();
        assertThat(rolloutManagement.findByRsql("name==*", true, PAGE).getContent()).hasSize(1);
        assertThat(rolloutManagement.findByRsql("name==*", false, PAGE).getContent()).isEmpty();
        assertThat(rolloutManagement.count()).isZero();
        assertThat(rolloutGroupManagement.findByRolloutWithDetailedStatus(createdRollout.getId(), PAGE).getContent()).hasSize(amountGroups);

        // verify that all scheduled actions are deleted
        assertThat(actionRepository.findByRolloutIdAndStatus(PAGE, deletedRollout.getId(), Status.SCHEDULED)
                .getNumberOfElements()).isZero();
        // verify that all running actions are force cancelled
        assertThat(actionRepository.findByRolloutIdAndStatus(PAGE, deletedRollout.getId(), Status.CANCELED)
                .getNumberOfElements()).isEqualTo(2);
    }

    /**
     * Verifies that returned result considers provided sort parameter.
     */
    @Test
    void findAllRolloutsConsidersSorting() {
        final String randomString = randomString(5);
        final DistributionSet testDs = testdataFactory.createDistributionSet(randomString + "-testDs");
        testdataFactory.createTargets(10, randomString + "-testTarget-");
        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder().withDefaults().build();

        final String prefixRolloutRunning = randomString + "1";
        final Create rolloutRunningCreate = Create.builder()
                .name(prefixRolloutRunning + "-testRollout").targetFilterQuery("name==" + randomString + "*").distributionSet(testDs)
                .build();

        Rollout rolloutRunning = rolloutManagement.create(rolloutRunningCreate, 1, false, conditions);
        // Let the executor handle created Rollout
        rolloutHandler.handleAll();
        // start the rollout, so it has active running actions and a group which
        // has been started
        rolloutManagement.start(rolloutRunning.getId());
        rolloutHandler.handleAll();
        rolloutRunning = reloadRollout(rolloutRunning);

        final String prefixRolloutReady = randomString + "2";
        final Create rolloutReadyCreate = Create.builder()
                .name(prefixRolloutReady + "-testRollout").targetFilterQuery("name==" + randomString + "*").distributionSet(testDs).build();
        Rollout rolloutReady = rolloutManagement.create(rolloutReadyCreate, 1, false, conditions);
        // Let the executor handle created Rollout
        rolloutHandler.handleAll();
        rolloutReady = reloadRollout(rolloutReady);

        final List<Rollout> rolloutsOrderedByStatus = rolloutManagement
                .findAll(false, PageRequest.of(0, 500, Sort.by(Direction.ASC, "status"))).getContent();
        assertThat(rolloutsOrderedByStatus).containsSubsequence(List.of(rolloutReady, rolloutRunning));

        final List<Rollout> rolloutsOrderedByName = rolloutManagement
                .findAll(false, PageRequest.of(0, 500, Sort.by(Direction.ASC, "name"))).getContent();
        assertThat(rolloutsOrderedByName).containsSubsequence(List.of(rolloutRunning, rolloutReady));
    }

    /**
     * Creating a rollout without weight value when multi assignment in enabled.
     */
    @Test
    void weightNotRequiredInMultiAssignmentMode() {
        enableMultiAssignments();
        final Rollout rollout = testdataFactory.createSimpleTestRolloutWithTargetsAndDistributionSet(10, 10, 2, "50",
                "80",
                ActionType.FORCED, null);
        assertThat(rollout).isNotNull();
    }

    /**
     * Creating a rollout with a weight causes an error when multi assignment in disabled.
     */
    @Test
    void weightAllowedWhenMultiAssignmentModeNotEnabled() {
        assertThat(
                testdataFactory.createSimpleTestRolloutWithTargetsAndDistributionSet(
                        10, 10, 2, "50", "80", ActionType.FORCED, 66))
                .isNotNull();
    }

    /**
     * Weight is validated and saved to the Rollout.
     */
    @Test
    void weightValidatedAndSaved() {
        final String targetPrefix = UUID.randomUUID().toString();
        testdataFactory.createTargets(4, targetPrefix);
        enableMultiAssignments();

        final String rolloutName = UUID.randomUUID().toString();
        final String targetPrefixName = UUID.randomUUID().toString();
        Assertions.assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> createTestRolloutWithTargetsAndDistributionSet(4, 2, "50", "80",
                        rolloutName, targetPrefixName, Action.WEIGHT_MAX + 1));
        final String rolloutName2 = UUID.randomUUID().toString();
        final String targetPrefixName2 = UUID.randomUUID().toString();
        Assertions.assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> createTestRolloutWithTargetsAndDistributionSet(4, 2, "50", "80",
                        rolloutName2, targetPrefixName2, Action.WEIGHT_MIN - 1));

        final Rollout createdRollout1 = createTestRolloutWithTargetsAndDistributionSet(4, 2, "50", "80",
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), Action.WEIGHT_MAX);
        final Rollout createdRollout2 = createTestRolloutWithTargetsAndDistributionSet(4, 2, "50", "80",
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), Action.WEIGHT_MIN);
        assertThat(rolloutRepository.findById(createdRollout1.getId()).get().getWeight()).get()
                .isEqualTo(Action.WEIGHT_MAX);
        assertThat(rolloutRepository.findById(createdRollout2.getId()).get().getWeight()).get()
                .isEqualTo(Action.WEIGHT_MIN);
    }

    /**
     * A Rollout with weight creates actions with weights
     */
    @Test
    void actionsWithWeightAreCreated() {
        final int amountOfTargets = 5;
        final int weight = 99;
        enableMultiAssignments();
        final Long rolloutId = testdataFactory
                .createSimpleTestRolloutWithTargetsAndDistributionSet(amountOfTargets, 2, amountOfTargets,
                        "80", "50", null, weight).getId();
        rolloutManagement.start(rolloutId);
        rolloutHandler.handleAll();
        final List<Action> actions = deploymentManagement.findActionsAll(PAGE).getContent();
        assertThat(actions) //
                .hasSize(amountOfTargets) //
                .allMatch(action -> action.getWeight().get() == weight);
    }

    /**
     * Rollout can be created without weight in single assignment and be started in multi assignment
     */
    @Test
    void createInSingleStartInMultiassignMode() {
        final int amountOfTargets = 5;
        final Long rolloutId = testdataFactory.createSimpleTestRolloutWithTargetsAndDistributionSet(amountOfTargets, 2,
                amountOfTargets,
                "80", "50", null, null).getId();

        enableMultiAssignments();
        rolloutManagement.start(rolloutId);
        rolloutHandler.handleAll();
        final List<Action> actions = deploymentManagement.findActionsAll(PAGE).getContent();
        // wight replaced with default
        assertThat(actions).hasSize(5).allMatch(action -> action.getWeight().isPresent());
    }

    /**
     * Verifies that an exception is thrown when trying to create a rollout with an invalidated distribution set.
     */
    @Test
    void createRolloutWithInvalidDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createAndInvalidateDistributionSet();

        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception")
                .isThrownBy(() -> testdataFactory.createRolloutByVariables("createRolloutWithInvalidDistributionSet",
                        "desc", 2, "name==*", distributionSet, "50", "80"));
    }

    /**
     * Verifies that an exception is thrown when trying to create a rollout with an incomplete distribution set.
     */
    @Test
    void createRolloutWithIncompleteDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createIncompleteDistributionSet();

        assertThatExceptionOfType(IncompleteDistributionSetException.class)
                .as("Incomplete distributionSet should throw an exception")
                .isThrownBy(() -> testdataFactory.createRolloutByVariables("createRolloutWithIncompleteDistributionSet",
                        "desc", 2, "name==*", distributionSet, "50", "80"));
    }

    /**
     * Verify the that only compatible targets are part of a Rollout.
     */
    @Test
    void createAndStartRolloutWithTargetTypes() {
        final String rolloutName = "rolloutTestCompatibility";

        final DistributionSet testDs = testdataFactory.createDistributionSet("test-ds");
        final TargetType incompatibleTargetType = testdataFactory.createTargetType("incompatible-type", Set.of());
        final TargetType compatibleTargetType = testdataFactory.createTargetType("compatible-type", Set.of(testDs.getType()));

        final List<Target> incompatibleTargets = testdataFactory.createTargetsWithType(10, "incompatible", incompatibleTargetType);
        final List<Target> targetsWithoutType = testdataFactory.createTargets(10, "testTarget-");
        final List<Target> targets = testdataFactory.createTargetsWithType(10, "compatibleTarget-", compatibleTargetType);
        targets.addAll(targetsWithoutType);

        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder().withDefaults().build();
        final Create rolloutToCreate = Create.builder().name(rolloutName).targetFilterQuery("name==*").distributionSet(testDs).build();

        final Rollout createdRollout = rolloutManagement.create(rolloutToCreate, 1, false, conditions);

        // Let the executor handle created Rollout
        rolloutHandler.handleAll();

        final Rollout testRollout = reloadRollout(createdRollout);
        final List<RolloutGroup> rolloutGroups = rolloutGroupManagement
                .findByRollout(testRollout.getId(), Pageable.unpaged()).getContent();

        assertThat(testRollout.getStatus()).isEqualTo(RolloutStatus.READY);
        assertThat(testRollout.getTotalTargets()).isEqualTo(targets.size());
        assertThat(rolloutGroups).hasSize(1);
        assertThat(rolloutGroups.get(0).getTotalTargets()).isEqualTo(targets.size());

        final List<Target> rolloutGroupTargets = rolloutGroupManagement
                .findTargetsOfRolloutGroup(rolloutGroups.get(0).getId(), Pageable.unpaged()).getContent();

        assertThat(rolloutGroupTargets).hasSize(targets.size()).containsExactlyInAnyOrderElementsOf(targets)
                .doesNotContainAnyElementsOf(incompatibleTargets);
    }

    /**
     * Verifying that next group is started on manual trigger next group.
     */
    @Test
    void checkRunningRolloutsManualTriggerNextGroup() {
        final int amountTargetsForRollout = 15;
        final int amountOtherTargets = 0;
        final int amountGroups = 3;
        final String successCondition = "100";
        final String errorCondition = "80";

        final Rollout createdRollout = testdataFactory.createAndStartRollout(amountTargetsForRollout, amountOtherTargets, amountGroups,
                successCondition, errorCondition);

        // triggers next group
        rolloutManagement.triggerNextGroup(createdRollout.getId());

        // second group should in running state
        List<RolloutGroup> rolloutGroups = rolloutGroupManagement
                .findByRollout(createdRollout.getId(), new OffsetBasedPageRequest(0, 10, Sort.by(Direction.ASC, "id")))
                .getContent();
        assertThat(rolloutGroups.get(0).getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);
        assertThat(rolloutGroups.get(1).getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);
        assertThat(rolloutGroups.get(2).getStatus()).isEqualTo(RolloutGroupStatus.SCHEDULED);

        // triggers next group
        rolloutManagement.triggerNextGroup(createdRollout.getId());

        // third group should be in running state
        rolloutGroups = rolloutGroupManagement
                .findByRollout(createdRollout.getId(), new OffsetBasedPageRequest(0, 10, Sort.by(Direction.ASC, "id")))
                .getContent();
        assertThat(rolloutGroups.get(0).getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);
        assertThat(rolloutGroups.get(1).getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);
        assertThat(rolloutGroups.get(2).getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);

        // finish action of all groups and verify rollout
        final Slice<JpaAction> runningActionsSlice = actionRepository.findByRolloutIdAndStatus(PAGE,
                createdRollout.getId(), Status.RUNNING);
        runningActionsSlice.getContent().forEach(this::finishAction);

        verifyRolloutAndAllGroupsAreFinished(createdRollout);
    }

    /**
     * Tests the rollout status mapping.
     */
    @Test
    void testRolloutStatusConvert() {
        final long id = testdataFactory.createAndStartRollout(1, 0, 1, "100", "80").getId();
        for (final RolloutStatus status : RolloutStatus.values()) {
            final JpaRollout rollout = (JpaRollout) rolloutManagement.get(id);
            rollout.setStatus(status);
            rolloutRepository.save(rollout);
            assertThat(rolloutManagement.get(id).getStatus()).isEqualTo(status);
        }
    }

    /**
     * Tests the rollout action type mapping.
     */
    @Test
    void testActionTypeConvert() {
        final long id = testdataFactory.createAndStartRollout(1, 0, 1, "100", "80").getId();
        for (final ActionType actionType : ActionType.values()) {
            final JpaRollout rollout = ((JpaRollout) rolloutManagement.get(id));
            rollout.setActionType(actionType);
            rolloutRepository.save(rollout);
            assertThat(rolloutManagement.get(id).getActionType()).isEqualTo(actionType);
        }
    }

    /**
     * Trigger next rollout group if rollout is in wrong state
     */
    @Test
    void triggeringNextGroupRolloutWrongState() {

        final int amountTargetsForRollout = 15;
        final int amountOtherTargets = 0;
        final int amountGroups = 3;
        final String successCondition = "100";
        final String errorCondition = "80";

        final String errorMessage = "Rollout is not in running state";

        final Rollout createdRollout = testdataFactory.createSimpleTestRolloutWithTargetsAndDistributionSet(amountTargetsForRollout,
                amountOtherTargets, amountGroups, successCondition, errorCondition);

        // check CREATING state
        final Long createdRolloutId = createdRollout.getId();
        assertThatExceptionOfType(RolloutIllegalStateException.class)
                .isThrownBy(() -> rolloutManagement.triggerNextGroup(createdRolloutId))
                .withMessageContaining(errorMessage);

        rolloutManagement.start(createdRolloutId);
        // check STARTING state
        assertThatExceptionOfType(RolloutIllegalStateException.class)
                .isThrownBy(() -> rolloutManagement.triggerNextGroup(createdRolloutId))
                .withMessageContaining(errorMessage);

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();
        final Rollout rollout = reloadRollout(createdRollout);

        rolloutManagement.pauseRollout(rollout.getId());

        // check STOPPED state
        assertThatExceptionOfType(RolloutIllegalStateException.class)
                .isThrownBy(() -> rolloutManagement.triggerNextGroup(createdRolloutId))
                .withMessageContaining(errorMessage);

        final Slice<JpaAction> runningActionsSlice = actionRepository.findByRolloutIdAndStatus(PAGE,
                createdRolloutId, Status.RUNNING);
        runningActionsSlice.getContent().forEach(this::finishAction);

        // check FINISHED state
        assertThatExceptionOfType(RolloutIllegalStateException.class)
                .isThrownBy(() -> rolloutManagement.triggerNextGroup(createdRolloutId))
                .withMessageContaining(errorMessage);
    }

    private static Stream<Arguments> simpleRolloutsPossibilities() {
        return Stream.of(Arguments.of(true, true, Status.WAIT_FOR_CONFIRMATION), //
                Arguments.of(true, false, Status.RUNNING), //
                Arguments.of(false, true, Status.RUNNING), //
                Arguments.of(false, false, Status.RUNNING));//
    }

    private static Map<TotalTargetCountStatus.Status, Long> createInitStatusMap() {
        final Map<TotalTargetCountStatus.Status, Long> map = new EnumMap<>(TotalTargetCountStatus.Status.class);
        for (final TotalTargetCountStatus.Status status : TotalTargetCountStatus.Status.values()) {
            map.put(status, 0L);
        }
        return map;
    }

    private void finishActionAndDeleteTargetsOfFirstRunningGroup(final Rollout createdRollout) {
        // finish group one by finishing targets and deleting targets
        final Slice<JpaAction> runningActionsSlice = actionRepository.findByRolloutIdAndStatus(PAGE,
                createdRollout.getId(), Status.RUNNING);
        final List<JpaAction> runningActions = runningActionsSlice.getContent();
        finishAction(runningActions.get(0));
        finishAction(runningActions.get(1));
        finishAction(runningActions.get(2));
        targetManagement.delete(
                Arrays.asList(runningActions.get(3).getTarget().getId(), runningActions.get(4).getTarget().getId()));
    }

    private void checkSecondGroupStatusIsRunning(final Rollout createdRollout) {
        rolloutHandler.handleAll();
        final List<RolloutGroup> runningRolloutGroups = rolloutGroupManagement
                .findByRollout(createdRollout.getId(), new OffsetBasedPageRequest(0, 10, Sort.by(Direction.ASC, "id")))
                .getContent();
        assertThat(runningRolloutGroups.get(0).getStatus()).isEqualTo(RolloutGroupStatus.FINISHED);
        assertThat(runningRolloutGroups.get(1).getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);
        assertThat(runningRolloutGroups.get(2).getStatus()).isEqualTo(RolloutGroupStatus.SCHEDULED);
    }

    private void finishActionAndDeleteTargetsOfSecondRunningGroup(final Rollout createdRollout) {
        final Slice<JpaAction> runningActionsSlice = actionRepository.findByRolloutIdAndStatus(PAGE,
                createdRollout.getId(), Status.RUNNING);
        final List<JpaAction> runningActions = runningActionsSlice.getContent();
        finishAction(runningActions.get(0));
        targetManagement.delete(
                Arrays.asList(runningActions.get(1).getTarget().getId(), runningActions.get(2).getTarget().getId(),
                        runningActions.get(3).getTarget().getId(), runningActions.get(4).getTarget().getId()));

    }

    private void deleteAllTargetsFromThirdGroup(final Rollout createdRollout) {
        final Slice<JpaAction> runningActionsSlice = actionRepository.findByRolloutIdAndStatus(PAGE,
                createdRollout.getId(), Status.SCHEDULED);
        final List<JpaAction> runningActions = runningActionsSlice.getContent();
        targetManagement.delete(Arrays.asList(runningActions.get(0).getTarget().getId(),
                runningActions.get(1).getTarget().getId(), runningActions.get(2).getTarget().getId(),
                runningActions.get(3).getTarget().getId(), runningActions.get(4).getTarget().getId()));
    }

    private void verifyRolloutAndAllGroupsAreFinished(final Rollout createdRollout) {
        rolloutHandler.handleAll();
        final List<RolloutGroup> runningRolloutGroups = rolloutGroupManagement
                .findByRollout(createdRollout.getId(), PAGE).getContent();
        assertThat(runningRolloutGroups.get(0).getStatus()).isEqualTo(RolloutGroupStatus.FINISHED);
        assertThat(runningRolloutGroups.get(1).getStatus()).isEqualTo(RolloutGroupStatus.FINISHED);
        assertThat(runningRolloutGroups.get(2).getStatus()).isEqualTo(RolloutGroupStatus.FINISHED);
        assertThat(reloadRollout(createdRollout).getStatus()).isEqualTo(RolloutStatus.FINISHED);

    }

    private Rollout reloadRollout(final Rollout r) {
        return rolloutManagement.get(r.getId());
    }

    private void assertRolloutGroup(final long rolloutGroupId, final RolloutGroupStatus status,
            final boolean isConfirmationRequired, final long totalTargets, final Status actionStatusToCheck) {
        assertThat(rolloutGroupManagement.get(rolloutGroupId)).satisfies(rolloutGroup -> {
            assertThat(rolloutGroup.getStatus()).isEqualTo(status);
            assertThat(rolloutGroup.isConfirmationRequired()).isEqualTo(isConfirmationRequired);
            assertThat(rolloutGroup.getTotalTargets()).isEqualTo(totalTargets);
            if (actionStatusToCheck != null) {
                assertAllActionOfRolloutGroupHavingStatus(rolloutGroup.getId(), actionStatusToCheck);
            }
        });
    }

    private void assertAllActionOfRolloutGroupHavingStatus(final long rolloutGroupId, final Status status) {
        final List<Target> targets = rolloutGroupManagement.findTargetsOfRolloutGroup(rolloutGroupId, PAGE)
                .getContent();
        targets.forEach(target -> {
            final List<Action> activeActions = deploymentManagement
                    .findActionsByTarget(target.getControllerId(), PAGE).getContent();
            assertThat(activeActions).hasSize(1);
            assertThat(activeActions.get(0).getStatus()).isEqualTo(status);
        });
    }

    private void forceQuitAllActionsOfRolloutGroup(final long rolloutGroupId) {
        final List<Target> targets = rolloutGroupManagement.findTargetsOfRolloutGroup(rolloutGroupId, PAGE)
                .getContent();
        targets.forEach(target -> {
            deploymentManagement.findActiveActionsByTarget(target.getControllerId(), PAGE).getContent().stream().map(Identifiable::getId)
                    .forEach(actionId -> {
                        deploymentManagement.cancelAction(actionId);
                        deploymentManagement.forceQuitAction(actionId);
                    });
        });
    }

    private GroupCreate generateRolloutGroup(final int index, final Integer percentage, final String targetFilter) {
        return generateRolloutGroup(index, percentage, targetFilter, false);
    }

    private GroupCreate generateRolloutGroup(final int index, final Integer percentage,
            final String targetFilter, final boolean confirmationRequired) {
        return GroupCreate.builder().name("Group" + index).description("Group" + index + "desc")
                .targetPercentage(Float.valueOf(percentage)).targetFilterQuery(targetFilter)
                .confirmationRequired(confirmationRequired).build();
    }

    private Create generateTargetsAndRollout(final String rolloutName, final int amountTargetsForRollout) {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsFor" + rolloutName);

        testdataFactory.createTargets(amountTargetsForRollout, rolloutName + "-", rolloutName);

        return Create.builder()
                .name(rolloutName).description("This is a test description for the rollout")
                .targetFilterQuery("controllerId==" + rolloutName + "-*").distributionSet(distributionSet)
                .build();
    }

    private void validateRolloutGroupActionStatus(
            final RolloutGroup rolloutGroup, final Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus) {
        validateStatus(
                rolloutGroupManagement.getWithDetailedStatus(rolloutGroup.getId()).getTotalTargetCountStatus(), expectedTargetCountStatus);
    }

    private void validateRolloutActionStatus(final Long rolloutId, final Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus) {
        validateStatus(rolloutManagement.getWithDetailedStatus(rolloutId).getTotalTargetCountStatus(), expectedTargetCountStatus);
    }

    private void validateStatus(final TotalTargetCountStatus totalTargetCountStatus,
            final Map<TotalTargetCountStatus.Status, Long> expectedTotalCountStates) {
        for (final Map.Entry<TotalTargetCountStatus.Status, Long> entry : expectedTotalCountStates.entrySet()) {
            final Long countReady = totalTargetCountStatus.getTotalTargetCountByStatus(entry.getKey());
            assertThat(countReady).as("targets in status " + entry.getKey()).isEqualTo(entry.getValue());
        }
    }

    private Rollout createTestRolloutWithTargetsAndDistributionSet(final int amountTargetsForRollout,
            final int groupSize, final String successCondition, final String errorCondition, final String rolloutName,
            final String targetPrefixName) {
        return createTestRolloutWithTargetsAndDistributionSet(amountTargetsForRollout, groupSize, successCondition,
                errorCondition, rolloutName, targetPrefixName, null);
    }

    private Rollout createTestRolloutWithTargetsAndDistributionSet(final int amountTargetsForRollout,
            final int groupSize, final String successCondition, final String errorCondition, final String rolloutName,
            final String targetPrefixName, final Integer weight) {
        final DistributionSet dsForRolloutTwo = testdataFactory.createDistributionSet("dsFor" + rolloutName);
        testdataFactory.createTargets(amountTargetsForRollout, targetPrefixName + "-", targetPrefixName);
        return testdataFactory.createRolloutByVariables(rolloutName, rolloutName + "description", groupSize,
                "controllerId==" + targetPrefixName + "-*", dsForRolloutTwo, successCondition, errorCondition,
                Action.ActionType.FORCED, weight, false);
    }

    private int changeStatusForAllRunningActions(final Rollout rollout, final Status status) {
        final List<Action> runningActions = findActionsByRolloutAndStatus(rollout, Status.RUNNING);
        for (final Action action : runningActions) {
            controllerManagement
                    .addUpdateActionStatus(ActionStatusCreate.builder().actionId(action.getId()).status(status).build());
        }
        return runningActions.size();
    }

    private int changeStatusForRunningActions(final Rollout rollout, final Status status,
            final int amountOfTargetsToGetChanged) {
        final List<Action> runningActions = findActionsByRolloutAndStatus(rollout, Status.RUNNING);
        assertThat(runningActions).hasSizeGreaterThanOrEqualTo(amountOfTargetsToGetChanged);
        for (int i = 0; i < amountOfTargetsToGetChanged; i++) {
            controllerManagement.addUpdateActionStatus(
                    ActionStatusCreate.builder().actionId(runningActions.get(i).getId()).status(status).build());
        }
        return runningActions.size();
    }
}