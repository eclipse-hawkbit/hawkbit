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
import static org.junit.Assert.fail;

import javax.validation.ConstraintViolationException;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("Controller Management")
public class ControllerManagementTest extends AbstractJpaIntegrationTest {
    @Autowired
    private RepositoryProperties repositoryProperties;

    @Test
    @Description("Controller adds a new action status.")
    public void controllerAddsActionStatus() {
        final Target target = new JpaTarget("4712");
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = targetManagement.createTarget(target);

        assertThat(savedTarget.getTargetInfo().getUpdateStatus()).isEqualTo(TargetUpdateStatus.UNKNOWN);

        savedTarget = assignDistributionSet(ds.getId(), savedTarget.getControllerId()).getAssignedEntity().iterator()
                .next();
        final JpaAction savedAction = (JpaAction) deploymentManagement.findActiveActionsByTarget(savedTarget).get(0);

        assertThat(targetManagement.findTargetByControllerID(savedTarget.getControllerId()).getTargetInfo()
                .getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);

        JpaActionStatus actionStatusMessage = new JpaActionStatus(Action.Status.RUNNING, System.currentTimeMillis());
        actionStatusMessage.addMessage("foobar");
        savedAction.setStatus(Status.RUNNING);
        controllerManagament.addUpdateActionStatus(savedAction.getId(), actionStatusMessage);
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.PENDING);

        actionStatusMessage = new JpaActionStatus(Action.Status.FINISHED, System.currentTimeMillis());
        actionStatusMessage.addMessage(RandomStringUtils.randomAscii(512));
        savedAction.setStatus(Status.FINISHED);
        controllerManagament.addUpdateActionStatus(savedAction.getId(), actionStatusMessage);
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.IN_SYNC);

        assertThat(actionStatusRepository.findAll(pageReq).getNumberOfElements()).isEqualTo(3);
        assertThat(deploymentManagement.findActionStatusByAction(pageReq, savedAction).getNumberOfElements())
                .isEqualTo(3);
    }

    @Test
    @Description("Register a controller which does not exist")
    public void testfindOrRegisterTargetIfItDoesNotexist() {
        final Target target = controllerManagament.findOrRegisterTargetIfItDoesNotexist("AA", null);
        assertThat(target).as("target should not be null").isNotNull();

        final Target sameTarget = controllerManagament.findOrRegisterTargetIfItDoesNotexist("AA", null);
        assertThat(target).as("Target should be the equals").isEqualTo(sameTarget);
        assertThat(targetRepository.count()).as("Only 1 target should be registred").isEqualTo(1L);

        // throws exception
        try {
            controllerManagament.findOrRegisterTargetIfItDoesNotexist("", null);
            fail("should fail as target does not exist");
        } catch (final ConstraintViolationException e) {

        }
    }

    @Test
    @Description("Controller trys to finish an update process after it has been finished by an error action status.")
    public void tryToFinishUpdateProcessMoreThanOnce() {

        // mock
        final Target target = new JpaTarget("Rabbit");
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = targetManagement.createTarget(target);
        savedTarget = assignDistributionSet(ds.getId(), savedTarget.getControllerId()).getAssignedEntity().iterator()
                .next();
        Action savedAction = deploymentManagement.findActiveActionsByTarget(savedTarget).get(0);

        // test and verify
        final JpaActionStatus actionStatusMessage = new JpaActionStatus(Action.Status.RUNNING,
                System.currentTimeMillis());
        actionStatusMessage.addMessage("running");
        savedAction = controllerManagament.addUpdateActionStatus(savedAction.getId(), actionStatusMessage);
        assertThat(targetManagement.findTargetByControllerID("Rabbit").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.PENDING);

        final JpaActionStatus actionStatusMessage2 = new JpaActionStatus(Action.Status.ERROR,
                System.currentTimeMillis());
        actionStatusMessage2.addMessage("error");
        savedAction = controllerManagament.addUpdateActionStatus(savedAction.getId(), actionStatusMessage2);
        assertThat(targetManagement.findTargetByControllerID("Rabbit").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.ERROR);

        // try with disabled late feedback
        repositoryProperties.setRejectActionStatusForClosedAction(true);
        final JpaActionStatus actionStatusMessage3 = new JpaActionStatus(Action.Status.FINISHED,
                System.currentTimeMillis());
        actionStatusMessage3.addMessage("finish");
        savedAction = controllerManagament.addUpdateActionStatus(savedAction.getId(), actionStatusMessage3);

        // test
        assertThat(targetManagement.findTargetByControllerID("Rabbit").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.ERROR);

        // try with enabled late feedback
        repositoryProperties.setRejectActionStatusForClosedAction(false);
        final JpaActionStatus actionStatusMessage4 = new JpaActionStatus(Action.Status.FINISHED,
                System.currentTimeMillis());
        actionStatusMessage4.addMessage("finish");
        controllerManagament.addUpdateActionStatus(savedAction.getId(), actionStatusMessage3);

        // test
        assertThat(targetManagement.findTargetByControllerID("Rabbit").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.ERROR);

    }

    @Test
    @Description("Controller trys to send an update feedback after it has been finished which is reject as the repository is "
            + "configured to reject that.")
    public void sendUpdatesForFinishUpdateProcessDropedIfDisabled() {
        repositoryProperties.setRejectActionStatusForClosedAction(true);

        final Action action = prepareFinishedUpdate("Rabbit");

        final JpaActionStatus actionStatusMessage1 = new JpaActionStatus(Action.Status.RUNNING,
                System.currentTimeMillis());
        actionStatusMessage1.addMessage("got some additional feedback");
        controllerManagament.addUpdateActionStatus(action.getId(), actionStatusMessage1);

        // nothing changed as "feedback after close" is disabled
        assertThat(targetManagement.findTargetByControllerID("Rabbit").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.IN_SYNC);
        assertThat(actionStatusRepository.findAll(pageReq).getNumberOfElements()).isEqualTo(3);
        assertThat(deploymentManagement.findActionStatusByAction(pageReq, action).getNumberOfElements()).isEqualTo(3);
    }

    @Test
    @Description("Controller trys to send an update feedback after it has been finished which is actepted as the repository is "
            + "configured to accept them.")
    public void sendUpdatesForFinishUpdateProcessAcceptedIfEnabled() {
        repositoryProperties.setRejectActionStatusForClosedAction(false);

        Action action = prepareFinishedUpdate("Rabbit");

        final JpaActionStatus actionStatusMessage1 = new JpaActionStatus(Action.Status.RUNNING,
                System.currentTimeMillis());
        actionStatusMessage1.addMessage("got some additional feedback");
        action = controllerManagament.addUpdateActionStatus(action.getId(), actionStatusMessage1);

        // nothing changed as "feedback after close" is disabled
        assertThat(targetManagement.findTargetByControllerID("Rabbit").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.IN_SYNC);
        assertThat(actionStatusRepository.findAll(pageReq).getNumberOfElements()).isEqualTo(4);
        assertThat(deploymentManagement.findActionStatusByAction(pageReq, action).getNumberOfElements()).isEqualTo(4);
    }

    private Action prepareFinishedUpdate(final String controllerId) {
        // mock
        final Target target = new JpaTarget(controllerId);
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = targetManagement.createTarget(target);
        savedTarget = assignDistributionSet(ds.getId(), savedTarget.getControllerId()).getAssignedEntity().iterator()
                .next();
        Action savedAction = deploymentManagement.findActiveActionsByTarget(savedTarget).get(0);

        // test and verify
        final JpaActionStatus actionStatusMessage = new JpaActionStatus(Action.Status.RUNNING,
                System.currentTimeMillis());
        actionStatusMessage.addMessage("running");
        savedAction = controllerManagament.addUpdateActionStatus(savedAction.getId(), actionStatusMessage);
        assertThat(targetManagement.findTargetByControllerID(controllerId).getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.PENDING);

        final JpaActionStatus actionStatusMessage2 = new JpaActionStatus(Action.Status.FINISHED,
                System.currentTimeMillis());
        actionStatusMessage2.addMessage("finish");
        savedAction = controllerManagament.addUpdateActionStatus(savedAction.getId(), actionStatusMessage2);

        // test
        assertThat(targetManagement.findTargetByControllerID(controllerId).getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.IN_SYNC);

        assertThat(actionStatusRepository.findAll(pageReq).getNumberOfElements()).isEqualTo(3);
        assertThat(deploymentManagement.findActionStatusByAction(pageReq, savedAction).getNumberOfElements())
                .isEqualTo(3);

        return savedAction;
    }
}
