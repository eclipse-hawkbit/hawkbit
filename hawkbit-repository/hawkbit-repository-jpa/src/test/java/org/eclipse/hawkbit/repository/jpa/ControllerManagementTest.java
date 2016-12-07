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

import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
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
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3) })
    public void controllerAddsActionStatus() {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget();

        assertThat(savedTarget.getTargetInfo().getUpdateStatus()).isEqualTo(TargetUpdateStatus.UNKNOWN);

        savedTarget = assignDistributionSet(ds.getId(), savedTarget.getControllerId()).getAssignedEntity().iterator()
                .next();
        final JpaAction savedAction = (JpaAction) deploymentManagement.findActiveActionsByTarget(savedTarget).get(0);

        assertThat(targetManagement.findTargetByControllerID(savedTarget.getControllerId()).getTargetInfo()
                .getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);

        controllerManagament.addUpdateActionStatus(
                entityFactory.actionStatus().create(savedAction.getId()).status(Action.Status.RUNNING));
        assertThat(targetManagement.findTargetByControllerID(TestdataFactory.DEFAULT_CONTROLLER_ID).getTargetInfo()
                .getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);

        controllerManagament.addUpdateActionStatus(
                entityFactory.actionStatus().create(savedAction.getId()).status(Action.Status.FINISHED));
        assertThat(targetManagement.findTargetByControllerID(TestdataFactory.DEFAULT_CONTROLLER_ID).getTargetInfo()
                .getUpdateStatus()).isEqualTo(TargetUpdateStatus.IN_SYNC);

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
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget();
        savedTarget = assignDistributionSet(ds.getId(), savedTarget.getControllerId()).getAssignedEntity().iterator()
                .next();
        Action savedAction = deploymentManagement.findActiveActionsByTarget(savedTarget).get(0);

        // test and verify
        savedAction = controllerManagament.addUpdateActionStatus(
                entityFactory.actionStatus().create(savedAction.getId()).status(Action.Status.RUNNING));
        assertThat(targetManagement.findTargetByControllerID(TestdataFactory.DEFAULT_CONTROLLER_ID).getTargetInfo()
                .getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);

        savedAction = controllerManagament.addUpdateActionStatus(
                entityFactory.actionStatus().create(savedAction.getId()).status(Action.Status.ERROR));
        assertThat(targetManagement.findTargetByControllerID(TestdataFactory.DEFAULT_CONTROLLER_ID).getTargetInfo()
                .getUpdateStatus()).isEqualTo(TargetUpdateStatus.ERROR);

        // try with disabled late feedback
        repositoryProperties.setRejectActionStatusForClosedAction(true);
        savedAction = controllerManagament.addUpdateActionStatus(
                entityFactory.actionStatus().create(savedAction.getId()).status(Action.Status.FINISHED));

        // test
        assertThat(targetManagement.findTargetByControllerID(TestdataFactory.DEFAULT_CONTROLLER_ID).getTargetInfo()
                .getUpdateStatus()).isEqualTo(TargetUpdateStatus.ERROR);

        // try with enabled late feedback
        repositoryProperties.setRejectActionStatusForClosedAction(false);
        controllerManagament.addUpdateActionStatus(
                entityFactory.actionStatus().create(savedAction.getId()).status(Action.Status.FINISHED));

        // test
        assertThat(targetManagement.findTargetByControllerID(TestdataFactory.DEFAULT_CONTROLLER_ID).getTargetInfo()
                .getUpdateStatus()).isEqualTo(TargetUpdateStatus.ERROR);

    }

    @Test
    @Description("Controller trys to send an update feedback after it has been finished which is reject as the repository is "
            + "configured to reject that.")
    public void sendUpdatesForFinishUpdateProcessDropedIfDisabled() {
        repositoryProperties.setRejectActionStatusForClosedAction(true);

        final Action action = prepareFinishedUpdate();

        controllerManagament.addUpdateActionStatus(
                entityFactory.actionStatus().create(action.getId()).status(Action.Status.RUNNING));

        // nothing changed as "feedback after close" is disabled
        assertThat(targetManagement.findTargetByControllerID(TestdataFactory.DEFAULT_CONTROLLER_ID).getTargetInfo()
                .getUpdateStatus()).isEqualTo(TargetUpdateStatus.IN_SYNC);
        assertThat(actionStatusRepository.findAll(pageReq).getNumberOfElements()).isEqualTo(3);
        assertThat(deploymentManagement.findActionStatusByAction(pageReq, action).getNumberOfElements()).isEqualTo(3);
    }

    @Test
    @Description("Controller trys to send an update feedback after it has been finished which is actepted as the repository is "
            + "configured to accept them.")
    public void sendUpdatesForFinishUpdateProcessAcceptedIfEnabled() {
        repositoryProperties.setRejectActionStatusForClosedAction(false);

        Action action = prepareFinishedUpdate();
        action = controllerManagament.addUpdateActionStatus(
                entityFactory.actionStatus().create(action.getId()).status(Action.Status.RUNNING));

        // nothing changed as "feedback after close" is disabled
        assertThat(targetManagement.findTargetByControllerID(TestdataFactory.DEFAULT_CONTROLLER_ID).getTargetInfo()
                .getUpdateStatus()).isEqualTo(TargetUpdateStatus.IN_SYNC);
        assertThat(actionStatusRepository.findAll(pageReq).getNumberOfElements()).isEqualTo(4);
        assertThat(deploymentManagement.findActionStatusByAction(pageReq, action).getNumberOfElements()).isEqualTo(4);
    }

}
