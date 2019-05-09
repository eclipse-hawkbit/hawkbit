/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.integration;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
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
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.dmf.amqp.api.EventTopic.DOWNLOAD;
import static org.eclipse.hawkbit.dmf.amqp.api.MessageType.EVENT;
import static org.eclipse.hawkbit.repository.model.Action.ActionType.DOWNLOAD_ONLY;

@Feature("Component Tests - Device Management Federation API")
@Story("Amqp Message Dispatcher Service")
public class AmqpMessageDispatcherServiceIntegrationTest extends AbstractAmqpServiceIntegrationTest {
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

        waitUntilTargetHasStatus(controllerId, TargetUpdateStatus.PENDING);
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

        waitUntilTargetHasStatus(controllerId, TargetUpdateStatus.PENDING);
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

        waitUntilTargetHasStatus(controllerId, TargetUpdateStatus.PENDING);
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
        assertCancelActionMessage(assignmentResult.getActionIds().get(0), controllerId);

        createAndSendTarget(controllerId, TENANT_EXIST);
        waitUntilTargetHasStatus(controllerId, TargetUpdateStatus.PENDING);
        assertCancelActionMessage(assignmentResult.getActionIds().get(0), controllerId);

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
        waitUntilTargetHasStatus(controllerId, TargetUpdateStatus.PENDING);
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

        final long actionId1 = assignDistributionSet(distributionSet, target).getActionIds().get(0);
        waitUntilTargetHasStatus(controllerId, TargetUpdateStatus.PENDING);
        final Message messageError = createActionStatusUpdateMessage(controllerId, TENANT_EXIST, actionId1,
                DmfActionStatus.ERROR);
        getDmfClient().send(messageError);
        waitUntilTargetHasStatus(controllerId, TargetUpdateStatus.ERROR);

        assertRequestAttributesUpdateMessageAbsent();

        final long actionId2 = assignDistributionSet(distributionSet, target).getActionIds().get(0);
        waitUntilTargetHasStatus(controllerId, TargetUpdateStatus.PENDING);
        final Message messageFin = createActionStatusUpdateMessage(controllerId, TENANT_EXIST, actionId2,
                DmfActionStatus.FINISHED);
        getDmfClient().send(messageFin);
        waitUntilTargetHasStatus(controllerId, TargetUpdateStatus.IN_SYNC);

        assertRequestAttributesUpdateMessage(controllerId);
    }

    @Test
    @Description("Tests the download_only assignment: asserts correct dmf Message topic, and assigned DS")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 6),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1), @Expect(type = TargetPollEvent.class, count = 1) })
    public void downloadOnlyAssignmentSendsDownloadMessageTopic() {
        final String controllerId = TARGET_PREFIX + "registerTargets_1";
        final DistributionSet distributionSet = createTargetAndDistributionSetAndAssign(controllerId, DOWNLOAD_ONLY);

        // verify
        final Message message = assertReplyMessageHeader(EventTopic.DOWNLOAD, controllerId);
        Mockito.verifyZeroInteractions(getDeadletterListener());

        assertThat(message).isNotNull();
        final Map<String, Object> headers = message.getMessageProperties().getHeaders();
        assertThat(headers).containsEntry("thingId", controllerId);
        assertThat(headers).containsEntry("type", EVENT.toString());
        assertThat(headers).containsEntry("topic", DOWNLOAD.toString());

        final Optional<Target> target = controllerManagement.getByControllerId(controllerId);
        assertThat(target).isPresent();

        // verify the DS was assigned to the Target
        final DistributionSet assignedDistributionSet = ((JpaTarget) target.get()).getAssignedDistributionSet();
        assertThat(assignedDistributionSet.getId()).isEqualTo(distributionSet.getId());
    }

    private void waitUntilTargetHasStatus(final String controllerId, final TargetUpdateStatus status) {
        waitUntil(() -> {
            final Optional<Target> findTargetByControllerID = targetManagement.getByControllerID(controllerId);
            return findTargetByControllerID.isPresent()
                    && status.equals(findTargetByControllerID.get().getUpdateStatus());
        });
    }

    private void waitUntil(final Callable<Boolean> callable) {
        createConditionFactory().until(() -> securityRule.runAsPrivileged(callable));
    }
}
