/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.integration;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Device Management Federation API")
@Stories("Amqp Message Dispatcher Service")
public class AmqpMessageDispatcherServiceIntegrationTest extends AmqpServiceIntegrationTest {

    @Test
    @Description("Verify that a distribution assignment send a download and install message.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1), @Expect(type = TargetPollEvent.class, count = 2) })
    public void sendDownloadAndInstallStatus() {
        registerTargetAndAssignDistributionSet();

        createAndSendTarget(TENANT_EXIST);
        waitUntil(() -> {
            final Optional<Target> findTargetByControllerID = targetManagement
                    .findTargetByControllerID(REGISTER_TARGET);
            return findTargetByControllerID.isPresent() && TargetUpdateStatus.PENDING
                    .equals(findTargetByControllerID.get().getTargetInfo().getUpdateStatus());
        });
        assertDownloadAndInstallMessage(getDistributionSet().getModules());
    }

    @Test
    @Description("Verify that a distribution assignment multiple times send cancel and assign events with right softwaremodules")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 2), @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 1), @Expect(type = TargetPollEvent.class, count = 3) })
    public void assignDistributionSetMultipleTimes() {
        final DistributionSetAssignmentResult assignmentResult = registerTargetAndAssignDistributionSet();

        final DistributionSet distributionSet2 = testdataFactory.createDistributionSet(UUID.randomUUID().toString());
        registerTargetAndAssignDistributionSet(distributionSet2.getId(), TargetUpdateStatus.PENDING,
                getDistributionSet().getModules());
        assertCancelActionMessage(assignmentResult.getActions().get(0));

        createAndSendTarget(TENANT_EXIST);
        waitUntil(() -> {
            final Optional<Target> findTargetByControllerID = targetManagement
                    .findTargetByControllerID(REGISTER_TARGET);
            return findTargetByControllerID.isPresent() && TargetUpdateStatus.PENDING
                    .equals(findTargetByControllerID.get().getTargetInfo().getUpdateStatus());
        });
        assertCancelActionMessage(assignmentResult.getActions().get(0));

    }

    @Test
    @Description("Verify that a cancel assignment send a cancel message.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1), @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1), @Expect(type = TargetPollEvent.class, count = 2) })
    public void sendCancelStatus() {
        final Long actionId = registerTargetAndCancelActionId();

        createAndSendTarget(TENANT_EXIST);
        waitUntil(() -> {
            final Optional<Target> findTargetByControllerID = targetManagement
                    .findTargetByControllerID(REGISTER_TARGET);
            return findTargetByControllerID.isPresent() && TargetUpdateStatus.PENDING
                    .equals(findTargetByControllerID.get().getTargetInfo().getUpdateStatus());
        });
        assertCancelActionMessage(actionId);

    }

    private void waitUntil(final Callable<Boolean> callable) {
        createConditionFactory().until(() -> {
            return securityRule.runAsPrivileged(callable);
        });
    }

    private void createAndSendTarget(final String tenant) {
        createAndSendTarget(REGISTER_TARGET, tenant);
    }

}
