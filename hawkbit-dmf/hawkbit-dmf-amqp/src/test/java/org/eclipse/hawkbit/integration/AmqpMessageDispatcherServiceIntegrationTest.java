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

import org.eclipse.hawkbit.dmf.json.model.DmfActionStatus;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.Test;
import org.springframework.amqp.core.Message;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Device Management Federation API")
@Stories("Amqp Message Dispatcher Service")
public class AmqpMessageDispatcherServiceIntegrationTest extends AmqpServiceIntegrationTest {
    private static final String TARGET_PREFIX = "Dmf_disp_";

    @Test
    @Description("Verify that a distribution assignment send a download and install message.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 6),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1), @Expect(type = TargetPollEvent.class, count = 1) })
    public void sendDownloadAndInstallStatus() {
        final String controllerId = TARGET_PREFIX + "sendDownloadAndInstallStatus";
        registerTargetAndAssignDistributionSet(controllerId);

        waitUntilTargetStatusIsPending(controllerId);
        assertDownloadAndInstallMessage(getDistributionSet().getModules(), controllerId);
    }

    @Test
    @Description("Verify that a distribution assignment sends a download message with window configured but before maintenance window start time.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 6),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1), @Expect(type = TargetPollEvent.class, count = 1) })
    public void sendDownloadStatusBeforeMaintenanceWindowStartTime() {
        final String controllerId = TARGET_PREFIX + "sendDownloadStatusBeforeWindowStartTime";

        registerAndAssertTargetWithExistingTenant(controllerId);
        final DistributionSet distributionSet = testdataFactory.createDistributionSet(UUID.randomUUID().toString());
        testdataFactory.addSoftwareModuleMetadata(distributionSet);
        assignDistributionSetWithMaintenanceWindow(distributionSet.getId(), controllerId, getTestSchedule(2),
                getTestDuration(10), getTestTimeZone());

        waitUntilTargetStatusIsPending(controllerId);
        assertDownloadMessage(distributionSet.getModules(), controllerId);
    }

    @Test
    @Description("Verify that a distribution assignment sends a download and install message with window configured and during maintenance window start time.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 6),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1), @Expect(type = TargetPollEvent.class, count = 1) })
    public void sendDownloadAndInstallStatusMessageDuringMaintenanceWindow() {
        final String controllerId = TARGET_PREFIX + "sendDAndIStatusMessageDuringWindow";

        registerAndAssertTargetWithExistingTenant(controllerId);
        final DistributionSet distributionSet = testdataFactory.createDistributionSet(UUID.randomUUID().toString());
        testdataFactory.addSoftwareModuleMetadata(distributionSet);
        assignDistributionSetWithMaintenanceWindow(distributionSet.getId(), controllerId, getTestSchedule(-5),
                getTestDuration(10), getTestTimeZone());

        waitUntilTargetStatusIsPending(controllerId);
        assertDownloadAndInstallMessage(distributionSet.getModules(), controllerId);
    }

    @Test
    @Description("Verify that a distribution assignment multiple times send cancel and assign events with right softwaremodules")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 2), @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6),
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 6),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 2), @Expect(type = TargetPollEvent.class, count = 3) })
    public void assignDistributionSetMultipleTimes() {
        final String controllerId = TARGET_PREFIX + "assignDistributionSetMultipleTimes";

        final DistributionSetAssignmentResult assignmentResult = registerTargetAndAssignDistributionSet(controllerId);

        final DistributionSet distributionSet2 = testdataFactory.createDistributionSet(UUID.randomUUID().toString());
        registerTargetAndAssignDistributionSet(distributionSet2.getId(), TargetUpdateStatus.PENDING,
                getDistributionSet().getModules(), controllerId);
        assertCancelActionMessage(assignmentResult.getActions().get(0), controllerId);

        createAndSendTarget(controllerId, TENANT_EXIST);
        waitUntilTargetStatusIsPending(controllerId);
        assertCancelActionMessage(assignmentResult.getActions().get(0), controllerId);

    }

    @Test
    @Description("Verify that a cancel assignment send a cancel message.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1), @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 6),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1), @Expect(type = TargetPollEvent.class, count = 2) })
    public void sendCancelStatus() {
        final String controllerId = TARGET_PREFIX + "sendCancelStatus";

        final Long actionId = registerTargetAndCancelActionId(controllerId);

        createAndSendTarget(controllerId, TENANT_EXIST);
        waitUntilTargetStatusIsPending(controllerId);
        assertCancelActionMessage(actionId, controllerId);
    }

    @Test
    @Description("Verify that when a target is deleted a target delete message is send.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1), @Expect(type = TargetDeletedEvent.class, count = 1) })
    public void sendDeleteMessage() {
        final String controllerId = TARGET_PREFIX + "sendDeleteMessage";

        registerAndAssertTargetWithExistingTenant(controllerId);
        targetManagement.deleteByControllerID(controllerId);
        assertDeleteMessage(controllerId);
    }


    @Test
    @Description("Verify that attribute update is requested after device successfully closed software update.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 2),
            @Expect(type = ActionUpdatedEvent.class, count = 2), @Expect(type = ActionCreatedEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 4),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1) })
    public void attributeRequestAfterSuccessfulUpdate() {
        final String controllerId = TARGET_PREFIX + "attributeUpdateRequest";
        registerAndAssertTargetWithExistingTenant(controllerId);
        final Target target = controllerManagement.getByControllerId(controllerId).get();
        final DistributionSet distributionSet = testdataFactory.createDistributionSet(UUID.randomUUID().toString());

        final long actionId1 = assignDistributionSet(distributionSet, target).getActions().get(0);
        waitUntilTargetStatusIsPending(controllerId);
        final Message messageError = createActionStatusUpdateMessage(controllerId, TENANT_EXIST, actionId1,
                DmfActionStatus.ERROR);
        getDmfClient().send(messageError);
        waitUntilTargetStatusIsError(controllerId);

        assertRequestAttributesUpdateMessageAbsent(controllerId);

        final long actionId2 = assignDistributionSet(distributionSet, target).getActions().get(0);
        waitUntilTargetStatusIsPending(controllerId);
        final Message messageFin = createActionStatusUpdateMessage(controllerId, TENANT_EXIST, actionId2,
                DmfActionStatus.FINISHED);
        getDmfClient().send(messageFin);
        waitUntilTargetStatusIsInSync(controllerId);

        assertRequestAttributesUpdateMessage(controllerId);
    }

    private void waitUntilTargetStatusIsPending(final String controllerId) {
        waitUntil(() -> {
            final Optional<Target> findTargetByControllerID = targetManagement.getByControllerID(controllerId);
            return findTargetByControllerID.isPresent()
                    && TargetUpdateStatus.PENDING.equals(findTargetByControllerID.get().getUpdateStatus());
        });
    }

    private void waitUntilTargetStatusIsError(final String controllerId) {
        waitUntil(() -> {
            final Optional<Target> findTargetByControllerID = targetManagement.getByControllerID(controllerId);
            return findTargetByControllerID.isPresent()
                    && TargetUpdateStatus.ERROR.equals(findTargetByControllerID.get().getUpdateStatus());
        });
    }

    private void waitUntilTargetStatusIsInSync(final String controllerId) {
        waitUntil(() -> {
            final Optional<Target> findTargetByControllerID = targetManagement.getByControllerID(controllerId);
            return findTargetByControllerID.isPresent()
                    && TargetUpdateStatus.IN_SYNC.equals(findTargetByControllerID.get().getUpdateStatus());
        });
    }

    private void waitUntil(final Callable<Boolean> callable) {
        createConditionFactory().until(() -> securityRule.runAsPrivileged(callable));
    }
}
