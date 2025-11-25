/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.amqp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.MULTI_ASSIGNMENTS_ENABLED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.DmfActionStatus;
import org.eclipse.hawkbit.dmf.json.model.DmfActionUpdateStatus;
import org.eclipse.hawkbit.dmf.json.model.DmfAttributeUpdate;
import org.eclipse.hawkbit.dmf.json.model.DmfAutoConfirmation;
import org.eclipse.hawkbit.dmf.json.model.DmfCreateThing;
import org.eclipse.hawkbit.dmf.json.model.DmfUpdateMode;
import org.eclipse.hawkbit.repository.ConfirmationManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.SecurityTokenGeneratorHolder;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.UpdateMode;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionProperties;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.security.SecurityTokenGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

/**
 * Feature: Component Tests - Device Management Federation API<br/>
 * Story: AmqpMessage Handler Service Test
 */
@ExtendWith(MockitoExtension.class)
class AmqpMessageHandlerServiceTest {

    private static final String FAIL_MESSAGE_AMQP_REJECT_REASON = AmqpRejectAndDontRequeueException.class.getSimpleName() + " was expected, ";

    private static final String VIRTUAL_HOST = "vHost";
    private static final String TENANT = "DEFAULT";

    private AmqpMessageHandlerService amqpMessageHandlerService;

    private MessageConverter messageConverter;

    @Mock
    private AmqpMessageDispatcherService amqpMessageDispatcherServiceMock;

    @Mock
    private ControllerManagement controllerManagementMock;

    @Mock
    private ConfirmationManagement confirmationManagementMock;
    @Mock
    private TenantConfigurationManagement tenantConfigurationManagement;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @Captor
    private ArgumentCaptor<Map<String, String>> attributesCaptor;
    @Captor
    private ArgumentCaptor<String> targetIdCaptor;
    @Captor
    private ArgumentCaptor<String> initiatorCaptor;
    @Captor
    private ArgumentCaptor<String> remarkCaptor;
    @Captor
    private ArgumentCaptor<String> targetNameCaptor;
    @Captor
    private ArgumentCaptor<String> targetTypeNameCaptor;
    @Captor
    private ArgumentCaptor<URI> uriCaptor;
    @Captor
    private ArgumentCaptor<UpdateMode> modeCaptor;

    @BeforeEach
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void before() {
        TenantConfigHelper.setTenantConfigurationManagement(tenantConfigurationManagement);
        messageConverter = new Jackson2JsonMessageConverter();
        lenient().when(rabbitTemplate.getMessageConverter()).thenReturn(messageConverter);
        final TenantConfigurationValue multiAssignmentConfig = TenantConfigurationValue.builder().value(Boolean.FALSE)
                .global(Boolean.FALSE).build();
        lenient().when(tenantConfigurationManagement.getConfigurationValue(MULTI_ASSIGNMENTS_ENABLED, Boolean.class))
                .thenReturn(multiAssignmentConfig);

        amqpMessageHandlerService = new AmqpMessageHandlerService(
                rabbitTemplate, amqpMessageDispatcherServiceMock, controllerManagementMock, confirmationManagementMock);
    }

    /**
     * Tests not allowed content-type in message
     */
    @Test
    void wrongContentType() {
        final MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType("xml");
        final Message message = new Message(new byte[0], messageProperties);
        final String type = MessageType.THING_CREATED.name();
        assertThatExceptionOfType(AmqpRejectAndDontRequeueException.class)
                .as(FAIL_MESSAGE_AMQP_REJECT_REASON + "due to wrong content type")
                .isThrownBy(() -> amqpMessageHandlerService.onMessage(message, type, TENANT,
                        VIRTUAL_HOST));
    }

    /**
     * Tests the creation of a target/thing by calling the same method that incoming RabbitMQ messages would access.
     */
    @Test
    void createThing() {
        final String knownThingId = "1";

        processThingCreatedMessage(knownThingId, null);

        assertThingIdCapturedField(knownThingId);
        assertReplyToCapturedField("MyTest");
    }

    /**
     * Tests the creation of a target/thing with specified name by calling the same method that incoming RabbitMQ messages would access.
     */
    @Test
    void createThingWithName() {
        final String knownThingId = "2";
        final String knownThingName = "NonDefaultTargetName";

        final DmfCreateThing payload = new DmfCreateThing(knownThingName, null, null);

        processThingCreatedMessage(knownThingId, payload);

        assertThingIdCapturedField(knownThingId);
        assertReplyToCapturedField("MyTest");
        assertThingNameCapturedField(knownThingName);
    }

    /**
     * Tests the creation of a target/thing with specified type name by calling the same method that incoming RabbitMQ messages would access.
     */
    @Test
    void createThingWithType() {
        final String knownThingId = "2";
        final String knownThingTypeName = "TargetTypeName";

        final DmfCreateThing payload = new DmfCreateThing(null, knownThingTypeName, null);

        processThingCreatedMessage(knownThingId, payload);

        assertThingIdCapturedField(knownThingId);
        assertReplyToCapturedField("MyTest");
        assertThingTypeCapturedField(knownThingTypeName);
    }

    /**
     * Tests not allowed body in message
     */
    @Test
    void createThingWithWrongBody() {
        final Message message = createMessage("Not allowed Body".getBytes(), getThingCreatedMessageProperties("3"));
        final String type = MessageType.THING_CREATED.name();
        assertThatExceptionOfType(MessageConversionException.class)
                .as("MessageConversionException was excepeted due to wrong body")
                .isThrownBy(() -> amqpMessageHandlerService.onMessage(message, type, TENANT, VIRTUAL_HOST));
    }

    /**
     * Tests the creation of a target/thing with specified attributes by calling the same method that incoming RabbitMQ messages would access.
     */
    @Test
    void createThingWithAttributes() {
        final String knownThingId = "4";

        final DmfAttributeUpdate attributeUpdate = dmfAttributeUpdate();

        final DmfCreateThing payload = new DmfCreateThing(null, null, attributeUpdate);

        processThingCreatedMessage(knownThingId, payload);

        assertThingIdCapturedField(knownThingId);
        assertReplyToCapturedField("MyTest");
        assertThingAttributesCapturedField(attributeUpdate.getAttributes());
    }

    /**
     * Tests the creation of a target/thing with specified name and attributes by calling the same method that incoming RabbitMQ messages would access.
     */
    @Test
    void createThingWithNameAndAttributes() {
        final String knownThingId = "5";
        final String knownThingName = "NonDefaultTargetName";

        final DmfAttributeUpdate attributeUpdate = new DmfAttributeUpdate(
                Map.of("testKey1", "testValue1", "testKey2", "testValue2"), DmfUpdateMode.REPLACE);
        final DmfCreateThing payload = new DmfCreateThing(knownThingName, null, attributeUpdate);

        processThingCreatedMessage(knownThingId, payload);

        assertThingIdCapturedField(knownThingId);
        assertReplyToCapturedField("MyTest");
        assertThingAttributesCapturedField(attributeUpdate.getAttributes());
        assertThingAttributesModeCapturedField(UpdateMode.REPLACE);
    }

    /**
     * Tests the target attribute update by calling the same method that incoming RabbitMQ messages would access.
     */
    @Test
    void updateAttributes() {
        final String knownThingId = "1";
        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        messageProperties.setHeader(MessageHeaderKey.THING_ID, knownThingId);
        messageProperties.setHeader(MessageHeaderKey.TOPIC, "UPDATE_ATTRIBUTES");
        final DmfAttributeUpdate attributeUpdate = dmfAttributeUpdate();

        final Message message = createMessage(attributeUpdate, messageProperties);

        when(controllerManagementMock.updateControllerAttributes(targetIdCaptor.capture(), attributesCaptor.capture(),
                modeCaptor.capture())).thenReturn(null);

        amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT, VIRTUAL_HOST);

        assertThingIdCapturedField(knownThingId);
        assertThingAttributesCapturedField(attributeUpdate.getAttributes());
    }

    /**
     * Verifies that the update mode is retrieved from the UPDATE_ATTRIBUTES message and passed to the controller management.
     */
    @Test
    void attributeUpdateModes() {
        final String knownThingId = "1";
        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        messageProperties.setHeader(MessageHeaderKey.THING_ID, knownThingId);
        messageProperties.setHeader(MessageHeaderKey.TOPIC, "UPDATE_ATTRIBUTES");
        DmfAttributeUpdate attributeUpdate = dmfAttributeUpdate();

        when(controllerManagementMock.updateControllerAttributes(targetIdCaptor.capture(), attributesCaptor.capture(),
                modeCaptor.capture())).thenReturn(null);

        // send a message which does not specify a update mode
        Message message = createMessage(attributeUpdate, messageProperties);
        amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT, VIRTUAL_HOST);
        // verify that NO fallback is made on the way to the controller
        // management layer
        assertThingAttributesModeCapturedField(null);

        // send a message which specifies update mode MERGE
        attributeUpdate = dmfAttributeUpdate(DmfUpdateMode.MERGE);
        message = createMessage(attributeUpdate, messageProperties);
        amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT, VIRTUAL_HOST);
        // verify that the update mode is converted and forwarded as expected
        assertThingAttributesModeCapturedField(UpdateMode.MERGE);

        // send a message which specifies update mode REPLACE
        attributeUpdate = dmfAttributeUpdate(DmfUpdateMode.REPLACE);
        message = createMessage(attributeUpdate, messageProperties);
        amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT, VIRTUAL_HOST);
        // verify that the update mode is converted and forwarded as expected
        assertThingAttributesModeCapturedField(UpdateMode.REPLACE);

        // send a message which specifies update mode REMOVE
        attributeUpdate = dmfAttributeUpdate(DmfUpdateMode.REMOVE);
        message = createMessage(attributeUpdate, messageProperties);
        amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT, VIRTUAL_HOST);
        // verify that the update mode is converted and forwarded as expected
        assertThingAttributesModeCapturedField(UpdateMode.REMOVE);
    }

    private static DmfAttributeUpdate dmfAttributeUpdate() {
        return dmfAttributeUpdate(null);
    }

    private static DmfAttributeUpdate dmfAttributeUpdate(final DmfUpdateMode mode) {
        return new DmfAttributeUpdate(
                Map.of("testKey1", "testValue1", "testKey2", "testValue2"), mode);
    }

    /**
     * Tests the creation of a thing without a 'reply to' header in message.
     */
    @Test
    void createThingWithoutReplyTo() {
        final MessageProperties messageProperties = createMessageProperties(MessageType.THING_CREATED, null);
        messageProperties.setHeader(MessageHeaderKey.THING_ID, "1");
        final Message message = createMessage("", messageProperties);
        final String type = MessageType.THING_CREATED.name();
        assertThatExceptionOfType(AmqpRejectAndDontRequeueException.class)
                .as(FAIL_MESSAGE_AMQP_REJECT_REASON + "since no replyTo header was set")
                .isThrownBy(() -> amqpMessageHandlerService.onMessage(message, type, TENANT, VIRTUAL_HOST));
    }

    /**
     * Tests the creation of a target/thing without a thingID by calling the same method that incoming RabbitMQ messages would access.
     */
    @Test
    void createThingWithoutID() {
        final MessageProperties messageProperties = createMessageProperties(MessageType.THING_CREATED);
        final Message message = createMessage(new byte[0], messageProperties);
        final String type = MessageType.THING_CREATED.name();
        assertThatExceptionOfType(AmqpRejectAndDontRequeueException.class)
                .as(FAIL_MESSAGE_AMQP_REJECT_REASON + "since no thingId was set")
                .isThrownBy(() -> amqpMessageHandlerService.onMessage(message, type, TENANT, VIRTUAL_HOST));
    }

    /**
     * Tests the call of the same method that incoming RabbitMQ messages would access with an unknown message type.
     */
    @Test
    void unknownMessageType() {
        final String type = "bumlux";
        final MessageProperties messageProperties = createMessageProperties(MessageType.THING_CREATED);
        messageProperties.setHeader(MessageHeaderKey.THING_ID, "");
        final Message message = createMessage(new byte[0], messageProperties);

        assertThatExceptionOfType(AmqpRejectAndDontRequeueException.class)
                .as(FAIL_MESSAGE_AMQP_REJECT_REASON + "due to unknown message type")
                .isThrownBy(() -> amqpMessageHandlerService.onMessage(message, type, TENANT, VIRTUAL_HOST));
    }

    /**
     * Tests a invalid message without event topic
     */
    @Test
    void invalidEventTopic() {
        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        final Message message = new Message(new byte[0], messageProperties);

        assertThatExceptionOfType(AmqpRejectAndDontRequeueException.class)
                .as(FAIL_MESSAGE_AMQP_REJECT_REASON + "due to unknown message type").isThrownBy(
                        () -> amqpMessageHandlerService.onMessage(message, "unknownMessageType", TENANT, VIRTUAL_HOST));

        final String type = MessageType.EVENT.name();
        messageProperties.setHeader(MessageHeaderKey.TOPIC, "wrongTopic");
        assertThatExceptionOfType(AmqpRejectAndDontRequeueException.class)
                .as(FAIL_MESSAGE_AMQP_REJECT_REASON + "due to unknown topic")
                .isThrownBy(() -> amqpMessageHandlerService.onMessage(message, type, TENANT, VIRTUAL_HOST));

        messageProperties.setHeader(MessageHeaderKey.TOPIC, EventTopic.CANCEL_DOWNLOAD.name());
        assertThatExceptionOfType(AmqpRejectAndDontRequeueException.class)
                .as(FAIL_MESSAGE_AMQP_REJECT_REASON + "because there was no event topic")
                .isThrownBy(() -> amqpMessageHandlerService.onMessage(message, type, TENANT, VIRTUAL_HOST));
    }

    /**
     * Tests the update of an action of a target without a exist action id
     */
    @Test
    void updateActionStatusWithoutActionId() {
        when(controllerManagementMock.findActionWithDetails(anyLong())).thenReturn(Optional.empty());
        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        messageProperties.setHeader(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ACTION_STATUS.name());
        final DmfActionUpdateStatus actionUpdateStatus = new DmfActionUpdateStatus(1L, DmfActionStatus.DOWNLOAD);
        final Message message = createMessage(actionUpdateStatus, messageProperties);
        final String type = MessageType.EVENT.name();
        assertThatExceptionOfType(AmqpRejectAndDontRequeueException.class)
                .as(FAIL_MESSAGE_AMQP_REJECT_REASON + "since no action id was set")
                .isThrownBy(() -> amqpMessageHandlerService.onMessage(message, type, TENANT, VIRTUAL_HOST));
    }

    /**
     * Tests the update of an action of a target without a exist action id
     */
    @Test
    void updateActionStatusWithoutExistActionId() {
        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        messageProperties.setHeader(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ACTION_STATUS.name());
        when(controllerManagementMock.findActionWithDetails(anyLong())).thenReturn(Optional.empty());

        final DmfActionUpdateStatus actionUpdateStatus = createActionUpdateStatus(DmfActionStatus.DOWNLOAD);
        final Message message = createMessage(actionUpdateStatus, messageProperties);
        final String type = MessageType.EVENT.name();
        assertThatExceptionOfType(AmqpRejectAndDontRequeueException.class)
                .as(FAIL_MESSAGE_AMQP_REJECT_REASON + "since no action id was set")
                .isThrownBy(() -> amqpMessageHandlerService.onMessage(message, type, TENANT,
                        VIRTUAL_HOST));
    }

    /**
     * Tests that messages which cause quota violations are not re-added to message queue so they would block other communication.
     */
    @Test
    void quotaExceeded() {
        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        messageProperties.setHeader(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ACTION_STATUS.name());

        final DmfActionUpdateStatus actionUpdateStatus = createActionUpdateStatus(DmfActionStatus.WARNING);
        final Message message = createMessage(actionUpdateStatus, messageProperties);
        final Action action = mock(Action.class);
        when(action.getId()).thenReturn(2L);

        when(controllerManagementMock.findActionWithDetails(anyLong())).thenReturn(Optional.of(action));
        when(controllerManagementMock.addUpdateActionStatus(any())).thenThrow(new AssignmentQuotaExceededException());

        final String type = MessageType.EVENT.name();
        assertThatExceptionOfType(AmqpRejectAndDontRequeueException.class)
                .isThrownBy(() -> amqpMessageHandlerService.onMessage(message, type, TENANT, VIRTUAL_HOST));
    }

    /**
     * Test next update is provided on finished action
     */
    @Test
    void lookupNextUpdateActionAfterFinished() throws IllegalAccessException {

        // Mock
        final Action action = createActionWithTarget(22L);
        when(controllerManagementMock.findActionWithDetails(anyLong())).thenReturn(Optional.of(action));
        when(controllerManagementMock.addUpdateActionStatus(any())).thenReturn(action);
        // for the test the same action can be used
        when(controllerManagementMock.findActiveActionWithHighestWeight(any())).thenReturn(Optional.of(action));

        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        messageProperties.setHeader(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ACTION_STATUS.name());
        final DmfActionUpdateStatus actionUpdateStatus = createActionUpdateStatus(DmfActionStatus.FINISHED, 23L);
        final Message message = createMessage(actionUpdateStatus, messageProperties);

        // test
        amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT, VIRTUAL_HOST);

        final ArgumentCaptor<ActionProperties> actionPropertiesCaptor = ArgumentCaptor.forClass(ActionProperties.class);
        final ArgumentCaptor<Target> targetCaptor = ArgumentCaptor.forClass(Target.class);

        verify(amqpMessageDispatcherServiceMock, times(1))
                .sendUpdateMessageToTarget(actionPropertiesCaptor.capture(), targetCaptor.capture(), any(Map.class));
        final ActionProperties actionProperties = actionPropertiesCaptor.getValue();
        assertThat(actionProperties).isNotNull();
        assertThat(actionProperties.getTenant()).as("event has tenant").isEqualTo("DEFAULT");
        assertThat(targetCaptor.getValue().getControllerId()).as("event has wrong controller id").isEqualTo("target1");
        assertThat(actionProperties.getId()).as("event has wrong action id").isEqualTo(22L);
    }

    /**
     * Test feedback code is persisted in messages when provided with DmfActionUpdateStatus
     */
    @Test
    void feedBackCodeIsPersistedInMessages() throws IllegalAccessException {
        // Mock
        final Action action = createActionWithTarget(22L);
        when(controllerManagementMock.findActionWithDetails(anyLong())).thenReturn(Optional.of(action));
        when(controllerManagementMock.addUpdateActionStatus(any())).thenReturn(action);
        // for the test the same action can be used
        when(controllerManagementMock.findActiveActionWithHighestWeight(any())).thenReturn(Optional.of(action));

        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        messageProperties.setHeader(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ACTION_STATUS.name());

        final DmfActionUpdateStatus actionUpdateStatus = new DmfActionUpdateStatus(
                23L, DmfActionStatus.RUNNING, null, 2L, null, 12);

        final Message message = createMessage(actionUpdateStatus, messageProperties);

        // test
        amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT, VIRTUAL_HOST);
    }

    /**
     * Tests the deletion of a target/thing, requested by the target itself.
     */
    @Test
    void deleteThing() {
        // prepare valid message
        final String knownThingId = "1";
        final MessageProperties messageProperties = createMessageProperties(MessageType.THING_REMOVED);
        messageProperties.setHeader(MessageHeaderKey.THING_ID, knownThingId);
        final Message message = createMessage(new byte[0], messageProperties);

        // test
        amqpMessageHandlerService.onMessage(message, MessageType.THING_REMOVED.name(), TENANT, VIRTUAL_HOST);

        // verify
        verify(controllerManagementMock).deleteExistingTarget(knownThingId);
    }

    /**
     * Tests the deletion of a target/thing with missing thingId
     */
    @Test
    void deleteThingWithoutThingId() {
        // prepare invalid message
        final MessageProperties messageProperties = createMessageProperties(MessageType.THING_REMOVED);
        final Message message = createMessage(new byte[0], messageProperties);

        final String type = MessageType.THING_REMOVED.name();
        assertThatExceptionOfType(AmqpRejectAndDontRequeueException.class)
                .as(FAIL_MESSAGE_AMQP_REJECT_REASON + "since no thingId was set")
                .isThrownBy(() -> amqpMessageHandlerService.onMessage(message, type, TENANT, VIRTUAL_HOST));
    }

    /**
     * Tests activating auto-confirmation on a target.
     */
    @Test
    void setAutoConfirmationStateActive() {
        final String knownThingId = "1";
        final String initiator = "iAmTheInitiator";
        final String remark = "remarkForTesting";

        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        messageProperties.setHeader(MessageHeaderKey.THING_ID, knownThingId);
        messageProperties.setHeader(MessageHeaderKey.TOPIC, "UPDATE_AUTO_CONFIRM");
        final DmfAutoConfirmation autoConfirmation = new DmfAutoConfirmation(true, initiator, remark);

        final Message message = createMessage(autoConfirmation, messageProperties);

        when(controllerManagementMock.activateAutoConfirmation(targetIdCaptor.capture(), initiatorCaptor.capture(),
                remarkCaptor.capture())).thenReturn(null);

        amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT, VIRTUAL_HOST);

        verify(controllerManagementMock, times(0)).deactivateAutoConfirmation(anyString());

        assertThingIdCapturedField(knownThingId);
        assertInitiatorCapturedField(initiator);
        assertRemarkCapturedField(remark);
    }

    /**
     * Tests deactivating auto-confirmation on a target.
     */
    @Test
    void setAutoConfirmationStateDeactivated() {
        final String knownThingId = "1";

        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        messageProperties.setHeader(MessageHeaderKey.THING_ID, knownThingId);
        messageProperties.setHeader(MessageHeaderKey.TOPIC, "UPDATE_AUTO_CONFIRM");
        final DmfAutoConfirmation autoConfirmation = new DmfAutoConfirmation(false, null, null);

        final Message message = createMessage(autoConfirmation, messageProperties);

        amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT, VIRTUAL_HOST);

        verify(controllerManagementMock).deactivateAutoConfirmation(targetIdCaptor.capture());
        verify(controllerManagementMock, times(0)).activateAutoConfirmation(anyString(), anyString(), anyString());

        assertThingIdCapturedField(knownThingId);
    }

    private void processThingCreatedMessage(final String thingId, final DmfCreateThing payload) {
        final MessageProperties messageProperties = getThingCreatedMessageProperties(thingId);
        final Message message = createMessage(payload != null ? payload : new byte[0], messageProperties);

        final Target targetMock = mock(Target.class);
        if (payload == null) {
            when(controllerManagementMock.findOrRegisterTargetIfItDoesNotExist(targetIdCaptor.capture(),
                    uriCaptor.capture())).thenReturn(targetMock);
        } else {
            when(controllerManagementMock.findOrRegisterTargetIfItDoesNotExist(targetIdCaptor.capture(),
                    uriCaptor.capture(), targetNameCaptor.capture(), targetTypeNameCaptor.capture()))
                    .thenReturn(targetMock);
            if (payload.getAttributeUpdate() != null) {
                when(controllerManagementMock.updateControllerAttributes(targetIdCaptor.capture(),
                        attributesCaptor.capture(), modeCaptor.capture())).thenReturn(null);
            }
        }
        when(controllerManagementMock.findActiveActionWithHighestWeight(any())).thenReturn(Optional.empty());

        amqpMessageHandlerService.onMessage(message, MessageType.THING_CREATED.name(), TENANT, VIRTUAL_HOST);
    }

    private void assertThingIdCapturedField(final String thingId) {
        assertThat(targetIdCaptor.getValue()).as("Thing id is wrong").isEqualTo(thingId);
    }

    private void assertReplyToCapturedField(final String replyTo) {
        assertThat(uriCaptor.getValue()).as("Uri is not right").hasToString("amqp://" + VIRTUAL_HOST + "/" + replyTo);
    }

    private void assertInitiatorCapturedField(final String initiator) {
        assertThat(initiatorCaptor.getValue()).as("Initiator is wrong").isEqualTo(initiator);
    }

    private void assertRemarkCapturedField(final String remark) {
        assertThat(remarkCaptor.getValue()).as("Remark is wrong").isEqualTo(remark);
    }

    private void assertThingNameCapturedField(final String thingName) {
        assertThat(targetNameCaptor.getValue()).as("Thing name is wrong").isEqualTo(thingName);
    }

    private void assertThingTypeCapturedField(final String thingType) {
        assertThat(targetTypeNameCaptor.getValue()).as("Thing type is wrong").isEqualTo(thingType);
    }

    private void assertThingAttributesCapturedField(final Map<String, String> attributes) {
        assertThat(attributesCaptor.getValue()).as("Attributes is not right").isEqualTo(attributes);
    }

    private void assertThingAttributesModeCapturedField(final UpdateMode attributesUpdateMode) {
        assertThat(modeCaptor.getValue()).as("Attributes update mode is not right").isEqualTo(attributesUpdateMode);
    }

    private DmfActionUpdateStatus createActionUpdateStatus(final DmfActionStatus status) {
        return createActionUpdateStatus(status, 2L);
    }

    private DmfActionUpdateStatus createActionUpdateStatus(final DmfActionStatus status, final Long id) {
        return new DmfActionUpdateStatus(id, status, null, 2L, null, null);
    }

    private MessageProperties createMessageProperties(final MessageType type) {
        return createMessageProperties(type, "MyTest");
    }

    private MessageProperties createMessageProperties(final MessageType type, final String replyTo) {
        final MessageProperties messageProperties = new MessageProperties();
        if (type != null) {
            messageProperties.setHeader(MessageHeaderKey.TYPE, type.name());
        }
        messageProperties.setHeader(MessageHeaderKey.TENANT, TENANT);
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setReplyTo(replyTo);
        return messageProperties;
    }

    private Action createActionWithTarget(final Long targetId) throws IllegalAccessException {
        // is needed for the creation of targets
        initializeSecurityTokenGenerator();

        // Mock
        final Action actionMock = mock(Action.class);
        final Target targetMock = mock(Target.class);
        final DistributionSet distributionSetMock = mock(DistributionSet.class);

        when(actionMock.getDistributionSet()).thenReturn(distributionSetMock);
        when(actionMock.getId()).thenReturn(targetId);
        when(actionMock.getTenant()).thenReturn("DEFAULT");
        when(actionMock.getTarget()).thenReturn(targetMock);
        when(actionMock.getActionType()).thenReturn(Action.ActionType.SOFT);
        when(targetMock.getControllerId()).thenReturn("target1");
        return actionMock;
    }

    private void initializeSecurityTokenGenerator() throws IllegalAccessException {
        final SecurityTokenGeneratorHolder instance = SecurityTokenGeneratorHolder.getInstance();
        final Field[] fields = instance.getClass().getDeclaredFields();
        for (final Field field : fields) {
            if (field.getType().isAssignableFrom(SecurityTokenGenerator.class)) {
                field.setAccessible(true);
                field.set(instance, new SecurityTokenGenerator());
            }
        }
    }

    private MessageProperties getThingCreatedMessageProperties(final String thingId) {
        final MessageProperties messageProperties = createMessageProperties(MessageType.THING_CREATED);
        messageProperties.setHeader(MessageHeaderKey.THING_ID, thingId);
        return messageProperties;
    }

    private Message createMessage(final Object object, final MessageProperties messageProperties) {
        return messageConverter.toMessage(object, messageProperties);
    }
}
