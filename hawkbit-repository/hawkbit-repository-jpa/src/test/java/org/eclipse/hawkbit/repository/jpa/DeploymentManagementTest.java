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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.ActionStatusFields;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.ForceQuitActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.specifications.DistributionSetSpecification;
import org.eclipse.hawkbit.repository.jpa.specifications.SpecificationsBuilder;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.TargetWithActionType;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort.Direction;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test class testing the functionality of triggering a deployment of
 * {@link DistributionSet}s to {@link Target}s.
 *
 */
@Features("Component Tests - Repository")
@Stories("Deployment Management")
public class DeploymentManagementTest extends AbstractJpaIntegrationTest {
    private EventHandlerStub eventHandlerStub;

    private CancelEventHandlerStub cancelEventHandlerStub;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Before
    public void addHandler() {
        eventHandlerStub = new EventHandlerStub();
        applicationContext.addApplicationListener(eventHandlerStub);

        cancelEventHandlerStub = new CancelEventHandlerStub();
        applicationContext.addApplicationListener(cancelEventHandlerStub);
    }

    @Test
    @Description("Verifies that management get access react as specfied on calls for non existing entities by means "
            + "of Optional not present.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void nonExistingEntityAccessReturnsNotPresent() {
        assertThat(deploymentManagement.findAction(1234L)).isNotPresent();
        assertThat(deploymentManagement.findActionWithDetails(NOT_EXIST_IDL)).isNotPresent();
    }

    @Test
    @Description("Verifies that management queries react as specfied on calls for non existing entities "
            + " by means of throwing EntityNotFoundException.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1) })
    public void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        final Target target = testdataFactory.createTarget();

        verifyThrownExceptionBy(() -> deploymentManagement.assignDistributionSet(NOT_EXIST_IDL,
                Arrays.asList(new TargetWithActionType(target.getControllerId()))), "DistributionSet");
        verifyThrownExceptionBy(() -> deploymentManagement.assignDistributionSet(NOT_EXIST_IDL,
                Arrays.asList(new TargetWithActionType(target.getControllerId())), "xxx"), "DistributionSet");
        verifyThrownExceptionBy(() -> deploymentManagement.assignDistributionSet(NOT_EXIST_IDL, ActionType.FORCED,
                System.currentTimeMillis(), Arrays.asList(target.getControllerId())), "DistributionSet");

        verifyThrownExceptionBy(() -> deploymentManagement.cancelAction(NOT_EXIST_IDL), "Action");
        verifyThrownExceptionBy(() -> deploymentManagement.countActionsByTarget(NOT_EXIST_ID), "Target");
        verifyThrownExceptionBy(() -> deploymentManagement.countActionsByTarget("xxx", NOT_EXIST_ID), "Target");

        verifyThrownExceptionBy(() -> deploymentManagement.findActionsByDistributionSet(PAGE, NOT_EXIST_IDL),
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
    public void findActionWithLazyDetails() {
        final DistributionSet testDs = testdataFactory.createDistributionSet("TestDs", "1.0",
                new ArrayList<DistributionSetTag>());
        final List<Target> testTarget = testdataFactory.createTargets(1);
        // one action with one action status is generated
        final Long actionId = assignDistributionSet(testDs, testTarget).getActions().get(0);
        final Action action = deploymentManagement.findActionWithDetails(actionId).get();

        assertThat(action.getDistributionSet()).as("DistributionSet in action").isNotNull();
        assertThat(action.getTarget()).as("Target in action").isNotNull();
        assertThat(deploymentManagement.getAssignedDistributionSet(action.getTarget().getControllerId()).get())
                .as("AssignedDistributionSet of target in action").isNotNull();

    }

    @Test
    @Description("Test verifies that actions of a target are found by using id-based search.")
    public void findActionByTargetId() {
        final DistributionSet testDs = testdataFactory.createDistributionSet("TestDs", "1.0",
                new ArrayList<DistributionSetTag>());
        final List<Target> testTarget = testdataFactory.createTargets(1);
        // one action with one action status is generated
        final Long actionId = assignDistributionSet(testDs, testTarget).getActions().get(0);

        // act
        final Slice<Action> actions = deploymentManagement.findActionsByTarget(testTarget.get(0).getControllerId(),
                PAGE);
        final Long count = deploymentManagement.countActionsByTarget(testTarget.get(0).getControllerId());

        assertThat(count).as("One Action for target").isEqualTo(1L).isEqualTo(actions.getContent().size());
        assertThat(actions.getContent().get(0).getId()).as("Action of target").isEqualTo(actionId);
    }

    @Test
    @Description("Test verifies that action-states of an action are found by using id-based search.")
    public void findActionStatusByActionId() {
        final DistributionSet testDs = testdataFactory.createDistributionSet("TestDs", "1.0",
                new ArrayList<DistributionSetTag>());
        final List<Target> testTarget = testdataFactory.createTargets(1);
        // one action with one action status is generated
        final Long actionId = assignDistributionSet(testDs, testTarget).getActions().get(0);
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
    public void findMessagesByActionStatusId() {
        final DistributionSet testDs = testdataFactory.createDistributionSet("TestDs", "1.0",
                new ArrayList<DistributionSetTag>());
        final List<Target> testTarget = testdataFactory.createTargets(1);
        // one action with one action status is generated
        final Long actionId = assignDistributionSet(testDs, testTarget).getActions().get(0);
        // create action-status entry with one message
        controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId)
                .status(Action.Status.FINISHED).messages(Arrays.asList("finished message")));
        final Page<ActionStatus> actionStates = deploymentManagement.findActionStatusByAction(PAGE, actionId);

        // find newly created action-status entry with message
        final JpaActionStatus actionStatusWithMessage = actionStates.getContent().stream().map(c -> (JpaActionStatus) c)
                .filter(entry -> entry.getMessages() != null && entry.getMessages().size() > 0).findFirst().get();
        final String expectedMsg = actionStatusWithMessage.getMessages().get(0);

        // act
        final Page<String> messages = deploymentManagement.findMessagesByActionStatusId(PAGE,
                actionStatusWithMessage.getId());

        assertThat(actionStates.getTotalElements()).as("Two action-states in total").isEqualTo(2L);
        assertThat(messages.getContent().get(0)).as("Message of action-status").isEqualTo(expectedMsg);
    }

    @Test
    @Description("Ensures that tag to distribution set assignment that does not exist will cause EntityNotFoundException.")
    public void assignDistributionSetToTagThatDoesNotExistThrowsException() {
        final List<Long> assignDS = Lists.newArrayListWithExpectedSize(5);
        for (int i = 0; i < 4; i++) {
            assignDS.add(testdataFactory.createDistributionSet("DS" + i, "1.0", Collections.emptyList()).getId());
        }
        // not exists
        assignDS.add(100L);

        final DistributionSetTag tag = distributionSetTagManagement.create(entityFactory.tag().create().name("Tag1"));

        assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> distributionSetManagement.assignTag(assignDS, tag.getId()))
                .withMessageContaining("DistributionSet").withMessageContaining(String.valueOf(tag.getId()));
    }

    @Test
    @Description("Test verifies that an assignment with automatic cancelation works correctly even if the update is split into multiple partitions on the database.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = Constants.MAX_ENTRIES_IN_STATEMENT + 10),
            @Expect(type = TargetUpdatedEvent.class, count = 2 * (Constants.MAX_ENTRIES_IN_STATEMENT + 10)),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 2 * (Constants.MAX_ENTRIES_IN_STATEMENT + 10)),
            @Expect(type = CancelTargetAssignmentEvent.class, count = Constants.MAX_ENTRIES_IN_STATEMENT + 10),
            @Expect(type = ActionUpdatedEvent.class, count = Constants.MAX_ENTRIES_IN_STATEMENT + 10),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6) })
    public void multiAssigmentHistoryOverMultiplePagesResultsInTwoActiveAction() {

        final DistributionSet cancelDs = testdataFactory.createDistributionSet("Canceled DS", "1.0",
                Collections.emptyList());

        final DistributionSet cancelDs2 = testdataFactory.createDistributionSet("Canceled DS", "1.2",
                Collections.emptyList());

        final List<Target> targets = testdataFactory.createTargets(Constants.MAX_ENTRIES_IN_STATEMENT + 10);

        assertThat(deploymentManagement.countActionsAll()).isEqualTo(0);

        assignDistributionSet(cancelDs, targets).getAssignedEntity();
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(Constants.MAX_ENTRIES_IN_STATEMENT + 10);
        assignDistributionSet(cancelDs2, targets).getAssignedEntity();
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(2 * (Constants.MAX_ENTRIES_IN_STATEMENT + 10));
    }

    @Test
    @Description("Cancels multiple active actions on a target. Expected behaviour is that with two active "
            + "actions after canceling the second active action the first one is still running as it is not touched by the cancelation. After canceling the first one "
            + "also the target goes back to IN_SYNC as no open action is left.")
    public void manualCancelWithMultipleAssignmentsCancelLastOneFirst() {
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
        assertThat(deploymentManagement.getAssignedDistributionSet("4712").get()).as("wrong ds").isEqualTo(dsFirst);
        assertThat(targetManagement.getByControllerID("4712").get().getUpdateStatus()).as("wrong update status")
                .isEqualTo(TargetUpdateStatus.PENDING);

        // we cancel first -> back to installed
        deploymentManagement.cancelAction(firstAction.getId());
        firstAction = (JpaAction) deploymentManagement.findActionWithDetails(firstAction.getId()).get();
        // confirm cancellation
        controllerManagement.addCancelActionStatus(
                entityFactory.actionStatus().create(firstAction.getId()).status(Status.CANCELED));
        assertThat(actionStatusRepository.findAll()).as("wrong size of action status").hasSize(9);
        assertThat(deploymentManagement.getAssignedDistributionSet("4712").get()).as("wrong assigned ds")
                .isEqualTo(dsInstalled);
        assertThat(targetManagement.getByControllerID("4712").get().getUpdateStatus()).as("wrong update status")
                .isEqualTo(TargetUpdateStatus.IN_SYNC);
    }

    @Test
    @Description("Cancels multiple active actions on a target. Expected behaviour is that with two active "
            + "actions after canceling the first active action the system switched to second one. After canceling this one "
            + "also the target goes back to IN_SYNC as no open action is left.")
    public void manualCancelWithMultipleAssignmentsCancelMiddleOneFirst() {
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
        assertThat(deploymentManagement.getAssignedDistributionSet("4712").get()).as("wrong assigned ds")
                .isEqualTo(dsSecond);
        assertThat(targetManagement.getByControllerID("4712").get().getUpdateStatus()).as("wrong target update status")
                .isEqualTo(TargetUpdateStatus.PENDING);

        // we cancel second -> remain assigned until finished cancellation
        deploymentManagement.cancelAction(secondAction.getId());
        secondAction = (JpaAction) deploymentManagement.findActionWithDetails(secondAction.getId()).get();
        assertThat(actionStatusRepository.findAll()).as("wrong size of action status").hasSize(8);
        assertThat(deploymentManagement.getAssignedDistributionSet("4712").get()).as("wrong assigned ds")
                .isEqualTo(dsSecond);
        // confirm cancellation
        controllerManagement.addCancelActionStatus(
                entityFactory.actionStatus().create(secondAction.getId()).status(Status.CANCELED));
        // cancelled success -> back to dsInstalled
        assertThat(deploymentManagement.getAssignedDistributionSet("4712").get()).as("wrong installed ds")
                .isEqualTo(dsInstalled);
        assertThat(targetManagement.getByControllerID("4712").get().getUpdateStatus())
                .as("wrong target info update status").isEqualTo(TargetUpdateStatus.IN_SYNC);
    }

    @Test
    @Description("Force Quit an Assignment. Expected behaviour is that the action is canceled and is marked as deleted. The assigned Software module")
    public void forceQuitSetActionToInactive() throws InterruptedException {
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
        assertThat(deploymentManagement.getAssignedDistributionSet("4712").get()).as("wrong assigned ds")
                .isEqualTo(dsInstalled);
        assertThat(targetManagement.getByControllerID("4712").get().getUpdateStatus()).as("wrong target update status")
                .isEqualTo(TargetUpdateStatus.IN_SYNC);
    }

    @Test
    @Description("Force Quit an not canceled Assignment. Expected behaviour is that the action can not be force quit and there is thrown an exception.")
    public void forceQuitNotAllowedThrowsException() {
        final Action action = prepareFinishedUpdate("4712", "installed", true);
        final Target target = action.getTarget();

        final DistributionSet ds = testdataFactory.createDistributionSet("newDS", true);

        // verify initial status
        assertThat(targetManagement.getByControllerID("4712").get().getUpdateStatus()).as("wrong update status")
                .isEqualTo(TargetUpdateStatus.IN_SYNC);

        final Action assigningAction = assignSet(target, ds);

        // verify assignment
        assertThat(actionRepository.findAll()).as("wrong size of action").hasSize(2);
        assertThat(actionStatusRepository.findAll()).as("wrong size of action status").hasSize(4);

        // force quit assignment
        try {
            deploymentManagement.forceQuitAction(assigningAction.getId());
            fail("expected ForceQuitActionNotAllowedException");
        } catch (final ForceQuitActionNotAllowedException ex) {
        }
    }

    private JpaAction assignSet(final Target target, final DistributionSet ds) {
        assignDistributionSet(ds.getId(), target.getControllerId());
        assertThat(targetManagement.getByControllerID(target.getControllerId()).get().getUpdateStatus())
                .as("wrong update status").isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(deploymentManagement.getAssignedDistributionSet(target.getControllerId()).get())
                .as("wrong assigned ds").isEqualTo(ds);
        final JpaAction action = actionRepository
                .findByTargetAndDistributionSet(PAGE, (JpaTarget) target, (JpaDistributionSet) ds).getContent().get(0);
        assertThat(action).as("action should not be null").isNotNull();
        return action;
    }

    @Test
    @Description("Simple offline deployment of a distribution set to a list of targets. Verifies that offline assigment "
            + "is correctly executed for targets that do not have a running update already. Those are ignored.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 20),
            @Expect(type = TargetUpdatedEvent.class, count = 20), @Expect(type = ActionCreatedEvent.class, count = 20),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    public void assignedDistributionSet() {
        final List<String> controllerIds = testdataFactory.createTargets(10).stream().map(Target::getControllerId)
                .collect(Collectors.toList());
        final List<Target> onlineAssignedTargets = testdataFactory.createTargets(10, "2");
        controllerIds.addAll(onlineAssignedTargets.stream().map(Target::getControllerId).collect(Collectors.toList()));

        final DistributionSet ds = testdataFactory.createDistributionSet();
        assignDistributionSet(testdataFactory.createDistributionSet("2"), onlineAssignedTargets);

        final long current = System.currentTimeMillis();
        final List<Target> targets = deploymentManagement.offlineAssignedDistributionSet(ds.getId(), controllerIds)
                .getAssignedEntity();
        assertThat(actionRepository.count()).isEqualTo(20);

        assertThat(targetManagement.findByInstalledDistributionSet(PAGE, ds.getId()).getContent()).containsAll(targets)
                .hasSize(10).containsAll(targetManagement.findByAssignedDistributionSet(PAGE, ds.getId()))
                .as("InstallationDate set").allMatch(target -> target.getInstallationDate() >= current)
                .as("TargetUpdateStatus IN_SYNC")
                .allMatch(target -> TargetUpdateStatus.IN_SYNC.equals(target.getUpdateStatus()))
                .as("InstallationDate equal to LastModifiedAt")
                .allMatch(target -> target.getLastModifiedAt() == target.getInstallationDate());
    }

    @Test
    @Description("Verifies that if an account is set to action autoclose running actions in case of a new assigned set get closed and set to CANCELED.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 10),
            @Expect(type = TargetUpdatedEvent.class, count = 20), @Expect(type = ActionCreatedEvent.class, count = 20),
            @Expect(type = ActionUpdatedEvent.class, count = 10),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 2) })
    public void assigneDistributionSetAndAutoCloseActiveActions() {
        tenantConfigurationManagement
                .addOrUpdateConfiguration(TenantConfigurationKey.REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED, true);

        try {
            final List<Target> targets = testdataFactory.createTargets(10);

            // First assignment
            final DistributionSet ds1 = testdataFactory.createDistributionSet("1");
            assignDistributionSet(ds1, targets);

            List<Action> assignmentOne = actionRepository.findByDistributionSetId(PAGE, ds1.getId()).getContent();
            assertThat(assignmentOne).hasSize(10).as("Is active").allMatch(Action::isActive).as("Is assigned DS")
                    .allMatch(action -> action.getDistributionSet().getId() == ds1.getId()).as("Is running")
                    .allMatch(action -> action.getStatus() == Status.RUNNING);

            // Second assignment
            final DistributionSet ds2 = testdataFactory.createDistributionSet("2");
            assignDistributionSet(ds2, targets);

            final List<Action> assignmentTwo = actionRepository.findByDistributionSetId(PAGE, ds2.getId()).getContent();
            assignmentOne = actionRepository.findByDistributionSetId(PAGE, ds1.getId()).getContent();
            assertThat(assignmentTwo).hasSize(10).as("Is active").allMatch(Action::isActive).as("Is assigned DS")
                    .allMatch(action -> action.getDistributionSet().getId() == ds2.getId()).as("Is running")
                    .allMatch(action -> action.getStatus() == Status.RUNNING);
            assertThat(assignmentOne).hasSize(10).as("Is active").allMatch(action -> !action.isActive())
                    .as("Is assigned to DS").allMatch(action -> action.getDistributionSet().getId() == ds1.getId())
                    .as("Is cancelled").allMatch(action -> action.getStatus() == Status.CANCELED);

            assertThat(targetManagement.findByAssignedDistributionSet(PAGE, ds2.getId()).getContent()).hasSize(10)
                    .as("InstallationDate not set").allMatch(target -> (target.getInstallationDate() == null));

        } finally {
            tenantConfigurationManagement
                    .addOrUpdateConfiguration(TenantConfigurationKey.REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED, false);
        }
    }

    /**
     * test a simple deployment by calling the
     * {@link TargetRepository#assignDistributionSet(DistributionSet, Iterable)}
     * and checking the active action and the action history of the targets.
     *
     * @throws InterruptedException
     */
    @Test
    @Description("Simple deployment or distribution set to target assignment test.")
    public void assignDistributionSet2Targets() throws InterruptedException {
        eventHandlerStub.setExpectedNumberOfEvents(1);

        final String myCtrlIDPref = "myCtrlID";
        final Iterable<Target> savedNakedTargets = testdataFactory.createTargets(10, myCtrlIDPref, "first description");

        final String myDeployedCtrlIDPref = "myDeployedCtrlID";
        List<Target> savedDeployedTargets = testdataFactory.createTargets(20, myDeployedCtrlIDPref,
                "first description");

        final DistributionSet ds = testdataFactory.createDistributionSet("");

        assignDistributionSet(ds, savedDeployedTargets);

        // verify that one Action for each assignDistributionSet
        assertThat(actionRepository.findAll(PAGE).getNumberOfElements()).as("wrong size of actions").isEqualTo(20);

        final Iterable<Target> allFoundTargets = targetManagement.findAll(PAGE).getContent();

        // get final updated version of targets
        savedDeployedTargets = targetManagement.getByControllerID(
                savedDeployedTargets.stream().map(target -> target.getControllerId()).collect(Collectors.toList()));

        assertThat(allFoundTargets).as("founded targets are wrong").containsAll(savedDeployedTargets)
                .containsAll(savedNakedTargets);
        assertThat(savedDeployedTargets).as("saved target are wrong")
                .doesNotContain(Iterables.toArray(savedNakedTargets, Target.class));
        assertThat(savedNakedTargets).as("saved target are wrong")
                .doesNotContain(Iterables.toArray(savedDeployedTargets, Target.class));

        for (final Target myt : savedNakedTargets) {
            final Target t = targetManagement.getByControllerID(myt.getControllerId()).get();
            assertThat(deploymentManagement.countActionsByTarget(t.getControllerId())).as("action should be empty")
                    .isEqualTo(0L);
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

        final List<TargetAssignDistributionSetEvent> events = eventHandlerStub.getEvents(10, TimeUnit.SECONDS);

        assertTargetAssignDistributionSetEvents(savedDeployedTargets, ds, events);
    }

    @Test
    @Description("Test that it is not possible to assign a distribution set that is not complete.")
    public void failDistributionSetAssigmentThatIsNotComplete() throws InterruptedException {
        eventHandlerStub.setExpectedNumberOfEvents(0);

        final List<Target> targets = testdataFactory.createTargets(10);

        final SoftwareModule ah = testdataFactory.createSoftwareModuleApp();
        final SoftwareModule os = testdataFactory.createSoftwareModuleOs();

        final DistributionSet incomplete = distributionSetManagement.create(entityFactory.distributionSet().create()
                .name("incomplete").version("v1").type(standardDsType).modules(Arrays.asList(ah.getId())));

        try {
            assignDistributionSet(incomplete, targets);
            fail("expected IncompleteDistributionSetException");
        } catch (final IncompleteDistributionSetException ex) {
        }

        // give some chance to receive events asynchronously
        Thread.sleep(1L);
        final List<TargetAssignDistributionSetEvent> events = eventHandlerStub.getEvents(5, TimeUnit.SECONDS);
        assertThat(events).as("events should be empty").isEmpty();

        final DistributionSet nowComplete = distributionSetManagement.assignSoftwareModules(incomplete.getId(),
                Sets.newHashSet(os.getId()));

        eventHandlerStub.setExpectedNumberOfEvents(1);

        assertThat(assignDistributionSet(nowComplete, targets).getAssigned()).as("assign ds doesn't work")
                .isEqualTo(10);

        assertTargetAssignDistributionSetEvents(targets, nowComplete, eventHandlerStub.getEvents(15, TimeUnit.SECONDS));
    }

    @Test
    @Description("Multiple deployments or distribution set to target assignment test. Expected behaviour is that a new deployment "
            + "overides unfinished old one which are canceled as part of the operation.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 5 + 4),
            @Expect(type = TargetUpdatedEvent.class, count = 3 * 4),
            @Expect(type = ActionCreatedEvent.class, count = 3 * 4),
            @Expect(type = ActionUpdatedEvent.class, count = 4 * 2),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 4 * 2),
            @Expect(type = DistributionSetCreatedEvent.class, count = 3),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 9),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    public void mutipleDeployments() throws InterruptedException {
        final String undeployedTargetPrefix = "undep-T";
        final int noOfUndeployedTargets = 5;

        final String deployedTargetPrefix = "dep-T";
        final int noOfDeployedTargets = 4;

        final int noOfDistributionSets = 3;

        // One assigment per DS
        final int expectedNumberOfEventsForAssignment = 1;
        eventHandlerStub.setExpectedNumberOfEvents(expectedNumberOfEventsForAssignment);

        // Each of the four targets get two more assignment the which are
        // cancelled (4 * 2 = 8)
        final int expectedNumberOfEventsForCancel = 8;
        cancelEventHandlerStub.setExpectedNumberOfEvents(expectedNumberOfEventsForCancel);

        final DeploymentResult deploymentResult = prepareComplexRepo(undeployedTargetPrefix, noOfUndeployedTargets,
                deployedTargetPrefix, noOfDeployedTargets, noOfDistributionSets, "myTestDS");

        final List<Long> deployedTargetIDs = deploymentResult.getDeployedTargetIDs();
        final List<Long> undeployedTargetIDs = deploymentResult.getUndeployedTargetIDs();
        final Collection<JpaTarget> savedNakedTargets = (Collection) deploymentResult.getUndeployedTargets();
        final Collection<JpaTarget> savedDeployedTargets = (Collection) deploymentResult.getDeployedTargets();

        // retrieving all Actions created by the assignDistributionSet call
        final Page<JpaAction> page = actionRepository.findAll(PAGE);
        // and verify the number
        assertThat(page.getTotalElements()).as("wrong size of actions")
                .isEqualTo(noOfDeployedTargets * noOfDistributionSets);

        // only records retrieved from the DB can be evaluated to be sure that
        // all fields are
        // populated;
        final List<JpaTarget> allFoundTargets = targetRepository.findAll();

        final List<JpaTarget> deployedTargetsFromDB = targetRepository.findAll(deployedTargetIDs);
        final List<JpaTarget> undeployedTargetsFromDB = targetRepository.findAll(undeployedTargetIDs);

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
        assertThat(deployedTargetsFromDB).as("content of deployed target is wrong").containsAll(savedDeployedTargets)
                .doesNotContain(Iterables.toArray(undeployedTargetsFromDB, JpaTarget.class));
        assertThat(undeployedTargetsFromDB).as("content of undeployed target is wrong").containsAll(savedNakedTargets)
                .doesNotContain(Iterables.toArray(deployedTargetsFromDB, JpaTarget.class));

        // For each of the 4 targets 1 distribution sets gets assigned
        eventHandlerStub.getEvents(10, TimeUnit.SECONDS);

        // For each of the 4 targets 2 distribution sets gets cancelled
        cancelEventHandlerStub.getEvents(10, TimeUnit.SECONDS);

    }

    @Test
    @Description("Multiple deployments or distribution set to target assignment test including finished response "
            + "from target/controller. Expected behaviour is that in case of OK finished update the target will go to "
            + "IN_SYNC status and installed DS is set to the assigned DS entry.")
    public void assignDistributionSetAndAddFinishedActionStatus() {
        final PageRequest pageRequest = new PageRequest(0, 100, Direction.ASC, ActionStatusFields.ID.getFieldName());

        final DeploymentResult deployResWithDsA = prepareComplexRepo("undep-A-T", 2, "dep-A-T", 4, 1, "dsA");
        final DeploymentResult deployResWithDsB = prepareComplexRepo("undep-B-T", 3, "dep-B-T", 5, 1, "dsB");
        final DeploymentResult deployResWithDsC = prepareComplexRepo("undep-C-T", 4, "dep-C-T", 6, 1, "dsC");

        // keep a reference to the created DistributionSets
        final JpaDistributionSet dsA = (JpaDistributionSet) deployResWithDsA.getDistributionSets().get(0);
        final JpaDistributionSet dsB = (JpaDistributionSet) deployResWithDsB.getDistributionSets().get(0);
        final JpaDistributionSet dsC = (JpaDistributionSet) deployResWithDsC.getDistributionSets().get(0);

        // retrieving the UpdateActions created by the assignments
        actionRepository.findByDistributionSetId(pageRequest, dsA.getId()).getContent().get(0);
        actionRepository.findByDistributionSetId(pageRequest, dsB.getId()).getContent().get(0);
        actionRepository.findByDistributionSetId(pageRequest, dsC.getId()).getContent().get(0);

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
            assertThat(deploymentManagement.getAssignedDistributionSet(t.getControllerId()).get())
                    .as("assigned ds is wrong").isEqualTo(dsA);
            assertThat(deploymentManagement.getInstalledDistributionSet(t.getControllerId()).get())
                    .as("installed ds is wrong").isEqualTo(dsA);
            assertThat(targetManagement.getByControllerID(t.getControllerId()).get().getUpdateStatus())
                    .as("wrong target info update status").isEqualTo(TargetUpdateStatus.IN_SYNC);
            assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, t.getControllerId()))
                    .as("no actions should be active").hasSize(0);
        }

        // deploy dsA to the target which already have dsB deployed -> must
        // remove updActB from
        // activeActions, add a corresponding cancelAction and another
        // UpdateAction for dsA
        final Iterable<Target> deployed2DS = assignDistributionSet(dsA, deployResWithDsB.getDeployedTargets())
                .getAssignedEntity();
        actionRepository.findByDistributionSetId(pageRequest, dsA.getId()).getContent().get(1);

        // get final updated version of targets
        final List<Target> deployResWithDsBTargets = targetManagement.getByControllerID(deployResWithDsB
                .getDeployedTargets().stream().map(Target::getControllerId).collect(Collectors.toList()));

        assertThat(deployed2DS).as("deployed ds is wrong").containsAll(deployResWithDsBTargets);
        assertThat(deployed2DS).as("deployed ds is wrong").hasSameSizeAs(deployResWithDsBTargets);

        for (final Target t_ : deployed2DS) {
            final Target t = targetManagement.getByControllerID(t_.getControllerId()).get();
            assertThat(deploymentManagement.getAssignedDistributionSet(t.getControllerId()).get())
                    .as("assigned ds is wrong").isEqualTo(dsA);
            assertThat(deploymentManagement.getInstalledDistributionSet(t.getControllerId()))
                    .as("installed ds should be null").isNotPresent();
            assertThat(targetManagement.getByControllerID(t.getControllerId()).get().getUpdateStatus())
                    .as("wrong target info update status").isEqualTo(TargetUpdateStatus.PENDING);

        }
    }

    /**
     * test the deletion of {@link DistributionSet}s including exception in case
     * of {@link Target}s are assigned by
     * {@link Target#getAssignedDistributionSet()} or
     * {@link Target#getInstalledDistributionSet()}
     */
    @Test
    @Description("Deletes distribution set. Expected behaviour is that a soft delete is performed "
            + "if the DS is assigned to a target and a hard delete if the DS is not in use at all.")
    public void deleteDistributionSet() {

        final PageRequest pageRequest = new PageRequest(0, 100, Direction.ASC, "id");

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
        assertThat(allFoundDS.size()).as("no ds should be founded").isEqualTo(0);

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
        assertThat(allFoundDS.size()).as("no ds should be founded").isEqualTo(0);
        assertThat(distributionSetRepository.findAll(SpecificationsBuilder.combineWithAnd(Arrays
                .asList(DistributionSetSpecification.isDeleted(true), DistributionSetSpecification.isCompleted(true))),
                PAGE).getContent()).as("wrong size of founded ds").hasSize(noOfDistributionSets);

    }

    @Test
    @Description("Deletes multiple targets and verfies that all related metadata is also deleted.")
    public void deletesTargetsAndVerifyCascadeDeletes() {

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
    public void alternatingAssignmentAndAddUpdateActionStatus() {

        final DistributionSet dsA = testdataFactory.createDistributionSet("a");
        final DistributionSet dsB = testdataFactory.createDistributionSet("b");
        List<Target> targs = Arrays.asList(testdataFactory.createTarget("target-id-A"));

        // doing the assignment
        targs = assignDistributionSet(dsA, targs).getAssignedEntity();
        Target targ = targetManagement.getByControllerID(targs.iterator().next().getControllerId()).get();

        // checking the revisions of the created entities
        // verifying that the revision of the object and the revision within the
        // DB has not changed
        assertThat(dsA.getOptLockRevision()).as("lock revision is wrong")
                .isEqualTo(distributionSetManagement.getWithDetails(dsA.getId()).get().getOptLockRevision());

        // verifying that the assignment is correct
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, targ.getControllerId()).getTotalElements())
                .as("Active target actions are wrong").isEqualTo(1);
        assertThat(deploymentManagement.countActionsByTarget(targ.getControllerId())).as("Target actions are wrong")
                .isEqualTo(1);
        assertThat(targ.getUpdateStatus()).as("UpdateStatus of target is wrong").isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(deploymentManagement.getAssignedDistributionSet(targ.getControllerId()).get())
                .as("Assigned distribution set of target is wrong").isEqualTo(dsA);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, targ.getControllerId()).getContent().get(0)
                .getDistributionSet()).as("Distribution set of actionn is wrong").isEqualTo(dsA);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, targ.getControllerId()).getContent().get(0)
                .getDistributionSet()).as("Installed distribution set of action should be null").isNotNull();

        final Page<Action> updAct = actionRepository.findByDistributionSetId(PAGE, dsA.getId());
        controllerManagement.addUpdateActionStatus(
                entityFactory.actionStatus().create(updAct.getContent().get(0).getId()).status(Status.FINISHED));

        targ = targetManagement.getByControllerID(targ.getControllerId()).get();

        assertEquals("active target actions are wrong", 0,
                deploymentManagement.findActiveActionsByTarget(PAGE, targ.getControllerId()).getTotalElements());
        assertEquals("active actions are wrong", 1,
                deploymentManagement.findInActiveActionsByTarget(PAGE, targ.getControllerId()).getTotalElements());

        assertEquals("tagret update status is not correct", TargetUpdateStatus.IN_SYNC, targ.getUpdateStatus());
        assertEquals("wrong assigned ds", dsA,
                deploymentManagement.getAssignedDistributionSet(targ.getControllerId()).get());
        assertEquals("wrong installed ds", dsA,
                deploymentManagement.getInstalledDistributionSet(targ.getControllerId()).get());

        targs = assignDistributionSet(dsB.getId(), "target-id-A").getAssignedEntity();

        targ = targs.iterator().next();

        assertEquals("active actions are wrong", 1,
                deploymentManagement.findActiveActionsByTarget(PAGE, targ.getControllerId()).getTotalElements());
        assertEquals("target status is wrong", TargetUpdateStatus.PENDING,
                targetManagement.getByControllerID(targ.getControllerId()).get().getUpdateStatus());
        assertEquals("wrong assigned ds", dsB,
                deploymentManagement.getAssignedDistributionSet(targ.getControllerId()).get());
        assertEquals("Installed ds is wrong", dsA.getId(),
                deploymentManagement.getInstalledDistributionSet(targ.getControllerId()).get().getId());
        assertEquals("Active ds is wrong", dsB, deploymentManagement
                .findActiveActionsByTarget(PAGE, targ.getControllerId()).getContent().get(0).getDistributionSet());

    }

    @Test
    @Description("The test verfies that the DS itself is not changed because of an target assignment"
            + " which is a relationship but not a changed on the entity itself..")
    public void checkThatDsRevisionsIsNotChangedWithTargetAssignment() {
        final DistributionSet dsA = testdataFactory.createDistributionSet("a");
        testdataFactory.createDistributionSet("b");
        final Target targ = testdataFactory.createTarget("target-id-A");

        assertThat(dsA.getOptLockRevision()).as("lock revision is wrong")
                .isEqualTo(distributionSetManagement.getWithDetails(dsA.getId()).get().getOptLockRevision());

        assignDistributionSet(dsA, Arrays.asList(targ));

        assertThat(dsA.getOptLockRevision()).as("lock revision is wrong")
                .isEqualTo(distributionSetManagement.getWithDetails(dsA.getId()).get().getOptLockRevision());
    }

    @Test
    @Description("Tests the switch from a soft to hard update by API")
    public void forceSoftAction() {
        // prepare
        final Target target = testdataFactory.createTarget("knownControllerId");
        final DistributionSet ds = testdataFactory.createDistributionSet("a");
        // assign ds to create an action
        final DistributionSetAssignmentResult assignDistributionSet = deploymentManagement.assignDistributionSet(
                ds.getId(), ActionType.SOFT,
                org.eclipse.hawkbit.repository.model.RepositoryModelConstants.NO_FORCE_TIME,
                Arrays.asList(target.getControllerId()));
        final Long actionId = assignDistributionSet.getActions().get(0);
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
    public void forceAlreadyForcedActionNothingChanges() {
        // prepare
        final Target target = testdataFactory.createTarget("knownControllerId");
        final DistributionSet ds = testdataFactory.createDistributionSet("a");
        // assign ds to create an action
        final DistributionSetAssignmentResult assignDistributionSet = deploymentManagement.assignDistributionSet(
                ds.getId(), ActionType.FORCED,
                org.eclipse.hawkbit.repository.model.RepositoryModelConstants.NO_FORCE_TIME,
                Arrays.asList(target.getControllerId()));
        final Long actionId = assignDistributionSet.getActions().get(0);
        // verify perparation
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
     * Helper methods that creates 2 lists of targets and a list of distribution
     * sets.
     * <p>
     * <b>All created distribution sets are assigned to all targets of the
     * target list deployedTargets.</b>
     *
     * @param undeployedTargetPrefix
     *            prefix to be used as target controller prefix
     * @param noOfUndeployedTargets
     *            number of targets which remain undeployed
     * @param deployedTargetPrefix
     *            prefix to be used as target controller prefix
     * @param noOfDeployedTargets
     *            number of targets to which the created distribution sets
     *            assigned
     * @param noOfDistributionSets
     *            number of distribution sets
     * @param distributionSetPrefix
     *            prefix for the created distribution sets
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
            deployedTargets = assignDistributionSet(ds, deployedTargets).getAssignedEntity();
        }

        return new DeploymentResult(deployedTargets, nakedTargets, dsList, deployedTargetPrefix, undeployedTargetPrefix,
                distributionSetPrefix);

    }

    private void assertTargetAssignDistributionSetEvents(final List<Target> targets, final DistributionSet ds,
            final List<TargetAssignDistributionSetEvent> events) {
        assertThat(events).hasSize(1);
        final TargetAssignDistributionSetEvent event = events.get(0);
        assertThat(event).isNotNull();
        assertThat(event.getDistributionSetId()).isEqualTo(ds.getId());

        assertThat(event.getActions()).isEqualTo(targets.stream()
                .map(target -> deploymentManagement.findActiveActionsByTarget(PAGE, target.getControllerId())
                        .getContent())
                .flatMap(List::stream)
                .collect(Collectors.toMap(action -> action.getTarget().getControllerId(), Action::getId)));
    }

    private class DeploymentResult {
        final List<Long> deployedTargetIDs = new ArrayList<>();
        final List<Long> undeployedTargetIDs = new ArrayList<>();
        final List<Long> distributionSetIDs = new ArrayList<>();

        private final List<Target> undeployedTargets = new ArrayList<>();
        private final List<Target> deployedTargets = new ArrayList<>();
        private final List<DistributionSet> distributionSets = new ArrayList<>();

        public DeploymentResult(final Iterable<Target> deployedTs, final Iterable<Target> undeployedTs,
                final Iterable<DistributionSet> dss, final String deployedTargetPrefix,
                final String undeployedTargetPrefix, final String distributionSetPrefix) {

            Iterables.addAll(deployedTargets, deployedTs);
            Iterables.addAll(undeployedTargets, undeployedTs);
            Iterables.addAll(distributionSets, dss);

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

    protected static class EventHandlerStub implements ApplicationListener<TargetAssignDistributionSetEvent> {
        private final List<TargetAssignDistributionSetEvent> events = Collections.synchronizedList(new LinkedList<>());
        private CountDownLatch latch;
        private int expectedNumberOfEvents;

        /**
         * @param expectedNumberOfEvents
         *            the expectedNumberOfEvents to set
         */
        public void setExpectedNumberOfEvents(final int expectedNumberOfEvents) {
            events.clear();
            this.expectedNumberOfEvents = expectedNumberOfEvents;
            this.latch = new CountDownLatch(expectedNumberOfEvents);
        }

        public List<TargetAssignDistributionSetEvent> getEvents(final long timeout, final TimeUnit unit)
                throws InterruptedException {
            latch.await(timeout, unit);
            final List<TargetAssignDistributionSetEvent> handledEvents = Collections
                    .unmodifiableList(new LinkedList<>(events));
            assertThat(handledEvents).as("Did not receive the expected amount of events (" + expectedNumberOfEvents
                    + ") within timeout. Received events are " + handledEvents).hasSize(expectedNumberOfEvents);
            return handledEvents;

        }

        @Override
        public void onApplicationEvent(final TargetAssignDistributionSetEvent event) {
            if (latch == null) {
                return;
            }
            events.add(event);
            latch.countDown();

        }
    }

    private static class CancelEventHandlerStub implements ApplicationListener<CancelTargetAssignmentEvent> {
        private final List<CancelTargetAssignmentEvent> events = Collections.synchronizedList(new LinkedList<>());
        private CountDownLatch latch;
        private int expectedNumberOfEvents;

        public void setExpectedNumberOfEvents(final int expectedNumberOfEvents) {
            events.clear();
            this.expectedNumberOfEvents = expectedNumberOfEvents;
            this.latch = new CountDownLatch(expectedNumberOfEvents);
        }

        public List<CancelTargetAssignmentEvent> getEvents(final long timeout, final TimeUnit unit)
                throws InterruptedException {
            latch.await(timeout, unit);
            final List<CancelTargetAssignmentEvent> handledEvents = new LinkedList<>(events);
            assertThat(handledEvents).as("Did not receive the expected amount of events (" + expectedNumberOfEvents
                    + ") within timeout. Received events are " + handledEvents).hasSize(expectedNumberOfEvents);
            return handledEvents;
        }

        @Override
        public void onApplicationEvent(final CancelTargetAssignmentEvent event) {
            if (latch == null) {
                return;
            }
            events.add(event);
            latch.countDown();
        }
    }

}
