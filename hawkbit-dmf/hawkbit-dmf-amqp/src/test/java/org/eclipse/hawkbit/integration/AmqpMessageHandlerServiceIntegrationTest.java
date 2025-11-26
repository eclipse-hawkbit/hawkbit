/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.repository.model.Action.ActionType.DOWNLOAD_ONLY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.hawkbit.amqp.AmqpMessageHandlerService;
import org.eclipse.hawkbit.amqp.AmqpProperties;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.json.model.DmfActionStatus;
import org.eclipse.hawkbit.dmf.json.model.DmfActionUpdateStatus;
import org.eclipse.hawkbit.dmf.json.model.DmfAttributeUpdate;
import org.eclipse.hawkbit.dmf.json.model.DmfUpdateMode;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.event.remote.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TenantConfigurationCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TenantConfigurationUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.eclipse.hawkbit.repository.test.util.TargetTestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Feature: Component Tests - Device Management Federation API<br/>
 * Story: Amqp Message Handler Service
 */
class AmqpMessageHandlerServiceIntegrationTest extends AbstractAmqpServiceIntegrationTest {

    private static final String DMF_REGISTER_TEST_CONTROLLER_ID = "Dmf_hand_registerTargets_1";
    private static final String DMF_ATTR_TEST_CONTROLLER_ID = "Dmf_hand_updateAttributes";
    private static final String UPDATE_ATTR_TEST_CONTROLLER_ID = "ControllerAttributeTestTarget";
    private static final String TARGET_PREFIX = "Dmf_hand_";
    
    @Autowired
    private AmqpProperties amqpProperties;

    @Autowired
    private AmqpMessageHandlerService amqpMessageHandlerService;

    /**
     * Tests DMF PING request and expected response.
     */
    @Test
    void pingDmfInterface() {
        final Message pingMessage = createPingMessage(CORRELATION_ID, TENANT_EXIST);
        getDmfClient().send(pingMessage);

        assertPingReplyMessage(CORRELATION_ID);

        Mockito.verifyNoInteractions(getDeadletterListener());
    }

    /**
     * Tests register target
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 2),
            @Expect(type = TargetPollEvent.class, count = 3) })
    void registerTargets() {
        final String controllerId = TARGET_PREFIX + "registerTargets";
        registerAndAssertTargetWithExistingTenant(controllerId, 1);

        final String target2 = "Target2";
        registerAndAssertTargetWithExistingTenant(target2, 2);

        registerSameTargetAndAssertBasedOnVersion(target2, 2, TargetUpdateStatus.REGISTERED);
        Mockito.verifyNoInteractions(getDeadletterListener());
    }

    /**
     * Tests register target with name
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 2) })
    void registerTargetWithName() {
        final String controllerId = TARGET_PREFIX + "registerTargetWithName";
        final String name = "NonDefaultTargetName";
        registerAndAssertTargetWithExistingTenant(controllerId, name, 1, TargetUpdateStatus.REGISTERED, CREATED_BY,
                null);

        registerSameTargetAndAssertBasedOnVersion(controllerId, name + "_updated", 1, TargetUpdateStatus.REGISTERED,
                null);

        Mockito.verifyNoInteractions(getDeadletterListener());
    }

    /**
     * Tests register target with attributes
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetPollEvent.class, count = 2) })
    void registerTargetWithAttributes() {
        final String controllerId = TARGET_PREFIX + "registerTargetWithAttributes";
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("testKey1", "testValue1");
        attributes.put("testKey2", "testValue2");

        registerAndAssertTargetWithExistingTenant(controllerId, null, 1, TargetUpdateStatus.REGISTERED, CREATED_BY,
                attributes);

        attributes.put("testKey3", "testValue3");
        registerSameTargetAndAssertBasedOnVersion(controllerId, null, 1, TargetUpdateStatus.REGISTERED, attributes);

        Mockito.verifyNoInteractions(getDeadletterListener());
    }

    /**
     * Tests register target with name and attributes
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 3),
            @Expect(type = TargetPollEvent.class, count = 2) })
    void registerTargetWithNameAndAttributes() {
        final String controllerId = TARGET_PREFIX + "registerTargetWithAttributes";
        final String name = "NonDefaultTargetName";
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("testKey1", "testValue1");
        attributes.put("testKey2", "testValue2");

        registerAndAssertTargetWithExistingTenant(controllerId, name, 1, TargetUpdateStatus.REGISTERED, CREATED_BY,
                attributes);

        attributes.put("testKey3", "testValue3");
        registerSameTargetAndAssertBasedOnVersion(controllerId, name + "_updated", 1, TargetUpdateStatus.REGISTERED,
                attributes);

        Mockito.verifyNoInteractions(getDeadletterListener());
    }

    @ParameterizedTest
    /**
     * Tests register invalid target with empty controller id.
     */
    @ValueSource(strings = { "", "Invalid Invalid" })
    @NullSource
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class) })
    void shouldNotRegisterTargetsWithInvalidControllerIds(String controllerId) {
        createAndSendThingCreated(controllerId);
        assertAllTargetsCount(0);
        verifyOneDeadLetterMessage();
    }

    /**
     * Tests register invalid target with too long controller id
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class) })
    void registerInvalidTargetWithTooLongControllerId() {
        createAndSendThingCreated(randomString(Target.CONTROLLER_ID_MAX_SIZE + 1));
        assertAllTargetsCount(0);
        verifyOneDeadLetterMessage();
    }

    /**
     * Tests null reply to property in message header. This message should forwarded to the deadletter queue
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class) })
    void missingReplyToProperty() {
        final String controllerId = TARGET_PREFIX + "missingReplyToProperty";
        final Message createTargetMessage = createTargetMessage(controllerId, TENANT_EXIST);
        createTargetMessage.getMessageProperties().setReplyTo(null);
        getDmfClient().send(createTargetMessage);

        verifyOneDeadLetterMessage();
        assertAllTargetsCount(0);
    }

    /**
     * Tests missing reply to property in message header. This message should forwarded to the deadletter queue
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class) })
    void emptyReplyToProperty() {
        final String controllerId = TARGET_PREFIX + "emptyReplyToProperty";
        final Message createTargetMessage = createTargetMessage(controllerId, TENANT_EXIST);
        createTargetMessage.getMessageProperties().setReplyTo("");
        getDmfClient().send(createTargetMessage);

        verifyOneDeadLetterMessage();
        assertAllTargetsCount(0);
    }

    /**
     * Tests missing thing id property in message. This message should forwarded to the deadletter queue
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class) })
    void missingThingIdProperty() {
        final Message createTargetMessage = createTargetMessage(null, TENANT_EXIST);
        createTargetMessage.getMessageProperties().getHeaders().remove(MessageHeaderKey.THING_ID);
        getDmfClient().send(createTargetMessage);

        verifyOneDeadLetterMessage();
        assertAllTargetsCount(0);
    }

    /**
     * Tests null thing id property in message. This message should forwarded to the deadletter queue
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class) })
    void nullThingIdProperty() {
        final Message createTargetMessage = createTargetMessage(null, TENANT_EXIST);
        getDmfClient().send(createTargetMessage);

        verifyOneDeadLetterMessage();
        assertAllTargetsCount(0);
    }

    /**
     * Tests missing tenant message header. This message should forwarded to the deadletter queue
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class) })
    void missingTenantHeader() {
        final String controllerId = TARGET_PREFIX + "missingTenantHeader";
        final Message createTargetMessage = createTargetMessage(controllerId, TENANT_EXIST);
        createTargetMessage.getMessageProperties().getHeaders().remove(MessageHeaderKey.TENANT);
        getDmfClient().send(createTargetMessage);

        verifyOneDeadLetterMessage();
        assertAllTargetsCount(0);
    }

    /**
     * Tests null tenant message header. This message should forwarded to the deadletter queue
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class) })
    void nullTenantHeader() {
        final String controllerId = TARGET_PREFIX + "nullTenantHeader";
        final Message createTargetMessage = createTargetMessage(controllerId, null);
        getDmfClient().send(createTargetMessage);

        verifyOneDeadLetterMessage();
        assertAllTargetsCount(0);
    }

    /**
     * Tests empty tenant message header. This message should forwarded to the deadletter queue
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class) })
    void emptyTenantHeader() {
        final String controllerId = TARGET_PREFIX + "emptyTenantHeader";
        final Message createTargetMessage = createTargetMessage(controllerId, "");
        getDmfClient().send(createTargetMessage);

        verifyOneDeadLetterMessage();
        assertAllTargetsCount(0);
    }

    /**
     * Tests missing type message header. This message should forwarded to the deadletter queue
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class) })
    void missingTypeHeader() {
        final Message createTargetMessage = createTargetMessage(null, TENANT_EXIST);
        createTargetMessage.getMessageProperties().getHeaders().remove(MessageHeaderKey.TYPE);
        getDmfClient().send(createTargetMessage);

        verifyOneDeadLetterMessage();
        assertAllTargetsCount(0);
    }

    @ParameterizedTest
    /**
     * Tests null type message header. This message should forwarded to the deadletter queue
     */
    @ValueSource(strings = { "", "NotExist" })
    @NullSource
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class) })
    void shouldNotCreateTargetsWithInvalidTypeInHeader(String type) {
        final Message createTargetMessage = createTargetMessage(null, TENANT_EXIST);
        createTargetMessage.getMessageProperties().getHeaders().put(MessageHeaderKey.TYPE, type);
        getDmfClient().send(createTargetMessage);

        verifyOneDeadLetterMessage();
        assertAllTargetsCount(0);
    }

    @ParameterizedTest
    /**
     * Tests null topic message header. This message should forwarded to the deadletter queue
     */
    @ValueSource(strings = { "", "NotExist" })
    @NullSource
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class) })
    void shouldNotSendMessagesWithInvalidTopic(String topic) {
        final Message eventMessage = createUpdateActionEventMessage("");
        eventMessage.getMessageProperties().getHeaders().put(MessageHeaderKey.TOPIC, topic);
        getDmfClient().send(eventMessage);

        verifyOneDeadLetterMessage();
    }

    /**
     * Tests missing topic message header. This message should forwarded to the deadletter queue
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class) })
    void missingTopicHeader() {
        final Message eventMessage = createUpdateActionEventMessage("");
        eventMessage.getMessageProperties().getHeaders().remove(MessageHeaderKey.TOPIC);
        getDmfClient().send(eventMessage);

        verifyOneDeadLetterMessage();
    }

    @ParameterizedTest
    /**
     * Tests invalid null message content. This message should forwarded to the deadletter queue
     */
    @ValueSource(strings = { "", "Invalid Content" })
    @NullSource
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class) })
    void shouldMoveUpdateActionStatusWithInvalidPayloadIntoDeadLetter(String payload) {
        final Message eventMessage = createUpdateActionEventMessage(payload);
        getDmfClient().send(eventMessage);
        verifyOneDeadLetterMessage();
    }

    /**
     * Tests invalid topic message header. This message should forwarded to the deadletter queue
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class) })
    void updateActionStatusWithInvalidActionId() {
        final DmfActionUpdateStatus actionUpdateStatus = new DmfActionUpdateStatus(1L, DmfActionStatus.RUNNING);
        final Message eventMessage = createUpdateActionEventMessage(actionUpdateStatus);
        getDmfClient().send(eventMessage);
        verifyOneDeadLetterMessage();
    }

    /**
     * Tests register target and send finished message
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 9), // implicit lock
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetPollEvent.class, count = 1) })
    void finishActionStatus() {
        final String controllerId = TARGET_PREFIX + "finishActionStatus";
        registerTargetAndSendAndAssertUpdateActionStatus(DmfActionStatus.FINISHED, Status.FINISHED, controllerId);
    }

    /**
     * Register a target and send a update action status (running). Verify if the updated action status is correct.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 9), // implicit lock
            @Expect(type = TargetUpdatedEvent.class, count = 1), 
            @Expect(type = TargetPollEvent.class, count = 1) })
    void runningActionStatus() {
        final String controllerId = TARGET_PREFIX + "runningActionStatus";
        registerTargetAndSendAndAssertUpdateActionStatus(DmfActionStatus.RUNNING, Status.RUNNING, controllerId);
    }

    /**
     * Register a target and send an update action status (downloaded). Verify if the updated action status is correct.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 9), // implicit lock
            @Expect(type = TargetUpdatedEvent.class, count = 1), 
            @Expect(type = TargetPollEvent.class, count = 1) })
    void downloadedActionStatus() {
        final String controllerId = TARGET_PREFIX + "downloadedActionStatus";
        registerTargetAndSendAndAssertUpdateActionStatus(DmfActionStatus.DOWNLOADED, Status.DOWNLOADED, controllerId);
    }

    /**
     * Register a target and send a update action status (download). Verify if the updated action status is correct.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 9), // implicit lock
            @Expect(type = TargetUpdatedEvent.class, count = 1), 
            @Expect(type = TargetPollEvent.class, count = 1) })
    void downloadActionStatus() {
        final String controllerId = TARGET_PREFIX + "downloadActionStatus";
        registerTargetAndSendAndAssertUpdateActionStatus(DmfActionStatus.DOWNLOAD, Status.DOWNLOAD, controllerId);
    }

    /**
     * Register a target and send a update action status (error). Verify if the updated action status is correct.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 9), // implicit lock
            @Expect(type = TargetUpdatedEvent.class, count = 2), 
            @Expect(type = TargetPollEvent.class, count = 1) })
    void errorActionStatus() {
        final String controllerId = TARGET_PREFIX + "errorActionStatus";
        registerTargetAndSendAndAssertUpdateActionStatus(DmfActionStatus.ERROR, Status.ERROR, controllerId);
    }

    /**
     * Register a target and send a update action status (warning). Verify if the updated action status is correct.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 9), // implicit lock
            @Expect(type = TargetUpdatedEvent.class, count = 1), 
            @Expect(type = TargetPollEvent.class, count = 1) })
    void warningActionStatus() {
        final String controllerId = TARGET_PREFIX + "warningActionStatus";
        registerTargetAndSendAndAssertUpdateActionStatus(DmfActionStatus.WARNING, Status.WARNING, controllerId);
    }

    /**
     * Register a target and send a update action status (retrieved). Verify if the updated action status is correct.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 9), // implicit lock
            @Expect(type = TargetUpdatedEvent.class, count = 1), 
            @Expect(type = TargetPollEvent.class, count = 1) })
    void retrievedActionStatus() {
        final String controllerId = TARGET_PREFIX + "retrievedActionStatus";
        registerTargetAndSendAndAssertUpdateActionStatus(DmfActionStatus.RETRIEVED, Status.RETRIEVED, controllerId);
    }

    /**
     * Register a target and send a invalid update action status (cancel). This message should forwarded to the deadletter queue
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 9), // implicit lock
            @Expect(type = TargetUpdatedEvent.class, count = 1), 
            @Expect(type = TargetPollEvent.class, count = 1) })
    void cancelNotAllowActionStatus() {
        final String controllerId = TARGET_PREFIX + "cancelNotAllowActionStatus";
        registerTargetAndSendActionStatus(DmfActionStatus.CANCELED, controllerId);
        verifyOneDeadLetterMessage();
    }

    /**
     * Verify receiving a download and install message if a deployment is done before the target has polled the first time.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 9), // implicit lock
            @Expect(type = TargetUpdatedEvent.class, count = 1), 
            @Expect(type = TargetPollEvent.class, count = 2) })
    void receiveDownloadAndInstallMessageAfterAssignment() {
        final String controllerId = TARGET_PREFIX + "receiveDownLoadAndInstallMessageAfterAssignment";

        // setup
        registerAndAssertTargetWithExistingTenant(controllerId);
        final DistributionSet distributionSet = testdataFactory.createDistributionSet(UUID.randomUUID().toString());
        testdataFactory.addSoftwareModuleMetadata(distributionSet);
        assignDistributionSet(distributionSet.getId(), controllerId);

        // test
        registerSameTargetAndAssertBasedOnVersion(controllerId, 1, TargetUpdateStatus.PENDING);

        // verify
        assertDownloadAndInstallMessage(distributionSet.getModules(), controllerId);
        Mockito.verifyNoInteractions(getDeadletterListener());
    }

    /**
     * Verify receiving a download message if a deployment is done with window configured but before maintenance window start time.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 9), // implicit lock
            @Expect(type = TargetUpdatedEvent.class, count = 1), 
            @Expect(type = TargetPollEvent.class, count = 2) })
    void receiveDownloadMessageBeforeMaintenanceWindowStartTime() {
        final String controllerId = TARGET_PREFIX + "receiveDownLoadMessageBeforeMaintenanceWindowStartTime";

        // setup
        registerAndAssertTargetWithExistingTenant(controllerId);
        final DistributionSet distributionSet = testdataFactory.createDistributionSet(UUID.randomUUID().toString());
        testdataFactory.addSoftwareModuleMetadata(distributionSet);
        assignDistributionSetWithMaintenanceWindow(distributionSet.getId(), controllerId, getTestSchedule(2),
                getTestDuration(1), getTestTimeZone());

        // test
        registerSameTargetAndAssertBasedOnVersion(controllerId, 1, TargetUpdateStatus.PENDING);

        // verify
        assertDownloadMessage(distributionSet.getModules(), controllerId);
        Mockito.verifyNoInteractions(getDeadletterListener());
    }

    /**
     * Verify receiving a download_and_install message if a deployment is done with window configured and during maintenance window start time.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 9), // implicit lock
            @Expect(type = TargetUpdatedEvent.class, count = 1), 
            @Expect(type = TargetPollEvent.class, count = 2) })
    void receiveDownloadAndInstallMessageDuringMaintenanceWindow() {
        final String controllerId = TARGET_PREFIX + "receiveDownLoadAndInstallMessageDuringMaintenanceWindow";

        // setup
        registerAndAssertTargetWithExistingTenant(controllerId);
        final DistributionSet distributionSet = testdataFactory.createDistributionSet(UUID.randomUUID().toString());
        testdataFactory.addSoftwareModuleMetadata(distributionSet);
        assignDistributionSetWithMaintenanceWindow(distributionSet.getId(), controllerId, getTestSchedule(-5),
                getTestDuration(10), getTestTimeZone());

        // test
        registerSameTargetAndAssertBasedOnVersion(controllerId, 1, TargetUpdateStatus.PENDING);

        // verify
        assertDownloadAndInstallMessage(distributionSet.getModules(), controllerId);
        Mockito.verifyNoInteractions(getDeadletterListener());
    }

    /**
     * Verify receiving a cancel update message if a deployment is canceled before the target has polled the first time.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 2) })
    void receiveCancelUpdateMessageAfterAssignmentWasCanceled() {
        final String controllerId = TARGET_PREFIX + "receiveCancelUpdateMessageAfterAssignmentWasCanceled";

        // Setup
        registerAndAssertTargetWithExistingTenant(controllerId);
        final DistributionSet distributionSet = testdataFactory.createDistributionSet(UUID.randomUUID().toString());
        final DistributionSetAssignmentResult distributionSetAssignmentResult = assignDistributionSet(
                distributionSet.getId(), controllerId);
        deploymentManagement.cancelAction(getFirstAssignedActionId(distributionSetAssignmentResult));

        // test
        registerSameTargetAndAssertBasedOnVersion(controllerId, 1, TargetUpdateStatus.PENDING);

        // verify
        assertCancelActionMessage(getFirstAssignedActionId(distributionSetAssignmentResult), controllerId);
        Mockito.verifyNoInteractions(getDeadletterListener());
    }

    /**
     * Register a target and send a invalid update action status (canceled). The current status (pending) is not a canceling state. This message should forwarded to the deadletter queue
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 9), // implicit lock
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1) })
    void actionNotExists() {
        final String controllerId = TARGET_PREFIX + "actionNotExists";

        final Long actionId = registerTargetAndCancelActionId(controllerId);
        final long actionNotExist = actionId + 1;

        createAndSendActionStatusUpdateMessage(controllerId, actionNotExist, DmfActionStatus.CANCELED);
        verifyOneDeadLetterMessage();
    }

    /**
     * Register a target and send a invalid update action status (cancel_rejected). This message should forwarded to the deadletter queue
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 9), // implicit lock
            @Expect(type = TargetUpdatedEvent.class, count = 1), 
            @Expect(type = TargetPollEvent.class, count = 1) })
    void canceledRejectedNotAllowActionStatus() {
        final String controllerId = TARGET_PREFIX + "canceledRejectedNotAllowActionStatus";
        registerTargetAndSendActionStatus(DmfActionStatus.CANCEL_REJECTED, controllerId);
        verifyOneDeadLetterMessage();
    }

    /**
     * Register a target and send a valid update action status (cancel_rejected). Verify if the updated action status is correct.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 9), // implicit lock
            @Expect(type = TargetUpdatedEvent.class, count = 1), 
            @Expect(type = TargetPollEvent.class, count = 1) })
    void canceledRejectedActionStatus() {
        final String controllerId = TARGET_PREFIX + "canceledRejectedActionStatus";

        final Long actionId = registerTargetAndCancelActionId(controllerId);

        createAndSendActionStatusUpdateMessage(controllerId, actionId, DmfActionStatus.CANCEL_REJECTED);
        assertAction(actionId, 1, Status.RUNNING, Status.CANCELING, Status.CANCEL_REJECTED);

        // retrieve action and ensure that it is in Running state
        final Action action = deploymentManagement.findAction(actionId).orElseThrow(() -> new AssertionError("Action not found!"));
        assertThat(action.getStatus()).isEqualTo(Status.RUNNING);

    }

    /**
     * Verify that sending an update controller attribute message to an existing target works. Verify that different update modes (merge, replace, remove) can be used.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 4),
            @Expect(type = TargetPollEvent.class, count = 1) })
    void updateAttributesWithDifferentUpdateModes() {
        final String controllerId = TARGET_PREFIX + "updateAttributes";

        // setup
        registerAndAssertTargetWithExistingTenant(controllerId);

        // no update mode specified
        updateAttributesWithoutUpdateMode();

        // update mode REPLACE
        updateAttributesWithUpdateModeReplace();

        // update mode MERGE
        updateAttributesWithUpdateModeMerge();

        // update mode REMOVE
        updateAttributesWithUpdateModeRemove();

    }

    /**
     * Verify that sending an update controller attribute message with no thingid header to an existing target does not work.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class),
            @Expect(type = TargetPollEvent.class, count = 1) })
    void updateAttributesWithNoThingId() {
        final String controllerId = TARGET_PREFIX + "updateAttributesWithNoThingId";

        // setup
        registerAndAssertTargetWithExistingTenant(controllerId);
        final DmfAttributeUpdate controllerAttribute = new DmfAttributeUpdate(
                Map.of("test1", "testA", "test2", "testB"), null);
        final Message createUpdateAttributesMessage = createUpdateAttributesMessage(null, TENANT_EXIST,
                controllerAttribute);
        createUpdateAttributesMessage.getMessageProperties().getHeaders().remove(MessageHeaderKey.THING_ID);

        // test
        getDmfClient().send(createUpdateAttributesMessage);

        // verify
        verifyOneDeadLetterMessage();
        final DmfAttributeUpdate controllerAttributeEmpty = new DmfAttributeUpdate(null, null);
        assertUpdateAttributes(controllerId, controllerAttributeEmpty.getAttributes());
    }

    /**
     * Verify that sending an update controller attribute message with invalid body to an existing target does not work.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class),
            @Expect(type = TargetPollEvent.class, count = 1) })
    void updateAttributesWithWrongBody() {
        // setup
        registerAndAssertTargetWithExistingTenant(UPDATE_ATTR_TEST_CONTROLLER_ID);
        final Message createUpdateAttributesMessageWrongBody = createUpdateAttributesMessageWrongBody( UPDATE_ATTR_TEST_CONTROLLER_ID);

        // test
        getDmfClient().send(createUpdateAttributesMessageWrongBody);

        // verify
        verifyOneDeadLetterMessage();
    }

    /**
     * Verifies that sending an UPDATE_ATTRIBUTES message with invalid attributes is handled correctly.
     */
    @Test
    void updateAttributesWithInvalidValues() {
        registerAndAssertTargetWithExistingTenant(UPDATE_ATTR_TEST_CONTROLLER_ID);

        sendUpdateAttributesMessageWithGivenAttributes(TargetTestData.ATTRIBUTE_KEY_TOO_LONG, TargetTestData.ATTRIBUTE_VALUE_VALID);
        sendUpdateAttributesMessageWithGivenAttributes(TargetTestData.ATTRIBUTE_KEY_TOO_LONG, TargetTestData.ATTRIBUTE_VALUE_TOO_LONG);
        sendUpdateAttributesMessageWithGivenAttributes(TargetTestData.ATTRIBUTE_KEY_VALID, TargetTestData.ATTRIBUTE_VALUE_TOO_LONG);

        verifyNumberOfDeadLetterMessages(3);
    }

    /**
     * Tests the download_only assignment: tests the handling of a target reporting DOWNLOADED
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 9), // implicit lock
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetPollEvent.class, count = 1) })
    void downloadOnlyAssignmentFinishesActionWhenTargetReportsDownloaded() throws IOException {
        // create target
        final String controllerId = TARGET_PREFIX + "registerTargets_1";
        final DistributionSet distributionSet = createTargetAndDistributionSetAndAssign(controllerId, DOWNLOAD_ONLY);

        // verify
        final Message message = assertReplyMessageHeader(EventTopic.DOWNLOAD, controllerId);
        Mockito.verifyNoInteractions(getDeadletterListener());

        // get actionId from Message
        final long actionId = Long.parseLong(getActionIdFromBody(message.getBody()));

        // Send DOWNLOADED message
        createAndSendActionStatusUpdateMessage(controllerId, actionId, DmfActionStatus.DOWNLOADED);
        assertAction(actionId, 1, Status.RUNNING, Status.DOWNLOADED);
        Mockito.verifyNoInteractions(getDeadletterListener());

        verifyAssignedDsAndInstalledDs(distributionSet.getId(), null);
    }

    /**
     * Tests the download_only assignment: tests the handling of a target reporting FINISHED
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 9), // implicit lock
            @Expect(type = TargetAttributesRequestedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 3),
            @Expect(type = TargetPollEvent.class, count = 1) })
    void downloadOnlyAssignmentAllowsActionStatusUpdatesWhenTargetReportsFinishedAndUpdatesInstalledDS()
            throws IOException {
        // create target
        final String controllerId = TARGET_PREFIX + "registerTargets_1";
        final DistributionSet distributionSet = createTargetAndDistributionSetAndAssign(controllerId, DOWNLOAD_ONLY);

        // verify
        final Message message = assertReplyMessageHeader(EventTopic.DOWNLOAD, controllerId);
        Mockito.verifyNoInteractions(getDeadletterListener());

        // get actionId from Message
        final long actionId = Long.parseLong(getActionIdFromBody(message.getBody()));

        // Send DOWNLOADED message, should result in the action being closed
        createAndSendActionStatusUpdateMessage(controllerId, actionId, DmfActionStatus.DOWNLOADED);
        assertAction(actionId, 1, Status.RUNNING, Status.DOWNLOADED);
        Mockito.verifyNoInteractions(getDeadletterListener());

        verifyAssignedDsAndInstalledDs(distributionSet.getId(), null);

        // Send FINISHED message
        createAndSendActionStatusUpdateMessage(controllerId, actionId, DmfActionStatus.FINISHED);
        assertAction(actionId, 2, Status.RUNNING, Status.DOWNLOADED, Status.FINISHED);
        Mockito.verifyNoInteractions(getDeadletterListener());

        verifyAssignedDsAndInstalledDs(distributionSet.getId(), distributionSet.getId());
    }

    /**
     * Messages that result into certain exceptions being raised should not be requeued. This message should forwarded to the deadletter queue
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class) })
    void ignoredExceptionTypesShouldNotBeRequeued() {
        final ControllerManagement mockedControllerManagement = Mockito.mock(ControllerManagement.class);
        final List<Class<? extends RuntimeException>> exceptionsThatShouldNotBeRequeued = Arrays
                .asList(IllegalArgumentException.class, EntityAlreadyExistsException.class);
        final String controllerId = "dummy_target";

        try {
            for (final Class<? extends RuntimeException> exceptionClass : exceptionsThatShouldNotBeRequeued) {
                doThrow(exceptionClass).when(mockedControllerManagement)
                        .findOrRegisterTargetIfItDoesNotExist(eq(controllerId), any());

                amqpMessageHandlerService.setControllerManagement(mockedControllerManagement);
                createAndSendThingCreated(controllerId);
                verifyOneDeadLetterMessage();
                assertThat(targetManagement.findByControllerId(controllerId)).isEmpty();
            }
        } finally {
            amqpMessageHandlerService.setControllerManagement(controllerManagement);
        }
    }

    /**
     * Register a target and send a update action status (confirmed). Verify if the updated action status is correct.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 9), // implicit lock
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1) })
    void confirmedActionStatus() {
        enableConfirmationFlow();
        final String controllerId = TARGET_PREFIX + "confirmedActionStatus";

        final DistributionSetAssignmentResult assignmentResult = registerTargetAndAssignDistributionSet(controllerId);
        final Long actionId = getFirstAssignedActionId(assignmentResult);
        createAndSendActionStatusUpdateMessage(controllerId, actionId, DmfActionStatus.CONFIRMED);
        assertActionStatusList(actionId, 2, Status.WAIT_FOR_CONFIRMATION, Status.RUNNING);

        // assert download and install message
        waitUntilEventMessagesAreDispatchedToTarget(EventTopic.CONFIRM, EventTopic.DOWNLOAD_AND_INSTALL);
        assertDownloadAndInstallMessage(assignmentResult.getDistributionSet().getModules(), controllerId);
    }

    /**
     * Verify the DMF confirmed feedback can be provided if confirmation flow is disabled
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 9), // implicit lock
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1),
            @Expect(type = TenantConfigurationUpdatedEvent.class, count = 1) })
    void verifyActionCanBeConfirmedOnDisabledConfirmationFlow() {
        enableConfirmationFlow();
        final String controllerId = TARGET_PREFIX + "confirmedActionStatus";

        final DistributionSetAssignmentResult assignmentResult = registerTargetAndAssignDistributionSet(controllerId);
        final Long actionId = getFirstAssignedActionId(assignmentResult);
        // verify action status is in WAIT_FOR_CONFIRMATION
        assertActionStatusList(actionId, 1, Status.WAIT_FOR_CONFIRMATION);

        disableConfirmationFlow();

        createAndSendActionStatusUpdateMessage(controllerId, actionId, DmfActionStatus.CONFIRMED);
        assertActionStatusList(actionId, 2, Status.WAIT_FOR_CONFIRMATION, Status.RUNNING);

        // assert download and install message
        waitUntilEventMessagesAreDispatchedToTarget(EventTopic.CONFIRM, EventTopic.DOWNLOAD_AND_INSTALL);
        assertDownloadAndInstallMessage(assignmentResult.getDistributionSet().getModules(), controllerId);
    }

    /**
     * Verify the DMF confirmed feedback can be provided if confirmation flow is disabled
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 0),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 9), // implicit lock
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1),
            @Expect(type = TenantConfigurationUpdatedEvent.class, count = 1) })
    void verifyActionCanBeDeniedOnDisabledConfirmationFlow() {
        enableConfirmationFlow();
        final String controllerId = TARGET_PREFIX + "confirmedActionStatus";

        final DistributionSetAssignmentResult assignmentResult = registerTargetAndAssignDistributionSet(controllerId);
        final Long actionId = getFirstAssignedActionId(assignmentResult);
        // verify action status is in WAIT_FOR_CONFIRMATION
        assertActionStatusList(actionId, 1, Status.WAIT_FOR_CONFIRMATION);

        disableConfirmationFlow();

        createAndSendActionStatusUpdateMessage(controllerId, actionId, DmfActionStatus.DENIED);
        assertActionStatusList(actionId, 2, Status.WAIT_FOR_CONFIRMATION);
    }

    /**
     * Verify the DMF download and install message is send directly if auto-confirmation is active
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetPollEvent.class, count = 1),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 9), // implicit lock
            @Expect(type = DistributionSetCreatedEvent.class, count = 1), })
    void verifyDownloadAndInstallDirectlySendOnAutoConfirmationEnabled() {
        enableConfirmationFlow();
        final String controllerId = TARGET_PREFIX + "confirmedActionStatus";

        registerAndAssertTargetWithExistingTenant(controllerId);

        assertThat(targetManagement.getWithAutoConfigurationStatus(controllerId).getAutoConfirmationStatus()).isNull();
        confirmationManagement.activateAutoConfirmation(controllerId, null, null);
        assertThat(targetManagement.getWithAutoConfigurationStatus(controllerId).getAutoConfirmationStatus()).isNotNull();

        final DistributionSetAssignmentResult assignmentResult = prepareDistributionSetAndAssign(controllerId);
        final Long actionId = getFirstAssignedActionId(assignmentResult);
        // verify action status is in WAIT_FOR_CONFIRMATION
        assertActionStatusList(actionId, 1, Status.WAIT_FOR_CONFIRMATION);

        // assert download and install message
        waitUntilEventMessagesAreDispatchedToTarget(EventTopic.DOWNLOAD_AND_INSTALL);
        assertDownloadAndInstallMessage(assignmentResult.getDistributionSet().getModules(), controllerId);
    }

    /**
     * Register a target and send a update action status (denied). Verify if the updated action status is correct.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 9), // implicit lock
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1) })
    void deniedActionStatus() {
        enableConfirmationFlow();
        final String controllerId = TARGET_PREFIX + "deniedActionStatus";
        final DistributionSetAssignmentResult assignmentResult = registerTargetAndAssignDistributionSet(controllerId);
        final Long actionId = getFirstAssignedActionId(assignmentResult);
        assertActionStatusList(actionId, 1, Status.WAIT_FOR_CONFIRMATION);
        createAndSendActionStatusUpdateMessage(controllerId, actionId, DmfActionStatus.DENIED);
        assertActionStatusList(actionId, 2, Status.WAIT_FOR_CONFIRMATION);
    }

    private static String getActionIdFromBody(final byte[] body) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final ObjectNode node = objectMapper.readValue(new String(body, Charset.defaultCharset()), ObjectNode.class);
        assertThat(node.has("actionId")).isTrue();
        return node.get("actionId").asText();
    }

    private void updateAttributesWithUpdateModeRemove() {

        // assemble the expected attributes
        final Map<String, String> expectedAttributes = targetManagement
                .getControllerAttributes(DMF_ATTR_TEST_CONTROLLER_ID);
        expectedAttributes.remove("k1");
        expectedAttributes.remove("k3");

        // send an update message with update mode
        final Map<String, String> removeAttributes = new HashMap<>();
        removeAttributes.put("k1", "foo");
        removeAttributes.put("k3", "bar");

        final DmfAttributeUpdate remove = new DmfAttributeUpdate(removeAttributes, DmfUpdateMode.REMOVE);
        sendUpdateAttributeMessage(remove);

        // validate
        assertUpdateAttributes(DMF_ATTR_TEST_CONTROLLER_ID, expectedAttributes);
    }

    private void updateAttributesWithUpdateModeMerge() {
        // get the current attributes
        final Map<String, String> attributes = new HashMap<>(
                targetManagement.getControllerAttributes(DMF_ATTR_TEST_CONTROLLER_ID));

        // send an update message with update mode MERGE
        final Map<String, String> mergeAttributes = new HashMap<>();
        mergeAttributes.put("k1", "v1_modified_again");
        mergeAttributes.put("k4", "v4");

        final DmfAttributeUpdate merge = new DmfAttributeUpdate(mergeAttributes, DmfUpdateMode.MERGE);
        sendUpdateAttributeMessage(merge);

        // validate
        final Map<String, String> expectedAttributes = new HashMap<>();
        expectedAttributes.putAll(attributes);
        expectedAttributes.putAll(mergeAttributes);
        assertUpdateAttributes(DMF_ATTR_TEST_CONTROLLER_ID, expectedAttributes);
    }

    private void updateAttributesWithUpdateModeReplace() {
        // send an update message with update mode REPLACE
        final Map<String, String> expectedAttributes = new HashMap<>();
        expectedAttributes.put("k1", "v1_modified");
        expectedAttributes.put("k2", "v2");
        expectedAttributes.put("k3", "v3");

        final DmfAttributeUpdate replace = new DmfAttributeUpdate(expectedAttributes, DmfUpdateMode.REPLACE);
        sendUpdateAttributeMessage(replace);

        // validate
        assertUpdateAttributes(DMF_ATTR_TEST_CONTROLLER_ID, expectedAttributes);
    }

    private void updateAttributesWithoutUpdateMode() {
        // send an update message which does not specify an update mode
        final Map<String, String> expectedAttributes = new HashMap<>();
        expectedAttributes.put("k0", "v0");
        expectedAttributes.put("k1", "v1");

        final DmfAttributeUpdate defaultUpdate = new DmfAttributeUpdate(expectedAttributes, null);
        sendUpdateAttributeMessage(defaultUpdate);

        // validate
        assertUpdateAttributes(DMF_ATTR_TEST_CONTROLLER_ID, expectedAttributes);
    }

    private void verifyAssignedDsAndInstalledDs(final Long assignedDsId, final Long installedDsId) {
        final Optional<Target> target = controllerManagement.findByControllerId(DMF_REGISTER_TEST_CONTROLLER_ID);
        assertThat(target).isPresent();

        // verify the DS was assigned to the Target
        final DistributionSet assignedDistributionSet = ((JpaTarget) target.get()).getAssignedDistributionSet();
        assertThat(assignedDsId).isNotNull();
        assertThat(assignedDistributionSet.getId()).isEqualTo(assignedDsId);

        // verify that the installed DS was not affected
        final JpaDistributionSet installedDistributionSet = ((JpaTarget) target.get()).getInstalledDistributionSet();
        if (installedDsId == null) {
            assertThat(installedDistributionSet).isNull();
        } else {
            assertThat(installedDistributionSet.getId()).isEqualTo(installedDsId);
        }
    }

    private void sendUpdateAttributesMessageWithGivenAttributes(final String key, final String value) {
        final DmfAttributeUpdate controllerAttribute = new DmfAttributeUpdate(Map.of(key, value), null);
        final Message message = createUpdateAttributesMessage(UPDATE_ATTR_TEST_CONTROLLER_ID, TENANT_EXIST, controllerAttribute);
        getDmfClient().send(message);
    }

    private Long registerTargetAndSendActionStatus(final DmfActionStatus sendActionStatus, final String controllerId) {
        final DistributionSetAssignmentResult assignmentResult = registerTargetAndAssignDistributionSet(controllerId);
        final Long actionId = getFirstAssignedActionId(assignmentResult);
        createAndSendActionStatusUpdateMessage(controllerId, actionId, sendActionStatus);
        return actionId;
    }

    private void registerTargetAndSendAndAssertUpdateActionStatus(final DmfActionStatus sendActionStatus,
            final Status expectedActionStatus, final String controllerId) {
        final Long actionId = registerTargetAndSendActionStatus(sendActionStatus, controllerId);
        assertAction(actionId, 1, Status.RUNNING, expectedActionStatus);
    }

    private void assertAction(final Long actionId, final int messages, final Status... expectedActionStates) {
        await().untilAsserted(() -> {
            try {
                SecurityContextSwitch.asPrivileged(() -> {
                    final List<ActionStatus> actionStatusList = deploymentManagement.findActionStatusByAction(actionId, PAGE).getContent();

                    // Check correlation ID
                    final List<String> messagesFromServer = actionStatusList.stream()
                            .flatMap(actionStatus -> deploymentManagement
                                    .findMessagesByActionStatusId(actionStatus.getId(), PAGE).getContent().stream())
                            .filter(Objects::nonNull)
                            .filter(message -> message.startsWith(RepositoryConstants.SERVER_MESSAGE_PREFIX + "DMF message"))
                            .toList();

                    assertThat(messagesFromServer).hasSize(messages).allMatch(message -> message.endsWith(CORRELATION_ID));

                    final List<Status> status = actionStatusList.stream().map(ActionStatus::getStatus).toList();
                    assertThat(status).containsOnly(expectedActionStates);

                    return null;
                });
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void sendUpdateAttributeMessage(final DmfAttributeUpdate attributeUpdate) {
        final Message updateMessage = createUpdateAttributesMessage(DMF_ATTR_TEST_CONTROLLER_ID,
                AbstractAmqpServiceIntegrationTest.TENANT_EXIST, attributeUpdate);
        getDmfClient().send(updateMessage);
    }

    private void assertActionStatusList(final Long actionId, final int statusListCount,
            final Status... expectedActionStates) {
        await().untilAsserted(() -> {
            try {
                SecurityContextSwitch.asPrivileged(() -> {
                    final List<ActionStatus> actionStatusList = deploymentManagement
                            .findActionStatusByAction(actionId, PAGE).getContent();
                    assertThat(actionStatusList).hasSize(statusListCount);

                    final List<Status> status = actionStatusList.stream().map(ActionStatus::getStatus)
                            .toList();
                    assertThat(status).containsOnly(expectedActionStates);

                    return null;
                });
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private int getAuthenticationMessageCount() {
        return Integer
                .parseInt(Objects.requireNonNull(getRabbitAdmin().getQueueProperties(amqpProperties.getReceiverQueue()))
                        .get(RabbitAdmin.QUEUE_MESSAGE_COUNT).toString());
    }

    private void assertEmptyReceiverQueueCount() {
        assertThat(getAuthenticationMessageCount()).isZero();
    }

    private void verifyOneDeadLetterMessage() {
        verifyNumberOfDeadLetterMessages(1);
    }

    private void verifyNumberOfDeadLetterMessages(final int numberOfInvocations) {
        assertEmptyReceiverQueueCount();
        await().untilAsserted(
                () -> Mockito.verify(getDeadletterListener(), Mockito.times(numberOfInvocations)).handleMessage(Mockito.any()));
        Mockito.reset(getDeadletterListener());
    }
}