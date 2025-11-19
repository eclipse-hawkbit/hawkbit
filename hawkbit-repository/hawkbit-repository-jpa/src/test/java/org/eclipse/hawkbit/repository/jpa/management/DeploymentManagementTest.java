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
import static org.eclipse.hawkbit.repository.model.Action.Status.RUNNING;
import static org.eclipse.hawkbit.repository.model.Action.Status.WAIT_FOR_CONFIRMATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.validation.ConstraintViolationException;

import lombok.Getter;
import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.repository.qfields.ActionStatusFields;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.event.remote.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.MultiActionAssignEvent;
import org.eclipse.hawkbit.repository.event.remote.MultiActionCancelEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TenantConfigurationCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TenantConfigurationUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.ForceQuitActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.IncompatibleTargetTypeException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InvalidDistributionSetException;
import org.eclipse.hawkbit.repository.exception.MultiAssignmentIsNotEnabledException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.specifications.DistributionSetSpecification;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionStatusCreate;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort.Direction;

/**
 * Test class testing the functionality of triggering a deployment of {@link DistributionSet}s to {@link Target}s.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Deployment Management
 */
class DeploymentManagementTest extends AbstractJpaIntegrationTest {

    private static final boolean STATE_ACTIVE = true;
    private static final boolean STATE_INACTIVE = false;

    /**
     * Tests that an exception is thrown when a target is assigned to an incomplete distribution set
     */
    @Test
    void verifyAssignTargetsToIncompleteDistribution() {
        final DistributionSet distributionSet = testdataFactory.createIncompleteDistributionSet();
        final Target target = testdataFactory.createTarget();

        assertThatExceptionOfType(IncompleteDistributionSetException.class)
                .as("Incomplete distributionSet should throw an exception")
                .isThrownBy(() -> assignDistributionSet(distributionSet, target));
    }

    /**
     * Tests that an exception is thrown when a target is assigned to an invalidated distribution set
     */
    @Test
    void verifyAssignTargetsToInvalidDistribution() {
        final DistributionSet distributionSet = testdataFactory.createAndInvalidateDistributionSet();
        final Target target = testdataFactory.createTarget();

        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception")
                .isThrownBy(() -> assignDistributionSet(distributionSet, target));
    }

    /**
     * Verifies that management get access react as specified on calls for non existing entities by means
     * of Optional not present.
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class) })
    void nonExistingEntityAccessReturnsNotPresent() {
        assertThat(deploymentManagement.findAction(1234L)).isNotPresent();
        assertThat(deploymentManagement.findActionWithDetails(NOT_EXIST_IDL)).isNotPresent();
    }

    /**
     * Verifies that management queries react as specified on calls for non existing entities
     * by means of throwing EntityNotFoundException.
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1) })
    void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        final Target target = testdataFactory.createTarget();
        final String dsName = "DistributionSet";

        verifyThrownExceptionBy(() -> assignDistributionSet(NOT_EXIST_IDL, target.getControllerId()), dsName);

        verifyThrownExceptionBy(() -> deploymentManagement.cancelAction(NOT_EXIST_IDL), "Action");
        verifyThrownExceptionBy(() -> deploymentManagement.countActionsByTarget(NOT_EXIST_ID), "Target");
        verifyThrownExceptionBy(() -> deploymentManagement.countActionsByTarget("xxx", NOT_EXIST_ID), "Target");

        verifyThrownExceptionBy(() -> findActionsByDistributionSet(PAGE, NOT_EXIST_IDL), "DistributionSet");
        verifyThrownExceptionBy(() -> deploymentManagement.findActionsByTarget(NOT_EXIST_ID, PAGE), "Target");
        verifyThrownExceptionBy(() -> deploymentManagement.findActionsByTarget("id==*", NOT_EXIST_ID, PAGE), "Target");

        verifyThrownExceptionBy(() -> deploymentManagement.findActiveActionsByTarget(NOT_EXIST_ID, PAGE), "Target");
        verifyThrownExceptionBy(() -> deploymentManagement.forceQuitAction(NOT_EXIST_IDL), "Action");
        verifyThrownExceptionBy(() -> deploymentManagement.forceTargetAction(NOT_EXIST_IDL), "Action");
    }

    /**
     * Test verifies that the repository retrieves the action including all defined (lazy) details.
     */
    @Test
    void findActionWithLazyDetails() {
        final DistributionSet testDs = testdataFactory.createDistributionSet("TestDs", "1.0", List.of());
        final List<Target> testTarget = testdataFactory.createTargets(1);
        // one action with one action status is generated
        final Long actionId = getFirstAssignedActionId(assignDistributionSet(testDs, testTarget));
        final Action action = deploymentManagement.findActionWithDetails(actionId).orElseThrow();

        assertThat(action.getDistributionSet()).as("DistributionSet in action").isNotNull();
        assertThat(action.getTarget()).as("Target in action").isNotNull();
        assertThat(deploymentManagement.findAssignedDistributionSet(action.getTarget().getControllerId()).orElseThrow())
                .as("AssignedDistributionSet of target in action")
                .isNotNull();
    }

    /**
     * Test verifies that actions of a target are found by using id-based search.
     */
    @Test
    void findActionByTargetId() {
        final DistributionSet testDs = testdataFactory.createDistributionSet("TestDs", "1.0", List.of());
        final List<Target> testTarget = testdataFactory.createTargets(1);
        // one action with one action status is generated
        final Long actionId = getFirstAssignedActionId(assignDistributionSet(testDs, testTarget));

        // act
        final Slice<Action> actions = deploymentManagement.findActionsByTarget(testTarget.get(0).getControllerId(), PAGE);
        final Long count = deploymentManagement.countActionsByTarget(testTarget.get(0).getControllerId());

        assertThat(count).as("One Action for target").isEqualTo(1L).isEqualTo(actions.getContent().size());
        assertThat(actions.getContent().get(0).getId()).as("Action of target").isEqualTo(actionId);
    }

    /**
     * Test verifies that the 'max actions per target' quota is enforced.
     */
    @Test
    void assertMaxActionsPerTargetQuotaIsEnforced() {
        enableMultiAssignments();

        final int maxActions = quotaManagement.getMaxActionsPerTarget();
        final Target testTarget = testdataFactory.createTarget();
        final Long ds1Id = testdataFactory.createDistributionSet("ds1").getId();

        final String controllerId = testTarget.getControllerId();
        for (int i = 0; i < maxActions; i++) {
            deploymentManagement.offlineAssignedDistributionSets(List.of(new SimpleEntry<>(controllerId, ds1Id)));
        }

        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> assignDistributionSet(ds1Id, controllerId, 77));
    }

    /**
     * An assignment request with more assignments than allowed by 'maxTargetDistributionSetAssignmentsPerManualAssignment' quota throws an exception.
     */
    @Test
    void assignmentRequestThatIsTooLarge() {
        final int maxActions = quotaManagement.getMaxTargetDistributionSetAssignmentsPerManualAssignment();
        final DistributionSet ds1 = testdataFactory.createDistributionSet("1");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2");

        final List<Target> targets = testdataFactory.createTargets(maxActions, "assignmentTest1");
        assignDistributionSet(ds1, targets);

        targets.add(testdataFactory.createTarget("assignmentTest2"));
        assertThatExceptionOfType(AssignmentQuotaExceededException.class).isThrownBy(() -> assignDistributionSet(ds2, targets));
    }

    /**
     * Test verifies that action-states of an action are found by using id-based search.
     */
    @Test
    void findActionStatusByActionId() {
        final DistributionSet testDs = testdataFactory.createDistributionSet("TestDs", "1.0", Collections.emptyList());
        final List<Target> testTarget = testdataFactory.createTargets(1);
        // one action with one action status is generated
        final Long actionId = getFirstAssignedActionId(assignDistributionSet(testDs, testTarget));
        final Slice<Action> actions = deploymentManagement.findActionsByTarget(testTarget.get(0).getControllerId(), PAGE);
        final ActionStatus expectedActionStatus = ((JpaAction) actions.getContent().get(0)).getActionStatus().get(0);

        // act
        final Page<ActionStatus> actionStates = deploymentManagement.findActionStatusByAction(actionId, PAGE);
        assertThat(actionStates.getContent()).hasSize(1);
        assertThat(actionStates.getContent().get(0)).as("Action-status of action").isEqualTo(expectedActionStatus);
    }

    /**
     * Test verifies that messages of an action-status are found by using id-based search.
     */
    @Test
    void findMessagesByActionStatusId() {
        final DistributionSet testDs = testdataFactory.createDistributionSet("TestDs", "1.0", List.of());
        final List<Target> testTarget = testdataFactory.createTargets(1);
        // one action with one action status is generated
        final Long actionId = getFirstAssignedActionId(assignDistributionSet(testDs, testTarget));
        // create action-status entry with one message
        controllerManagement.addUpdateActionStatus(ActionStatusCreate.builder()
                .actionId(actionId).status(Action.Status.FINISHED).messages(List.of("finished message")).build());
        final Page<ActionStatus> actionStates = deploymentManagement.findActionStatusByAction(actionId, PAGE);

        // find newly created action-status entry with message
        final JpaActionStatus actionStatusWithMessage = actionStates.getContent().stream()
                .map(JpaActionStatus.class::cast)
                .filter(entry -> entry.getMessages() != null && !entry.getMessages().isEmpty())
                .findFirst().orElseThrow();
        final String expectedMsg = actionStatusWithMessage.getMessages().get(0);

        // act
        final Page<String> messages = deploymentManagement.findMessagesByActionStatusId(actionStatusWithMessage.getId(), PAGE);
        assertThat(actionStates.getTotalElements()).as("Two action-states in total").isEqualTo(2L);
        assertThat(messages.getContent().get(0)).as("Message of action-status").isEqualTo(expectedMsg);
    }

    /**
     * Ensures that tag to distribution set assignment that does not exist will cause EntityNotFoundException.
     */
    @Test
    void assignDistributionSetToTagThatDoesNotExistThrowsException() {
        final List<Long> assignDS = new ArrayList<>(5);
        for (int i = 0; i < 4; i++) {
            assignDS.add(testdataFactory.createDistributionSet("DS" + i, "1.0", Collections.emptyList()).getId());
        }
        // not exists
        assignDS.add(100_000L);

        final Long tagId = distributionSetTagManagement.create(DistributionSetTagManagement.Create.builder().name("Tag1").build()).getId();
        assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> distributionSetManagement.assignTag(assignDS, tagId))
                .withMessageContaining("DistributionSet")
                .withMessageContaining(String.valueOf(100L));
    }

    /**
     * Test verifies that an assignment with automatic cancelation works correctly even if the update is split into multiple partitions on the database.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 20),
            @Expect(type = TargetUpdatedEvent.class, count = 40),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 2),
            @Expect(type = ActionCreatedEvent.class, count = 40),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 20),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 2), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 6) })
    // implicit lock
    void multiAssigmentHistoryOverMultiplePagesResultsInTwoActiveAction() {
        final DistributionSet cancelDs = testdataFactory.createDistributionSet("Canceled DS", "1.0", Collections.emptyList());
        final DistributionSet cancelDs2 = testdataFactory.createDistributionSet("Canceled DS", "1.2", Collections.emptyList());
        final List<Target> targets = testdataFactory.createTargets(quotaManagement.getMaxTargetsPerAutoAssignment());

        assertThat(deploymentManagement.countActionsAll()).isZero();

        assignDistributionSet(cancelDs, targets).getAssignedEntity();
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(quotaManagement.getMaxTargetsPerAutoAssignment());
        assignDistributionSet(cancelDs2, targets).getAssignedEntity();
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(2L * quotaManagement.getMaxTargetsPerAutoAssignment());
    }

    /**
     * Cancels multiple active actions on a target. Expected behaviour is that with two active
     * After canceling the first one also the target goes back to IN_SYNC as no open action is left.
     */
    @Test
    void manualCancelWithMultipleAssignmentsCancelLastOneFirst() {
        final Action action = prepareFinishedUpdate("4712", "installed", true);
        final Target target = action.getTarget();
        final DistributionSet dsFirst = testdataFactory.createDistributionSet("", true);
        final DistributionSet dsSecond = testdataFactory.createDistributionSet("2", true);
        final DistributionSet dsInstalled = action.getDistributionSet();

        // check initial status
        assertThat(targetManagement.getByControllerId("4712").getUpdateStatus())
                .as("target has update status")
                .isEqualTo(TargetUpdateStatus.IN_SYNC);

        // assign the two sets in a row
        JpaAction firstAction = assignSet(target, dsFirst);
        JpaAction secondAction = assignSet(target, dsSecond);

        assertThat(actionRepository.findAll()).as("wrong size of actions").hasSize(3);
        assertThat(actionStatusRepository.findAll()).as("wrong size of action status").hasSize(5);

        // we cancel second -> back to first
        deploymentManagement.cancelAction(secondAction.getId());
        secondAction = (JpaAction) deploymentManagement.findActionWithDetails(secondAction.getId()).orElseThrow();
        // confirm cancellation
        controllerManagement.addCancelActionStatus(ActionStatusCreate.builder()
                .actionId(secondAction.getId()).status(Status.CANCELED).build());
        assertThat(actionStatusRepository.findAll()).as("wrong size of actions status").hasSize(7);
        assertThat(deploymentManagement.findAssignedDistributionSet("4712")).as("wrong ds").contains(dsFirst);
        assertThat(targetManagement.getByControllerId("4712").getUpdateStatus())
                .as("wrong update status")
                .isEqualTo(TargetUpdateStatus.PENDING);

        // we cancel first -> back to installed
        deploymentManagement.cancelAction(firstAction.getId());
        firstAction = (JpaAction) deploymentManagement.findActionWithDetails(firstAction.getId()).orElseThrow();
        // confirm cancellation
        controllerManagement.addCancelActionStatus(
                ActionStatusCreate.builder().actionId(firstAction.getId()).status(Status.CANCELED).build());
        assertThat(actionStatusRepository.findAll()).as("wrong size of action status").hasSize(9);
        assertThat(deploymentManagement.findAssignedDistributionSet("4712")).as("wrong assigned ds").contains(dsInstalled);
        assertThat(targetManagement.getByControllerId("4712").getUpdateStatus())
                .as("wrong update status")
                .isEqualTo(TargetUpdateStatus.IN_SYNC);
    }

    /**
     * Cancels multiple active actions on a target. Expected behaviour is that with two active
     * also the target goes back to IN_SYNC as no open action is left.
     */
    @Test
    void manualCancelWithMultipleAssignmentsCancelMiddleOneFirst() {
        final Action action = prepareFinishedUpdate("4712", "installed", true);
        final Target target = action.getTarget();
        final DistributionSet dsFirst = testdataFactory.createDistributionSet("", true);
        final DistributionSet dsSecond = testdataFactory.createDistributionSet("2", true);
        final DistributionSet dsInstalled = action.getDistributionSet();

        // check initial status
        assertThat(targetManagement.getByControllerId("4712").getUpdateStatus())
                .as("wrong update status")
                .isEqualTo(TargetUpdateStatus.IN_SYNC);

        // assign the two sets in a row
        JpaAction firstAction = assignSet(target, dsFirst);
        JpaAction secondAction = assignSet(target, dsSecond);

        assertThat(actionRepository.findAll()).as("wrong size of actions").hasSize(3);
        assertThat(actionStatusRepository.findAll()).as("wrong size of action status").hasSize(5);

        // we cancel first -> second is left
        deploymentManagement.cancelAction(firstAction.getId());
        // confirm cancellation
        firstAction = (JpaAction) deploymentManagement.findActionWithDetails(firstAction.getId()).orElseThrow();
        controllerManagement.addCancelActionStatus(ActionStatusCreate.builder().actionId(firstAction.getId()).status(Status.CANCELED).build());
        assertThat(actionStatusRepository.findAll()).as("wrong size of action status").hasSize(7);
        assertThat(deploymentManagement.findAssignedDistributionSet("4712")).as("wrong assigned ds").contains(dsSecond);
        assertThat(targetManagement.getByControllerId("4712").getUpdateStatus())
                .as("wrong target update status")
                .isEqualTo(TargetUpdateStatus.PENDING);

        // we cancel second -> remain assigned until finished cancellation
        deploymentManagement.cancelAction(secondAction.getId());
        secondAction = (JpaAction) deploymentManagement.findActionWithDetails(secondAction.getId()).get();
        assertThat(actionStatusRepository.findAll()).as("wrong size of action status").hasSize(8);
        assertThat(deploymentManagement.findAssignedDistributionSet("4712")).as("wrong assigned ds").contains(dsSecond);
        // confirm cancellation
        controllerManagement.addCancelActionStatus(ActionStatusCreate.builder().actionId(secondAction.getId()).status(Status.CANCELED).build());
        // cancelled success -> back to dsInstalled
        assertThat(deploymentManagement.findAssignedDistributionSet("4712"))
                .as("wrong installed ds")
                .contains(dsInstalled);
        assertThat(targetManagement.getByControllerId("4712").getUpdateStatus())
                .as("wrong target info update status")
                .isEqualTo(TargetUpdateStatus.IN_SYNC);
    }

    /**
     * Force Quit an Assignment. Expected behaviour is that the action is canceled and is marked as deleted. The assigned Software module
     */
    @Test
    void forceQuitSetActionToInactive() {
        final Action action = prepareFinishedUpdate("4712", "installed", true);
        final Target target = action.getTarget();
        final DistributionSet dsInstalled = action.getDistributionSet();

        final DistributionSet ds = testdataFactory.createDistributionSet("newDS", true);

        // verify initial status
        assertThat(targetManagement.getByControllerId("4712").getUpdateStatus())
                .as("wrong target info update status").isEqualTo(TargetUpdateStatus.IN_SYNC);

        Action assigningAction = assignSet(target, ds);

        // verify assignment
        assertThat(actionRepository.findAll()).as("wrong size of action").hasSize(2);
        assertThat(actionStatusRepository.findAll()).as("wrong size of action status").hasSize(4);

        // force quit assignment
        deploymentManagement.cancelAction(assigningAction.getId());
        assigningAction = deploymentManagement.findActionWithDetails(assigningAction.getId()).get();

        deploymentManagement.forceQuitAction(assigningAction.getId());

        assigningAction = deploymentManagement.findActionWithDetails(assigningAction.getId()).get();

        // verify
        assertThat(assigningAction.getStatus()).as("wrong size of status").isEqualTo(Status.CANCELED);
        assertThat(deploymentManagement.findAssignedDistributionSet("4712"))
                .as("wrong assigned ds").contains(dsInstalled);
        assertThat(targetManagement.getByControllerId("4712").getUpdateStatus())
                .as("wrong target update status").isEqualTo(TargetUpdateStatus.IN_SYNC);
    }

    /**
     * Force Quit an not canceled Assignment. Expected behaviour is that the action can not be force quit and there is thrown an exception.
     */
    @Test
    void forceQuitNotAllowedThrowsException() {
        final Action action = prepareFinishedUpdate("4712", "installed", true);
        // verify initial status
        assertThat(targetManagement.getByControllerId("4712").getUpdateStatus())
                .as("wrong update status").isEqualTo(TargetUpdateStatus.IN_SYNC);

        final Target target = action.getTarget();
        final DistributionSet ds = testdataFactory.createDistributionSet("newDS", true);
        final Long assigningActionId = assignSet(target, ds).getId();

        // verify assignment
        assertThat(actionRepository.findAll()).as("wrong size of action").hasSize(2);
        assertThat(actionStatusRepository.findAll()).as("wrong size of action status").hasSize(4);

        // force quit assignment
        assertThatExceptionOfType(ForceQuitActionNotAllowedException.class)
                .as("expected ForceQuitActionNotAllowedException")
                .isThrownBy(() -> deploymentManagement.forceQuitAction(assigningActionId));
    }

    /**
     * Simple offline deployment of a distribution set to a list of targets. Verifies that offline assigment
     * is correctly executed for targets that do not have a running update already. Those are ignored.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 20),
            @Expect(type = TargetUpdatedEvent.class, count = 20),
            @Expect(type = ActionCreatedEvent.class, count = 20),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 2), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 6), // implicit lock
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void assignedDistributionSet() {
        final List<Target> onlineAssignedTargets = testdataFactory.createTargets(10, "2");
        final List<String> controllerIds = Stream.concat(testdataFactory.createTargets(10).stream(), onlineAssignedTargets.stream())
                .map(Target::getControllerId).toList();

        final DistributionSet ds = testdataFactory.createDistributionSet();
        assignDistributionSet(testdataFactory.createDistributionSet("2"), onlineAssignedTargets);

        final long current = System.currentTimeMillis();

        final List<Entry<String, Long>> offlineAssignments = controllerIds.stream()
                .map(targetId -> (Entry<String, Long>) new SimpleEntry<>(targetId, ds.getId())).toList();
        final List<DistributionSetAssignmentResult> assignmentResults = deploymentManagement
                .offlineAssignedDistributionSets(offlineAssignments);
        assertThat(assignmentResults).hasSize(1);
        final List<Target> targets = assignmentResults.get(0).getAssignedEntity().stream().map(Action::getTarget).toList();

        assertThat(actionRepository.count()).isEqualTo(20);
        assertThat(findActionsByDistributionSet(PAGE, ds.getId())).as("Offline actions are not active")
                .allMatch(action -> !action.isActive()).as("Actions should be initiated by current user")
                .allMatch(a -> a.getInitiatedBy().equals(TenantAware.getCurrentUsername()));

        assertThat(targetManagement.findByInstalledDistributionSet(ds.getId(), PAGE).getContent())
                .usingElementComparator(controllerIdComparator()).containsAll(targets).hasSize(10)
                .containsAll(targetManagement.findByAssignedDistributionSet(ds.getId(), PAGE))
                .as("InstallationDate set").allMatch(target -> target.getInstallationDate() >= current)
                .as("TargetUpdateStatus IN_SYNC")
                .allMatch(target -> TargetUpdateStatus.IN_SYNC.equals(target.getUpdateStatus()))
                .as("InstallationDate equal to LastModifiedAt")
                .allMatch(target -> target.getLastModifiedAt() == target.getInstallationDate());
    }

    /**
     * Offline assign multiple DSs to a single Target in multiassignment mode.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 4),
            @Expect(type = ActionCreatedEvent.class, count = 4),
            @Expect(type = DistributionSetCreatedEvent.class, count = 4),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 12),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 4), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 12), // implicit lock
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1) })
    void multiOfflineAssignment() {
        final List<String> targetIds = testdataFactory.createTargets(1).stream().map(Target::getControllerId).toList();
        final List<Long> dsIds = testdataFactory.createDistributionSets(4).stream().map(DistributionSet::getId).toList();

        enableMultiAssignments();
        final List<Entry<String, Long>> offlineAssignments = new ArrayList<>();
        targetIds.forEach(targetId -> dsIds.forEach(dsId -> offlineAssignments.add(new SimpleEntry<>(targetId, dsId))));
        final List<DistributionSetAssignmentResult> assignmentResults = deploymentManagement
                .offlineAssignedDistributionSets(offlineAssignments);

        assertThat(getResultingActionCount(assignmentResults)).isEqualTo(4);
        targetIds.forEach(controllerId -> {
            final List<Long> assignedDsIds = deploymentManagement.findActionsByTarget(controllerId, PAGE).stream()
                    .map(a -> {
                        // don't use peek since it is by documentation mainly for debugging and could be skipped in some cases
                        assertThat(a.getInitiatedBy())
                                .as("Actions should be initiated by current user")
                                .isEqualTo(TenantAware.getCurrentUsername());
                        return a;
                    })
                    .map(action -> action.getDistributionSet().getId()).toList();
            assertThat(assignedDsIds).containsExactlyInAnyOrderElementsOf(dsIds);
        });
    }

    /**
     * Verifies that if an account is set to action autoclose running actions in case of a new assigned set get closed and set to CANCELED.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 10),
            @Expect(type = TargetUpdatedEvent.class, count = 20),
            @Expect(type = ActionCreatedEvent.class, count = 20),
            @Expect(type = ActionUpdatedEvent.class, count = 10),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 2), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 6), // implicit lock
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 2),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1),
            @Expect(type = TenantConfigurationUpdatedEvent.class, count = 1) })
    void assignDistributionSetAndAutoCloseActiveActions() {
        tenantConfigurationManagement().addOrUpdateConfiguration(TenantConfigurationKey.REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED, true);

        try {
            final List<Target> targets = testdataFactory.createTargets(10);

            // First assignment
            final DistributionSet ds1 = testdataFactory.createDistributionSet("1");
            assignDistributionSet(ds1, targets);

            assertDsExclusivelyAssignedToTargets(targets, ds1.getId(), STATE_ACTIVE, RUNNING);

            // Second assignment
            final DistributionSet ds2 = testdataFactory.createDistributionSet("2");
            assignDistributionSet(ds2, targets);

            assertDsExclusivelyAssignedToTargets(targets, ds2.getId(), STATE_ACTIVE, RUNNING);
            assertDsExclusivelyAssignedToTargets(targets, ds1.getId(), STATE_INACTIVE, Status.CANCELED);

            assertThat(targetManagement.findByAssignedDistributionSet(ds2.getId(), PAGE).getContent()).hasSize(10)
                    .as("InstallationDate not set").allMatch(target -> (target.getInstallationDate() == null));

        } finally {
            tenantConfigurationManagement().addOrUpdateConfiguration(TenantConfigurationKey.REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED, false);
        }
    }

    /**
     * If multi-assignment is enabled, verify that the previous Distribution Set assignment is not canceled when a new one is assigned.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 10),
            @Expect(type = TargetUpdatedEvent.class, count = 20),
            @Expect(type = ActionCreatedEvent.class, count = 20),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 2), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 6), // implicit lock
            @Expect(type = MultiActionAssignEvent.class, count = 2),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1) })
    void previousAssignmentsAreNotCanceledInMultiAssignMode() {
        enableMultiAssignments();
        final List<Target> targets = testdataFactory.createTargets(10);
        final List<String> targetIds = targets.stream().map(Target::getControllerId).toList();

        // First assignment
        final DistributionSet ds1 = testdataFactory.createDistributionSet("Multi-assign-1");
        assignDistributionSet(ds1.getId(), targetIds, 77);

        assertDsExclusivelyAssignedToTargets(targets, ds1.getId(), STATE_ACTIVE, RUNNING);

        // Second assignment
        final DistributionSet ds2 = testdataFactory.createDistributionSet("Multi-assign-2");
        assignDistributionSet(ds2.getId(), targetIds, 45);

        assertDsExclusivelyAssignedToTargets(targets, ds2.getId(), STATE_ACTIVE, RUNNING);
        assertDsExclusivelyAssignedToTargets(targets, ds1.getId(), STATE_ACTIVE, RUNNING);
    }

    /**
     * Assign multiple DSs to a single Target in one request in multiassignment mode.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 4),
            @Expect(type = ActionCreatedEvent.class, count = 4),
            @Expect(type = DistributionSetCreatedEvent.class, count = 4),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 12),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 4), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 12), // implicit lock
            @Expect(type = MultiActionAssignEvent.class, count = 1),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1) })
    void multiAssignmentInOneRequest() {
        final List<Target> targets = testdataFactory.createTargets(1);
        final List<DistributionSet> distributionSets = testdataFactory.createDistributionSets(4);
        final List<DeploymentRequest> deploymentRequests = createAssignmentRequests(distributionSets, targets, 34);

        enableMultiAssignments();
        final List<DistributionSetAssignmentResult> results = deploymentManagement.assignDistributionSets(deploymentRequests);

        assertThat(getResultingActionCount(results)).isEqualTo(deploymentRequests.size());
        final List<Long> dsIds = distributionSets.stream().map(DistributionSet::getId).toList();
        targets.forEach(target -> {
            final List<Long> assignedDsIds = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE)
                    .stream()
                    .map(a -> {
                        // don't use peek since it is by documentation mainly for debugging and could be skipped in some cases
                        assertThat(a.getInitiatedBy()).as("Initiated by current user")
                                .isEqualTo(TenantAware.getCurrentUsername());
                        return a;
                    })
                    .map(action -> action.getDistributionSet().getId()).toList();
            assertThat(assignedDsIds).containsExactlyInAnyOrderElementsOf(dsIds);
        });
    }

    /**
     * Assign multiple DSs to single Target in one request in multiAssignment mode and cancel each created action afterwards.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 4),
            @Expect(type = ActionCreatedEvent.class, count = 4),
            @Expect(type = DistributionSetCreatedEvent.class, count = 4),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 12),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 4), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 12), // implicit lock
            @Expect(type = MultiActionAssignEvent.class, count = 1),
            @Expect(type = MultiActionCancelEvent.class, count = 4),
            @Expect(type = ActionUpdatedEvent.class, count = 4),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1) })
    void cancelMultiAssignmentActions() {
        final List<Target> targets = testdataFactory.createTargets(1);
        final List<DistributionSet> distributionSets = testdataFactory.createDistributionSets(4);
        final List<DeploymentRequest> deploymentRequests = createAssignmentRequests(distributionSets, targets, 34, false);

        enableMultiAssignments();
        final List<DistributionSetAssignmentResult> results = deploymentManagement.assignDistributionSets(deploymentRequests);

        assertThat(getResultingActionCount(results)).isEqualTo(deploymentRequests.size());

        final List<Long> dsIds = distributionSets.stream().map(DistributionSet::getId).toList();
        targets.forEach(target ->
                deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).forEach(action -> {
                    assertThat(action.getDistributionSet().getId()).isIn(dsIds);
                    assertThat(action.getInitiatedBy())
                            .as("Should be Initiated by current user").isEqualTo(TenantAware.getCurrentUsername());
                    deploymentManagement.cancelAction(action.getId());
                }));
    }

    /**
     * A Request resulting in multiple assignments to a single target is only allowed when multiassignment is enabled.
     */
    @Test
    void multipleAssignmentsToTargetOnlyAllowedInMultiAssignMode() {
        final Target target = testdataFactory.createTarget();
        final List<DistributionSet> distributionSets = testdataFactory.createDistributionSets(2);

        final DeploymentRequest targetToDS0 = DeploymentRequest
                .builder(target.getControllerId(), distributionSets.get(0).getId()).weight(78).build();

        final DeploymentRequest targetToDS1 = DeploymentRequest
                .builder(target.getControllerId(), distributionSets.get(1).getId()).weight(565).build();

        final List<DeploymentRequest> deploymentRequests = List.of(targetToDS0, targetToDS1);
        Assertions.assertThatExceptionOfType(MultiAssignmentIsNotEnabledException.class)
                .isThrownBy(() -> deploymentManagement.assignDistributionSets(deploymentRequests));

        enableMultiAssignments();
        assertThat(getResultingActionCount(deploymentManagement.assignDistributionSets(Arrays.asList(targetToDS0, targetToDS1)))).isEqualTo(2);
    }

    /**
     * Assigning distribution set to the list of targets with a non-existing one leads to successful assignment of valid targets, while not found targets are silently ignored.
     */
    @Test
    void assignDistributionSetToNotExistingTarget() {
        final String notExistingId = "notExistingTarget";

        final DistributionSet createdDs = testdataFactory.createDistributionSet();

        final List<String> knownTargetIds = new ArrayList<>();
        knownTargetIds.add("1");
        knownTargetIds.add("2");
        testdataFactory.createTargets(knownTargetIds.toArray(new String[0]));

        // add not existing target to targets
        knownTargetIds.add(notExistingId);

        final List<DistributionSetAssignmentResult> assignDistributionSetsResults = assignDistributionSetToTargets(
                createdDs, knownTargetIds, false);

        for (final DistributionSetAssignmentResult assignDistributionSetsResult : assignDistributionSetsResults) {
            assertThat(assignDistributionSetsResult.getAlreadyAssigned()).isZero();
            assertThat(assignDistributionSetsResult.getAssigned()).isEqualTo(2);
            assertThat(assignDistributionSetsResult.getTotal()).isEqualTo(2);
        }
    }

    /**
     * Assignments with confirmation flow active will result in actions in 'WAIT_FOR_CONFIRMATION' state
     */
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void assignmentWithConfirmationFlowActive(final boolean confirmationRequired) {
        final List<String> controllerIds = testdataFactory.createTargets(1).stream().map(Target::getControllerId).toList();
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();

        enableConfirmationFlow();
        List<DistributionSetAssignmentResult> results = assignDistributionSetToTargets(distributionSet, controllerIds, confirmationRequired);

        assertThat(getResultingActionCount(results)).isEqualTo(controllerIds.size());

        controllerIds.forEach(controllerId ->
                deploymentManagement.findActionsByTarget(controllerId, PAGE).forEach(action -> {
                    assertThat(action.getDistributionSet().getId()).isIn(distributionSet.getId());
                    assertThat(action.getInitiatedBy())
                            .as("Should be Initiated by current user").isEqualTo(TenantAware.getCurrentUsername());
                    if (confirmationRequired) {
                        assertThat(action.getStatus()).isEqualTo(Status.WAIT_FOR_CONFIRMATION);
                    } else {
                        assertThat(action.getStatus()).isEqualTo(RUNNING);
                    }
                }));
    }

    /**
     * Verify auto confirmation assignments and check action status with messages
     */
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void assignmentWithAutoConfirmationWillBeHandledCorrectly(final boolean confirmationRequired) {
        enableConfirmationFlow();

        final Target target = testdataFactory.createTarget();
        assertThat(target.getAutoConfirmationStatus()).isNull();

        confirmationManagement.activateAutoConfirmation(target.getControllerId(), "not_bumlux", "my personal remark");

        assertThat(targetManagement.getWithAutoConfigurationStatus(target.getControllerId()).getAutoConfirmationStatus()).isNotNull();

        final DistributionSet distributionSet = testdataFactory.createDistributionSet();

        assignDistributionSets(List.of(
                DeploymentRequest.builder(target.getControllerId(), distributionSet.getId())
                        .confirmationRequired(confirmationRequired).build()));

        assertThat(deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent()).hasSize(1)
                .allSatisfy(action -> {
                    assertThat(action.getStatus()).isEqualTo(RUNNING);
                    assertThat(actionStatusRepository.findByActionId(PAGE, action.getId())).hasSize(1)
                            .allSatisfy(status -> {
                                final JpaActionStatus actionStatus = (JpaActionStatus) status;
                                assertThat(actionStatus.getStatus()).isEqualTo(WAIT_FOR_CONFIRMATION);
                                if (confirmationRequired) {
                                    // confirmation of assignment is basically required, but active
                                    // auto-confirmation will perform the confirmation
                                    assertThat(actionStatus.getMessages())
                                            .contains("Assignment initiated by user 'bumlux'")
                                            .contains("""
                                                    Assignment automatically confirmed by initiator 'not_bumlux'.\s
                                                    
                                                    Auto confirmation activated by system user: 'bumlux'\s
                                                    
                                                    Remark: my personal remark""");
                                } else {
                                    // assignment never required confirmation, auto-confirmation will not be
                                    // applied.
                                    // assignment initiator has confirmed the action already
                                    assertThat(actionStatus.getMessages())
                                            .contains("Assignment initiated by user 'bumlux'")
                                            .contains("Assignment confirmed by initiator [bumlux].");
                                }
                            });
                });
    }

    /**
     * Multiple assignments with confirmation flow active will result in correct cancel behaviour
     */
    @Test
    void multipleAssignmentWithConfirmationFlowActiveVerifyCancelBehaviour() {
        final Target target = testdataFactory.createTarget("firstDevice");
        final DistributionSet firstDs = testdataFactory.createDistributionSet();
        final DistributionSet secondDs = testdataFactory.createDistributionSet();

        enableConfirmationFlow();
        final List<Action> resultActions = assignDistributionSet(firstDs.getId(), target.getControllerId()).getAssignedEntity();
        assertThat(resultActions).hasSize(1);

        assertThat(resultActions.get(0)).satisfies(action -> {
            assertThat(action.getDistributionSet().getId()).isEqualTo(firstDs.getId());
            assertThat(action.getStatus()).isEqualTo(Status.WAIT_FOR_CONFIRMATION);
        });

        final List<Action> resultActions2 = assignDistributionSet(secondDs.getId(), target.getControllerId()).getAssignedEntity();
        assertThat(resultActions2).hasSize(1);
        assertThat(resultActions2.get(0)).satisfies(action -> {
            assertThat(action.getDistributionSet().getId()).isEqualTo(secondDs.getId());
            assertThat(action.getStatus()).isEqualTo(Status.WAIT_FOR_CONFIRMATION);
        });

        final List<Action> actions = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent();
        assertThat(actions).hasSize(2)
                .anyMatch(action -> Objects.equals(action.getDistributionSet().getId(), firstDs.getId())
                        && action.getStatus() == Status.CANCELING)
                .anyMatch(action -> Objects.equals(action.getDistributionSet().getId(), secondDs.getId())
                        && action.getStatus() == Status.WAIT_FOR_CONFIRMATION);
    }

    /**
     * Assignments with confirmation flow deactivated will result in actions in only in 'RUNNING' state
     */
    @Test
    void verifyConfirmationRequiredFlagHaveNoInfluenceIfFlowIsDeactivated() {
        final List<String> targets1 = testdataFactory.createTargets("group1", 1).stream().map(Target::getControllerId).toList();
        final List<String> targets2 = testdataFactory.createTargets("group2", 1).stream().map(Target::getControllerId).toList();
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();

        final List<DistributionSetAssignmentResult> results = Stream
                .concat(assignDistributionSetToTargets(distributionSet, targets1, true).stream(),
                        assignDistributionSetToTargets(distributionSet, targets2, false).stream())
                .toList();

        final List<String> controllerIds = Stream.concat(targets1.stream(), targets2.stream()).toList();
        assertThat(getResultingActionCount(results)).isEqualTo(controllerIds.size());
        controllerIds.forEach(controllerId ->
                deploymentManagement.findActionsByTarget(controllerId, PAGE).forEach(action -> {
                    assertThat(action.getDistributionSet().getId()).isIn(distributionSet.getId());
                    assertThat(action.getInitiatedBy())
                            .as("Should be Initiated by current user").isEqualTo(TenantAware.getCurrentUsername());
                    assertThat(action.getStatus()).isEqualTo(RUNNING);
                }));
    }

    /**
     * Duplicate Assignments are removed from a request when multi-assignment is disabled, otherwise not
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = MultiActionAssignEvent.class, count = 1),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1) })
    void duplicateAssignmentsInRequestAreRemovedIfMultiassignmentEnabled() {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();
        final List<DeploymentRequest> twoEqualAssignments = Collections.nCopies(2, DeploymentRequest.builder(targetId, dsId).build());
        assertThat(getResultingActionCount(deploymentManagement.assignDistributionSets(twoEqualAssignments))).isEqualTo(1);

        enableMultiAssignments();
        final List<DeploymentRequest> twoEqualAssignmentsWithWeight = Collections.nCopies(
                2, DeploymentRequest.builder(targetId, dsId).weight(555).build());

        assertThat(getResultingActionCount(deploymentManagement.assignDistributionSets(twoEqualAssignmentsWithWeight))).isEqualTo(1);
    }

    /**
     * An assignment request is not accepted if it would lead to a target exceeding the max actions per target quota.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 21), // max actions per target are 20 for test
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3 * 21),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1) })
    void maxActionsPerTargetIsCheckedBeforeAssignmentExecution() {
        final int maxActions = quotaManagement.getMaxActionsPerTarget();
        assertThat(maxActions)
                .as("Expect 20 as maxActionPerTarget. If not the case change @Expect counts for " +
                        "DistributionSetCreatedEvent and SoftwareModuleCreatedEvent accordingly!")
                .isEqualTo(20);
        final int size = maxActions + 1;
        final String controllerId = testdataFactory.createTarget().getControllerId();

        final List<DeploymentRequest> deploymentRequests = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            final Long dsId = testdataFactory.createDistributionSet().getId();
            deploymentRequests.add(DeploymentRequest.builder(controllerId, dsId).weight(24).build());
        }

        enableMultiAssignments();
        Assertions.assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> deploymentManagement.assignDistributionSets(deploymentRequests));
        assertThat(actionRepository.countByTargetControllerId(controllerId)).isZero();
    }

    /**
     * An assignment request without a weight is ok when multi assignment in enabled.
     */
    @Test
    void weightNotRequiredInMultiAssignmentMode() {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final DeploymentRequest assignWithoutWeight = DeploymentRequest.builder(targetId, dsId).build();
        final DeploymentRequest assignWithWeight = DeploymentRequest.builder(targetId, dsId).weight(567).build();

        enableMultiAssignments();
        assertThat(deploymentManagement.assignDistributionSets(List.of(assignWithoutWeight, assignWithWeight))).isNotNull();
    }

    /**
     * An assignment request containing a weight don't causes an error when multi assignment in disabled.
     */
    @Test
    void weightAllowedWhenMultiAssignmentModeNotEnabled() {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final DeploymentRequest assignWithoutWeight = DeploymentRequest.builder(targetId, dsId).weight(456).build();
        assertThat(deploymentManagement.assignDistributionSets(Collections.singletonList(assignWithoutWeight))).isNotNull().size().isEqualTo(1);
    }

    /**
     * Weights are validated and contained in the resulting Action.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = MultiActionAssignEvent.class, count = 2),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1) })
    void weightValidatedAndSaved() {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();
        final DeploymentRequest validRequest1 = DeploymentRequest.builder(targetId, dsId).weight(Action.WEIGHT_MAX).build();
        final DeploymentRequest validRequest2 = DeploymentRequest.builder(targetId, dsId).weight(Action.WEIGHT_MIN).build();
        final DeploymentRequest weightTooLow = DeploymentRequest.builder(targetId, dsId).weight(Action.WEIGHT_MIN - 1).build();
        final DeploymentRequest weightTooHigh = DeploymentRequest.builder(targetId, dsId).weight(Action.WEIGHT_MAX + 1).build();
        enableMultiAssignments();
        final List<DeploymentRequest> deploymentRequestsTooLow = Collections.singletonList(weightTooLow);
        Assertions.assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> deploymentManagement.assignDistributionSets(deploymentRequestsTooLow));
        final List<DeploymentRequest> deploymentRequestsTooHigh = Collections.singletonList(weightTooHigh);
        Assertions.assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(
                () -> deploymentManagement.assignDistributionSets(deploymentRequestsTooHigh));
        final Long validActionId1 = getFirstAssignedAction(
                deploymentManagement.assignDistributionSets(Collections.singletonList(validRequest1)).get(0)).getId();
        final Long validActionId2 = getFirstAssignedAction(
                deploymentManagement.assignDistributionSets(Collections.singletonList(validRequest2)).get(0)).getId();
        assertThat(actionRepository.findById(validActionId1).get().getWeight()).get().isEqualTo(Action.WEIGHT_MAX);
        assertThat(actionRepository.findById(validActionId2).get().getWeight()).get().isEqualTo(Action.WEIGHT_MIN);
    }

    /**
     * Test a simple deployment by calling the {@link TargetRepository#assignDistributionSet(DistributionSet, Iterable)} and
     * checking the active action and the action history of the targets.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = TargetCreatedEvent.class, count = 30),
            @Expect(type = ActionCreatedEvent.class, count = 20),
            @Expect(type = TargetUpdatedEvent.class, count = 20) })
    void assignDistributionSet2Targets() {
        final String myCtrlIDPref = "myCtrlID";
        final Iterable<Target> savedNakedTargets = testdataFactory.createTargets(10, myCtrlIDPref, "first description");

        final String myDeployedCtrlIDPref = "myDeployedCtrlID";
        List<Target> savedDeployedTargets = testdataFactory.createTargets(20, myDeployedCtrlIDPref,
                "first description");

        final DistributionSet ds = testdataFactory.createDistributionSet("");
        assignDistributionSet(ds, savedDeployedTargets);
        implicitLock(ds);

        // verify that one Action for each assignDistributionSet
        final Page<JpaAction> actions = actionRepository.findAll(PAGE);
        assertThat(actions.getNumberOfElements()).as("wrong size of actions").isEqualTo(20);
        assertThat(actions).as("Actions should be initiated by current user")
                .allMatch(a -> a.getInitiatedBy().equals(TenantAware.getCurrentUsername()));

        final Iterable<? extends Target> allFoundTargets = targetManagement.findAll(PAGE).getContent();

        // get final updated version of targets
        savedDeployedTargets = targetManagement.findByControllerId(savedDeployedTargets.stream().map(Target::getControllerId).toList());

        Assertions.<Target> assertThat(allFoundTargets).as("founded targets are wrong")
                .containsAll(savedDeployedTargets)
                .containsAll(savedNakedTargets);
        assertThat(savedDeployedTargets).as("saved target are wrong").doesNotContain(toArray(savedNakedTargets, Target.class));
        assertThat(savedNakedTargets).as("saved target are wrong").doesNotContain(toArray(savedDeployedTargets, Target.class));

        for (final Target myt : savedNakedTargets) {
            final Target t = targetManagement.getByControllerId(myt.getControllerId());
            assertThat(deploymentManagement.countActionsByTarget(t.getControllerId())).as("action should be empty").isZero();
        }

        for (final Target myt : savedDeployedTargets) {
            final Target t = targetManagement.getByControllerId(myt.getControllerId());
            final List<Action> activeActionsByTarget = deploymentManagement.findActiveActionsByTarget(t.getControllerId(), PAGE).getContent();
            assertThat(activeActionsByTarget).as("action should not be empty").isNotEmpty();
            assertThat(t.getUpdateStatus()).as("wrong target update status").isEqualTo(TargetUpdateStatus.PENDING);
            for (final Action ua : activeActionsByTarget) {
                assertThat(ua.getDistributionSet()).as("action has wrong ds").isEqualTo(ds);
            }
        }
    }

    /**
     * Test that it is not possible to assign a distribution set that is not complete.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 2),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 2), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 2), // implicit lock
            @Expect(type = TargetCreatedEvent.class, count = 10),
            @Expect(type = ActionCreatedEvent.class, count = 10),
            @Expect(type = TargetUpdatedEvent.class, count = 10) })
    void failDistributionSetAssigmentThatIsNotComplete() {
        final List<Target> targets = testdataFactory.createTargets(10);

        final SoftwareModule ah = testdataFactory.createSoftwareModuleApp();
        final SoftwareModule os = testdataFactory.createSoftwareModuleOs();

        final DistributionSet incomplete = distributionSetManagement.create(
                DistributionSetManagement.Create.builder()
                        .type(standardDsType)
                        .name("incomplete").version("v1")
                        .modules(Set.of(ah))
                        .build());

        assertThatExceptionOfType(IncompleteDistributionSetException.class)
                .as("expected IncompleteDistributionSetException")
                .isThrownBy(() -> assignDistributionSet(incomplete, targets));

        final DistributionSet nowComplete = distributionSetManagement.assignSoftwareModules(incomplete.getId(), Set.of(os.getId()));

        assertThat(assignDistributionSet(nowComplete, targets).getAssigned()).as("assign ds doesn't work").isEqualTo(10);
    }

    /**
     * Multiple deployments or distribution set to target assignment test. Expected behaviour is that a new deployment
     * overrides unfinished old one which are canceled as part of the operation.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 5 + 4),
            @Expect(type = TargetUpdatedEvent.class, count = 3 * 4),
            @Expect(type = ActionCreatedEvent.class, count = 3 * 4),
            @Expect(type = ActionUpdatedEvent.class, count = 4 * 2),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 2),
            @Expect(type = DistributionSetCreatedEvent.class, count = 3),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 9),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 9), // implicit lock
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 3) })
    void multipleDeployments() {
        final String undeployedTargetPrefix = "undep-T";
        final int noOfUndeployedTargets = 5;

        final String deployedTargetPrefix = "dep-T";
        final int noOfDeployedTargets = 4;
        final int noOfDistributionSets = 3;

        final DeploymentResult deploymentResult = prepareComplexRepo(undeployedTargetPrefix, noOfUndeployedTargets,
                deployedTargetPrefix, noOfDeployedTargets, noOfDistributionSets, "myTestDS");

        final List<Long> deployedTargetIDs = deploymentResult.getDeployedTargetIDs();
        final List<Long> undeployedTargetIDs = deploymentResult.getUndeployedTargetIDs();
        final Collection<JpaTarget> savedNakedTargets = (Collection) deploymentResult.getUndeployedTargets();
        final Collection<JpaTarget> savedDeployedTargets = (Collection) deploymentResult.getDeployedTargets();

        // retrieving all Actions created by the assignDistributionSet call
        final Page<JpaAction> page = actionRepository.findAll(PAGE);
        assertThat(page).as("Actions should be initiated by current user")
                .allMatch(a -> a.getInitiatedBy().equals(TenantAware.getCurrentUsername()));
        // and verify the number
        assertThat(page.getTotalElements()).as("wrong size of actions")
                .isEqualTo(noOfDeployedTargets * noOfDistributionSets);

        // only records retrieved from the DB can be evaluated to be sure that all fields are populated
        final List<JpaTarget> allFoundTargets = targetRepository.findAll();

        final List<JpaTarget> deployedTargetsFromDB = targetRepository.findAllById(deployedTargetIDs);
        final List<JpaTarget> undeployedTargetsFromDB = targetRepository.findAllById(undeployedTargetIDs);

        // test that number of Targets
        assertThat(allFoundTargets.spliterator().getExactSizeIfKnown()).as("number of target is wrong")
                .isEqualTo(deployedTargetsFromDB.spliterator().getExactSizeIfKnown()
                        + undeployedTargetsFromDB.spliterator().getExactSizeIfKnown());
        assertThat(deployedTargetsFromDB.spliterator().getExactSizeIfKnown()).as("number of target is wrong")
                .isEqualTo(noOfDeployedTargets);
        assertThat(undeployedTargetsFromDB.spliterator().getExactSizeIfKnown()).as("number of target is wrong")
                .isEqualTo(noOfUndeployedTargets);

        // test the content of different lists
        assertThat(allFoundTargets).as("content of founded target is wrong").containsAll(deployedTargetsFromDB)
                .containsAll(undeployedTargetsFromDB);

        assertThat(deployedTargetsFromDB).as("content of deployed target is wrong")
                .usingElementComparator(controllerIdComparator()).containsAll(savedDeployedTargets)
                .doesNotContain(toArray(undeployedTargetsFromDB, JpaTarget.class));
        assertThat(undeployedTargetsFromDB).as("content of undeployed target is wrong").containsAll(savedNakedTargets)
                .doesNotContain(toArray(deployedTargetsFromDB, JpaTarget.class));
    }

    /**
     * Multiple deployments or distribution set to target assignment test including finished response
     * IN_SYNC status and installed DS is set to the assigned DS entry.
     */
    @Test
    void assignDistributionSetAndAddFinishedActionStatus() {
        final PageRequest pageRequest = PageRequest.of(0, 100, Direction.ASC, ActionStatusFields.ID.getName());

        final DeploymentResult deployResWithDsA = prepareComplexRepo("undep-A-T", 2, "dep-A-T", 4, 1, "dsA");
        final DeploymentResult deployResWithDsB = prepareComplexRepo("undep-B-T", 3, "dep-B-T", 5, 1, "dsB");
        final DeploymentResult deployResWithDsC = prepareComplexRepo("undep-C-T", 4, "dep-C-T", 6, 1, "dsC");

        // keep a reference to the created DistributionSets
        final JpaDistributionSet dsA = (JpaDistributionSet) deployResWithDsA.getDistributionSets().get(0);
        final JpaDistributionSet dsB = (JpaDistributionSet) deployResWithDsB.getDistributionSets().get(0);
        final JpaDistributionSet dsC = (JpaDistributionSet) deployResWithDsC.getDistributionSets().get(0);

        // retrieving the UpdateActions created by the assignments
        assertThat(findActionsByDistributionSet(pageRequest, dsA.getId()).getContent()).isNotEmpty();
        assertThat(findActionsByDistributionSet(pageRequest, dsB.getId()).getContent()).isNotEmpty();
        assertThat(findActionsByDistributionSet(pageRequest, dsC.getId()).getContent()).isNotEmpty();

        // verifying the correctness of the assignments
        for (final Target t : deployResWithDsA.getDeployedTargets()) {
            assertThat(deploymentManagement.findAssignedDistributionSet(t.getControllerId()).get().getId())
                    .as("assignment is not correct").isEqualTo(dsA.getId());
            assertThat(deploymentManagement.findInstalledDistributionSet(t.getControllerId()))
                    .as("installed ds should be null").isNotPresent();
        }
        for (final Target t : deployResWithDsB.getDeployedTargets()) {
            assertThat(deploymentManagement.findAssignedDistributionSet(t.getControllerId()).get().getId())
                    .as("assigned ds is wrong").isEqualTo(dsB.getId());
            assertThat(deploymentManagement.findInstalledDistributionSet(t.getControllerId()))
                    .as("installed ds should be null").isNotPresent();
        }
        for (final Target t : deployResWithDsC.getDeployedTargets()) {
            assertThat(deploymentManagement.findAssignedDistributionSet(t.getControllerId()).get().getId())
                    .isEqualTo(dsC.getId());
            assertThat(deploymentManagement.findInstalledDistributionSet(t.getControllerId()))
                    .as("installed ds should not be null").isNotPresent();
            assertThat(targetManagement.getByControllerId(t.getControllerId()).getUpdateStatus())
                    .as("wrong target info update status").isEqualTo(TargetUpdateStatus.PENDING);
        }

        final List<Target> updatedTsDsA = testdataFactory
                .sendUpdateActionStatusToTargets(deployResWithDsA.getDeployedTargets(), Status.FINISHED,
                        Collections.singletonList("alles gut"))
                .stream().map(Action::getTarget).toList();

        // verify, that dsA is deployed correctly
        for (final Target t_ : updatedTsDsA) {
            final Target t = targetManagement.getByControllerId(t_.getControllerId());
            assertThat(deploymentManagement.findAssignedDistributionSet(t.getControllerId()))
                    .as("assigned ds is wrong").contains(dsA);
            assertThat(deploymentManagement.findInstalledDistributionSet(t.getControllerId()))
                    .as("installed ds is wrong").contains(dsA);
            assertThat(targetManagement.getByControllerId(t.getControllerId()).getUpdateStatus())
                    .as("wrong target info update status").isEqualTo(TargetUpdateStatus.IN_SYNC);
            assertThat(deploymentManagement.findActiveActionsByTarget(t.getControllerId(), PAGE))
                    .as("no actions should be active").isEmpty();
        }

        // deploy dsA to the target which already have dsB deployed -> must remove updActB from
        // activeActions, add a corresponding cancelAction and another UpdateAction for dsA
        final List<Target> deployed2DS = assignDistributionSet(dsA, deployResWithDsB.getDeployedTargets())
                .getAssignedEntity().stream().map(Action::getTarget).toList();
        findActionsByDistributionSet(pageRequest, dsA.getId()).getContent().get(1);

        // get final updated version of targets
        final List<Target> deployResWithDsBTargets = targetManagement.findByControllerId(
                deployResWithDsB.getDeployedTargets().stream().map(Target::getControllerId).toList());

        assertThat(deployed2DS).as("deployed ds is wrong").usingElementComparator(controllerIdComparator())
                .containsAll(deployResWithDsBTargets);
        assertThat(deployed2DS).as("deployed ds is wrong").hasSameSizeAs(deployResWithDsBTargets);

        for (final Target t_ : deployed2DS) {
            final Target t = targetManagement.getByControllerId(t_.getControllerId());
            assertThat(deploymentManagement.findAssignedDistributionSet(t.getControllerId())).as("assigned ds is wrong")
                    .contains(dsA);
            assertThat(deploymentManagement.findInstalledDistributionSet(t.getControllerId()))
                    .as("installed ds should be null").isNotPresent();
            assertThat(targetManagement.getByControllerId(t.getControllerId()).getUpdateStatus())
                    .as("wrong target info update status").isEqualTo(TargetUpdateStatus.PENDING);

        }
    }

    /**
     * Test the deletion of {@link DistributionSet}s including exception in case of
     * {@link Target}s are assigned by {@link Target#getAssignedDistributionSet()}
     * or {@link Target#getInstalledDistributionSet()}
     */
    @Test
    void deleteDistributionSet() {
        final PageRequest pageRequest = PageRequest.of(0, 100, Direction.ASC, "id");

        final String undeployedTargetPrefix = "undep-T";
        final int noOfUndeployedTargets = 2;

        final String deployedTargetPrefix = "dep-T";
        final int noOfDeployedTargets = 4;

        final int noOfDistributionSets = 3;

        final DeploymentResult deploymentResult = prepareComplexRepo(undeployedTargetPrefix, noOfUndeployedTargets,
                deployedTargetPrefix, noOfDeployedTargets, noOfDistributionSets, "myTestDS");

        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        distributionSetManagement.delete(dsA.getId());

        assertThat(distributionSetManagement.find(dsA.getId())).isNotPresent();

        // // verify that the ds is not physically deleted
        for (final DistributionSet ds : deploymentResult.getDistributionSets()) {
            distributionSetManagement.delete(ds.getId());
            final DistributionSet foundDS = distributionSetManagement.find(ds.getId()).get();
            assertThat(foundDS).as("founded should not be null").isNotNull();
            assertThat(foundDS.isDeleted()).as("found ds should be deleted").isTrue();
        }

        // verify that deleted attribute is used correctly
        List<? extends DistributionSet> allFoundDS = distributionSetManagement.findAll(PAGE).getContent();
        assertThat(allFoundDS).as("no ds should be founded").isEmpty();

        assertThat(distributionSetRepository.findAll(DistributionSetSpecification.isDeleted(true), PAGE).getContent())
                .as("wrong size of founded ds").hasSize(noOfDistributionSets);

        IntStream.range(0, deploymentResult.getDistributionSets().size()).forEach(i -> testdataFactory.sendUpdateActionStatusToTargets(
                deploymentResult.getDeployedTargets(), Status.FINISHED, Collections.singletonList("blabla alles gut")));

        // try to delete again
        distributionSetManagement.delete(deploymentResult.getDistributionSetIDs());
        // verify that the result is the same, even though distributionSet dsA has been installed
        // successfully and no activeAction is referring to created distribution sets
        allFoundDS = distributionSetManagement.findAll(pageRequest).getContent();
        assertThat(allFoundDS).as("no ds should be found").isEmpty();
        assertThat(distributionSetRepository.findAll(DistributionSetSpecification.isDeleted(true), PAGE).getContent())
                .as("wrong size of founded ds").hasSize(noOfDistributionSets);
    }

    /**
     * Deletes multiple targets and verifies that all related metadata is also deleted.
     */
    @Test
    void deletesTargetsAndVerifyCascadeDeletes() {
        final String undeployedTargetPrefix = "undep-T";
        final int noOfUndeployedTargets = 2;

        final String deployedTargetPrefix = "dep-T";
        final int noOfDeployedTargets = 4;
        final int noOfDistributionSets = 3;

        final DeploymentResult deploymentResult = prepareComplexRepo(undeployedTargetPrefix, noOfUndeployedTargets,
                deployedTargetPrefix, noOfDeployedTargets, noOfDistributionSets, "myTestDS");

        IntStream.range(0, deploymentResult.getDistributionSets().size()).forEach(i -> testdataFactory.sendUpdateActionStatusToTargets(
                deploymentResult.getDeployedTargets(), Status.FINISHED, Collections.singletonList("blabla alles gut")));

        assertThat(targetManagement.count()).as("size of targets is wrong").isNotZero();
        assertThat(actionStatusRepository.count()).as("size of action status is wrong").isNotZero();

        targetManagement.delete(deploymentResult.getUndeployedTargetIDs());
        targetManagement.delete(deploymentResult.getDeployedTargetIDs());

        assertThat(targetManagement.count()).as("size of targets should be zero").isZero();
        assertThat(actionStatusRepository.count()).as("size of action status is wrong").isZero();
    }

    /**
     * Testing if changing target and the status without refreshing the entities from the DB (e.g. concurrent changes from UI and from controller) works
     */
    @Test
    void alternatingAssignmentAndAddUpdateActionStatus() {
        final DistributionSet dsA = testdataFactory.createDistributionSet("a");
        final DistributionSet dsB = testdataFactory.createDistributionSet("b");
        List<Target> targs = Collections.singletonList(testdataFactory.createTarget("target-id-A"));

        // doing the assignment
        targs = assignDistributionSet(dsA, targs).getAssignedEntity().stream().map(Action::getTarget).toList();
        implicitLock(dsA);
        Target targ = targetManagement.getByControllerId(targs.iterator().next().getControllerId());

        // checking the revisions of the created entities
        // verifying that the revision of the object and the revision within the
        // DB has incremented by implicit lock
        assertThat(dsA.getOptLockRevision())
                .as("lock revision is wrong")
                .isEqualTo(distributionSetManagement.getWithDetails(dsA.getId()).getOptLockRevision());

        // verifying that the assignment is correct
        assertThat(deploymentManagement.findActiveActionsByTarget(targ.getControllerId(), PAGE).getTotalElements())
                .as("Active target actions are wrong").isEqualTo(1);
        assertThat(deploymentManagement.countActionsByTarget(targ.getControllerId())).as("Target actions are wrong")
                .isEqualTo(1);
        assertThat(targ.getUpdateStatus()).as("UpdateStatus of target is wrong").isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(deploymentManagement.findAssignedDistributionSet(targ.getControllerId()))
                .as("Assigned distribution set of target is wrong").contains(dsA);
        assertThat(deploymentManagement.findActiveActionsByTarget(targ.getControllerId(), PAGE).getContent().get(0)
                .getDistributionSet()).as("Distribution set of action is wrong").isEqualTo(dsA);
        assertThat(deploymentManagement.findActiveActionsByTarget(targ.getControllerId(), PAGE).getContent().get(0)
                .getDistributionSet()).as("Installed distribution set of action should be null").isNotNull();

        final Slice<Action> updAct = findActionsByDistributionSet(PAGE, dsA.getId());
        controllerManagement.addUpdateActionStatus(
                ActionStatusCreate.builder().actionId(updAct.getContent().get(0).getId()).status(Status.FINISHED).build());

        targ = targetManagement.getByControllerId(targ.getControllerId());
        assertEquals(0, deploymentManagement.findActiveActionsByTarget(targ.getControllerId(), PAGE).getTotalElements(),
                "active target actions are wrong");

        assertEquals(TargetUpdateStatus.IN_SYNC, targ.getUpdateStatus(), "tagret update status is not correct");
        assertEquals(dsA, deploymentManagement.findAssignedDistributionSet(targ.getControllerId()).get(), "wrong assigned ds");
        assertEquals(dsA, deploymentManagement.findInstalledDistributionSet(targ.getControllerId()).get(), "wrong installed ds");

        targs = assignDistributionSet(dsB.getId(), "target-id-A").getAssignedEntity().stream().map(Action::getTarget).toList();
        implicitLock(dsB);

        targ = targs.iterator().next();

        assertEquals(1, deploymentManagement.findActiveActionsByTarget(targ.getControllerId(), PAGE).getTotalElements(),
                "active actions are wrong");
        assertEquals(TargetUpdateStatus.PENDING, targetManagement.getByControllerId(targ.getControllerId()).getUpdateStatus(),
                "target status is wrong");
        assertEquals(dsB, deploymentManagement.findAssignedDistributionSet(targ.getControllerId()).get(),
                "wrong assigned ds");
        assertEquals(dsA.getId(), deploymentManagement.findInstalledDistributionSet(targ.getControllerId()).get().getId(),
                "Installed ds is wrong");
        assertEquals(dsB, deploymentManagement.findActiveActionsByTarget(targ.getControllerId(), PAGE).getContent().get(0).getDistributionSet(),
                "Active ds is wrong");
    }

    /**
     * The test verifies that the DS itself is not changed because of an target assignment
     * which is a relationship but not a changed on the entity itself..
     */
    @Test
    void checkThatDsRevisionsIsNotChangedWithTargetAssignment() {
        final DistributionSet dsA = testdataFactory.createDistributionSet("a");
        testdataFactory.createDistributionSet("b");
        final Target targ = testdataFactory.createTarget("target-id-A");

        assertThat(dsA.getOptLockRevision())
                .as("lock revision is wrong")
                .isEqualTo(distributionSetManagement.getWithDetails(dsA.getId()).getOptLockRevision());

        assignDistributionSet(dsA, Collections.singletonList(targ));

        // implicit lock - incremented the version
        assertThat(dsA.getOptLockRevision() + 1).as("lock revision is wrong")
                .isEqualTo(distributionSetManagement.getWithDetails(dsA.getId()).getOptLockRevision());
    }

    /**
     * Tests the switch from a soft to hard update by API
     */
    @Test
    void forceSoftAction() {
        // prepare
        final Target target = testdataFactory.createTarget("knownControllerId");
        final DistributionSet ds = testdataFactory.createDistributionSet("a");
        // assign ds to create an action
        final DistributionSetAssignmentResult assignDistributionSet = assignDistributionSet(ds.getId(),
                target.getControllerId(), ActionType.SOFT);
        final Long actionId = getFirstAssignedActionId(assignDistributionSet);
        // verify preparation
        Action findAction = deploymentManagement.findAction(actionId).get();
        assertThat(findAction.getActionType()).as("action type is wrong").isEqualTo(ActionType.SOFT);

        // test
        deploymentManagement.forceTargetAction(actionId);

        // verify test
        findAction = deploymentManagement.findAction(actionId).get();
        assertThat(findAction.getActionType()).as("action type is wrong").isEqualTo(ActionType.FORCED);
    }

    /**
     * Tests the switch from a hard to hard update by API, e.g. which in fact should not change anything.
     */
    @Test
    void forceAlreadyForcedActionNothingChanges() {
        // prepare
        final Target target = testdataFactory.createTarget("knownControllerId");
        final DistributionSet ds = testdataFactory.createDistributionSet("a");
        // assign ds to create an action
        final DistributionSetAssignmentResult assignDistributionSet = assignDistributionSet(ds.getId(),
                target.getControllerId(), ActionType.FORCED);
        final Long actionId = getFirstAssignedActionId(assignDistributionSet);
        // verify preparation
        Action findAction = deploymentManagement.findAction(actionId).get();
        assertThat(findAction.getActionType()).as("action type is wrong").isEqualTo(ActionType.FORCED);

        // test
        final Action forceTargetAction = deploymentManagement.forceTargetAction(actionId);

        // verify test
        assertThat(forceTargetAction.getActionType()).as("action type is wrong").isEqualTo(ActionType.FORCED);
        findAction = deploymentManagement.findAction(actionId).get();
        assertThat(findAction.getActionType()).as("action type is wrong").isEqualTo(ActionType.FORCED);
    }

    /**
     * Tests the computation of already assigned entities returned as a result of an assignment
     */
    @Test
    void testAlreadyAssignedAndAssignedActionsInAssignmentResult() {
        // create target1, distributionSet, assign ds to target1 and finish update (close all actions)
        final Action action = prepareFinishedUpdate("target1", "ds", false);
        final Target target2 = testdataFactory.createTarget("target2");
        final Target target3 = testdataFactory.createTarget("target3");

        // assign ds to target2, but don't finish update (actions should be still open)
        assignDistributionSet(action.getDistributionSet().getId(), target2.getControllerId());

        final DistributionSetAssignmentResult assignmentResult = assignDistributionSet(
                action.getDistributionSet().getId(),
                List.of(action.getTarget().getControllerId(), target3.getControllerId()), ActionType.FORCED);

        assertThat(assignmentResult).isNotNull();
        assertThat(assignmentResult.getTotal()).as("Total count of assigned and already assigned targets").isEqualTo(2);
        assertThat(assignmentResult.getAssigned()).as("Total count of assigned targets").isEqualTo(1);
        assertThat(assignmentResult.getAlreadyAssigned()).as("Total count of already assigned targets").isEqualTo(1);
        assertThat(assignmentResult.getAssignedEntity()).isNotEmpty();
        final DistributionSet actionDistributionSet = distributionSetRepository.getById(action.getDistributionSet().getId());
        assertThat(assignmentResult.getAssignedEntity()).allMatch(
                a -> a.getTarget().equals(target3) && a.getDistributionSet().equals(actionDistributionSet));
        assertThat(assignmentResult.getAssignedEntity()).noneMatch(a -> a.getTarget().getControllerId().equals("target1"));
    }

    /**
     * Verify that the DistributionSetAssignmentResult not contains already assigned targets.
     */
    @Test
    void verifyDistributionSetAssignmentResultNotContainsAlreadyAssignedTargets() {
        final DistributionSet dsToTargetAssigned = testdataFactory.createDistributionSet("ds-3");

        // create assigned DS
        final Target savedTarget = testdataFactory.createTarget();
        DistributionSetAssignmentResult assignmentResult = assignDistributionSet(dsToTargetAssigned.getId(), savedTarget.getControllerId());
        assertThat(assignmentResult.getAssignedEntity()).hasSize(1);

        assignmentResult = assignDistributionSet(dsToTargetAssigned.getId(), savedTarget.getControllerId());
        assertThat(assignmentResult.getAssignedEntity()).isEmpty();

        assertThat(distributionSetRepository.findAll()).hasSize(1);
    }

    /**
     * Verify that the DistributionSet assignments work for multiple targets of the same target type within the same request.
     */
    @Test
    void verifyDSAssignmentForMultipleTargetsWithSameTargetType() {
        final DistributionSet ds = testdataFactory.createDistributionSet("test-ds");
        final TargetType targetType = testdataFactory.createTargetType("test-type", Set.of(ds.getType()));

        final List<DeploymentRequest> deploymentRequests = new ArrayList<>();
        for (int i = 0; i < quotaManagement.getMaxTargetDistributionSetAssignmentsPerManualAssignment(); i++) {
            final Target target = testdataFactory.createTarget("test-target-" + i, "test-target-" + i, targetType);
            final DeploymentRequest deployment = DeploymentRequest.builder(target.getControllerId(), ds.getId()).build();
            deploymentRequests.add(deployment);
        }

        deploymentManagement.assignDistributionSets(deploymentRequests);
        implicitLock(ds);

        final List<? extends Target> content = targetManagement.findAll(Pageable.unpaged()).getContent();
        content.stream().map(JpaTarget.class::cast)
                .forEach(jpaTarget -> assertThat(jpaTarget.getAssignedDistributionSet()).isEqualTo(ds));
    }

    /**
     * Verify that the DistributionSet assignments work for multiple targets of different target types.
     */
    @Test
    void verifyDSAssignmentForMultipleTargetsWithDifferentTargetTypes() {
        final DistributionSet ds = testdataFactory.createDistributionSet("test-ds");
        final TargetType targetType1 = testdataFactory.createTargetType("test-type1", Set.of(ds.getType()));
        final TargetType targetType2 = testdataFactory.createTargetType("test-type2", Set.of(ds.getType()));
        final Target target1 = testdataFactory.createTarget("test-target1", "test-target1", targetType1);
        final Target target2 = testdataFactory.createTarget("test-target2", "test-target2", targetType2);

        final DeploymentRequest deployment1 = DeploymentRequest.builder(target1.getControllerId(), ds.getId()).build();
        final DeploymentRequest deployment2 = DeploymentRequest.builder(target2.getControllerId(), ds.getId()).build();
        final List<DeploymentRequest> deploymentRequests = Arrays.asList(deployment1, deployment2);

        deploymentManagement.assignDistributionSets(deploymentRequests);
        implicitLock(ds);

        final DistributionSet assignedDsTarget1 = ((JpaTarget) targetManagement
                .getWithDetails(target1.getControllerId(), "assignedDistributionSet")).getAssignedDistributionSet();
        final DistributionSet assignedDsTarget2 = ((JpaTarget) targetManagement
                .getWithDetails(target1.getControllerId(), "assignedDistributionSet")).getAssignedDistributionSet();

        assertThat(assignedDsTarget1).isEqualTo(ds);
        assertThat(assignedDsTarget2).isEqualTo(ds);
    }

    /**
     * Verify that the DistributionSet assignment fails for target with incompatible target type.
     */
    @Test
    void verifyDSAssignmentFailsForTargetsWithIncompatibleTargetTypes() {
        final DistributionSet ds = testdataFactory.createDistributionSet("test-ds");
        final DistributionSetType dsType = testdataFactory.findOrCreateDistributionSetType("test-ds-type", "dsType");
        final TargetType targetType = testdataFactory.createTargetType("target-type", Set.of(dsType));
        final Target target = testdataFactory.createTarget("test-target", "test-target", targetType);

        final DeploymentRequest deploymentRequest = DeploymentRequest.builder(target.getControllerId(), ds.getId()).build();
        final List<DeploymentRequest> deploymentRequests = List.of(deploymentRequest);

        assertThatExceptionOfType(IncompatibleTargetTypeException.class)
                .isThrownBy(() -> deploymentManagement.assignDistributionSets(deploymentRequests));
    }

    /**
     * Verify that the DistributionSet assignment fails for target with target type that is not compatible with any dsType.
     */
    @Test
    void verifyDSAssignmentFailsForTargetsWithTargetTypesThatAreNotCompatibleWithAnyDs() {
        final DistributionSet ds = testdataFactory.createDistributionSet("test-ds");
        final TargetType emptyTargetType = testdataFactory.createTargetType("target-type", Set.of());
        final Target targetWithEmptyType = testdataFactory.createTarget("test-target", "test-target", emptyTargetType);

        final DeploymentRequest deploymentRequestWithEmptyType = DeploymentRequest
                .builder(targetWithEmptyType.getControllerId(), ds.getId()).build();
        final List<DeploymentRequest> deploymentRequests = Collections.singletonList(deploymentRequestWithEmptyType);
        assertThatExceptionOfType(IncompatibleTargetTypeException.class)
                .isThrownBy(() -> deploymentManagement.assignDistributionSets(deploymentRequests));
    }

    @Test
    void testManualAssignmentsActionsPurge() {
        Target target = testdataFactory.createTarget();

        for (int i = 0; i < 20; i++) {
            DistributionSet distributionSet = testdataFactory.createDistributionSet("ds_" + i);
            assignDistributionSet(distributionSet.getId(), target.getControllerId());
        }

        long actions = deploymentManagement.countActionsByTarget(target.getControllerId());
        // quota in tests is set to 20 ...
        assertEquals(20, actions);

        // extract the first 5 action ids
        final List<Action> firstSample = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent();
        List<Long> shouldBePurgedActionsList = firstSample.stream().map(Identifiable::getId).limit(5).toList();

        DistributionSet exceededQuotaDsAssign = testdataFactory.createDistributionSet("exceededQuotaAssignment");

        // should throw quota exception if not explicitly configured to purge actions
        assertThrows(AssignmentQuotaExceededException.class,
                () -> assignDistributionSet(exceededQuotaDsAssign.getId(), target.getControllerId()));

        // set purge config to 25 %
        tenantConfigurationManagement().addOrUpdateConfiguration(TenantConfigurationKey.ACTION_CLEANUP_ON_QUOTA_HIT_PERCENTAGE, 25);

        // assign again
        assignDistributionSet(exceededQuotaDsAssign.getId(), target.getControllerId());
        // 16 actions should be present
        actions = deploymentManagement.countActionsByTarget(target.getControllerId());
        assertEquals(16, actions);

        final List<Action> actionsList = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent();
        // first 5 should have been purged so the first actionId should be the last purged action id + 1
        assertEquals(shouldBePurgedActionsList.get(shouldBePurgedActionsList.size() - 1) + 1, actionsList.get(0).getId());
        assertEquals(firstSample.get(firstSample.size() - 1).getId() + 1, actionsList.get(15).getId());
    }

    @Test
    void testRolloutAssignmentsActionsPurge() {
        final Target target = testdataFactory.createTarget();
        for (int i = 0; i < 20; i++) {
            DistributionSet distributionSet = testdataFactory.createDistributionSet();
            Rollout rollout = testdataFactory.createRolloutByVariables(
                    "rollout-" + i, "Description", 1, "controllerId==" + target.getControllerId(), distributionSet, "50", "50");
            rolloutManagement.start(rollout.getId());
            rolloutHandler.handleAll();
        }

        assertEquals(20, deploymentManagement.countActionsByTarget(target.getControllerId()));
        List<Action> firstSample = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent();
        List<Long> shouldBePurgedActionsList = firstSample.stream().map(Identifiable::getId).limit(5).toList();

        DistributionSet distributionSet = testdataFactory.createDistributionSet();
        Rollout rollout = testdataFactory.createRolloutByVariables(
                "rollout-quota", "Description", 1, "controllerId==" + target.getControllerId(), distributionSet, "50", "50");
        rolloutManagement.start(rollout.getId());
        // don't assert quota exception here because rollout executor does not throw such in order to not interrupt other executions
        rolloutHandler.handleAll();
        //check that the old number of actions remain instead
        assertEquals(20, deploymentManagement.countActionsByTarget(target.getControllerId()));

        // set purge config to 25 %
        tenantConfigurationManagement().addOrUpdateConfiguration(TenantConfigurationKey.ACTION_CLEANUP_ON_QUOTA_HIT_PERCENTAGE, 25);
        rolloutHandler.handleAll();
        assertEquals(16, deploymentManagement.countActionsByTarget(target.getControllerId()));

        List<Action> actionsList = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent();
        // first 5 should have been purged so the first actionId should be the last purged action id + 1
        assertEquals(shouldBePurgedActionsList.get(shouldBePurgedActionsList.size() - 1) + 1, actionsList.get(0).getId());
        assertEquals(firstSample.get(firstSample.size() - 1).getId() + 1, actionsList.get(15).getId());
    }

    @Test
    void testThatOnlyNeededNumberOfActionsIsPurged() {
        final Target target = testdataFactory.createTarget();
        for (int i = 0; i < 18; i++) {
            DistributionSet distributionSet = testdataFactory.createDistributionSet();
            Rollout rollout = testdataFactory.createRolloutByVariables(
                    "rollout-" + i, "Description", 1, "controllerId==" + target.getControllerId(), distributionSet, "50", "50");
            rolloutManagement.start(rollout.getId());
            rolloutHandler.handleAll();
        }

        tenantConfigurationManagement().addOrUpdateConfiguration(TenantConfigurationKey.ACTION_CLEANUP_ON_QUOTA_HIT_PERCENTAGE, 25);
        deploymentManagement.handleMaxAssignmentsExceeded(target.getId(), 5L, new AssignmentQuotaExceededException());
        // only 3 actions should be deleted in such case :
        assertEquals(15, deploymentManagement.countActionsByTarget(target.getControllerId()));

        // should throw the quota exception if requested is bigger than the configured limit of actions purge
        assertThrows(AssignmentQuotaExceededException.class, () ->
                deploymentManagement.handleMaxAssignmentsExceeded(target.getId(), 10L, new AssignmentQuotaExceededException()));

    }

    private List<DeploymentRequest> createAssignmentRequests(
            final Collection<DistributionSet> distributionSets, final Collection<Target> targets, final int weight) {
        return createAssignmentRequests(distributionSets, targets, weight, false);
    }

    private List<DeploymentRequest> createAssignmentRequests(final Collection<DistributionSet> distributionSets,
            final Collection<Target> targets, final int weight, final boolean confirmationRequired) {
        final List<DeploymentRequest> deploymentRequests = new ArrayList<>();
        distributionSets.forEach(ds -> targets.forEach(target -> deploymentRequests.add(
                DeploymentRequest.builder(target.getControllerId(), ds.getId())
                        .weight(weight).confirmationRequired(confirmationRequired).build())));
        return deploymentRequests;
    }

    private JpaAction assignSet(final Target target, final DistributionSet ds) {
        assignDistributionSet(ds.getId(), target.getControllerId());
        implicitLock(ds);
        assertThat(targetManagement.getByControllerId(target.getControllerId()).getUpdateStatus())
                .as("wrong update status").isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(deploymentManagement.findAssignedDistributionSet(target.getControllerId()))
                .as("wrong assigned ds").contains(ds);
        final JpaAction action = actionRepository
                .findAll(
                        (root, query, cb) ->
                                cb.and(
                                        cb.equal(root.get(JpaAction_.target).get(JpaTarget_.id), target.getId()),
                                        cb.equal(root.get(JpaAction_.distributionSet).get(JpaDistributionSet_.id), ds.getId())),
                        PAGE).getContent().get(0);
        assertThat(action).as("action should not be null").isNotNull();
        return action;
    }

    private void assertDsExclusivelyAssignedToTargets(final List<Target> targets, final long dsId, final boolean active, final Status status) {
        final List<Action> assignment = findActionsByDistributionSet(PAGE, dsId).getContent();
        final String currentUsername = TenantAware.getCurrentUsername();

        assertThat(assignment).hasSize(10).allMatch(action -> action.isActive() == active)
                .as("Is assigned to DS " + dsId).allMatch(action -> action.getDistributionSet().getId().equals(dsId))
                .as("State is " + status).allMatch(action -> action.getStatus() == status)
                .as("Initiated by " + currentUsername).allMatch(a -> a.getInitiatedBy().equals(currentUsername));
        final long[] targetIds = targets.stream().mapToLong(Target::getId).toArray();
        assertThat(targetIds).as("All targets represented in assignment").containsExactlyInAnyOrder(
                assignment.stream().mapToLong(action -> action.getTarget().getId()).toArray());
    }

    private List<DistributionSetAssignmentResult> assignDistributionSetToTargets(
            final DistributionSet distributionSet, final Iterable<String> targetIds, final boolean confirmationRequired) {
        final List<DeploymentRequest> deploymentRequests = new ArrayList<>();
        for (final String controllerId : targetIds) {
            deploymentRequests.add(new DeploymentRequest(controllerId, distributionSet.getId(), ActionType.FORCED, 0,
                    null, null, null, null, confirmationRequired));
        }
        return deploymentManagement.assignDistributionSets(deploymentRequests);
    }

    private int getResultingActionCount(final List<DistributionSetAssignmentResult> results) {
        return results.stream().map(DistributionSetAssignmentResult::getTotal).reduce(0, Integer::sum);
    }

    /**
     * Helper methods that creates 2 lists of targets and a list of distribution
     * sets.
     * <p>
     * <b>All created distribution sets are assigned to all targets of the target
     * list deployedTargets.</b>
     *
     * @param undeployedTargetPrefix prefix to be used as target controller prefix
     * @param noOfUndeployedTargets number of targets which remain undeployed
     * @param deployedTargetPrefix prefix to be used as target controller prefix
     * @param noOfDeployedTargets number of targets to which the created distribution sets assigned
     * @param noOfDistributionSets number of distribution sets
     * @param distributionSetPrefix prefix for the created distribution sets
     * @return the {@link DeploymentResult} containing all created targets, the distribution sets, the corresponding IDs for later evaluation in
     *         tests
     */
    private DeploymentResult prepareComplexRepo(
            final String undeployedTargetPrefix, final int noOfUndeployedTargets,
            final String deployedTargetPrefix, final int noOfDeployedTargets, final int noOfDistributionSets,
            final String distributionSetPrefix) {
        final Iterable<Target> nakedTargets = testdataFactory.createTargets(noOfUndeployedTargets,
                undeployedTargetPrefix, "first description");

        List<Target> deployedTargets = testdataFactory.createTargets(noOfDeployedTargets, deployedTargetPrefix,
                "first description");

        // creating 10 DistributionSets
        final Collection<DistributionSet> dsList = testdataFactory.createDistributionSets(distributionSetPrefix, noOfDistributionSets);

        // assigning all DistributionSet to the Target in the list
        // deployedTargets
        for (final DistributionSet ds : dsList) {
            deployedTargets = assignDistributionSet(ds, deployedTargets).getAssignedEntity().stream().map(Action::getTarget).toList();
            implicitLock(ds);
        }

        return new DeploymentResult(deployedTargets, nakedTargets, dsList);
    }

    private Slice<Action> findActionsByDistributionSet(final Pageable pageable, final long distributionSetId) {
        distributionSetManagement.find(distributionSetId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSet.class, distributionSetId));
        return actionRepository.findAll(byDistributionSetId(distributionSetId), pageable).map(Action.class::cast);
    }

    @Getter
    private static class DeploymentResult {

        private final List<Long> deployedTargetIDs = new ArrayList<>();
        private final List<Long> undeployedTargetIDs = new ArrayList<>();
        private final List<Long> distributionSetIDs = new ArrayList<>();

        private final List<Target> undeployedTargets = new ArrayList<>();
        private final List<Target> deployedTargets = new ArrayList<>();
        private final List<DistributionSet> distributionSets = new ArrayList<>();

        private DeploymentResult(final Iterable<Target> deployedTs, final Iterable<Target> undeployedTs, final Iterable<DistributionSet> dss) {
            deployedTargets.addAll(toList(deployedTs));
            undeployedTargets.addAll(toList(undeployedTs));
            distributionSets.addAll(toList(dss));

            deployedTargets.forEach(t -> deployedTargetIDs.add(t.getId()));
            undeployedTargets.forEach(t -> undeployedTargetIDs.add(t.getId()));
            distributionSets.forEach(ds -> distributionSetIDs.add(ds.getId()));
        }
    }
}