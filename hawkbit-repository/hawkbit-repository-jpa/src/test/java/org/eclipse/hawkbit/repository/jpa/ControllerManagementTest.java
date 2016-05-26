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

import java.util.ArrayList;
import java.util.List;

import javax.validation.ConstraintViolationException;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("Controller Management")
public class ControllerManagementTest extends AbstractIntegrationTest {

    @Test
    @Description("Controller adds a new action status.")
    public void controllerAddsActionStatus() {
        final Target target = new JpaTarget("4712");
        final DistributionSet ds = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        Target savedTarget = targetManagement.createTarget(target);

        final List<Target> toAssign = new ArrayList<>();
        toAssign.add(savedTarget);

        assertThat(savedTarget.getTargetInfo().getUpdateStatus()).isEqualTo(TargetUpdateStatus.UNKNOWN);

        savedTarget = deploymentManagement.assignDistributionSet(ds, toAssign).getAssignedEntity().iterator().next();
        final Action savedAction = deploymentManagement.findActiveActionsByTarget(savedTarget).get(0);

        assertThat(targetManagement.findTargetByControllerID(savedTarget.getControllerId()).getTargetInfo()
                .getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);

        ActionStatus actionStatusMessage = new JpaActionStatus(savedAction, Action.Status.RUNNING,
                System.currentTimeMillis());
        actionStatusMessage.addMessage("foobar");
        savedAction.setStatus(Status.RUNNING);
        controllerManagament.addUpdateActionStatus(actionStatusMessage);
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.PENDING);

        actionStatusMessage = new JpaActionStatus(savedAction, Action.Status.FINISHED, System.currentTimeMillis());
        actionStatusMessage.addMessage(RandomStringUtils.randomAscii(512));
        savedAction.setStatus(Status.FINISHED);
        controllerManagament.addUpdateActionStatus(actionStatusMessage);
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
    public void tryToFinishUpdateProcessMoreThenOnce() {

        // mock
        final Target target = new JpaTarget("Rabbit");
        final DistributionSet ds = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        Target savedTarget = targetManagement.createTarget(target);
        final List<Target> toAssign = new ArrayList<>();
        toAssign.add(savedTarget);
        savedTarget = deploymentManagement.assignDistributionSet(ds, toAssign).getAssignedEntity().iterator().next();
        Action savedAction = deploymentManagement.findActiveActionsByTarget(savedTarget).get(0);

        // test and verify
        final ActionStatus actionStatusMessage = new JpaActionStatus(savedAction, Action.Status.RUNNING,
                System.currentTimeMillis());
        actionStatusMessage.addMessage("running");
        savedAction = controllerManagament.addUpdateActionStatus(actionStatusMessage);
        assertThat(targetManagement.findTargetByControllerID("Rabbit").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.PENDING);

        final ActionStatus actionStatusMessage2 = new JpaActionStatus(savedAction, Action.Status.ERROR,
                System.currentTimeMillis());
        actionStatusMessage2.addMessage("error");
        savedAction = controllerManagament.addUpdateActionStatus(actionStatusMessage2);
        assertThat(targetManagement.findTargetByControllerID("Rabbit").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.ERROR);

        final ActionStatus actionStatusMessage3 = new JpaActionStatus(savedAction, Action.Status.FINISHED,
                System.currentTimeMillis());
        actionStatusMessage3.addMessage("finish");
        controllerManagament.addUpdateActionStatus(actionStatusMessage3);

        targetManagement.findTargetByControllerID("Rabbit").getTargetInfo().getUpdateStatus();

        // test
        assertThat(targetManagement.findTargetByControllerID("Rabbit").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.ERROR);

    }
}
