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

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.validation.ConstraintViolationException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.repository.ActionStatusFields;
import org.eclipse.hawkbit.repository.DeploymentManagement;
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
import org.eclipse.hawkbit.repository.jpa.specifications.ActionSpecifications;
import org.eclipse.hawkbit.repository.jpa.specifications.DistributionSetSpecification;
import org.eclipse.hawkbit.repository.jpa.specifications.SpecificationsBuilder;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.DeploymentRequestBuilder;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
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
 */
@Feature("Component Tests - Repository")
@Story("Deployment Management")
class DeploymentManagementTest extends AbstractJpaIntegrationTest {

    private static final boolean STATE_ACTIVE = true;
    private static final boolean STATE_INACTIVE = false;

    @Test
    @Description("Tests that an exception is thrown when a target is assigned to an incomplete distribution set")
    public void verifyAssignTargetsToIncompleteDistribution() {
        final DistributionSet distributionSet = testdataFactory.createIncompleteDistributionSet();
        final Target target = testdataFactory.createTarget();

        assertThatExceptionOfType(IncompleteDistributionSetException.class)
                .as("Incomplete distributionSet should throw an exception")
                .isThrownBy(() -> assignDistributionSet(distributionSet, target));

    }

    @Test
    @Description("Tests that an exception is thrown when a target is assigned to an invalidated distribution set")
    public void verifyAssignTargetsToInvalidDistribution() {
        final DistributionSet distributionSet = testdataFactory.createAndInvalidateDistributionSet();
        final Target target = testdataFactory.createTarget();

        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception")
                .isThrownBy(() -> assignDistributionSet(distributionSet, target));

    }

    protected List<DeploymentRequest> createAssignmentRequests(final Collection<DistributionSet> distributionSets,
            final Collection<Target> targets, final int weight) {
        return createAssignmentRequests(distributionSets, targets, weight, false);
    }

    protected List<DeploymentRequest> createAssignmentRequests(final Collection<DistributionSet> distributionSets,
            final Collection<Target> targets, final int weight, final boolean confirmationRequired) {
        final List<DeploymentRequest> deploymentRequests = new ArrayList<>();
        distributionSets.forEach(ds -> targets.forEach(target -> deploymentRequests
                .add(DeploymentManagement.deploymentRequest(target.getControllerId(), ds.getId()).setWeight(weight)
                        .setConfirmationRequired(confirmationRequired).build())));
        return deploymentRequests;
    }

    @Test
    @Description("Verifies that management get access react as specified on calls for non existing entities by means " +
            "of Optional not present.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    void nonExistingEntityAccessReturnsNotPresent() {
        assertThat(deploymentManagement.findAction(1234L)).isNotPresent();
        assertThat(deploymentManagement.findActionWithDetails(NOT_EXIST_IDL)).isNotPresent();
    }

    @Test
    @Description("Verifies that management queries react as specified on calls for non existing entities " +
            " by means of throwing EntityNotFoundException.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1) })
    void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        final Target target = testdataFactory.createTarget();
        final String dsName = "DistributionSet";

        verifyThrownExceptionBy(() -> assignDistributionSet(NOT_EXIST_IDL, target.getControllerId()), dsName);

        verifyThrownExceptionBy(() -> deploymentManagement.cancelAction(NOT_EXIST_IDL), "Action");
        verifyThrownExceptionBy(() -> deploymentManagement.countActionsByTarget(NOT_EXIST_ID), "Target");
        verifyThrownExceptionBy(() -> deploymentManagement.countActionsByTarget("xxx", NOT_EXIST_ID), "Target");

        verifyThrownExceptionBy(() -> findActionsByDistributionSet(PAGE, NOT_EXIST_IDL),
                "DistributionSet");
        verifyThrownExceptionBy(() -> deploymentManagement.findActionsByTarget(NOT_EXIST_ID, PAGE), "Target");
        verifyThrownExceptionBy(() -> deploymentManagement.findActionsByTarget("id==*", NOT_EXIST_ID, PAGE), "Target");

        verifyThrownExceptionBy(() -> deploymentManagement.findActiveActionsByTarget(PAGE, NOT_EXIST_ID), "Target");
        verifyThrownExceptionBy(() -> deploymentManagement.findInActiveActionsByTarget(PAGE, NOT_EXIST_ID), "Target");
        verifyThrownExceptionBy(() -> deploymentManagement.forceQuitAction(NOT_EXIST_IDL), "Action");
        verifyThrownExceptionBy(() -> deploymentManagement.forceTargetAction(NOT_EXIST_IDL), "Action");
    }

    @Test
    @Description("Test verifies that the repistory retrieves the action including all defined (lazy) details.")
    void findActionWithLazyDetails() {
        final DistributionSet testDs = testdataFactory.createDistributionSet("TestDs", "1.0",
                new ArrayList<>());
        final List<Target> testTarget = testdataFactory.createTargets(1);
        // one action with one action status is generated
        final Long actionId = getFirstAssignedActionId(assignDistributionSet(testDs, testTarget));
        final Action action = deploymentManagement.findActionWithDetails(actionId).get();

        assertThat(action.getDistributionSet()).as("DistributionSet in action").isNotNull();
        assertThat(action.getTarget()).as("Target in action").isNotNull();
        assertThat(deploymentManagement.getAssignedDistributionSet(action.getTarget().getControllerId()).get())
                .as("AssignedDistributionSet of target in action").isNotNull();

    }

    @Test
    @Description("Test verifies that actions of a target are found by using id-based search.")
    void findActionByTargetId() {
        final DistributionSet testDs = testdataFactory.createDistributionSet("TestDs", "1.0",
                new ArrayList<>());
        final List<Target> testTarget = testdataFactory.createTargets(1);
        // one action with one action status is generated
        final Long actionId = getFirstAssignedActionId(assignDistributionSet(testDs, testTarget));

        // act
        final Slice<Action> actions = deploymentManagement.findActionsByTarget(testTarget.get(0).getControllerId(),
                PAGE);
        final Long count = deploymentManagement.countActionsByTarget(testTarget.get(0).getControllerId());

        assertThat(count).as("One Action for target").isEqualTo(1L).isEqualTo(actions.getContent().size());
        assertThat(actions.getContent().get(0).getId()).as("Action of target").isEqualTo(actionId);
    }

    @Test
    @Description("Test verifies that the 'max actions per target' quota is enforced.")
    void assertMaxActionsPerTargetQuotaIsEnforced() {

        final int maxActions = quotaManagement.getMaxActionsPerTarget();
        final Target testTarget = testdataFactory.createTarget();
        final DistributionSet ds1 = testdataFactory.createDistributionSet("ds1");

        enableMultiAssignments();
        for (int i = 0; i < maxActions; i++) {
            deploymentManagement.offlineAssignedDistributionSets(Collections
                    .singletonList(new SimpleEntry<String, Long>(testTarget.getControllerId(), ds1.getId())));
        }

        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> assignDistributionSet(ds1.getId(), testTarget.getControllerId(), 77));
    }

    @Test
    @Description("An assignment request with more assignments than allowed by 'maxTargetDistributionSetAssignmentsPerManualAssignment' quota throws an exception.")
    void assignmentRequestThatIsTooLarge() {
        final int maxActions = quotaManagement.getMaxTargetDistributionSetAssignmentsPerManualAssignment();
        final DistributionSet ds1 = testdataFactory.createDistributionSet("1");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2");

        final List<Target> targets = testdataFactory.createTargets(maxActions, "assignmentTest1");
        assignDistributionSet(ds1, targets);

        targets.add(testdataFactory.createTarget("assignmentTest2"));
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> assignDistributionSet(ds2, targets));
    }

    @Test
    @Description("Test verifies that action-states of an action are found by using id-based search.")
    void findActionStatusByActionId() {
        final DistributionSet testDs = testdataFactory.createDistributionSet("TestDs", "1.0", Collections.emptyList());
        final List<Target> testTarget = testdataFactory.createTargets(1);
        // one action with one action status is generated
        final Long actionId = getFirstAssignedActionId(assignDistributionSet(testDs, testTarget));
        final Slice<Action> actions = deploymentManagement.findActionsByTarget(testTarget.get(0).getControllerId(),
                PAGE);
        final ActionStatus expectedActionStatus = ((JpaAction) actions.getContent().get(0)).getActionStatus().get(0);

        // act
        final Page<ActionStatus> actionStates = deploymentManagement.findActionStatusByAction(PAGE, actionId);

        assertThat(actionStates.getContent()).hasSize(1);
        assertThat(actionStates.getContent().get(0)).as("Action-status of action").isEqualTo(expectedActionStatus);
    }

    @Test
    @Description("Test verifies that messages of an action-status are found by using id-based search.")
    void findMessagesByActionStatusId() {
        final DistributionSet testDs = testdataFactory.createDistributionSet("TestDs", "1.0",
                new ArrayList<DistributionSetTag>());
        final List<Target> testTarget = testdataFactory.createTargets(1);
        // one action with one action status is generated
        final Long actionId = getFirstAssignedActionId(assignDistributionSet(testDs, testTarget));
        // create action-status entry with one message
        controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId)
                .status(Action.Status.FINISHED).messages(Collections.singletonList("finished message")));
        final Page<ActionStatus> actionStates = deploymentManagement.findActionStatusByAction(PAGE, actionId);

        // find newly created action-status entry with message
        final JpaActionStatus actionStatusWithMessage = actionStates.getContent().stream().map(c -> (JpaActionStatus) c)
                .filter(entry -> entry.getMessages() != null && entry.getMessages().size() > 0).findFirst().get();
        final String expectedMsg = actionStatusWithMessage.getMessages().get(0);

        // act
        final Page<String> messages = deploymentManagement.findMessagesByActionStatusId(PAGE, actionStatusWithMessage.getId());

        assertThat(actionStates.getTotalElements()).as("Two action-states in total").isEqualTo(2L);
        assertThat(messages.getContent().get(0)).as("Message of action-status").isEqualTo(expectedMsg);
    }

    @Test
    @Description("Ensures that tag to distribution set assignment that does not exist will cause EntityNotFoundException.")
    void assignDistributionSetToTagThatDoesNotExistThrowsException() {
        final List<Long> assignDS = new ArrayList<>(5);
        for (int i = 0; i < 4; i++) {
            assignDS.add(testdataFactory.createDistributionSet("DS" + i, "1.0", Collections.emptyList()).getId());
        }
        // not exists
        assignDS.add(100L);

        final DistributionSetTag tag = distributionSetTagManagement.create(entityFactory.tag().create().name("Tag1"));

        assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> distributionSetManagement.assignTag(assignDS, tag.getId()))
                .withMessageContaining("DistributionSet").withMessageContaining(String.valueOf(100L));
    }

    @Test
    @Description("Test verifies that an assignment with automatic cancelation works correctly even if the update is split into multiple partitions on the database.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 20),
            @Expect(type = TargetUpdatedEvent.class, count = 40),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 2),
            @Expect(type = ActionCreatedEvent.class, count = 40),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 20),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 2), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 6) })
        // implicit lock })
    void multiAssigmentHistoryOverMultiplePagesResultsInTwoActiveAction() {

        final DistributionSet cancelDs = testdataFactory.createDistributionSet("Canceled DS", "1.0",
                Collections.emptyList());

        final DistributionSet cancelDs2 = testdataFactory.createDistributionSet("Canceled DS", "1.2",
                Collections.emptyList());

        final List<Target> targets = testdataFactory.createTargets(quotaManagement.getMaxTargetsPerAutoAssignment());

        assertThat(deploymentManagement.countActionsAll()).isZero();

        assignDistributionSet(cancelDs, targets).getAssignedEntity();
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(quotaManagement.getMaxTargetsPerAutoAssignment());
        assignDistributionSet(cancelDs2, targets).getAssignedEntity();
        assertThat(deploymentManagement.countActionsAll())
                .isEqualTo(2L * quotaManagement.getMaxTargetsPerAutoAssignment());
    }

    @Test
    @Description("Cancels multiple active actions on a target. Expected behaviour is that with two active "
            + "actions after canceling the second active action the first one is still running as it is not touched by the cancelation. After canceling the first one "
            + "also the target goes back to IN_SYNC as no open action is left.")
    void manualCancelWithMultipleAssignmentsCancelLastOneFirst() {
        final Action action = prepareFinishedUpdate("4712", "installed", true);
        final Target target = action.getTarget();
        final DistributionSet dsFirst = testdataFactory.createDistributionSet("", true);
        final DistributionSet dsSecond = testdataFactory.createDistributionSet("2", true);
        final DistributionSet dsInstalled = action.getDistributionSet();

        // check initial status
        assertThat(targetManagement.getByControllerID("4712").get().getUpdateStatus()).as("target has update status")
                .isEqualTo(TargetUpdateStatus.IN_SYNC);

        // assign the two sets in a row
        JpaAction firstAction = assignSet(target, dsFirst);
        JpaAction secondAction = assignSet(target, dsSecond);

        assertThat(actionRepository.findAll()).as("wrong size of actions").hasSize(3);
        assertThat(actionStatusRepository.findAll()).as("wrong size of action status").hasSize(5);

        // we cancel second -> back to first
        deploymentManagement.cancelAction(secondAction.getId());
        secondAction = (JpaAction) deploymentManagement.findActionWithDetails(secondAction.getId()).get();
        // confirm cancellation
        controllerManagement.addCancelActionStatus(
                entityFactory.actionStatus().create(secondAction.getId()).status(Status.CANCELED));
        assertThat(actionStatusRepository.findAll()).as("wrong size of actions status").hasSize(7);
        assertThat(deploymentManagement.getAssignedDistributionSet("4712")).as("wrong ds").contains(dsFirst);
        assertThat(targetManagement.getByControllerID("4712").get().getUpdateStatus()).as("wrong update status")
                .isEqualTo(TargetUpdateStatus.PENDING);

        // we cancel first -> back to installed
        deploymentManagement.cancelAction(firstAction.getId());
        firstAction = (JpaAction) deploymentManagement.findActionWithDetails(firstAction.getId()).get();
        // confirm cancellation
        controllerManagement.addCancelActionStatus(
                entityFactory.actionStatus().create(firstAction.getId()).status(Status.CANCELED));
        assertThat(actionStatusRepository.findAll()).as("wrong size of action status").hasSize(9);
        assertThat(deploymentManagement.getAssignedDistributionSet("4712")).as("wrong assigned ds")
                .contains(dsInstalled);
        assertThat(targetManagement.getByControllerID("4712").get().getUpdateStatus()).as("wrong update status")
                .isEqualTo(TargetUpdateStatus.IN_SYNC);
    }

    @Test
    @Description("Cancels multiple active actions on a target. Expected behaviour is that with two active "
            + "actions after canceling the first active action the system switched to second one. After canceling this one "
            + "also the target goes back to IN_SYNC as no open action is left.")
    void manualCancelWithMultipleAssignmentsCancelMiddleOneFirst() {
        final Action action = prepareFinishedUpdate("4712", "installed", true);
        final Target target = action.getTarget();
        final DistributionSet dsFirst = testdataFactory.createDistributionSet("", true);
        final DistributionSet dsSecond = testdataFactory.createDistributionSet("2", true);
        final DistributionSet dsInstalled = action.getDistributionSet();

        // check initial status
        assertThat(targetManagement.getByControllerID("4712").get().getUpdateStatus()).as("wrong update status")
                .isEqualTo(TargetUpdateStatus.IN_SYNC);

        // assign the two sets in a row
        JpaAction firstAction = assignSet(target, dsFirst);
        JpaAction secondAction = assignSet(target, dsSecond);

        assertThat(actionRepository.findAll()).as("wrong size of actions").hasSize(3);
        assertThat(actionStatusRepository.findAll()).as("wrong size of action status").hasSize(5);

        // we cancel first -> second is left
        deploymentManagement.cancelAction(firstAction.getId());
        // confirm cancellation
        firstAction = (JpaAction) deploymentManagement.findActionWithDetails(firstAction.getId()).get();
        controllerManagement.addCancelActionStatus(
                entityFactory.actionStatus().create(firstAction.getId()).status(Status.CANCELED));
        assertThat(actionStatusRepository.findAll()).as("wrong size of action status").hasSize(7);
        assertThat(deploymentManagement.getAssignedDistributionSet("4712")).as("wrong assigned ds").contains(dsSecond);
        assertThat(targetManagement.getByControllerID("4712").get().getUpdateStatus()).as("wrong target update status")
                .isEqualTo(TargetUpdateStatus.PENDING);

        // we cancel second -> remain assigned until finished cancellation
        deploymentManagement.cancelAction(secondAction.getId());
        secondAction = (JpaAction) deploymentManagement.findActionWithDetails(secondAction.getId()).get();
        assertThat(actionStatusRepository.findAll()).as("wrong size of action status").hasSize(8);
        assertThat(deploymentManagement.getAssignedDistributionSet("4712")).as("wrong assigned ds").contains(dsSecond);
        // confirm cancellation
        controllerManagement.addCancelActionStatus(
                entityFactory.actionStatus().create(secondAction.getId()).status(Status.CANCELED));
        // cancelled success -> back to dsInstalled
        assertThat(deploymentManagement.getAssignedDistributionSet("4712")).as("wrong installed ds")
                .contains(dsInstalled);
        assertThat(targetManagement.getByControllerID("4712").get().getUpdateStatus())
                .as("wrong target info update status").isEqualTo(TargetUpdateStatus.IN_SYNC);
    }

    @Test
    @Description("Force Quit an Assignment. Expected behaviour is that the action is canceled and is marked as deleted. The assigned Software module")
    void forceQuitSetActionToInactive() throws InterruptedException {
        final Action action = prepareFinishedUpdate("4712", "installed", true);
        final Target target = action.getTarget();
        final DistributionSet dsInstalled = action.getDistributionSet();

        final DistributionSet ds = testdataFactory.createDistributionSet("newDS", true);

        // verify initial status
        assertThat(targetManagement.getByControllerID("4712").get().getUpdateStatus())
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
        assertThat(deploymentManagement.getAssignedDistributionSet("4712")).as("wrong assigned ds")
                .contains(dsInstalled);
        assertThat(targetManagement.getByControllerID("4712").get().getUpdateStatus()).as("wrong target update status")
                .isEqualTo(TargetUpdateStatus.IN_SYNC);
    }

    @Test
    @Description("Force Quit an not canceled Assignment. Expected behaviour is that the action can not be force quit and there is thrown an exception.")
    void forceQuitNotAllowedThrowsException() {
        final Action action = prepareFinishedUpdate("4712", "installed", true);
        // verify initial status
        assertThat(targetManagement.getByControllerID("4712").get().getUpdateStatus()).as("wrong update status")
                .isEqualTo(TargetUpdateStatus.IN_SYNC);

        final Target target = action.getTarget();
        final DistributionSet ds = testdataFactory.createDistributionSet("newDS", true);
        final Action assigningAction = assignSet(target, ds);

        // verify assignment
        assertThat(actionRepository.findAll()).as("wrong size of action").hasSize(2);
        assertThat(actionStatusRepository.findAll()).as("wrong size of action status").hasSize(4);

        // force quit assignment
        assertThatExceptionOfType(ForceQuitActionNotAllowedException.class)
                .as("expected ForceQuitActionNotAllowedException")
                .isThrownBy(() -> deploymentManagement.forceQuitAction(assigningAction.getId()));
    }

    @Test
    @Description("Simple offline deployment of a distribution set to a list of targets. Verifies that offline assigment "
            + "is correctly executed for targets that do not have a running update already. Those are ignored.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 20),
            @Expect(type = TargetUpdatedEvent.class, count = 20), @Expect(type = ActionCreatedEvent.class, count = 20),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 2), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 6), // implicit lock
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void assignedDistributionSet() {

        final List<String> controllerIds = testdataFactory.createTargets(10).stream().map(Target::getControllerId)
                .collect(Collectors.toList());
        final List<Target> onlineAssignedTargets = testdataFactory.createTargets(10, "2");
        controllerIds.addAll(onlineAssignedTargets.stream().map(Target::getControllerId).collect(Collectors.toList()));

        final DistributionSet ds = testdataFactory.createDistributionSet();
        assignDistributionSet(testdataFactory.createDistributionSet("2"), onlineAssignedTargets);

        final long current = System.currentTimeMillis();

        final List<Entry<String, Long>> offlineAssignments = controllerIds.stream()
                .map(targetId -> new SimpleEntry<String, Long>(targetId, ds.getId())).collect(Collectors.toList());
        final List<DistributionSetAssignmentResult> assignmentResults = deploymentManagement
                .offlineAssignedDistributionSets(offlineAssignments);
        assertThat(assignmentResults).hasSize(1);
        final List<Target> targets = assignmentResults.get(0).getAssignedEntity().stream().map(Action::getTarget)
                .collect(Collectors.toList());

        assertThat(actionRepository.count()).isEqualTo(20);
        assertThat(findActionsByDistributionSet(PAGE, ds.getId())).as("Offline actions are not active")
                .allMatch(action -> !action.isActive()).as("Actions should be initiated by current user")
                .allMatch(a -> a.getInitiatedBy().equals(tenantAware.getCurrentUsername()));

        assertThat(targetManagement.findByInstalledDistributionSet(PAGE, ds.getId()).getContent())
                .usingElementComparator(controllerIdComparator()).containsAll(targets).hasSize(10)
                .containsAll(targetManagement.findByAssignedDistributionSet(PAGE, ds.getId()))
                .as("InstallationDate set").allMatch(target -> target.getInstallationDate() >= current)
                .as("TargetUpdateStatus IN_SYNC")
                .allMatch(target -> TargetUpdateStatus.IN_SYNC.equals(target.getUpdateStatus()))
                .as("InstallationDate equal to LastModifiedAt")
                .allMatch(target -> target.getLastModifiedAt() == target.getInstallationDate());
    }

    @Test
    @Description("Offline assign multiple DSs to a single Target in multiassignment mode.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 4), @Expect(type = ActionCreatedEvent.class, count = 4),
            @Expect(type = DistributionSetCreatedEvent.class, count = 4),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 12),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 4), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 12), // implicit lock
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1) })
    void multiOfflineAssignment() {
        final List<String> targetIds = testdataFactory.createTargets(1).stream().map(Target::getControllerId)
                .collect(Collectors.toList());
        final List<Long> dsIds = testdataFactory.createDistributionSets(4).stream().map(DistributionSet::getId)
                .collect(Collectors.toList());

        enableMultiAssignments();
        final List<Entry<String, Long>> offlineAssignments = new ArrayList<>();
        targetIds.forEach(targetId -> dsIds
                .forEach(dsId -> offlineAssignments.add(new SimpleEntry<String, Long>(targetId, dsId))));
        final List<DistributionSetAssignmentResult> assignmentResults = deploymentManagement
                .offlineAssignedDistributionSets(offlineAssignments);

        assertThat(getResultingActionCount(assignmentResults)).isEqualTo(4);
        targetIds.forEach(controllerId -> {
            final List<Long> assignedDsIds = deploymentManagement.findActionsByTarget(controllerId, PAGE).stream()
                    .map(a -> {
                        // don't use peek since it is by documentation mainly for debugging and could be skipped in some cases
                        assertThat(a.getInitiatedBy()).as("Actions should be initiated by current user")
                                .isEqualTo(tenantAware.getCurrentUsername());
                        return a;
                    })
                    .map(action -> action.getDistributionSet().getId()).collect(Collectors.toList());
            assertThat(assignedDsIds).containsExactlyInAnyOrderElementsOf(dsIds);
        });
    }

    @Test
    @Description("Verifies that if an account is set to action autoclose running actions in case of a new assigned set get closed and set to CANCELED.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 10),
            @Expect(type = TargetUpdatedEvent.class, count = 20), @Expect(type = ActionCreatedEvent.class, count = 20),
            @Expect(type = ActionUpdatedEvent.class, count = 10),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 2), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 6), // implicit lock
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 2),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1),
            @Expect(type = TenantConfigurationUpdatedEvent.class, count = 1) })
    void assignDistributionSetAndAutoCloseActiveActions() {
        tenantConfigurationManagement
                .addOrUpdateConfiguration(TenantConfigurationKey.REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED, true);

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

            assertThat(targetManagement.findByAssignedDistributionSet(PAGE, ds2.getId()).getContent()).hasSize(10)
                    .as("InstallationDate not set").allMatch(target -> (target.getInstallationDate() == null));

        } finally {
            tenantConfigurationManagement
                    .addOrUpdateConfiguration(TenantConfigurationKey.REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED, false);
        }
    }

    @Test
    @Description("If multi-assignment is enabled, verify that the previous Distribution Set assignment is not canceled when a new one is assigned.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 10),
            @Expect(type = TargetUpdatedEvent.class, count = 20), @Expect(type = ActionCreatedEvent.class, count = 20),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 2), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 6), // implicit lock
            @Expect(type = MultiActionAssignEvent.class, count = 2),
            @Expect(type = MultiActionCancelEvent.class, count = 0),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 0),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1) })
    void previousAssignmentsAreNotCanceledInMultiAssignMode() {
        enableMultiAssignments();
        final List<Target> targets = testdataFactory.createTargets(10);
        final List<String> targetIds = targets.stream().map(Target::getControllerId).collect(Collectors.toList());

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

    @Test
    @Description("Assign multiple DSs to a single Target in one request in multiassignment mode.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 4), @Expect(type = ActionCreatedEvent.class, count = 4),
            @Expect(type = DistributionSetCreatedEvent.class, count = 4),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 12),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 4), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 12), // implicit lock
            @Expect(type = MultiActionAssignEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 0),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1) })
    void multiAssignmentInOneRequest() {
        final List<Target> targets = testdataFactory.createTargets(1);
        final List<DistributionSet> distributionSets = testdataFactory.createDistributionSets(4);
        final List<DeploymentRequest> deploymentRequests = createAssignmentRequests(distributionSets, targets, 34);

        enableMultiAssignments();
        final List<DistributionSetAssignmentResult> results = deploymentManagement
                .assignDistributionSets(deploymentRequests);

        assertThat(getResultingActionCount(results)).isEqualTo(deploymentRequests.size());
        final List<Long> dsIds = distributionSets.stream().map(DistributionSet::getId).collect(Collectors.toList());
        targets.forEach(target -> {
            final List<Long> assignedDsIds = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE)
                    .stream()
                    .map(a -> {
                        // don't use peek since it is by documentation mainly for debugging and could be skipped in some cases
                        assertThat(a.getInitiatedBy()).as("Initiated by current user")
                                .isEqualTo(tenantAware.getCurrentUsername());
                        return a;
                    })
                    .map(action -> action.getDistributionSet().getId()).collect(Collectors.toList());
            assertThat(assignedDsIds).containsExactlyInAnyOrderElementsOf(dsIds);
        });
    }

    @Test
    @Description("Assign multiple DSs to single Target in one request in multiAssignment mode and cancel each created action afterwards.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 4), @Expect(type = ActionCreatedEvent.class, count = 4),
            @Expect(type = DistributionSetCreatedEvent.class, count = 4),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 12),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 4), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 12), // implicit lock
            @Expect(type = MultiActionAssignEvent.class, count = 1),
            @Expect(type = MultiActionCancelEvent.class, count = 4),
            @Expect(type = ActionUpdatedEvent.class, count = 4),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 0),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1) })
    void cancelMultiAssignmentActions() {
        final List<Target> targets = testdataFactory.createTargets(1);
        final List<DistributionSet> distributionSets = testdataFactory.createDistributionSets(4);
        final List<DeploymentRequest> deploymentRequests = createAssignmentRequests(distributionSets, targets, 34,
                false);

        enableMultiAssignments();
        final List<DistributionSetAssignmentResult> results = deploymentManagement
                .assignDistributionSets(deploymentRequests);

        assertThat(getResultingActionCount(results)).isEqualTo(deploymentRequests.size());

        final List<Long> dsIds = distributionSets.stream().map(DistributionSet::getId).collect(Collectors.toList());
        targets.forEach(target -> {
            deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).forEach(action -> {
                assertThat(action.getDistributionSet().getId()).isIn(dsIds);
                assertThat(action.getInitiatedBy()).as("Should be Initiated by current user")
                        .isEqualTo(tenantAware.getCurrentUsername());
                deploymentManagement.cancelAction(action.getId());
            });
        });
    }

    @Test
    @Description("A Request resulting in multiple assignments to a single target is only allowed when multiassignment is enabled.")
    void multipleAssignmentsToTargetOnlyAllowedInMultiAssignMode() {
        final Target target = testdataFactory.createTarget();
        final List<DistributionSet> distributionSets = testdataFactory.createDistributionSets(2);

        final DeploymentRequest targetToDS0 = DeploymentManagement
                .deploymentRequest(target.getControllerId(), distributionSets.get(0).getId()).setWeight(78).build();

        final DeploymentRequest targetToDS1 = DeploymentManagement
                .deploymentRequest(target.getControllerId(), distributionSets.get(1).getId()).setWeight(565).build();

        Assertions.assertThatExceptionOfType(MultiAssignmentIsNotEnabledException.class)
                .isThrownBy(() -> deploymentManagement.assignDistributionSets(Arrays.asList(targetToDS0, targetToDS1)));

        enableMultiAssignments();
        assertThat(getResultingActionCount(
                deploymentManagement.assignDistributionSets(Arrays.asList(targetToDS0, targetToDS1)))).isEqualTo(2);
    }

    @Test
    @Description("Assigning distribution set to the list of targets with a non-existing one leads to successful assignment of valid targets, while not found targets are silently ignored.")
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

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    @Description("Assignments with confirmation flow active will result in actions in 'WAIT_FOR_CONFIRMATION' state")
    void assignmentWithConfirmationFlowActive(final boolean confirmationRequired) {
        final List<String> controllerIds = testdataFactory.createTargets(1).stream().map(Target::getControllerId)
                .collect(Collectors.toList());
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();

        enableConfirmationFlow();
        List<DistributionSetAssignmentResult> results = assignDistributionSetToTargets(distributionSet, controllerIds,
                confirmationRequired);

        assertThat(getResultingActionCount(results)).isEqualTo(controllerIds.size());

        controllerIds.forEach(controllerId -> {
            deploymentManagement.findActionsByTarget(controllerId, PAGE).forEach(action -> {
                assertThat(action.getDistributionSet().getId()).isIn(distributionSet.getId());
                assertThat(action.getInitiatedBy()).as("Should be Initiated by current user")
                        .isEqualTo(tenantAware.getCurrentUsername());
                if (confirmationRequired) {
                    assertThat(action.getStatus()).isEqualTo(Status.WAIT_FOR_CONFIRMATION);
                } else {
                    assertThat(action.getStatus()).isEqualTo(RUNNING);
                }
            });
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    @Description("Verify auto confirmation assignments and check action status with messages")
    void assignmentWithAutoConfirmationWillBeHandledCorrectly(final boolean confirmationRequired) {
        enableConfirmationFlow();

        final Target target = testdataFactory.createTarget();
        assertThat(target.getAutoConfirmationStatus()).isNull();

        confirmationManagement.activateAutoConfirmation(target.getControllerId(), "not_bumlux", "my personal remark");

        assertThat(targetManagement.getByControllerID(target.getControllerId()))
                .hasValueSatisfying(t -> assertThat(t.getAutoConfirmationStatus()).isNotNull());

        final DistributionSet distributionSet = testdataFactory.createDistributionSet();

        assignDistributionSets(Collections
                .singletonList(new DeploymentRequestBuilder(target.getControllerId(), distributionSet.getId())
                        .setConfirmationRequired(confirmationRequired).build()));

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
                                            .contains("Assignment automatically confirmed by initiator 'not_bumlux'. \n"
                                                    + "\n" + "Auto confirmation activated by system user: 'bumlux' \n"
                                                    + "\n" + "Remark: my personal remark");
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

    @Test
    @Description("Multiple assignments with confirmation flow active will result in correct cancel behaviour")
    void multipleAssignmentWithConfirmationFlowActiveVerifyCancelBehaviour() {
        final Target target = testdataFactory.createTarget("firstDevice");
        final DistributionSet firstDs = testdataFactory.createDistributionSet();
        final DistributionSet secondDs = testdataFactory.createDistributionSet();

        enableConfirmationFlow();
        final List<Action> resultActions = assignDistributionSet(firstDs.getId(), target.getControllerId())
                .getAssignedEntity();
        assertThat(resultActions).hasSize(1);

        assertThat(resultActions.get(0)).satisfies(action -> {
            assertThat(action.getDistributionSet().getId()).isEqualTo(firstDs.getId());
            assertThat(action.getStatus()).isEqualTo(Status.WAIT_FOR_CONFIRMATION);
        });

        final List<Action> resultActions2 = assignDistributionSet(secondDs.getId(), target.getControllerId())
                .getAssignedEntity();

        assertThat(resultActions2).hasSize(1);
        assertThat(resultActions2.get(0)).satisfies(action -> {
            assertThat(action.getDistributionSet().getId()).isEqualTo(secondDs.getId());
            assertThat(action.getStatus()).isEqualTo(Status.WAIT_FOR_CONFIRMATION);
        });

        final List<Action> actions = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE)
                .getContent();
        assertThat(actions).hasSize(2)
                .anyMatch(action -> Objects.equals(action.getDistributionSet().getId(), firstDs.getId())
                        && action.getStatus() == Status.CANCELING)
                .anyMatch(action -> Objects.equals(action.getDistributionSet().getId(), secondDs.getId())
                        && action.getStatus() == Status.WAIT_FOR_CONFIRMATION);

    }

    @Test
    @Description("Assignments with confirmation flow deactivated will result in actions in only in 'RUNNING' state")
    void verifyConfirmationRequiredFlagHaveNoInfluenceIfFlowIsDeactivated() {
        final List<String> targets1 = testdataFactory.createTargets("group1", 1).stream().map(Target::getControllerId)
                .collect(Collectors.toList());
        final List<String> targets2 = testdataFactory.createTargets("group2", 1).stream().map(Target::getControllerId)
                .collect(Collectors.toList());
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();

        final List<DistributionSetAssignmentResult> results = Stream
                .concat(assignDistributionSetToTargets(distributionSet, targets1, true).stream(), //
                        assignDistributionSetToTargets(distributionSet, targets2, false).stream()) //
                .collect(Collectors.toList());

        final List<String> controllerIds = Stream.concat(targets1.stream(), targets2.stream())
                .collect(Collectors.toList());

        assertThat(getResultingActionCount(results)).isEqualTo(controllerIds.size());

        controllerIds.forEach(controllerId -> {
            deploymentManagement.findActionsByTarget(controllerId, PAGE).forEach(action -> {
                assertThat(action.getDistributionSet().getId()).isIn(distributionSet.getId());
                assertThat(action.getInitiatedBy()).as("Should be Initiated by current user")
                        .isEqualTo(tenantAware.getCurrentUsername());
                assertThat(action.getStatus()).isEqualTo(RUNNING);
            });
        });
    }

    @Test
    @Description("Duplicate Assignments are removed from a request when multiassignment is disabled, otherwise not")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 2), @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = MultiActionAssignEvent.class, count = 1),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1) })
    void duplicateAssignmentsInRequestAreRemovedIfMultiassignmentEnabled() {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();
        final List<DeploymentRequest> twoEqualAssignments = Collections.nCopies(2,
                DeploymentManagement.deploymentRequest(targetId, dsId).build());

        assertThat(getResultingActionCount(deploymentManagement.assignDistributionSets(twoEqualAssignments)))
                .isEqualTo(1);

        enableMultiAssignments();
        final List<DeploymentRequest> twoEqualAssignmentsWithWeight = Collections.nCopies(2,
                DeploymentManagement.deploymentRequest(targetId, dsId).setWeight(555).build());

        assertThat(getResultingActionCount(deploymentManagement.assignDistributionSets(twoEqualAssignmentsWithWeight)))
                .isEqualTo(1);
    }

    @Test
    @Description("An assignment request is not accepted if it would lead to a target exceeding the max actions per target quota.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 21), // max actions per target are 20 for test
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3 * 21),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 0),
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
            deploymentRequests.add(
                    DeploymentManagement.deploymentRequest(controllerId, dsId).setWeight(24).build());
        }

        enableMultiAssignments();
        Assertions.assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> deploymentManagement.assignDistributionSets(deploymentRequests));
        assertThat(actionRepository.countByTargetControllerId(controllerId)).isZero();
    }

    @Test
    @Description("An assignment request without a weight is ok when multi assignment in enabled.")
    void weightNotRequiredInMultiAssignmentMode() {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final DeploymentRequest assignWithoutWeight = DeploymentManagement.deploymentRequest(targetId, dsId).build();
        final DeploymentRequest assignWithWeight = DeploymentManagement.deploymentRequest(targetId, dsId).setWeight(567).build();

        enableMultiAssignments();
        assertThat(deploymentManagement.assignDistributionSets(List.of(assignWithoutWeight, assignWithWeight))).isNotNull();
    }

    @Test
    @Description("An assignment request containing a weight don't causes an error when multi assignment in disabled.")
    void weightAllowedWhenMultiAssignmentModeNotEnabled() {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final DeploymentRequest assignWithoutWeight = DeploymentManagement.deploymentRequest(targetId, dsId).setWeight(456).build();
        assertThat(deploymentManagement.assignDistributionSets(Collections.singletonList(assignWithoutWeight))).isNotNull().size().isEqualTo(1);
    }

    @Test
    @Description("Weights are validated and contained in the resulting Action.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 2), @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = MultiActionAssignEvent.class, count = 2),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1) })
    void weightValidatedAndSaved() {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();
        final DeploymentRequest valideRequest1 = DeploymentManagement.deploymentRequest(targetId, dsId)
                .setWeight(Action.WEIGHT_MAX).build();
        final DeploymentRequest valideRequest2 = DeploymentManagement.deploymentRequest(targetId, dsId)
                .setWeight(Action.WEIGHT_MIN).build();
        final DeploymentRequest weightTooLow = DeploymentManagement.deploymentRequest(targetId, dsId)
                .setWeight(Action.WEIGHT_MIN - 1).build();
        final DeploymentRequest weightTooHigh = DeploymentManagement.deploymentRequest(targetId, dsId)
                .setWeight(Action.WEIGHT_MAX + 1).build();
        enableMultiAssignments();
        Assertions.assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> deploymentManagement.assignDistributionSets(Collections.singletonList(weightTooLow)));
        Assertions.assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(
                () -> deploymentManagement.assignDistributionSets(Collections.singletonList(weightTooHigh)));
        final Long valideActionId1 = getFirstAssignedAction(
                deploymentManagement.assignDistributionSets(Collections.singletonList(valideRequest1)).get(0)).getId();
        final Long valideActionId2 = getFirstAssignedAction(
                deploymentManagement.assignDistributionSets(Collections.singletonList(valideRequest2)).get(0)).getId();
        assertThat(actionRepository.findWithDetailsById(valideActionId1).get().getWeight()).get().isEqualTo(Action.WEIGHT_MAX);
        assertThat(actionRepository.findWithDetailsById(valideActionId2).get().getWeight()).get().isEqualTo(Action.WEIGHT_MIN);
    }

    /**
     * test a simple deployment by calling the
     * {@link TargetRepository#assignDistributionSet(DistributionSet, Iterable)} and
     * checking the active action and the action history of the targets.
     */
    @Test
    @Description("Simple deployment or distribution set to target assignment test.")
    @ExpectEvents({ @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = TargetCreatedEvent.class, count = 30), @Expect(type = ActionCreatedEvent.class, count = 20),
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
                .allMatch(a -> a.getInitiatedBy().equals(tenantAware.getCurrentUsername()));

        final Iterable<Target> allFoundTargets = targetManagement.findAll(PAGE).getContent();

        // get final updated version of targets
        savedDeployedTargets = targetManagement.getByControllerID(
                savedDeployedTargets.stream().map(target -> target.getControllerId()).collect(Collectors.toList()));

        assertThat(allFoundTargets).as("founded targets are wrong").containsAll(savedDeployedTargets)
                .containsAll(savedNakedTargets);
        assertThat(savedDeployedTargets).as("saved target are wrong")
                .doesNotContain(toArray(savedNakedTargets, Target.class));
        assertThat(savedNakedTargets).as("saved target are wrong")
                .doesNotContain(toArray(savedDeployedTargets, Target.class));

        for (final Target myt : savedNakedTargets) {
            final Target t = targetManagement.getByControllerID(myt.getControllerId()).get();
            assertThat(deploymentManagement.countActionsByTarget(t.getControllerId())).as("action should be empty")
                    .isZero();
        }

        for (final Target myt : savedDeployedTargets) {
            final Target t = targetManagement.getByControllerID(myt.getControllerId()).get();
            final List<Action> activeActionsByTarget = deploymentManagement
                    .findActiveActionsByTarget(PAGE, t.getControllerId()).getContent();
            assertThat(activeActionsByTarget).as("action should not be empty").isNotEmpty();
            assertThat(t.getUpdateStatus()).as("wrong target update status").isEqualTo(TargetUpdateStatus.PENDING);
            for (final Action ua : activeActionsByTarget) {
                assertThat(ua.getDistributionSet()).as("action has wrong ds").isEqualTo(ds);
            }
        }
    }

    @Test
    @Description("Test that it is not possible to assign a distribution set that is not complete.")
    @ExpectEvents({ @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 2),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 2), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 2), // implicit lock
            @Expect(type = TargetCreatedEvent.class, count = 10), @Expect(type = ActionCreatedEvent.class, count = 10),
            @Expect(type = TargetUpdatedEvent.class, count = 10) })
    void failDistributionSetAssigmentThatIsNotComplete() throws InterruptedException {
        final List<Target> targets = testdataFactory.createTargets(10);

        final SoftwareModule ah = testdataFactory.createSoftwareModuleApp();
        final SoftwareModule os = testdataFactory.createSoftwareModuleOs();

        final DistributionSet incomplete = distributionSetManagement.create(entityFactory.distributionSet().create()
                .name("incomplete").version("v1").type(standardDsType).modules(Collections.singletonList(ah.getId())));

        assertThatExceptionOfType(IncompleteDistributionSetException.class)
                .as("expected IncompleteDistributionSetException")
                .isThrownBy(() -> assignDistributionSet(incomplete, targets));

        final DistributionSet nowComplete = distributionSetManagement.assignSoftwareModules(incomplete.getId(),
                Set.of(os.getId()));

        assertThat(assignDistributionSet(nowComplete, targets).getAssigned()).as("assign ds doesn't work")
                .isEqualTo(10);
    }

    @Test
    @Description("Multiple deployments or distribution set to target assignment test. Expected behaviour is that a new deployment "
            + "overides unfinished old one which are canceled as part of the operation.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 5 + 4),
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
                .allMatch(a -> a.getInitiatedBy().equals(tenantAware.getCurrentUsername()));
        // and verify the number
        assertThat(page.getTotalElements()).as("wrong size of actions")
                .isEqualTo(noOfDeployedTargets * noOfDistributionSets);

        // only records retrieved from the DB can be evaluated to be sure that
        // all fields are
        // populated;
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

    @Test
    @Description("Multiple deployments or distribution set to target assignment test including finished response "
            + "from target/controller. Expected behaviour is that in case of OK finished update the target will go to "
            + "IN_SYNC status and installed DS is set to the assigned DS entry.")
    void assignDistributionSetAndAddFinishedActionStatus() {
        final PageRequest pageRequest = PageRequest.of(0, 100, Direction.ASC, ActionStatusFields.ID.getJpaEntityFieldName());

        final DeploymentResult deployResWithDsA = prepareComplexRepo("undep-A-T", 2, "dep-A-T", 4, 1, "dsA");
        final DeploymentResult deployResWithDsB = prepareComplexRepo("undep-B-T", 3, "dep-B-T", 5, 1, "dsB");
        final DeploymentResult deployResWithDsC = prepareComplexRepo("undep-C-T", 4, "dep-C-T", 6, 1, "dsC");

        // keep a reference to the created DistributionSets
        final JpaDistributionSet dsA = (JpaDistributionSet) deployResWithDsA.getDistributionSets().get(0);
        final JpaDistributionSet dsB = (JpaDistributionSet) deployResWithDsB.getDistributionSets().get(0);
        final JpaDistributionSet dsC = (JpaDistributionSet) deployResWithDsC.getDistributionSets().get(0);

        // retrieving the UpdateActions created by the assignments
        findActionsByDistributionSet(pageRequest, dsA.getId()).getContent().get(0);
        findActionsByDistributionSet(pageRequest, dsB.getId()).getContent().get(0);
        findActionsByDistributionSet(pageRequest, dsC.getId()).getContent().get(0);

        // verifying the correctness of the assignments
        for (final Target t : deployResWithDsA.getDeployedTargets()) {
            assertThat(deploymentManagement.getAssignedDistributionSet(t.getControllerId()).get().getId())
                    .as("assignment is not correct").isEqualTo(dsA.getId());
            assertThat(deploymentManagement.getInstalledDistributionSet(t.getControllerId()))
                    .as("installed ds should be null").isNotPresent();
        }
        for (final Target t : deployResWithDsB.getDeployedTargets()) {
            assertThat(deploymentManagement.getAssignedDistributionSet(t.getControllerId()).get().getId())
                    .as("assigned ds is wrong").isEqualTo(dsB.getId());
            assertThat(deploymentManagement.getInstalledDistributionSet(t.getControllerId()))
                    .as("installed ds should be null").isNotPresent();
        }
        for (final Target t : deployResWithDsC.getDeployedTargets()) {
            assertThat(deploymentManagement.getAssignedDistributionSet(t.getControllerId()).get().getId())
                    .isEqualTo(dsC.getId());
            assertThat(deploymentManagement.getInstalledDistributionSet(t.getControllerId()))
                    .as("installed ds should not be null").isNotPresent();
            assertThat(targetManagement.getByControllerID(t.getControllerId()).get().getUpdateStatus())
                    .as("wrong target info update status").isEqualTo(TargetUpdateStatus.PENDING);
        }

        final List<Target> updatedTsDsA = testdataFactory
                .sendUpdateActionStatusToTargets(deployResWithDsA.getDeployedTargets(), Status.FINISHED,
                        Collections.singletonList("alles gut"))
                .stream().map(Action::getTarget).collect(Collectors.toList());

        // verify, that dsA is deployed correctly
        for (final Target t_ : updatedTsDsA) {
            final Target t = targetManagement.getByControllerID(t_.getControllerId()).get();
            assertThat(deploymentManagement.getAssignedDistributionSet(t.getControllerId()))
                    .as("assigned ds is wrong").contains(dsA);
            assertThat(deploymentManagement.getInstalledDistributionSet(t.getControllerId()))
                    .as("installed ds is wrong").contains(dsA);
            assertThat(targetManagement.getByControllerID(t.getControllerId()).get().getUpdateStatus())
                    .as("wrong target info update status").isEqualTo(TargetUpdateStatus.IN_SYNC);
            assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, t.getControllerId()))
                    .as("no actions should be active").isEmpty();
        }

        // deploy dsA to the target which already have dsB deployed -> must
        // remove updActB from
        // activeActions, add a corresponding cancelAction and another
        // UpdateAction for dsA
        final List<Target> deployed2DS = assignDistributionSet(dsA, deployResWithDsB.getDeployedTargets())
                .getAssignedEntity().stream().map(Action::getTarget).collect(Collectors.toList());
        findActionsByDistributionSet(pageRequest, dsA.getId()).getContent().get(1);

        // get final updated version of targets
        final List<Target> deployResWithDsBTargets = targetManagement.getByControllerID(deployResWithDsB
                .getDeployedTargets().stream().map(Target::getControllerId).collect(Collectors.toList()));

        assertThat(deployed2DS).as("deployed ds is wrong").usingElementComparator(controllerIdComparator())
                .containsAll(deployResWithDsBTargets);
        assertThat(deployed2DS).as("deployed ds is wrong").hasSameSizeAs(deployResWithDsBTargets);

        for (final Target t_ : deployed2DS) {
            final Target t = targetManagement.getByControllerID(t_.getControllerId()).get();
            assertThat(deploymentManagement.getAssignedDistributionSet(t.getControllerId())).as("assigned ds is wrong")
                    .contains(dsA);
            assertThat(deploymentManagement.getInstalledDistributionSet(t.getControllerId()))
                    .as("installed ds should be null").isNotPresent();
            assertThat(targetManagement.getByControllerID(t.getControllerId()).get().getUpdateStatus())
                    .as("wrong target info update status").isEqualTo(TargetUpdateStatus.PENDING);

        }
    }

    /**
     * test the deletion of {@link DistributionSet}s including exception in case of
     * {@link Target}s are assigned by {@link Target#getAssignedDistributionSet()}
     * or {@link Target#getInstalledDistributionSet()}
     */
    @Test
    @Description("Deletes distribution set. Expected behaviour is that a soft delete is performed "
            + "if the DS is assigned to a target and a hard delete if the DS is not in use at all.")
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

        assertThat(distributionSetManagement.get(dsA.getId())).isNotPresent();

        // // verify that the ds is not physically deleted
        for (final DistributionSet ds : deploymentResult.getDistributionSets()) {
            distributionSetManagement.delete(ds.getId());
            final DistributionSet foundDS = distributionSetManagement.get(ds.getId()).get();
            assertThat(foundDS).as("founded should not be null").isNotNull();
            assertThat(foundDS.isDeleted()).as("found ds should be deleted").isTrue();
        }

        // verify that deleted attribute is used correctly
        List<DistributionSet> allFoundDS = distributionSetManagement.findByCompleted(PAGE, true).getContent();
        assertThat(allFoundDS.size()).as("no ds should be founded").isZero();

        assertThat(distributionSetRepository.findAll(SpecificationsBuilder.combineWithAnd(Arrays
                        .asList(DistributionSetSpecification.isDeleted(true), DistributionSetSpecification.isCompleted(true))),
                PAGE).getContent()).as("wrong size of founded ds").hasSize(noOfDistributionSets);

        for (final DistributionSet ds : deploymentResult.getDistributionSets()) {
            testdataFactory.sendUpdateActionStatusToTargets(deploymentResult.getDeployedTargets(), Status.FINISHED,
                    Collections.singletonList("blabla alles gut"));
        }
        // try to delete again
        distributionSetManagement.delete(deploymentResult.getDistributionSetIDs());
        // verify that the result is the same, even though distributionSet dsA
        // has been installed
        // successfully and no activeAction is referring to created distribution
        // sets
        allFoundDS = distributionSetManagement.findByCompleted(pageRequest, true).getContent();
        assertThat(allFoundDS.size()).as("no ds should be founded").isZero();
        assertThat(distributionSetRepository.findAll(SpecificationsBuilder.combineWithAnd(Arrays
                        .asList(DistributionSetSpecification.isDeleted(true), DistributionSetSpecification.isCompleted(true))),
                PAGE).getContent()).as("wrong size of founded ds").hasSize(noOfDistributionSets);

    }

    @Test
    @Description("Deletes multiple targets and verifies that all related metadata is also deleted.")
    void deletesTargetsAndVerifyCascadeDeletes() {

        final String undeployedTargetPrefix = "undep-T";
        final int noOfUndeployedTargets = 2;

        final String deployedTargetPrefix = "dep-T";
        final int noOfDeployedTargets = 4;

        final int noOfDistributionSets = 3;

        final DeploymentResult deploymentResult = prepareComplexRepo(undeployedTargetPrefix, noOfUndeployedTargets,
                deployedTargetPrefix, noOfDeployedTargets, noOfDistributionSets, "myTestDS");

        for (final DistributionSet ds : deploymentResult.getDistributionSets()) {
            testdataFactory.sendUpdateActionStatusToTargets(deploymentResult.getDeployedTargets(), Status.FINISHED,
                    Collections.singletonList("blabla alles gut"));
        }

        assertThat(targetManagement.count()).as("size of targets is wrong").isNotZero();
        assertThat(actionStatusRepository.count()).as("size of action status is wrong").isNotZero();

        targetManagement.delete(deploymentResult.getUndeployedTargetIDs());
        targetManagement.delete(deploymentResult.getDeployedTargetIDs());

        assertThat(targetManagement.count()).as("size of targets should be zero").isZero();
        assertThat(actionStatusRepository.count()).as("size of action status is wrong").isZero();
    }

    @Test
    @Description("Testing if changing target and the status without refreshing the entities from the DB (e.g. concurrent changes from UI and from controller) works")
    void alternatingAssignmentAndAddUpdateActionStatus() {

        final DistributionSet dsA = testdataFactory.createDistributionSet("a");
        final DistributionSet dsB = testdataFactory.createDistributionSet("b");
        List<Target> targs = Collections.singletonList(testdataFactory.createTarget("target-id-A"));

        // doing the assignment
        targs = assignDistributionSet(dsA, targs).getAssignedEntity().stream().map(Action::getTarget)
                .collect(Collectors.toList());
        implicitLock(dsA);
        Target targ = targetManagement.getByControllerID(targs.iterator().next().getControllerId()).get();

        // checking the revisions of the created entities
        // verifying that the revision of the object and the revision within the
        // DB has incremented by implicit lock
        assertThat(dsA.getOptLockRevision()).as("lock revision is wrong")
                .isEqualTo(distributionSetManagement.getWithDetails(dsA.getId()).get().getOptLockRevision());

        // verifying that the assignment is correct
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, targ.getControllerId()).getTotalElements())
                .as("Active target actions are wrong").isEqualTo(1);
        assertThat(deploymentManagement.countActionsByTarget(targ.getControllerId())).as("Target actions are wrong")
                .isEqualTo(1);
        assertThat(targ.getUpdateStatus()).as("UpdateStatus of target is wrong").isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(deploymentManagement.getAssignedDistributionSet(targ.getControllerId()))
                .as("Assigned distribution set of target is wrong").contains(dsA);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, targ.getControllerId()).getContent().get(0)
                .getDistributionSet()).as("Distribution set of actionn is wrong").isEqualTo(dsA);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, targ.getControllerId()).getContent().get(0)
                .getDistributionSet()).as("Installed distribution set of action should be null").isNotNull();

        final Slice<Action> updAct = findActionsByDistributionSet(PAGE, dsA.getId());
        controllerManagement.addUpdateActionStatus(
                entityFactory.actionStatus().create(updAct.getContent().get(0).getId()).status(Status.FINISHED));

        targ = targetManagement.getByControllerID(targ.getControllerId()).get();

        assertEquals(0, deploymentManagement.findActiveActionsByTarget(PAGE, targ.getControllerId()).getTotalElements(),
                "active target actions are wrong");
        assertEquals(1,
                deploymentManagement.findInActiveActionsByTarget(PAGE, targ.getControllerId()).getTotalElements(),
                "active actions are wrong");

        assertEquals(TargetUpdateStatus.IN_SYNC, targ.getUpdateStatus(), "tagret update status is not correct");
        assertEquals(dsA, deploymentManagement.getAssignedDistributionSet(targ.getControllerId()).get(),
                "wrong assigned ds");
        assertEquals(dsA, deploymentManagement.getInstalledDistributionSet(targ.getControllerId()).get(),
                "wrong installed ds");

        targs = assignDistributionSet(dsB.getId(), "target-id-A").getAssignedEntity().stream().map(Action::getTarget)
                .collect(Collectors.toList());
        implicitLock(dsB);

        targ = targs.iterator().next();

        assertEquals(1, deploymentManagement.findActiveActionsByTarget(PAGE, targ.getControllerId()).getTotalElements(),
                "active actions are wrong");
        assertEquals(TargetUpdateStatus.PENDING,
                targetManagement.getByControllerID(targ.getControllerId()).get().getUpdateStatus(),
                "target status is wrong");
        assertEquals(dsB, deploymentManagement.getAssignedDistributionSet(targ.getControllerId()).get(),
                "wrong assigned ds");
        assertEquals(dsA.getId(),
                deploymentManagement.getInstalledDistributionSet(targ.getControllerId()).get().getId(),
                "Installed ds is wrong");
        assertEquals(dsB, deploymentManagement.findActiveActionsByTarget(PAGE, targ.getControllerId()).getContent()
                .get(0).getDistributionSet(), "Active ds is wrong");
    }

    @Test
    @Description("The test verifies that the DS itself is not changed because of an target assignment"
            + " which is a relationship but not a changed on the entity itself..")
    void checkThatDsRevisionsIsNotChangedWithTargetAssignment() {
        final DistributionSet dsA = testdataFactory.createDistributionSet("a");
        testdataFactory.createDistributionSet("b");
        final Target targ = testdataFactory.createTarget("target-id-A");

        assertThat(dsA.getOptLockRevision()).as("lock revision is wrong")
                .isEqualTo(distributionSetManagement.getWithDetails(dsA.getId()).get().getOptLockRevision());

        assignDistributionSet(dsA, Collections.singletonList(targ));

        // implicit lock - incremented the version
        assertThat(dsA.getOptLockRevision() + 1).as("lock revision is wrong")
                .isEqualTo(distributionSetManagement.getWithDetails(dsA.getId()).get().getOptLockRevision());
    }

    @Test
    @Description("Tests the switch from a soft to hard update by API")
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

    @Test
    @Description("Tests the switch from a hard to hard update by API, e.g. which in fact should not change anything.")
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

    @Test
    @Description("Tests the computation of already assigned entities returned as a result of an assignment")
    void testAlreadyAssignedAndAssignedActionsInAssignmentResult() {
        // create target1, distributionSet, assign ds to target1 and finish
        // update (close all actions)
        final Action action = prepareFinishedUpdate("target1", "ds", false);
        final Target target2 = testdataFactory.createTarget("target2");
        final Target target3 = testdataFactory.createTarget("target3");

        // assign ds to target2, but don't finish update (actions should be
        // still open)
        assignDistributionSet(action.getDistributionSet().getId(), target2.getControllerId());

        final DistributionSetAssignmentResult assignmentResult = assignDistributionSet(
                action.getDistributionSet().getId(),
                Arrays.asList(action.getTarget().getControllerId(), target3.getControllerId()), ActionType.FORCED);

        assertThat(assignmentResult).isNotNull();
        assertThat(assignmentResult.getTotal()).as("Total count of assigned and already assigned targets").isEqualTo(2);
        assertThat(assignmentResult.getAssigned()).as("Total count of assigned targets").isEqualTo(1);
        assertThat(assignmentResult.getAlreadyAssigned()).as("Total count of already assigned targets").isEqualTo(1);
        assertThat(assignmentResult.getAssignedEntity()).isNotEmpty();
        assertThat(assignmentResult.getAssignedEntity()).allMatch(
                a -> a.getTarget().equals(target3) && a.getDistributionSet().equals(action.getDistributionSet()));
        assertThat(assignmentResult.getAssignedEntity())
                .noneMatch(a -> a.getTarget().getControllerId().equals("target1"));
    }

    @Test
    @Description("Verify that the DistributionSetAssignmentResult not contains already assigned targets.")
    void verifyDistributionSetAssignmentResultNotContainsAlreadyAssignedTargets() {
        final DistributionSet dsToTargetAssigned = testdataFactory.createDistributionSet("ds-3");

        // create assigned DS
        final Target savedTarget = testdataFactory.createTarget();
        DistributionSetAssignmentResult assignmentResult = assignDistributionSet(dsToTargetAssigned.getId(),
                savedTarget.getControllerId());
        assertThat(assignmentResult.getAssignedEntity()).hasSize(1);

        assignmentResult = assignDistributionSet(dsToTargetAssigned.getId(), savedTarget.getControllerId());
        assertThat(assignmentResult.getAssignedEntity()).isEmpty();

        assertThat(distributionSetRepository.findAll()).hasSize(1);
    }

    @Test
    @Description("Verify that the DistributionSet assignments work for multiple targets of the same target type within the same request.")
    void verifyDSAssignmentForMultipleTargetsWithSameTargetType() {
        final DistributionSet ds = testdataFactory.createDistributionSet("test-ds");
        final TargetType targetType = testdataFactory.createTargetType("test-type",
                Collections.singletonList(ds.getType()));

        final List<DeploymentRequest> deploymentRequests = new ArrayList<>();
        for (int i = 0; i < quotaManagement.getMaxTargetDistributionSetAssignmentsPerManualAssignment(); i++) {
            final Target target = testdataFactory.createTarget("test-target-" + i, "test-target-" + i,
                    targetType.getId());
            final DeploymentRequest deployment = DeploymentManagement
                    .deploymentRequest(target.getControllerId(), ds.getId()).build();
            deploymentRequests.add(deployment);
        }

        deploymentManagement.assignDistributionSets(deploymentRequests);
        implicitLock(ds);

        final List<Target> content = targetManagement.findAll(Pageable.unpaged()).getContent();

        content.stream().map(JpaTarget.class::cast)
                .forEach(jpaTarget -> assertThat(jpaTarget.getAssignedDistributionSet()).isEqualTo(ds));
    }

    @Test
    @Description("Verify that the DistributionSet assignments work for multiple targets of different target types.")
    void verifyDSAssignmentForMultipleTargetsWithDifferentTargetTypes() {
        final DistributionSet ds = testdataFactory.createDistributionSet("test-ds");
        final TargetType targetType1 = testdataFactory.createTargetType("test-type1",
                Collections.singletonList(ds.getType()));
        final TargetType targetType2 = testdataFactory.createTargetType("test-type2",
                Collections.singletonList(ds.getType()));
        final Target target1 = testdataFactory.createTarget("test-target1", "test-target1", targetType1.getId());
        final Target target2 = testdataFactory.createTarget("test-target2", "test-target2", targetType2.getId());

        final DeploymentRequest deployment1 = DeploymentManagement
                .deploymentRequest(target1.getControllerId(), ds.getId()).build();
        final DeploymentRequest deployment2 = DeploymentManagement
                .deploymentRequest(target2.getControllerId(), ds.getId()).build();
        final List<DeploymentRequest> deploymentRequests = Arrays.asList(deployment1, deployment2);

        deploymentManagement.assignDistributionSets(deploymentRequests);
        implicitLock(ds);

        final Optional<DistributionSet> assignedDsTarget1 = targetManagement
                .getByControllerID(target1.getControllerId()).map(JpaTarget.class::cast)
                .map(JpaTarget::getAssignedDistributionSet);
        final Optional<DistributionSet> assignedDsTarget2 = targetManagement
                .getByControllerID(target2.getControllerId()).map(JpaTarget.class::cast)
                .map(JpaTarget::getAssignedDistributionSet);

        assertThat(assignedDsTarget1).contains(ds);
        assertThat(assignedDsTarget2).contains(ds);
    }

    @Test
    @Description("Verify that the DistributionSet assignment fails for target with incompatible target type.")
    void verifyDSAssignmentFailsForTargetsWithIncompatibleTargetTypes() {
        final DistributionSet ds = testdataFactory.createDistributionSet("test-ds");
        final DistributionSetType dsType = testdataFactory.findOrCreateDistributionSetType("test-ds-type", "dsType");
        final TargetType targetType = testdataFactory.createTargetType("target-type",
                Collections.singletonList(dsType));
        final Target target = testdataFactory.createTarget("test-target", "test-target", targetType.getId());

        final DeploymentRequest deploymentRequest = DeploymentManagement
                .deploymentRequest(target.getControllerId(), ds.getId()).build();
        final List<DeploymentRequest> deploymentRequests = Collections.singletonList(deploymentRequest);

        assertThatExceptionOfType(IncompatibleTargetTypeException.class)
                .isThrownBy(() -> deploymentManagement.assignDistributionSets(deploymentRequests));
    }

    @Test
    @Description("Verify that the DistributionSet assignment fails for target with target type that is not compatible with any dsType.")
    void verifyDSAssignmentFailsForTargetsWithTargetTypesThatAreNotCompatibleWithAnyDs() {
        final DistributionSet ds = testdataFactory.createDistributionSet("test-ds");
        final TargetType emptyTargetType = testdataFactory.createTargetType("target-type", Collections.emptyList());
        final Target targetWithEmptyType = testdataFactory.createTarget("test-target", "test-target",
                emptyTargetType.getId());

        final DeploymentRequest deploymentRequestWithEmptyType = DeploymentManagement
                .deploymentRequest(targetWithEmptyType.getControllerId(), ds.getId()).build();
        final List<DeploymentRequest> deploymentRequests = Collections.singletonList(deploymentRequestWithEmptyType);
        assertThatExceptionOfType(IncompatibleTargetTypeException.class)
                .isThrownBy(() -> deploymentManagement.assignDistributionSets(deploymentRequests));
    }

    private JpaAction assignSet(final Target target, final DistributionSet ds) {
        assignDistributionSet(ds.getId(), target.getControllerId());
        implicitLock(ds);
        assertThat(targetManagement.getByControllerID(target.getControllerId()).get().getUpdateStatus())
                .as("wrong update status").isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(deploymentManagement.getAssignedDistributionSet(target.getControllerId())).as("wrong assigned ds")
                .contains(ds);
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

    private void assertDsExclusivelyAssignedToTargets(final List<Target> targets, final long dsId, final boolean active,
            final Status status) {
        final List<Action> assignment = findActionsByDistributionSet(PAGE, dsId).getContent();
        final String currentUsername = tenantAware.getCurrentUsername();

        assertThat(assignment).hasSize(10).allMatch(action -> action.isActive() == active)
                .as("Is assigned to DS " + dsId).allMatch(action -> action.getDistributionSet().getId().equals(dsId))
                .as("State is " + status).allMatch(action -> action.getStatus() == status)
                .as("Initiated by " + currentUsername).allMatch(a -> a.getInitiatedBy().equals(currentUsername));
        final long[] targetIds = targets.stream().mapToLong(Target::getId).toArray();
        assertThat(targetIds).as("All targets represented in assignment").containsExactlyInAnyOrder(
                assignment.stream().mapToLong(action -> action.getTarget().getId()).toArray());
    }

    private List<DistributionSetAssignmentResult> assignDistributionSetToTargets(final DistributionSet distributionSet,
            final Iterable<String> targetIds, final boolean confirmationRequired) {
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
     * @return the {@link DeploymentResult} containing all created targets, the
     *         distribution sets, the corresponding IDs for later evaluation in
     *         tests
     */
    private DeploymentResult prepareComplexRepo(final String undeployedTargetPrefix, final int noOfUndeployedTargets,
            final String deployedTargetPrefix, final int noOfDeployedTargets, final int noOfDistributionSets,
            final String distributionSetPrefix) {
        final Iterable<Target> nakedTargets = testdataFactory.createTargets(noOfUndeployedTargets,
                undeployedTargetPrefix, "first description");

        List<Target> deployedTargets = testdataFactory.createTargets(noOfDeployedTargets, deployedTargetPrefix,
                "first description");

        // creating 10 DistributionSets
        final Collection<DistributionSet> dsList = testdataFactory.createDistributionSets(distributionSetPrefix,
                noOfDistributionSets);
        String time = String.valueOf(System.currentTimeMillis());
        time = time.substring(time.length() - 5);

        // assigning all DistributionSet to the Target in the list
        // deployedTargets
        for (final DistributionSet ds : dsList) {
            deployedTargets = assignDistributionSet(ds, deployedTargets).getAssignedEntity().stream()
                    .map(Action::getTarget).collect(Collectors.toList());
            implicitLock(ds);
        }

        return new DeploymentResult(deployedTargets, nakedTargets, dsList, deployedTargetPrefix, undeployedTargetPrefix,
                distributionSetPrefix);
    }

    private Slice<Action> findActionsByDistributionSet(final Pageable pageable, final long distributionSetId) {
        distributionSetManagement.get(distributionSetId).orElseThrow(() -> new EntityNotFoundException(
                DistributionSet.class, distributionSetId));
        return actionRepository
                .findAll(ActionSpecifications.byDistributionSetId(distributionSetId), pageable)
                .map(Action.class::cast);
    }

    private static class DeploymentResult {

        final List<Long> deployedTargetIDs = new ArrayList<>();
        final List<Long> undeployedTargetIDs = new ArrayList<>();
        final List<Long> distributionSetIDs = new ArrayList<>();

        private final List<Target> undeployedTargets = new ArrayList<>();
        private final List<Target> deployedTargets = new ArrayList<>();
        private final List<DistributionSet> distributionSets = new ArrayList<>();

        public DeploymentResult(final Iterable<Target> deployedTs, final Iterable<Target> undeployedTs,
                final Iterable<DistributionSet> dss, final String deployedTargetPrefix,
                final String undeployedTargetPrefix, final String distributionSetPrefix) {

            deployedTargets.addAll(toList(deployedTs));
            undeployedTargets.addAll(toList(undeployedTs));
            distributionSets.addAll(toList(dss));

            deployedTargets.forEach(t -> deployedTargetIDs.add(t.getId()));

            undeployedTargets.forEach(t -> undeployedTargetIDs.add(t.getId()));

            distributionSets.forEach(ds -> distributionSetIDs.add(ds.getId()));

        }

        public List<Long> getDistributionSetIDs() {
            return distributionSetIDs;
        }

        public List<Long> getDeployedTargetIDs() {
            return deployedTargetIDs;
        }

        public List<Target> getUndeployedTargets() {
            return undeployedTargets;
        }

        public List<DistributionSet> getDistributionSets() {
            return distributionSets;
        }

        public List<Target> getDeployedTargets() {
            return deployedTargets;
        }

        public List<Long> getUndeployedTargetIDs() {
            return undeployedTargetIDs;
        }
    }
}