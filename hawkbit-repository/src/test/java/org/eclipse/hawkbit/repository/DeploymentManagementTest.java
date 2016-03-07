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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.Constants;
import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.eventbus.event.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.eventbus.event.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.exception.ForceQuitActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.ActionWithStatusCount;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test class testing the functionality of triggering a deployment of
 * {@link DistributionSet}s to {@link Target}s.
 *
 */
@Features("Component Tests - Repository")
@Stories("Deployment Management")
public class DeploymentManagementTest extends AbstractIntegrationTest {

    @Autowired
    private EventBus eventBus;

    @Test
    @Description("Test verifies that the repistory retrieves the action including all defined (lazy) details.")
    public void findActionWithLazyDetails() {
        final DistributionSet testDs = TestDataUtil.generateDistributionSet("TestDs", "1.0", softwareManagement,
                distributionSetManagement, new ArrayList<DistributionSetTag>());
        final List<Target> testTarget = targetManagement.createTargets(TestDataUtil.generateTargets(1));
        // one action with one action status is generated
        final Long actionId = deploymentManagement.assignDistributionSet(testDs, testTarget).getActions().get(0);
        final Action action = deploymentManagement.findActionWithDetails(actionId);

        assertThat(action.getDistributionSet()).as("DistributionSet in action").isNotNull();
        assertThat(action.getTarget()).as("Target in action").isNotNull();
        assertThat(action.getTarget().getAssignedDistributionSet()).as("AssignedDistributionSet of target in action")
                .isNotNull();
    }

    @Test
    @Description("Test verifies that the custom query to find all actions include the count of action status is working correctly")
    public void findActionsWithStatusCountByTarget() {
        final DistributionSet testDs = TestDataUtil.generateDistributionSet("TestDs", "1.0", softwareManagement,
                distributionSetManagement, new ArrayList<DistributionSetTag>());
        final List<Target> testTarget = targetManagement.createTargets(TestDataUtil.generateTargets(1));
        // one action with one action status is generated
        final Action action = deploymentManagement.findActionWithDetails(
                deploymentManagement.assignDistributionSet(testDs, testTarget).getActions().get(0));
        // save 2 action status
        actionStatusRepository.save(new ActionStatus(action, Status.RETRIEVED, System.currentTimeMillis()));
        actionStatusRepository.save(new ActionStatus(action, Status.RUNNING, System.currentTimeMillis()));

        final List<ActionWithStatusCount> findActionsWithStatusCountByTarget = deploymentManagement
                .findActionsWithStatusCountByTargetOrderByIdDesc(testTarget.get(0));

        assertThat(findActionsWithStatusCountByTarget).as("wrong action size").hasSize(1);
        assertThat(findActionsWithStatusCountByTarget.get(0).getActionStatusCount()).as("wrong action status size")
                .isEqualTo(3L);
    }

    @Test
    @Description("Ensures that distribution sets can assigned and unassigned to a  distribution set tag. Not exists  distribution set will be ignored for the assignment.")
    public void assignAndUnassignDistributionSetToTag() {
        final List<Long> assignDS = new ArrayList<Long>();
        for (int i = 0; i < 4; i++) {
            assignDS.add(TestDataUtil.generateDistributionSet("DS" + i, "1.0", softwareManagement,
                    distributionSetManagement, new ArrayList<DistributionSetTag>()).getId());
        }
        // not exists
        assignDS.add(Long.valueOf(100));
        final DistributionSetTag tag = tagManagement.createDistributionSetTag(new DistributionSetTag("Tag1"));

        final List<DistributionSet> assignedDS = distributionSetManagement.assignTag(assignDS, tag);
        assertThat(assignedDS.size()).as("assigned ds has wrong size").isEqualTo(4);
        assignedDS.forEach(ds -> assertThat(ds.getTags().size()).as("ds has wrong tag size").isEqualTo(1));

        DistributionSetTag findDistributionSetTag = tagManagement.findDistributionSetTag("Tag1");
        assertThat(assignedDS.size()).as("assigned ds has wrong size")
                .isEqualTo(findDistributionSetTag.getAssignedToDistributionSet().size());

        assertThat(distributionSetManagement.unAssignTag(Long.valueOf(100), findDistributionSetTag))
                .as("unassign tag result should be null").isNull();

        final DistributionSet unAssignDS = distributionSetManagement.unAssignTag(assignDS.get(0),
                findDistributionSetTag);
        assertThat(unAssignDS.getId()).as("unassigned ds is wrong").isEqualTo(assignDS.get(0));
        assertThat(unAssignDS.getTags().size()).as("unassigned ds has wrong tag size").isEqualTo(0);
        findDistributionSetTag = tagManagement.findDistributionSetTag("Tag1");
        assertThat(findDistributionSetTag.getAssignedToDistributionSet().size()).as("ds tag ds has wrong ds size")
                .isEqualTo(3);

        final List<DistributionSet> unAssignTargets = distributionSetManagement
                .unAssignAllDistributionSetsByTag(findDistributionSetTag);
        findDistributionSetTag = tagManagement.findDistributionSetTag("Tag1");
        assertThat(findDistributionSetTag.getAssignedToDistributionSet().size()).as("ds tag has wrong ds size")
                .isEqualTo(0);
        assertThat(unAssignTargets.size()).as("unassigned target has wrong size").isEqualTo(3);
        unAssignTargets
                .forEach(target -> assertThat(target.getTags().size()).as("target has wrong tag size").isEqualTo(0));
    }

    @Test
    @Description("Test verifies that an assignment with automatic cancelation works correctly even if the update is split into multiple partitions on the database.")
    public void multiAssigmentHistoryOverMultiplePagesResultsInTwoActiveAction() {

        final DistributionSet cancelDs = TestDataUtil.generateDistributionSet("Canceled DS", "1.0", softwareManagement,
                distributionSetManagement, new ArrayList<DistributionSetTag>());

        final DistributionSet cancelDs2 = TestDataUtil.generateDistributionSet("Canceled DS", "1.2", softwareManagement,
                distributionSetManagement, new ArrayList<DistributionSetTag>());

        List<Target> targets = targetManagement
                .createTargets(TestDataUtil.generateTargets(Constants.MAX_ENTRIES_IN_STATEMENT + 10));

        targets = deploymentManagement.assignDistributionSet(cancelDs, targets).getAssignedTargets();
        targets = deploymentManagement.assignDistributionSet(cancelDs2, targets).getAssignedTargets();

        targetManagement.findAllTargetIds().forEach(targetIdName -> {
            assertThat(deploymentManagement.findActiveActionsByTarget(
                    targetManagement.findTargetByControllerID(targetIdName.getControllerId())))
                            .as("active action has wrong size").hasSize(2);
        });
    }

    @Test
    @Description("Cancels multiple active actions on a target. Expected behaviour is that with two active "
            + "actions after canceling the second active action the first one is still running as it is not touched by the cancelation. After canceling the first one "
            + "also the target goes back to IN_SYNC as no open action is left.")
    public void manualCancelWithMultipleAssignmentsCancelLastOneFirst() {
        Target target = new Target("4712");
        final DistributionSet dsFirst = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement, true);
        dsFirst.setRequiredMigrationStep(true);
        final DistributionSet dsSecond = TestDataUtil.generateDistributionSet("2", softwareManagement,
                distributionSetManagement, true);
        final DistributionSet dsInstalled = TestDataUtil.generateDistributionSet("installed", softwareManagement,
                distributionSetManagement, true);

        target.getTargetInfo().setInstalledDistributionSet(dsInstalled);
        target = targetManagement.createTarget(target);

        // check initial status
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .as("target has update status").isEqualTo(TargetUpdateStatus.UNKNOWN);

        // assign the two sets in a row
        Action firstAction = assignSet(target, dsFirst);
        Action secondAction = assignSet(target, dsSecond);

        assertThat(actionRepository.findAll()).as("wrong size of actions").hasSize(2);
        assertThat(actionStatusRepository.findAll()).as("wrong size of action status").hasSize(2);

        // we cancel second -> back to first
        deploymentManagement.cancelAction(secondAction,
                targetManagement.findTargetByControllerID(target.getControllerId()));
        secondAction = deploymentManagement.findActionWithDetails(secondAction.getId());
        // confirm cancellation
        secondAction.setStatus(Status.CANCELED);
        controllerManagement.addCancelActionStatus(
                new ActionStatus(secondAction, Status.CANCELED, System.currentTimeMillis()), secondAction);
        assertThat(actionStatusRepository.findAll()).as("wrong size of actions status").hasSize(4);
        assertThat(targetManagement.findTargetByControllerID("4712").getAssignedDistributionSet()).as("wrong ds")
                .isEqualTo(dsFirst);
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .as("wrong update status").isEqualTo(TargetUpdateStatus.PENDING);

        // we cancel first -> back to installed
        deploymentManagement.cancelAction(firstAction,
                targetManagement.findTargetByControllerID(target.getControllerId()));
        firstAction = deploymentManagement.findActionWithDetails(firstAction.getId());
        // confirm cancellation
        firstAction.setStatus(Status.CANCELED);
        controllerManagement.addCancelActionStatus(
                new ActionStatus(firstAction, Status.CANCELED, System.currentTimeMillis()), firstAction);
        assertThat(actionStatusRepository.findAll()).as("wrong size of action status").hasSize(6);
        assertThat(targetManagement.findTargetByControllerID("4712").getAssignedDistributionSet())
                .as("wrong assigned ds").isEqualTo(dsInstalled);
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .as("wrong update status").isEqualTo(TargetUpdateStatus.IN_SYNC);
    }

    @Test
    @Description("Cancels multiple active actions on a target. Expected behaviour is that with two active "
            + "actions after canceling the first active action the system switched to second one. After canceling this one "
            + "also the target goes back to IN_SYNC as no open action is left.")
    public void manualCancelWithMultipleAssignmentsCancelMiddleOneFirst() {
        Target target = new Target("4712");
        final DistributionSet dsFirst = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement, true);
        dsFirst.setRequiredMigrationStep(true);
        final DistributionSet dsSecond = TestDataUtil.generateDistributionSet("2", softwareManagement,
                distributionSetManagement, true);
        final DistributionSet dsInstalled = TestDataUtil.generateDistributionSet("installed", softwareManagement,
                distributionSetManagement, true);

        target.getTargetInfo().setInstalledDistributionSet(dsInstalled);
        target = targetManagement.createTarget(target);

        // check initial status
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .as("wrong update status").isEqualTo(TargetUpdateStatus.UNKNOWN);

        // assign the two sets in a row
        Action firstAction = assignSet(target, dsFirst);
        Action secondAction = assignSet(target, dsSecond);

        assertThat(actionRepository.findAll()).as("wrong size of actions").hasSize(2);
        assertThat(actionStatusRepository.findAll()).as("wrong size of action status").hasSize(2);

        // we cancel first -> second is left
        deploymentManagement.cancelAction(firstAction,
                targetManagement.findTargetByControllerID(target.getControllerId()));
        // confirm cancellation
        firstAction = deploymentManagement.findActionWithDetails(firstAction.getId());
        firstAction.setStatus(Status.CANCELED);
        controllerManagement.addCancelActionStatus(
                new ActionStatus(firstAction, Status.CANCELED, System.currentTimeMillis()), firstAction);
        assertThat(actionStatusRepository.findAll()).as("wrong size of action status").hasSize(4);
        assertThat(targetManagement.findTargetByControllerID("4712").getAssignedDistributionSet())
                .as("wrong assigned ds").isEqualTo(dsSecond);
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .as("wrong target update status").isEqualTo(TargetUpdateStatus.PENDING);

        // we cancel second -> remain assigned until finished cancellation
        deploymentManagement.cancelAction(secondAction,
                targetManagement.findTargetByControllerID(target.getControllerId()));
        secondAction = deploymentManagement.findActionWithDetails(secondAction.getId());
        assertThat(actionStatusRepository.findAll()).as("wrong size of action status").hasSize(5);
        assertThat(targetManagement.findTargetByControllerID("4712").getAssignedDistributionSet())
                .as("wrong assigned ds").isEqualTo(dsSecond);
        // confirm cancellation
        secondAction.setStatus(Status.CANCELED);
        controllerManagement.addCancelActionStatus(
                new ActionStatus(secondAction, Status.CANCELED, System.currentTimeMillis()), secondAction);
        // cancelled success -> back to dsInstalled
        assertThat(targetManagement.findTargetByControllerID("4712").getAssignedDistributionSet())
                .as("wrong installed ds").isEqualTo(dsInstalled);
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .as("wrong target info update status").isEqualTo(TargetUpdateStatus.IN_SYNC);
    }

    @Test
    @Description("Force Quit an Assignment. Expected behaviour is that the action is canceled and is marked as deleted. The assigned Software module")
    public void forceQuitSetActionToInactive() throws InterruptedException {

        Target target = new Target("4712");
        final DistributionSet dsInstalled = TestDataUtil.generateDistributionSet("installed", softwareManagement,
                distributionSetManagement, true);
        target.getTargetInfo().setInstalledDistributionSet(dsInstalled);
        target = targetManagement.createTarget(target);

        final DistributionSet ds = TestDataUtil
                .generateDistributionSet("newDS", softwareManagement, distributionSetManagement, true)
                .setRequiredMigrationStep(true);

        // verify initial status
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .as("wrong target info update status").isEqualTo(TargetUpdateStatus.UNKNOWN);

        Action assigningAction = assignSet(target, ds);

        // verify assignment
        assertThat(actionRepository.findAll()).as("wrong size of action").hasSize(1);
        assertThat(actionStatusRepository.findAll()).as("wrong size of action status").hasSize(1);

        target = targetManagement.findTargetByControllerID(target.getControllerId());

        // force quit assignment
        deploymentManagement.cancelAction(assigningAction, target);
        assigningAction = deploymentManagement.findActionWithDetails(assigningAction.getId());

        deploymentManagement.forceQuitAction(assigningAction);

        assigningAction = deploymentManagement.findActionWithDetails(assigningAction.getId());

        // verify
        assertThat(assigningAction.getStatus()).as("wrong size of status").isEqualTo(Status.CANCELED);
        assertThat(targetManagement.findTargetByControllerID("4712").getAssignedDistributionSet())
                .as("wrong assigned ds").isEqualTo(dsInstalled);
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .as("wrong target update status").isEqualTo(TargetUpdateStatus.IN_SYNC);
    }

    @Test
    @Description("Force Quit an not canceled Assignment. Expected behaviour is that the action can not be force quit and there is thrown an exception.")
    public void forceQuitNotAllowedThrowsException() {

        Target target = new Target("4712");
        final DistributionSet dsInstalled = TestDataUtil.generateDistributionSet("installed", softwareManagement,
                distributionSetManagement, true);
        target.getTargetInfo().setInstalledDistributionSet(dsInstalled);
        target = targetManagement.createTarget(target);

        final DistributionSet ds = TestDataUtil
                .generateDistributionSet("newDS", softwareManagement, distributionSetManagement, true)
                .setRequiredMigrationStep(true);

        // verify initial status
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .as("wrong update status").isEqualTo(TargetUpdateStatus.UNKNOWN);

        final Action assigningAction = assignSet(target, ds);

        // verify assignment
        assertThat(actionRepository.findAll()).as("wrong size of action").hasSize(1);
        assertThat(actionStatusRepository.findAll()).as("wrong size of action status").hasSize(1);

        // force quit assignment
        try {
            deploymentManagement.forceQuitAction(assigningAction);
            fail("expected ForceQuitActionNotAllowedException");
        } catch (final ForceQuitActionNotAllowedException ex) {
        }
    }

    private Action assignSet(final Target target, final DistributionSet ds) {
        deploymentManagement.assignDistributionSet(ds.getId(), new String[] { target.getControllerId() });
        assertThat(
                targetManagement.findTargetByControllerID(target.getControllerId()).getTargetInfo().getUpdateStatus())
                        .as("wrong update status").isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(targetManagement.findTargetByControllerID(target.getControllerId()).getAssignedDistributionSet())
                .as("wrong assigned ds").isEqualTo(ds);
        final Action action = actionRepository.findByTargetAndDistributionSet(pageReq, target, ds).getContent().get(0);
        assertThat(action).as("action should not be null").isNotNull();
        return action;
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

        final EventHandlerMock eventHandlerMock = new EventHandlerMock(20);
        eventBus.register(eventHandlerMock);

        final String myCtrlIDPref = "myCtrlID";
        final Iterable<Target> savedNakedTargets = targetManagement
                .createTargets(TestDataUtil.buildTargetFixtures(10, myCtrlIDPref, "first description"));

        final String myDeployedCtrlIDPref = "myDeployedCtrlID";
        final List<Target> savedDeployedTargets = targetManagement
                .createTargets(TestDataUtil.buildTargetFixtures(20, myDeployedCtrlIDPref, "first description"));

        final DistributionSet ds = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);

        deploymentManagement.assignDistributionSet(ds, savedDeployedTargets);

        // verify that one Action for each assignDistributionSet
        assertThat(actionRepository.findAll(pageReq).getNumberOfElements()).as("wrong size of actions").isEqualTo(20);

        final Iterable<Target> allFoundTargets = targetManagement.findTargetsAll(pageReq).getContent();

        assertThat(allFoundTargets).as("founded targets are wrong").containsAll(savedDeployedTargets)
                .containsAll(savedNakedTargets);
        assertThat(savedDeployedTargets).as("saved target are wrong")
                .doesNotContain(Iterables.toArray(savedNakedTargets, Target.class));
        assertThat(savedNakedTargets).as("saved target are wrong")
                .doesNotContain(Iterables.toArray(savedDeployedTargets, Target.class));

        for (final Target myt : savedNakedTargets) {
            final Target t = targetManagement.findTargetByControllerID(myt.getControllerId());
            assertThat(deploymentManagement.findActionsByTarget(t)).as("action should be empty").isEmpty();
        }

        for (final Target myt : savedDeployedTargets) {
            final Target t = targetManagement.findTargetByControllerID(myt.getControllerId());
            final List<Action> activeActionsByTarget = deploymentManagement.findActiveActionsByTarget(t);
            assertThat(activeActionsByTarget).as("action should not be empty").isNotEmpty();
            assertThat(t.getTargetInfo().getUpdateStatus()).as("wrong target update status")
                    .isEqualTo(TargetUpdateStatus.PENDING);
            for (final Action ua : activeActionsByTarget) {
                assertThat(ua.getDistributionSet()).as("action has wrong ds").isEqualTo(ds);
            }
        }

        final List<TargetAssignDistributionSetEvent> events = eventHandlerMock.getEvents(10, TimeUnit.SECONDS);

        assertTargetAssignDistributionSetEvents(savedDeployedTargets, ds, events);
    }

    @Test
    @Description("Test that it is not possible to assign a distribution set that is not complete.")
    public void failDistributionSetAssigmentThatIsNotComplete() throws InterruptedException {
        final EventHandlerMock eventHandlerMock = new EventHandlerMock(0);
        eventBus.register(eventHandlerMock);

        final List<Target> targets = targetManagement.createTargets(TestDataUtil.generateTargets(10));

        final SoftwareModule ah = softwareManagement
                .createSoftwareModule(new SoftwareModule(appType, "agent-hub", "1.0.1", null, ""));
        final SoftwareModule jvm = softwareManagement
                .createSoftwareModule(new SoftwareModule(runtimeType, "oracle-jre", "1.7.2", null, ""));
        final SoftwareModule os = softwareManagement
                .createSoftwareModule(new SoftwareModule(osType, "poky", "3.0.2", null, ""));

        final DistributionSet incomplete = distributionSetManagement.createDistributionSet(
                new DistributionSet("incomplete", "v1", "", standardDsType, Lists.newArrayList(ah, jvm)));

        try {
            deploymentManagement.assignDistributionSet(incomplete, targets);
            fail("expected IncompleteDistributionSetException");
        } catch (final IncompleteDistributionSetException ex) {
        }

        incomplete.addModule(os);
        final DistributionSet nowComplete = distributionSetManagement.updateDistributionSet(incomplete);

        // give some chance to receive events asynchronously
        Thread.sleep(300);
        final List<TargetAssignDistributionSetEvent> events = eventHandlerMock.getEvents(1, TimeUnit.MILLISECONDS);
        assertThat(events).as("events should be empty").isEmpty();

        final EventHandlerMock eventHandlerMockAfterCompletionOfDs = new EventHandlerMock(10);
        eventBus.register(eventHandlerMockAfterCompletionOfDs);

        assertThat(deploymentManagement.assignDistributionSet(nowComplete, targets).getAssigned())
                .as("assign ds doesn't work").isEqualTo(10);
        assertTargetAssignDistributionSetEvents(targets, nowComplete,
                eventHandlerMockAfterCompletionOfDs.getEvents(10, TimeUnit.SECONDS));
    }

    @Test
    @Description("Multiple deployments or distribution set to target assignment test. Expected behaviour is that a new deployment "
            + "overides unfinished old one which are canceled as part of the operation.")
    public void mutipleDeployments() throws InterruptedException {
        final String undeployedTargetPrefix = "undep-T";
        final int noOfUndeployedTargets = 5;

        final String deployedTargetPrefix = "dep-T";
        final int noOfDeployedTargets = 4;

        final int noOfDistributionSets = 3;

        // Each of the four targets get one assignment (4 * 1 = 4)
        final int expectedNumberOfEventsForAssignment = 4;
        final EventHandlerMock eventHandlerMock = new EventHandlerMock(expectedNumberOfEventsForAssignment);
        eventBus.register(eventHandlerMock);

        // Each of the four targets get two more assignment the which are
        // cancelled (4 * 2 = 8)
        final int expectedNumberOfEventsForCancel = 8;
        final CancelEventHandlerMock cancelEventHandlerMock = new CancelEventHandlerMock(
                expectedNumberOfEventsForCancel);
        eventBus.register(cancelEventHandlerMock);

        final DeploymentResult deploymentResult = prepareComplexRepo(undeployedTargetPrefix, noOfUndeployedTargets,
                deployedTargetPrefix, noOfDeployedTargets, noOfDistributionSets, "myTestDS");

        final List<Long> deployedTargetIDs = deploymentResult.getDeployedTargetIDs();
        final List<Long> undeployedTargetIDs = deploymentResult.getUndeployedTargetIDs();
        final List<Target> savedNakedTargets = deploymentResult.getUndeployedTargets();
        final List<Target> savedDeployedTargets = deploymentResult.getDeployedTargets();

        // retrieving all Actions created by the assignDistributionSet call
        final Page<Action> page = actionRepository.findAll(pageReq);
        // and verify the number
        assertThat(page.getTotalElements()).as("wrong size of actions")
                .isEqualTo(noOfDeployedTargets * noOfDistributionSets);

        // only records retrieved from the DB can be evaluated to be sure that
        // all fields are
        // populated;
        final Iterable<Target> allFoundTargets = targetRepository.findAll();

        final Iterable<Target> deployedTargetsFromDB = targetRepository.findAll(deployedTargetIDs);
        final Iterable<Target> undeployedTargetsFromDB = targetRepository.findAll(undeployedTargetIDs);

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
                .doesNotContain(Iterables.toArray(undeployedTargetsFromDB, Target.class));
        assertThat(undeployedTargetsFromDB).as("content of undeployed target is wrong").containsAll(savedNakedTargets)
                .doesNotContain(Iterables.toArray(deployedTargetsFromDB, Target.class));

        // For each of the 4 targets 1 distribution sets gets assigned
        eventHandlerMock.getEvents(10, TimeUnit.SECONDS);

        // For each of the 4 targets 2 distribution sets gets cancelled
        cancelEventHandlerMock.getEvents(10, TimeUnit.SECONDS);

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
        final DistributionSet dsA = deployResWithDsA.getDistributionSets().get(0);
        final DistributionSet dsB = deployResWithDsB.getDistributionSets().get(0);
        final DistributionSet dsC = deployResWithDsC.getDistributionSets().get(0);

        // retrieving the UpdateActions created by the assignments
        actionRepository.findByDistributionSet(pageRequest, dsA).getContent().get(0);
        actionRepository.findByDistributionSet(pageRequest, dsB).getContent().get(0);
        actionRepository.findByDistributionSet(pageRequest, dsC).getContent().get(0);

        // verifying the correctness of the assignments
        for (final Target t : deployResWithDsA.getDeployedTargets()) {
            assertThat(t.getAssignedDistributionSet().getId()).as("assignment is not correct").isEqualTo(dsA.getId());
            assertThat(targetManagement.findTargetByControllerID(t.getControllerId()).getTargetInfo()
                    .getInstalledDistributionSet()).as("installed ds should be null").isNull();
        }
        for (final Target t : deployResWithDsB.getDeployedTargets()) {
            assertThat(t.getAssignedDistributionSet().getId()).as("assigned ds is wrong").isEqualTo(dsB.getId());
            assertThat(targetManagement.findTargetByControllerID(t.getControllerId()).getTargetInfo()
                    .getInstalledDistributionSet()).as("installed ds should be null").isNull();
        }
        for (final Target t : deployResWithDsC.getDeployedTargets()) {
            assertThat(t.getAssignedDistributionSet().getId()).isEqualTo(dsC.getId());
            assertThat(targetManagement.findTargetByControllerID(t.getControllerId()).getTargetInfo()
                    .getInstalledDistributionSet()).as("installed ds should not be null").isNull();
            assertThat(targetManagement.findTargetByControllerID(t.getControllerId()).getTargetInfo().getUpdateStatus())
                    .as("wrong target info update status").isEqualTo(TargetUpdateStatus.PENDING);
        }

        final List<Target> updatedTsDsA = sendUpdateActionStatusToTargets(dsA, deployResWithDsA.getDeployedTargets(),
                Status.FINISHED, new String[] { "alles gut" });

        // verify, that dsA is deployed correctly
        assertThat(updatedTsDsA).as("ds is not deployed correctly").isEqualTo(deployResWithDsA.getDeployedTargets());
        for (final Target t_ : updatedTsDsA) {
            final Target t = targetManagement.findTargetByControllerID(t_.getControllerId());
            assertThat(t.getAssignedDistributionSet()).as("assigned ds is wrong").isEqualTo(dsA);
            assertThat(targetManagement.findTargetByControllerID(t.getControllerId()).getTargetInfo()
                    .getInstalledDistributionSet()).as("installed ds is wrong").isEqualTo(dsA);
            assertThat(targetManagement.findTargetByControllerID(t.getControllerId()).getTargetInfo().getUpdateStatus())
                    .as("wrong target info update status").isEqualTo(TargetUpdateStatus.IN_SYNC);
            assertThat(deploymentManagement.findActiveActionsByTarget(t)).as("no actions should be active").hasSize(0);
        }

        // deploy dsA to the target which already have dsB deployed -> must
        // remove updActB from
        // activeActions, add a corresponding cancelAction and another
        // UpdateAction for dsA
        final Iterable<Target> deployed2DS = deploymentManagement
                .assignDistributionSet(dsA, deployResWithDsB.getDeployedTargets()).getAssignedTargets();
        actionRepository.findByDistributionSet(pageRequest, dsA).getContent().get(1);

        assertThat(deployed2DS).as("deployed ds is wrong").containsAll(deployResWithDsB.getDeployedTargets());
        assertThat(deployed2DS).as("deployed ds is wrong").hasSameSizeAs(deployResWithDsB.getDeployedTargets());

        for (final Target t_ : deployed2DS) {
            final Target t = targetManagement.findTargetByControllerID(t_.getControllerId());
            assertThat(t.getAssignedDistributionSet()).as("assigned ds is wrong").isEqualTo(dsA);
            assertThat(targetManagement.findTargetByControllerID(t.getControllerId()).getTargetInfo()
                    .getInstalledDistributionSet()).as("installed ds should be null").isNull();
            assertThat(targetManagement.findTargetByControllerID(t.getControllerId()).getTargetInfo().getUpdateStatus())
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

        DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement, distributionSetManagement);

        distributionSetManagement.deleteDistributionSet(dsA.getId());
        dsA = distributionSetManagement.findDistributionSetById(dsA.getId());
        assertThat(dsA).as("ds should be null").isNull();

        // // verify that the ds is not physically deleted
        for (final DistributionSet ds : deploymentResult.getDistributionSets()) {
            distributionSetManagement.deleteDistributionSet(ds.getId());
            final DistributionSet foundDS = distributionSetManagement.findDistributionSetById(ds.getId());
            assertThat(foundDS).as("founded should not be null").isNotNull();
            assertThat(foundDS.isDeleted()).as("founded ds should be deleted").isTrue();
        }

        // verify that deleted attribute is used correctly
        List<DistributionSet> allFoundDS = distributionSetManagement.findDistributionSetsAll(pageReq, false, true)
                .getContent();
        assertThat(allFoundDS.size()).as("no ds should be founded").isEqualTo(0);
        allFoundDS = distributionSetManagement.findDistributionSetsAll(pageRequest, true, true).getContent();
        assertThat(allFoundDS).as("wrong size of founded ds").hasSize(noOfDistributionSets);

        for (final DistributionSet ds : deploymentResult.getDistributionSets()) {
            sendUpdateActionStatusToTargets(ds, deploymentResult.getDeployedTargets(), Status.FINISHED,
                    "blabla alles gut");
        }
        // try to delete again
        distributionSetManagement.deleteDistributionSet(deploymentResult.getDistributionSetIDs()
                .toArray(new Long[deploymentResult.getDistributionSetIDs().size()]));
        // verify that the result is the same, even though distributionSet dsA
        // has been installed
        // successfully and no activeAction is referring to created distribution
        // sets
        allFoundDS = distributionSetManagement.findDistributionSetsAll(pageRequest, false, true).getContent();
        assertThat(allFoundDS.size()).as("no ds should be founded").isEqualTo(0);
        allFoundDS = distributionSetManagement.findDistributionSetsAll(pageRequest, true, true).getContent();
        assertThat(allFoundDS).as("size of founded ds is wrong").hasSize(noOfDistributionSets);

    }

    @Test
    @Description("Deletes multiple targets and verfies that all related metadat is also deleted.")
    public void deletesTargetsAndVerifyCascadeDeletes() {

        final String undeployedTargetPrefix = "undep-T";
        final int noOfUndeployedTargets = 2;

        final String deployedTargetPrefix = "dep-T";
        final int noOfDeployedTargets = 4;

        final int noOfDistributionSets = 3;

        final DeploymentResult deploymentResult = prepareComplexRepo(undeployedTargetPrefix, noOfUndeployedTargets,
                deployedTargetPrefix, noOfDeployedTargets, noOfDistributionSets, "myTestDS");

        for (final DistributionSet ds : deploymentResult.getDistributionSets()) {
            sendUpdateActionStatusToTargets(ds, deploymentResult.getDeployedTargets(), Status.FINISHED,
                    "blabla alles gut");
        }

        assertThat(targetManagement.countTargetsAll()).as("size of targets is wrong").isNotZero();
        assertThat(actionStatusRepository.count()).as("size of action status is wrong").isNotZero();

        targetManagement
                .deleteTargets(deploymentResult.getUndeployedTargetIDs().toArray(new Long[noOfUndeployedTargets]));
        targetManagement.deleteTargets(deploymentResult.getDeployedTargetIDs().toArray(new Long[noOfDeployedTargets]));

        assertThat(targetManagement.countTargetsAll()).as("size of targets should be zero").isZero();
        assertThat(actionStatusRepository.count()).as("size of action status is wrong").isZero();
    }

    private List<Target> sendUpdateActionStatusToTargets(final DistributionSet dsA, final Iterable<Target> targs,
            final Status status, final String... msgs) {
        final List<Target> result = new ArrayList<Target>();
        for (final Target t : targs) {
            final List<Action> findByTarget = actionRepository.findByTarget(t);
            for (final Action action : findByTarget) {
                result.add(sendUpdateActionStatusToTarget(status, action, t, msgs));
            }
        }
        return result;
    }

    private Target sendUpdateActionStatusToTarget(final Status status, final Action updActA, final Target t,
            final String... msgs) {
        updActA.setStatus(status);

        final ActionStatus statusMessages = new ActionStatus();
        statusMessages.setAction(updActA);
        statusMessages.setOccurredAt(System.currentTimeMillis());
        statusMessages.setStatus(status);
        for (final String msg : msgs) {
            statusMessages.addMessage(msg);
        }
        controllerManagament.addUpdateActionStatus(statusMessages, updActA);
        return targetManagement.findTargetByControllerID(t.getControllerId());
    }

    @Test
    @Description("Testing if changing target and the status without refreshing the entities from the DB (e.g. concurrent changes from UI and from controller) works")
    public void alternatingAssignmentAndAddUpdateActionStatus() {

        final DistributionSet dsA = TestDataUtil.generateDistributionSet("a", softwareManagement,
                distributionSetManagement);
        final DistributionSet dsB = TestDataUtil.generateDistributionSet("b", softwareManagement,
                distributionSetManagement);
        Target targ = targetManagement
                .createTarget(TestDataUtil.buildTargetFixture("target-id-A", "first description"));

        List<Target> targs = new ArrayList<Target>();
        targs.add(targ);

        // doing the assignment
        targs = deploymentManagement.assignDistributionSet(dsA, targs).getAssignedTargets();
        targ = targetManagement.findTargetByControllerID(targs.iterator().next().getControllerId());

        // checking the revisions of the created entities
        // verifying that the revision of the object and the revision within the
        // DB has not changed
        assertThat(dsA.getOptLockRevision()).as("lock revision is wrong").isEqualTo(
                distributionSetManagement.findDistributionSetByIdWithDetails(dsA.getId()).getOptLockRevision());

        // verifying that the assignment is correct
        assertThat(deploymentManagement.findActiveActionsByTarget(targ).size()).as("Active target actions are wrong")
                .isEqualTo(1);
        assertThat(deploymentManagement.findActionsByTarget(targ).size()).as("Target actions are wrong").isEqualTo(1);
        assertThat(targ.getTargetInfo().getUpdateStatus()).as("UpdateStatus of target is wrong")
                .isEqualTo(TargetUpdateStatus.PENDING);

        assertThat(targ.getAssignedDistributionSet()).as("Assigned distribution set of target is wrong").isEqualTo(dsA);
        assertThat(deploymentManagement.findActiveActionsByTarget(targ).get(0).getDistributionSet())
                .as("Distribution set of actionn is wrong").isEqualTo(dsA);
        assertThat(deploymentManagement.findActiveActionsByTarget(targ).get(0).getDistributionSet())
                .as("Installed distribution set of action should be null").isNotNull();

        final Page<Action> updAct = actionRepository.findByDistributionSet(pageReq, dsA);
        final Action action = updAct.getContent().get(0);
        action.setStatus(Status.FINISHED);
        final ActionStatus statusMessage = new ActionStatus(action, Status.FINISHED, System.currentTimeMillis(), "");
        controllerManagament.addUpdateActionStatus(statusMessage, action);

        targ = targetManagement.findTargetByControllerID(targ.getControllerId());

        assertThat(deploymentManagement.findActiveActionsByTarget(targ).size()).as("Active action of target")
                .isEqualTo(0);
        assertThat(deploymentManagement.findInActiveActionsByTarget(targ).size()).as("Inactive action of target")
                .isEqualTo(1);
        assertThat(targ.getTargetInfo().getUpdateStatus()).as("UpdateStatus of target")
                .isEqualTo(TargetUpdateStatus.IN_SYNC);

        assertThat(targ.getAssignedDistributionSet()).as("Assigned distribution set of target").isEqualTo(dsA);
        assertThat(targ.getTargetInfo().getInstalledDistributionSet()).as("Installed distribution set of target")
                .isEqualTo(dsA);

        targs = deploymentManagement.assignDistributionSet(dsB.getId(), new String[] { "target-id-A" })
                .getAssignedTargets();

        targ = targs.iterator().next();

        assertEquals("active actions are wrong", 1, deploymentManagement.findActiveActionsByTarget(targ).size());
        assertEquals("target status is wrong", TargetUpdateStatus.PENDING,
                targetManagement.findTargetByControllerID(targ.getControllerId()).getTargetInfo().getUpdateStatus());
        assertEquals(dsB, targ.getAssignedDistributionSet());
        assertEquals("Installed ds is wrong", dsA.getId(),
                targetManagement.findTargetByControllerIDWithDetails(targ.getControllerId()).getTargetInfo()
                        .getInstalledDistributionSet().getId());
        assertEquals("Active ds is wrong", dsB,
                deploymentManagement.findActiveActionsByTarget(targ).get(0).getDistributionSet());

    }

    @Test
    @Description("The test verfies that the DS itself is not changed because of an target assignment"
            + " which is a relationship but not a changed on the entity itself..")
    public void checkThatDsRevisionsIsNotChangedWithTargetAssignment() {
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("a", softwareManagement,
                distributionSetManagement);
        TestDataUtil.generateDistributionSet("b", softwareManagement, distributionSetManagement);
        Target targ = targetManagement
                .createTarget(TestDataUtil.buildTargetFixture("target-id-A", "first description"));

        assertThat(dsA.getOptLockRevision()).as("lock revision is wrong").isEqualTo(
                distributionSetManagement.findDistributionSetByIdWithDetails(dsA.getId()).getOptLockRevision());

        final List<Target> targs = new ArrayList<Target>();
        targs.add(targ);
        final Iterable<Target> savedTargs = deploymentManagement.assignDistributionSet(dsA, targs).getAssignedTargets();
        targ = savedTargs.iterator().next();

        assertThat(dsA.getOptLockRevision()).as("lock revision is wrong").isEqualTo(
                distributionSetManagement.findDistributionSetByIdWithDetails(dsA.getId()).getOptLockRevision());
    }

    @Test
    @Description("Tests the switch from a soft to hard update by API")
    public void forceSoftAction() {
        // prepare
        final Target target = targetManagement.createTarget(new Target("knownControllerId"));
        final DistributionSet ds = TestDataUtil.generateDistributionSet("a", softwareManagement,
                distributionSetManagement);
        // assign ds to create an action
        final DistributionSetAssignmentResult assignDistributionSet = deploymentManagement
                .assignDistributionSet(ds.getId(), ActionType.SOFT, Action.NO_FORCE_TIME, target.getControllerId());
        final Action action = deploymentManagement.findActionWithDetails(assignDistributionSet.getActions().get(0));
        // verify preparation
        Action findAction = deploymentManagement.findAction(action.getId());
        assertThat(findAction.getActionType()).as("action type is wrong").isEqualTo(ActionType.SOFT);

        // test
        deploymentManagement.forceTargetAction(action.getId());

        // verify test
        findAction = deploymentManagement.findAction(action.getId());
        assertThat(findAction.getActionType()).as("action type is wrong").isEqualTo(ActionType.FORCED);
    }

    @Test
    @Description("Tests the switch from a hard to hard update by API, e.g. which in fact should not change anything.")
    public void forceAlreadyForcedActionNothingChanges() {
        // prepare
        final Target target = targetManagement.createTarget(new Target("knownControllerId"));
        final DistributionSet ds = TestDataUtil.generateDistributionSet("a", softwareManagement,
                distributionSetManagement);
        // assign ds to create an action
        final DistributionSetAssignmentResult assignDistributionSet = deploymentManagement
                .assignDistributionSet(ds.getId(), ActionType.FORCED, Action.NO_FORCE_TIME, target.getControllerId());
        final Action action = deploymentManagement.findActionWithDetails(assignDistributionSet.getActions().get(0));
        // verify perparation
        Action findAction = deploymentManagement.findAction(action.getId());
        assertThat(findAction.getActionType()).as("action type is wrong").isEqualTo(ActionType.FORCED);

        // test
        final Action forceTargetAction = deploymentManagement.forceTargetAction(action.getId());

        // verify test
        assertThat(forceTargetAction.getActionType()).as("action type is wrong").isEqualTo(ActionType.FORCED);
        findAction = deploymentManagement.findAction(action.getId());
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
        final Iterable<Target> nakedTargets = targetManagement.createTargets(
                TestDataUtil.buildTargetFixtures(noOfUndeployedTargets, undeployedTargetPrefix, "first description"));

        List<Target> deployedTargets = targetManagement.createTargets(
                TestDataUtil.buildTargetFixtures(noOfDeployedTargets, deployedTargetPrefix, "first description"));

        // creating 10 DistributionSets
        final List<DistributionSet> dsList = TestDataUtil.generateDistributionSets(distributionSetPrefix,
                noOfDistributionSets, softwareManagement, distributionSetManagement);
        String time = String.valueOf(System.currentTimeMillis());
        time = time.substring(time.length() - 5);

        // assigning all DistributionSet to the Target in the list
        // deployedTargets
        for (final DistributionSet ds : dsList) {
            deployedTargets = deploymentManagement.assignDistributionSet(ds, deployedTargets).getAssignedTargets();
        }

        final DeploymentResult deploymentResult = new DeploymentResult(deployedTargets, nakedTargets, dsList,
                deployedTargetPrefix, undeployedTargetPrefix, distributionSetPrefix);
        return deploymentResult;

    }

    private void assertTargetAssignDistributionSetEvents(final List<Target> targets, final DistributionSet ds,
            final List<TargetAssignDistributionSetEvent> events) {
        for (final Target myt : targets) {
            boolean found = false;
            for (final TargetAssignDistributionSetEvent event : events) {
                if (event.getControllerId().equals(myt.getControllerId())) {
                    found = true;
                    final List<Action> activeActionsByTarget = deploymentManagement.findActiveActionsByTarget(myt);
                    assertThat(activeActionsByTarget).as("size of active actions for target is wrong").isNotEmpty();
                    assertThat(event.getActionId()).as("Action id in database and event do not match")
                            .isEqualTo(activeActionsByTarget.get(0).getId());
                    assertThat(event.getSoftwareModules()).as("softwaremodule size is not correct")
                            .containsOnly(ds.getModules().toArray(new SoftwareModule[ds.getModules().size()]));
                }
            }
            assertThat(found).as("No event found for controller " + myt.getControllerId()).isTrue();
        }
    }

    /**
     *
     *
     */
    private class DeploymentResult

    {
        final List<Long> deployedTargetIDs = new ArrayList<Long>();
        final List<Long> undeployedTargetIDs = new ArrayList<Long>();
        final List<Long> distributionSetIDs = new ArrayList<Long>();

        private final List<Target> undeployedTargets = new ArrayList<Target>();
        private final List<Target> deployedTargets = new ArrayList<Target>();
        private final List<DistributionSet> distributionSets = new ArrayList<DistributionSet>();

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

        /**
         * @return the distributionSetIDs
         */
        public List<Long> getDistributionSetIDs() {
            return distributionSetIDs;
        }

        /**
         * @return
         */
        public List<Long> getDeployedTargetIDs() {
            return deployedTargetIDs;
        }

        /**
         * @return
         */
        public List<Target> getUndeployedTargets() {
            return undeployedTargets;
        }

        /**
         * @return
         */
        public List<DistributionSet> getDistributionSets() {
            return distributionSets;
        }

        /**
         * @return
         */
        public List<Target> getDeployedTargets() {
            return deployedTargets;
        }

        /**
         * @return the undeployedTargetIDs
         */
        public List<Long> getUndeployedTargetIDs() {
            return undeployedTargetIDs;
        }

    }

    private static class EventHandlerMock {
        private final List<TargetAssignDistributionSetEvent> events = Collections
                .synchronizedList(new LinkedList<TargetAssignDistributionSetEvent>());
        private final CountDownLatch latch;
        private final int expectedNumberOfEvents;

        private EventHandlerMock(final int expectedNumberOfEvents) {
            this.expectedNumberOfEvents = expectedNumberOfEvents;
            this.latch = new CountDownLatch(expectedNumberOfEvents);
        }

        @Subscribe
        public void handleEvent(final TargetAssignDistributionSetEvent event) {
            events.add(event);
            latch.countDown();
        }

        public List<TargetAssignDistributionSetEvent> getEvents(final long timeout, final TimeUnit unit)
                throws InterruptedException {
            latch.await(timeout, unit);
            final List<TargetAssignDistributionSetEvent> handledEvents = new LinkedList<TargetAssignDistributionSetEvent>(
                    events);
            assertThat(handledEvents).as("Did not receive the expected amount of events (" + expectedNumberOfEvents
                    + ") within timeout. Received events are " + handledEvents).hasSize(expectedNumberOfEvents);

            return handledEvents;
        }
    }

    private static class CancelEventHandlerMock {
        private final List<CancelTargetAssignmentEvent> events = Collections
                .synchronizedList(new LinkedList<CancelTargetAssignmentEvent>());
        private final CountDownLatch latch;
        private final int expectedNumberOfEvents;

        private CancelEventHandlerMock(final int expectedNumberOfEvents) {
            this.expectedNumberOfEvents = expectedNumberOfEvents;
            this.latch = new CountDownLatch(expectedNumberOfEvents);
        }

        @Subscribe
        public void handleEvent(final CancelTargetAssignmentEvent event) {
            events.add(event);
            latch.countDown();
        }

        public List<CancelTargetAssignmentEvent> getEvents(final long timeout, final TimeUnit unit)
                throws InterruptedException {
            latch.await(timeout, unit);
            final List<CancelTargetAssignmentEvent> handledEvents = new LinkedList<CancelTargetAssignmentEvent>(events);
            assertThat(handledEvents).as("Did not receive the expected amount of events (" + expectedNumberOfEvents
                    + ") within timeout. Received events are " + handledEvents).hasSize(expectedNumberOfEvents);
            return handledEvents;
        }
    }

}
