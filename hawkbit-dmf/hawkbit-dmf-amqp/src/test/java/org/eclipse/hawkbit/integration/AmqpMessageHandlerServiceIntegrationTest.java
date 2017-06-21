/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.amqp.AmqpProperties;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.DmfActionStatus;
import org.eclipse.hawkbit.dmf.json.model.DmfActionUpdateStatus;
import org.eclipse.hawkbit.dmf.json.model.DmfAttributeUpdate;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Device Management Federation API")
@Stories("Amqp Message Handler Service")
public class AmqpMessageHandlerServiceIntegrationTest extends AmqpServiceIntegrationTest {
    private static final String CORRELATION_ID = UUID.randomUUID().toString();

    @Autowired
    private AmqpProperties amqpProperties;

    @Test
    @Description("Tests register target")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 2),
            @Expect(type = TargetPollEvent.class, count = 3) })
    public void registerTargets() {
        registerAndAssertTargetWithExistingTenant(REGISTER_TARGET, 1);

        final String target2 = "Target2";
        registerAndAssertTargetWithExistingTenant(target2, 2);
        final Long pollingTimeTarget2 = controllerManagement.findByControllerId(target2).get().getLastTargetQuery();
        registerSameTargetAndAssertBasedOnLastPolling(target2, 2, TargetUpdateStatus.REGISTERED, pollingTimeTarget2);
        Mockito.verifyZeroInteractions(getDeadletterListener());
    }

    @Test
    @Description("Tests register invalid target withy empty controller id. Tests register invalid target with null controller id")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void registerEmptyTarget() {
        createAndSendTarget("", TENANT_EXIST);
        assertAllTargetsCount(0);
        verifyOneDeadLetterMessage();

    }

    @Test
    @Description("Tests register invalid target with whitspace controller id. Tests register invalid target with null controller id")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void registerWhitespaceTarget() {
        createAndSendTarget("Invalid Invalid", TENANT_EXIST);
        assertAllTargetsCount(0);
        verifyOneDeadLetterMessage();

    }

    @Test
    @Description("Tests register invalid target with null controller id. Tests register invalid target with null controller id")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void registerInvalidNullTargets() {
        createAndSendTarget(null, TENANT_EXIST);
        assertAllTargetsCount(0);
        verifyOneDeadLetterMessage();

    }

    @Test
    @Description("Tests not allowed content-type in message. This message should forwarded to the deadletter queue")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void wrongContentType() {
        final Message createTargetMessage = createTargetMessage(REGISTER_TARGET, TENANT_EXIST);
        createTargetMessage.getMessageProperties().setContentType("WrongContentType");
        getDmfClient().send(createTargetMessage);

        verifyOneDeadLetterMessage();
        assertAllTargetsCount(0);
    }

    @Test
    @Description("Tests null reply to property in message header. This message should forwarded to the deadletter queue")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void missingReplyToProperty() {
        final Message createTargetMessage = createTargetMessage(REGISTER_TARGET, TENANT_EXIST);
        createTargetMessage.getMessageProperties().setReplyTo(null);
        getDmfClient().send(createTargetMessage);

        verifyOneDeadLetterMessage();
        assertAllTargetsCount(0);
    }

    @Test
    @Description("Tests missing reply to property in message header. This message should forwarded to the deadletter queue")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void emptyReplyToProperty() {
        final Message createTargetMessage = createTargetMessage(REGISTER_TARGET, TENANT_EXIST);
        createTargetMessage.getMessageProperties().setReplyTo("");
        getDmfClient().send(createTargetMessage);

        verifyOneDeadLetterMessage();
        assertAllTargetsCount(0);
    }

    @Test
    @Description("Tests missing thing id property in message. This message should forwarded to the deadletter queue")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void missingThingIdProperty() {
        final Message createTargetMessage = createTargetMessage(null, TENANT_EXIST);
        createTargetMessage.getMessageProperties().getHeaders().remove(MessageHeaderKey.THING_ID);
        getDmfClient().send(createTargetMessage);

        verifyOneDeadLetterMessage();
        assertAllTargetsCount(0);
    }

    @Test
    @Description("Tests null thing id property in message. This message should forwarded to the deadletter queue")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void nullThingIdProperty() {
        final Message createTargetMessage = createTargetMessage(null, TENANT_EXIST);
        getDmfClient().send(createTargetMessage);

        verifyOneDeadLetterMessage();
        assertAllTargetsCount(0);
    }

    @Test
    @Description("Tests missing tenant message header. This message should forwarded to the deadletter queue")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void missingTenantHeader() {
        final Message createTargetMessage = createTargetMessage(REGISTER_TARGET, TENANT_EXIST);
        createTargetMessage.getMessageProperties().getHeaders().remove(MessageHeaderKey.TENANT);
        getDmfClient().send(createTargetMessage);

        verifyOneDeadLetterMessage();
        assertAllTargetsCount(0);
    }

    @Test
    @Description("Tests null tenant message header. This message should forwarded to the deadletter queue")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void nullTenantHeader() {
        final Message createTargetMessage = createTargetMessage(REGISTER_TARGET, null);
        getDmfClient().send(createTargetMessage);

        verifyOneDeadLetterMessage();
        assertAllTargetsCount(0);
    }

    @Test
    @Description("Tests empty tenant message header. This message should forwarded to the deadletter queue")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void emptyTenantHeader() {
        final Message createTargetMessage = createTargetMessage(REGISTER_TARGET, "");
        getDmfClient().send(createTargetMessage);

        verifyOneDeadLetterMessage();
        assertAllTargetsCount(0);
    }

    @Test
    @Description("Tests tenant not exist. This message should forwarded to the deadletter queue")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void tenantNotExist() {
        final Message createTargetMessage = createTargetMessage(REGISTER_TARGET, "TenantNotExist");
        getDmfClient().send(createTargetMessage);

        verifyOneDeadLetterMessage();
        assertThat(systemManagement.findTenants(PAGE)).hasSize(1);
    }

    @Test
    @Description("Tests missing type message header. This message should forwarded to the deadletter queue")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void missingTypeHeader() {
        final Message createTargetMessage = createTargetMessage(null, TENANT_EXIST);
        createTargetMessage.getMessageProperties().getHeaders().remove(MessageHeaderKey.TYPE);
        getDmfClient().send(createTargetMessage);

        verifyOneDeadLetterMessage();
        assertAllTargetsCount(0);
    }

    @Test
    @Description("Tests null type message header. This message should forwarded to the deadletter queue")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void nullTypeHeader() {
        final Message createTargetMessage = createTargetMessage(null, TENANT_EXIST);
        createTargetMessage.getMessageProperties().getHeaders().put(MessageHeaderKey.TYPE, null);
        getDmfClient().send(createTargetMessage);

        verifyOneDeadLetterMessage();
        assertAllTargetsCount(0);
    }

    @Test
    @Description("Tests empty type message header. This message should forwarded to the deadletter queue")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void emptyTypeHeader() {
        final Message createTargetMessage = createTargetMessage(null, TENANT_EXIST);
        createTargetMessage.getMessageProperties().getHeaders().put(MessageHeaderKey.TYPE, "");
        getDmfClient().send(createTargetMessage);

        verifyOneDeadLetterMessage();
        assertAllTargetsCount(0);
    }

    @Test
    @Description("Tests invalid type message header. This message should forwarded to the deadletter queue")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void invalidTypeHeader() {
        final Message createTargetMessage = createTargetMessage(null, TENANT_EXIST);
        createTargetMessage.getMessageProperties().getHeaders().put(MessageHeaderKey.TYPE, "NotExist");
        getDmfClient().send(createTargetMessage);

        verifyOneDeadLetterMessage();
        assertAllTargetsCount(0);
    }

    @Test
    @Description("Tests null topic message header. This message should forwarded to the deadletter queue")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void nullTopicHeader() {
        final Message eventMessage = createEventMessage(TENANT_EXIST, EventTopic.UPDATE_ACTION_STATUS, "");
        eventMessage.getMessageProperties().getHeaders().put(MessageHeaderKey.TOPIC, null);
        getDmfClient().send(eventMessage);

        verifyOneDeadLetterMessage();
    }

    @Test
    @Description("Tests null topic message header. This message should forwarded to the deadletter queue")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void emptyTopicHeader() {
        final Message eventMessage = createEventMessage(TENANT_EXIST, EventTopic.UPDATE_ACTION_STATUS, "");
        eventMessage.getMessageProperties().getHeaders().put(MessageHeaderKey.TOPIC, "");
        getDmfClient().send(eventMessage);

        verifyOneDeadLetterMessage();
    }

    @Test
    @Description("Tests null topic message header. This message should forwarded to the deadletter queue")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void invalidTopicHeader() {
        final Message eventMessage = createEventMessage(TENANT_EXIST, EventTopic.UPDATE_ACTION_STATUS, "");
        eventMessage.getMessageProperties().getHeaders().put(MessageHeaderKey.TOPIC, "NotExist");
        getDmfClient().send(eventMessage);

        verifyOneDeadLetterMessage();
    }

    @Test
    @Description("Tests missing topic message header. This message should forwarded to the deadletter queue")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void missingTopicHeader() {
        final Message eventMessage = createEventMessage(TENANT_EXIST, EventTopic.UPDATE_ACTION_STATUS, "");
        eventMessage.getMessageProperties().getHeaders().remove(MessageHeaderKey.TOPIC);
        getDmfClient().send(eventMessage);

        verifyOneDeadLetterMessage();
    }

    @Test
    @Description("Tests invalid null message content. This message should forwarded to the deadletter queue")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void updateActionStatusWithNullContent() {
        final Message eventMessage = createEventMessage(TENANT_EXIST, EventTopic.UPDATE_ACTION_STATUS, null);
        getDmfClient().send(eventMessage);
        verifyOneDeadLetterMessage();
    }

    @Test
    @Description("Tests invalid empty message content. This message should forwarded to the deadletter queue")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void updateActionStatusWithEmptyContent() {
        final Message eventMessage = createEventMessage(TENANT_EXIST, EventTopic.UPDATE_ACTION_STATUS, "");
        getDmfClient().send(eventMessage);
        verifyOneDeadLetterMessage();
    }

    @Test
    @Description("Tests invalid json message content. This message should forwarded to the deadletter queue")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void updateActionStatusWithInvalidJsonContent() {
        final Message eventMessage = createEventMessage(TENANT_EXIST, EventTopic.UPDATE_ACTION_STATUS,
                "Invalid Content");
        getDmfClient().send(eventMessage);
        verifyOneDeadLetterMessage();
    }

    @Test
    @Description("Tests invalid topic message header. This message should forwarded to the deadletter queue")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void updateActionStatusWithInvalidActionId() {
        final DmfActionUpdateStatus actionUpdateStatus = new DmfActionUpdateStatus(1L, DmfActionStatus.RUNNING);
        final Message eventMessage = createEventMessage(TENANT_EXIST, EventTopic.UPDATE_ACTION_STATUS,
                actionUpdateStatus);
        getDmfClient().send(eventMessage);
        verifyOneDeadLetterMessage();
    }

    @Test
    @Description("Tests register target and cancel a assignment")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1), @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2), @Expect(type = TargetPollEvent.class, count = 1) })
    public void finishActionStatus() {
        registerTargetAndSendAndAssertUpdateActionStatus(DmfActionStatus.FINISHED, Status.FINISHED);
    }

    @Test
    @Description("Register a target and send a update action status (running). Verfiy if the updated action status is correct.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 0), @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1), @Expect(type = TargetPollEvent.class, count = 1) })
    public void runningActionStatus() {
        registerTargetAndSendAndAssertUpdateActionStatus(DmfActionStatus.RUNNING, Status.RUNNING);
    }

    @Test
    @Description("Register a target and send a update action status (download). Verfiy if the updated action status is correct.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1), @Expect(type = TargetPollEvent.class, count = 1) })
    public void downloadActionStatus() {
        registerTargetAndSendAndAssertUpdateActionStatus(DmfActionStatus.DOWNLOAD, Status.DOWNLOAD);
    }

    @Test
    @Description("Register a target and send a update action status (error). Verfiy if the updated action status is correct.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1), @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2), @Expect(type = TargetPollEvent.class, count = 1) })
    public void errorActionStatus() {
        registerTargetAndSendAndAssertUpdateActionStatus(DmfActionStatus.ERROR, Status.ERROR);
    }

    @Test
    @Description("Register a target and send a update action status (warning). Verfiy if the updated action status is correct.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1), @Expect(type = TargetPollEvent.class, count = 1) })
    public void warningActionStatus() {
        registerTargetAndSendAndAssertUpdateActionStatus(DmfActionStatus.WARNING, Status.WARNING);
    }

    @Test
    @Description("Register a target and send a update action status (retrieved). Verfiy if the updated action status is correct.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1), @Expect(type = TargetPollEvent.class, count = 1) })
    public void retrievedActionStatus() {
        registerTargetAndSendAndAssertUpdateActionStatus(DmfActionStatus.RETRIEVED, Status.RETRIEVED);
    }

    @Test
    @Description("Register a target and send a invalid update action status (cancel). This message should forwarded to the deadletter queue")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1), @Expect(type = TargetPollEvent.class, count = 1) })
    public void cancelNotAllowActionStatus() {
        registerTargetAndSendActionStatus(DmfActionStatus.CANCELED);
        verifyOneDeadLetterMessage();
    }

    @Test
    @Description("Verfiy receiving a download and install message if a deployment is done before the target has polled the first time.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1), @Expect(type = TargetPollEvent.class, count = 2) })
    public void receiveDownLoadAndInstallMessageAfterAssignment() {

        // setup
        controllerManagement.findOrRegisterTargetIfItDoesNotexist(REGISTER_TARGET, TEST_URI);
        final DistributionSet distributionSet = testdataFactory.createDistributionSet(UUID.randomUUID().toString());
        assignDistributionSet(distributionSet.getId(), REGISTER_TARGET);

        // test
        registerAndAssertTargetWithExistingTenant(REGISTER_TARGET, 1, TargetUpdateStatus.PENDING, "bumlux");

        // verify
        assertDownloadAndInstallMessage(distributionSet.getModules());
        Mockito.verifyZeroInteractions(getDeadletterListener());
    }

    @Test
    @Description("Verfiy receiving a cancel update message if a deployment is canceled before the target has polled the first time.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 2) })
    public void receiveCancelUpdateMessageAfterAssignmentWasCanceled() {

        // Setup
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotexist(REGISTER_TARGET, TEST_URI);
        final DistributionSet distributionSet = testdataFactory.createDistributionSet(UUID.randomUUID().toString());
        final DistributionSetAssignmentResult distributionSetAssignmentResult = assignDistributionSet(
                distributionSet.getId(), REGISTER_TARGET);
        deploymentManagement.cancelAction(distributionSetAssignmentResult.getActions().get(0));

        // test
        registerSameTargetAndAssertBasedOnLastPolling(REGISTER_TARGET, 1, TargetUpdateStatus.PENDING,
                target.getLastTargetQuery());

        // verify
        assertCancelActionMessage(distributionSetAssignmentResult.getActions().get(0));
        Mockito.verifyZeroInteractions(getDeadletterListener());
    }

    @Test
    @Description("Register a target and send a invalid update action status (canceled). The current status (pending) is not a canceling state. This message should forwarded to the deadletter queue")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1), @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1), @Expect(type = TargetPollEvent.class, count = 1) })
    public void actionNotExists() {
        final Long actionId = registerTargetAndCancelActionId();
        final Long actionNotExist = actionId + 1;

        sendActionUpdateStatus(new DmfActionUpdateStatus(actionNotExist, DmfActionStatus.CANCELED));
        verifyOneDeadLetterMessage();
    }

    @Test
    @Description("Register a target and send a invalid update action status (cancel_rejected). This message should forwarded to the deadletter queue")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1), @Expect(type = TargetPollEvent.class, count = 1) })
    public void canceledRejectedNotAllowActionStatus() {
        registerTargetAndSendActionStatus(DmfActionStatus.CANCEL_REJECTED);
        verifyOneDeadLetterMessage();
    }

    @Test
    @Description("Register a target and send a valid update action status (cancel_rejected). Verfiy if the updated action status is correct.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1), @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1), @Expect(type = TargetPollEvent.class, count = 1) })
    public void canceledRejectedActionStatus() {
        final Long actionId = registerTargetAndCancelActionId();

        sendActionUpdateStatus(new DmfActionUpdateStatus(actionId, DmfActionStatus.CANCEL_REJECTED));
        assertAction(actionId, 1, Status.RUNNING, Status.CANCELING, Status.CANCEL_REJECTED);
    }

    @Test
    @Description("Verify that sending an update controller attribute message to an existing target works.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1), @Expect(type = TargetPollEvent.class, count = 1) })
    public void updateAttributes() {

        // setup
        final String target = "ControllerAttributeTestTarget";
        registerAndAssertTargetWithExistingTenant(target, 1);
        final DmfAttributeUpdate controllerAttribute = new DmfAttributeUpdate();
        controllerAttribute.getAttributes().put("test1", "testA");
        controllerAttribute.getAttributes().put("test2", "testB");

        // test
        sendUpdateAttributeMessage(target, TENANT_EXIST, controllerAttribute);

        // validate
        assertUpdateAttributes(target, controllerAttribute.getAttributes());
    }

    @Test
    @Description("Verify that sending an update controller attribute message with no thingid header to an existing target does not work.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 0), @Expect(type = TargetPollEvent.class, count = 1) })
    public void updateAttributesWithNoThingId() {

        // setup
        final String target = "ControllerAttributeTestTarget";
        registerAndAssertTargetWithExistingTenant(target, 1);
        final DmfAttributeUpdate controllerAttribute = new DmfAttributeUpdate();
        controllerAttribute.getAttributes().put("test1", "testA");
        controllerAttribute.getAttributes().put("test2", "testB");
        final Message createUpdateAttributesMessage = createUpdateAttributesMessage(null, TENANT_EXIST,
                controllerAttribute);
        createUpdateAttributesMessage.getMessageProperties().getHeaders().remove(MessageHeaderKey.THING_ID);

        // test
        getDmfClient().send(createUpdateAttributesMessage);

        // verify
        verifyOneDeadLetterMessage();
        final DmfAttributeUpdate controllerAttributeEmpty = new DmfAttributeUpdate();
        assertUpdateAttributes(target, controllerAttributeEmpty.getAttributes());
    }

    @Test
    @Description("Verify that sending an update controller attribute message with invalid body to an existing target does not work.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 0), @Expect(type = TargetPollEvent.class, count = 1) })
    public void updateAttributesWithWrongBody() {

        // setup
        final String target = "ControllerAttributeTestTarget";
        registerAndAssertTargetWithExistingTenant(target, 1);
        final DmfAttributeUpdate controllerAttribute = new DmfAttributeUpdate();
        controllerAttribute.getAttributes().put("test1", "testA");
        controllerAttribute.getAttributes().put("test2", "testB");
        final Message createUpdateAttributesMessageWrongBody = createUpdateAttributesMessageWrongBody(target,
                TENANT_EXIST);

        // test
        getDmfClient().send(createUpdateAttributesMessageWrongBody);

        // verify
        verifyOneDeadLetterMessage();
    }

    private Long registerTargetAndSendActionStatus(final DmfActionStatus sendActionStatus) {
        final DistributionSetAssignmentResult assignmentResult = registerTargetAndAssignDistributionSet();
        final Long actionId = assignmentResult.getActions().get(0);
        sendActionUpdateStatus(new DmfActionUpdateStatus(actionId, sendActionStatus));
        return actionId;
    }

    private void sendActionUpdateStatus(final DmfActionUpdateStatus actionStatus) {
        final Message eventMessage = createEventMessage(TENANT_EXIST, EventTopic.UPDATE_ACTION_STATUS, actionStatus);
        getDmfClient().send(eventMessage);
    }

    private void registerTargetAndSendAndAssertUpdateActionStatus(final DmfActionStatus sendActionStatus,
            final Status expectedActionStatus) {
        final Long actionId = registerTargetAndSendActionStatus(sendActionStatus);
        assertAction(actionId, 1, Status.RUNNING, expectedActionStatus);
    }

    private void assertAction(final Long actionId, final int messages, final Status... expectedActionStates) {
        createConditionFactory().await().until(() -> {
            try {
                securityRule.runAsPrivileged(() -> {
                    final List<org.eclipse.hawkbit.repository.model.ActionStatus> actionStatusList = deploymentManagement
                            .findActionStatusByAction(PAGE, actionId).getContent();

                    // Check correlation ID
                    final List<String> messagesFromServer = actionStatusList.stream()
                            .flatMap(actionStatus -> deploymentManagement
                                    .findMessagesByActionStatusId(PAGE, actionStatus.getId()).getContent().stream())
                            .filter(Objects::nonNull)
                            .filter(message -> message
                                    .startsWith(RepositoryConstants.SERVER_MESSAGE_PREFIX + "DMF message"))
                            .collect(Collectors.toList());

                    assertThat(messagesFromServer).hasSize(messages)
                            .allMatch(message -> message.endsWith(CORRELATION_ID));

                    final List<Status> status = actionStatusList.stream().map(actionStatus -> actionStatus.getStatus())
                            .collect(Collectors.toList());
                    assertThat(status).containsOnly(expectedActionStates);

                    return null;
                });
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Message createEventMessage(final String tenant, final EventTopic eventTopic, final Object payload) {
        final MessageProperties messageProperties = createMessagePropertiesWithTenant(tenant);
        messageProperties.getHeaders().put(MessageHeaderKey.TYPE, MessageType.EVENT.toString());
        messageProperties.getHeaders().put(MessageHeaderKey.TOPIC, eventTopic.toString());
        messageProperties.setCorrelationId(CORRELATION_ID.getBytes());

        return createMessage(payload, messageProperties);
    }

    private void sendUpdateAttributeMessage(final String target, final String tenant,
            final DmfAttributeUpdate attributeUpdate) {
        final Message updateMessage = createUpdateAttributesMessage(target, tenant, attributeUpdate);
        getDmfClient().send(updateMessage);
    }

    private int getAuthenticationMessageCount() {
        return Integer.parseInt(getRabbitAdmin().getQueueProperties(amqpProperties.getReceiverQueue())
                .get(RabbitAdmin.QUEUE_MESSAGE_COUNT).toString());
    }

    private void assertEmptyReceiverQueueCount() {
        assertThat(getAuthenticationMessageCount()).isEqualTo(0);
    }

    private void verifyOneDeadLetterMessage() {
        assertEmptyReceiverQueueCount();
        createConditionFactory().until(() -> {
            Mockito.verify(getDeadletterListener(), Mockito.times(1)).handleMessage(Mockito.any());
        });
    }
}
