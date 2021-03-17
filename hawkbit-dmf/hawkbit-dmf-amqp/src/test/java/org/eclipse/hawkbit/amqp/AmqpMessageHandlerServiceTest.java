/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.MULTI_ASSIGNMENTS_ENABLED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

import org.eclipse.hawkbit.api.HostnameResolver;
import org.eclipse.hawkbit.artifact.repository.ArtifactRepository;
import org.eclipse.hawkbit.cache.DownloadIdCache;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.DmfActionStatus;
import org.eclipse.hawkbit.dmf.json.model.DmfActionUpdateStatus;
import org.eclipse.hawkbit.dmf.json.model.DmfAttributeUpdate;
import org.eclipse.hawkbit.dmf.json.model.DmfCreateThing;
import org.eclipse.hawkbit.dmf.json.model.DmfDownloadResponse;
import org.eclipse.hawkbit.dmf.json.model.DmfUpdateMode;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.UpdateMode;
import org.eclipse.hawkbit.repository.builder.ActionStatusBuilder;
import org.eclipse.hawkbit.repository.builder.ActionStatusCreate;
import org.eclipse.hawkbit.repository.jpa.model.helper.SecurityTokenGeneratorHolder;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionProperties;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.security.DmfTenantSecurityToken;
import org.eclipse.hawkbit.security.DmfTenantSecurityToken.FileResource;
import org.eclipse.hawkbit.security.SecurityContextTenantAware;
import org.eclipse.hawkbit.security.SecurityTokenGenerator;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.tenancy.UserAuthoritiesResolver;
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
import org.springframework.http.HttpStatus;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@ExtendWith(MockitoExtension.class)
@Feature("Component Tests - Device Management Federation API")
@Story("AmqpMessage Handler Service Test")
public class AmqpMessageHandlerServiceTest {

    private static final String FAIL_MESSAGE_AMQP_REJECT_REASON = AmqpRejectAndDontRequeueException.class
            .getSimpleName() + " was expected, ";

    private static final String SHA1 = "12345";
    private static final String VIRTUAL_HOST = "vHost";
    private static final String TENANT = "DEFAULT";
    private static final Long TENANT_ID = 123L;
    private static final String CONTROLLER_ID = "123";
    private static final Long TARGET_ID = 123L;

    private AmqpMessageHandlerService amqpMessageHandlerService;
    private AmqpAuthenticationMessageHandler amqpAuthenticationMessageHandlerService;

    private MessageConverter messageConverter;

    @Mock
    private AmqpMessageDispatcherService amqpMessageDispatcherServiceMock;

    @Mock
    private ControllerManagement controllerManagementMock;

    @Mock
    private EntityFactory entityFactoryMock;

    @Mock
    private ArtifactManagement artifactManagementMock;

    @Mock
    private TenantConfigurationManagement tenantConfigurationManagement;

    @Mock
    private AmqpControllerAuthentication authenticationManagerMock;

    @Mock
    private ArtifactRepository artifactRepositoryMock;

    @Mock
    private DownloadIdCache downloadIdCache;

    @Mock
    private HostnameResolver hostnameResolverMock;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private TenantAware tenantAwareMock;

    @Mock
    private UserAuthoritiesResolver authoritiesResolver;

    @Captor
    private ArgumentCaptor<Map<String, String>> attributesCaptor;

    @Captor
    private ArgumentCaptor<String> targetIdCaptor;

    @Captor
    private ArgumentCaptor<URI> uriCaptor;

    @Captor
    private ArgumentCaptor<UpdateMode> modeCaptor;

    @BeforeEach
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void before() throws Exception {
        messageConverter = new Jackson2JsonMessageConverter();
        lenient().when(rabbitTemplate.getMessageConverter()).thenReturn(messageConverter);
        final TenantConfigurationValue multiAssignmentConfig = TenantConfigurationValue.builder().value(Boolean.FALSE)
                .global(Boolean.FALSE).build();
        lenient().when(tenantConfigurationManagement.getConfigurationValue(MULTI_ASSIGNMENTS_ENABLED, Boolean.class))
                .thenReturn(multiAssignmentConfig);

        final SecurityContextTenantAware tenantAware = new SecurityContextTenantAware(authoritiesResolver);
        final SystemSecurityContext systemSecurityContext = new SystemSecurityContext(tenantAware);

        amqpMessageHandlerService = new AmqpMessageHandlerService(rabbitTemplate, amqpMessageDispatcherServiceMock,
                controllerManagementMock, entityFactoryMock, systemSecurityContext, tenantConfigurationManagement);
        amqpAuthenticationMessageHandlerService = new AmqpAuthenticationMessageHandler(rabbitTemplate,
                authenticationManagerMock, artifactManagementMock, downloadIdCache, hostnameResolverMock,
                controllerManagementMock, tenantAwareMock);
    }

    @Test
    @Description("Tests not allowed content-type in message")
    public void wrongContentType() {
        final MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType("xml");
        final Message message = new Message(new byte[0], messageProperties);
        assertThatExceptionOfType(AmqpRejectAndDontRequeueException.class)
                .as(FAIL_MESSAGE_AMQP_REJECT_REASON + "due to wrong content type")
                .isThrownBy(() -> amqpMessageHandlerService.onMessage(message, MessageType.THING_CREATED.name(), TENANT,
                        VIRTUAL_HOST));
    }

    @Test
    @Description("Tests the creation of a target/thing by calling the same method that incoming RabbitMQ messages would access.")
    public void createThing() {
        final String knownThingId = "1";
        final MessageProperties messageProperties = createMessageProperties(MessageType.THING_CREATED);
        messageProperties.setHeader(MessageHeaderKey.THING_ID, "1");
        final Message message = messageConverter.toMessage(new byte[0], messageProperties);

        final Target targetMock = mock(Target.class);

        final ArgumentCaptor<String> targetIdCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        when(controllerManagementMock.findOrRegisterTargetIfItDoesNotExist(targetIdCaptor.capture(),
                uriCaptor.capture())).thenReturn(targetMock);
        when(controllerManagementMock.findActiveActionWithHighestWeight(any())).thenReturn(Optional.empty());

        amqpMessageHandlerService.onMessage(message, MessageType.THING_CREATED.name(), TENANT, VIRTUAL_HOST);

        // verify
        assertThat(targetIdCaptor.getValue()).as("Thing id is wrong").isEqualTo(knownThingId);
        assertThat(uriCaptor.getValue().toString()).as("Uri is not right")
                .isEqualTo("amqp://" + VIRTUAL_HOST + "/MyTest");
    }

    @Test
    @Description("Tests the creation of a target/thing with specified name by calling the same method that incoming RabbitMQ messages would access.")
    public void createThingWithName() {
        final String knownThingId = "2";
        final DmfCreateThing targetProperties = new DmfCreateThing();
        targetProperties.setName("NonDefaultTargetName");

        final Target targetMock = mock(Target.class);

        targetIdCaptor = ArgumentCaptor.forClass(String.class);
        uriCaptor = ArgumentCaptor.forClass(URI.class);
        final ArgumentCaptor<String> targetNameCaptor = ArgumentCaptor.forClass(String.class);

        when(controllerManagementMock.findOrRegisterTargetIfItDoesNotExist(targetIdCaptor.capture(),
                uriCaptor.capture(), targetNameCaptor.capture())).thenReturn(targetMock);
        when(controllerManagementMock.findActiveActionWithHighestWeight(any())).thenReturn(Optional.empty());

        amqpMessageHandlerService.onMessage(
                createMessage(targetProperties, getThingCreatedMessageProperties(knownThingId)),
                MessageType.THING_CREATED.name(), TENANT, "vHost");

        assertThat(targetIdCaptor.getValue()).as("Thing id is wrong").isEqualTo(knownThingId);
        assertThat(uriCaptor.getValue().toString()).as("Uri is not right").isEqualTo("amqp://vHost/MyTest");
        assertThat(targetNameCaptor.getValue()).as("Thing name is not right").isEqualTo(targetProperties.getName());
    }

    @Test
    @Description("Tests not allowed body in message")
    public void createThingWithWrongBody() {
        final String knownThingId = "3";

        assertThatExceptionOfType(MessageConversionException.class)
                .as("MessageConversionException was excepeted due to wrong body")
                .isThrownBy(() -> amqpMessageHandlerService.onMessage(
                        createMessage("Not allowed Body".getBytes(), getThingCreatedMessageProperties(knownThingId)),
                        MessageType.THING_CREATED.name(), TENANT, VIRTUAL_HOST));
    }

    @Test
    @Description("Tests the target attribute update by calling the same method that incoming RabbitMQ messages would access.")
    public void updateAttributes() {
        final String knownThingId = "1";
        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        messageProperties.setHeader(MessageHeaderKey.THING_ID, knownThingId);
        messageProperties.setHeader(MessageHeaderKey.TOPIC, "UPDATE_ATTRIBUTES");
        final DmfAttributeUpdate attributeUpdate = new DmfAttributeUpdate();
        attributeUpdate.getAttributes().put("testKey1", "testValue1");
        attributeUpdate.getAttributes().put("testKey2", "testValue2");

        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(attributeUpdate,
                messageProperties);

        when(controllerManagementMock.updateControllerAttributes(targetIdCaptor.capture(), attributesCaptor.capture(),
                modeCaptor.capture())).thenReturn(null);

        amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT, VIRTUAL_HOST);

        // verify
        assertThat(targetIdCaptor.getValue()).as("Thing id is wrong").isEqualTo(knownThingId);
        assertThat(attributesCaptor.getValue()).as("Attributes is not right")
                .isEqualTo(attributeUpdate.getAttributes());
    }

    @Test
    @Description("Verifies that the update mode is retrieved from the UPDATE_ATTRIBUTES message and passed to the controller management.")
    public void attributeUpdateModes() {
        final String knownThingId = "1";
        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        messageProperties.setHeader(MessageHeaderKey.THING_ID, knownThingId);
        messageProperties.setHeader(MessageHeaderKey.TOPIC, "UPDATE_ATTRIBUTES");
        final DmfAttributeUpdate attributeUpdate = new DmfAttributeUpdate();
        attributeUpdate.getAttributes().put("testKey1", "testValue1");
        attributeUpdate.getAttributes().put("testKey2", "testValue2");

        when(controllerManagementMock.updateControllerAttributes(targetIdCaptor.capture(), attributesCaptor.capture(),
                modeCaptor.capture())).thenReturn(null);

        // send a message which does not specify a update mode
        Message message = amqpMessageHandlerService.getMessageConverter().toMessage(attributeUpdate, messageProperties);
        amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT, VIRTUAL_HOST);
        // verify that NO fallback is made on the way to the controller
        // management layer
        assertThat(modeCaptor.getValue()).isNull();

        // send a message which specifies update mode MERGE
        attributeUpdate.setMode(DmfUpdateMode.MERGE);
        message = amqpMessageHandlerService.getMessageConverter().toMessage(attributeUpdate, messageProperties);
        amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT, VIRTUAL_HOST);
        // verify that the update mode is converted and forwarded as expected
        assertThat(modeCaptor.getValue()).isEqualTo(UpdateMode.MERGE);

        // send a message which specifies update mode REPLACE
        attributeUpdate.setMode(DmfUpdateMode.REPLACE);
        message = amqpMessageHandlerService.getMessageConverter().toMessage(attributeUpdate, messageProperties);
        amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT, VIRTUAL_HOST);
        // verify that the update mode is converted and forwarded as expected
        assertThat(modeCaptor.getValue()).isEqualTo(UpdateMode.REPLACE);

        // send a message which specifies update mode REMOVE
        attributeUpdate.setMode(DmfUpdateMode.REMOVE);
        message = amqpMessageHandlerService.getMessageConverter().toMessage(attributeUpdate, messageProperties);
        amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT, VIRTUAL_HOST);
        // verify that the update mode is converted and forwarded as expected
        assertThat(modeCaptor.getValue()).isEqualTo(UpdateMode.REMOVE);
    }

    @Test
    @Description("Tests the creation of a thing without a 'reply to' header in message.")
    public void createThingWithoutReplyTo() {
        final MessageProperties messageProperties = createMessageProperties(MessageType.THING_CREATED, null);
        messageProperties.setHeader(MessageHeaderKey.THING_ID, "1");
        final Message message = messageConverter.toMessage("", messageProperties);

        assertThatExceptionOfType(AmqpRejectAndDontRequeueException.class)
                .as(FAIL_MESSAGE_AMQP_REJECT_REASON + "since no replyTo header was set")
                .isThrownBy(() -> amqpMessageHandlerService.onMessage(message, MessageType.THING_CREATED.name(), TENANT,
                        VIRTUAL_HOST));
    }

    @Test
    @Description("Tests the creation of a target/thing without a thingID by calling the same method that incoming RabbitMQ messages would access.")
    public void createThingWithoutID() {
        final MessageProperties messageProperties = createMessageProperties(MessageType.THING_CREATED);
        final Message message = messageConverter.toMessage(new byte[0], messageProperties);

        assertThatExceptionOfType(AmqpRejectAndDontRequeueException.class)
                .as(FAIL_MESSAGE_AMQP_REJECT_REASON + "since no thingId was set")
                .isThrownBy(() -> amqpMessageHandlerService.onMessage(message, MessageType.THING_CREATED.name(), TENANT,
                        VIRTUAL_HOST));
    }

    @Test
    @Description("Tests the call of the same method that incoming RabbitMQ messages would access with an unknown message type.")
    public void unknownMessageType() {
        final String type = "bumlux";
        final MessageProperties messageProperties = createMessageProperties(MessageType.THING_CREATED);
        messageProperties.setHeader(MessageHeaderKey.THING_ID, "");
        final Message message = messageConverter.toMessage(new byte[0], messageProperties);

        assertThatExceptionOfType(AmqpRejectAndDontRequeueException.class)
                .as(FAIL_MESSAGE_AMQP_REJECT_REASON + "due to unknown message type")
                .isThrownBy(() -> amqpMessageHandlerService.onMessage(message, type, TENANT, VIRTUAL_HOST));
    }

    @Test
    @Description("Tests a invalid message without event topic")
    public void invalidEventTopic() {
        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        final Message message = new Message(new byte[0], messageProperties);

        assertThatExceptionOfType(AmqpRejectAndDontRequeueException.class)
                .as(FAIL_MESSAGE_AMQP_REJECT_REASON + "due to unknown message type").isThrownBy(
                        () -> amqpMessageHandlerService.onMessage(message, "unknownMessageType", TENANT, VIRTUAL_HOST));

        messageProperties.setHeader(MessageHeaderKey.TOPIC, "wrongTopic");
        assertThatExceptionOfType(AmqpRejectAndDontRequeueException.class)
                .as(FAIL_MESSAGE_AMQP_REJECT_REASON + "due to unknown topic").isThrownBy(() -> amqpMessageHandlerService
                        .onMessage(message, MessageType.EVENT.name(), TENANT, VIRTUAL_HOST));

        messageProperties.setHeader(MessageHeaderKey.TOPIC, EventTopic.CANCEL_DOWNLOAD.name());
        assertThatExceptionOfType(AmqpRejectAndDontRequeueException.class)
                .as(FAIL_MESSAGE_AMQP_REJECT_REASON + "because there was no event topic")
                .isThrownBy(() -> amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT,
                        VIRTUAL_HOST));
    }

    @Test
    @Description("Tests the update of an action of a target without a exist action id")
    public void updateActionStatusWithoutActionId() {
        when(controllerManagementMock.findActionWithDetails(anyLong())).thenReturn(Optional.empty());
        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        messageProperties.setHeader(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ACTION_STATUS.name());
        final DmfActionUpdateStatus actionUpdateStatus = new DmfActionUpdateStatus(1L, DmfActionStatus.DOWNLOAD);
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(actionUpdateStatus,
                messageProperties);

        assertThatExceptionOfType(AmqpRejectAndDontRequeueException.class)
                .as(FAIL_MESSAGE_AMQP_REJECT_REASON + "since no action id was set")
                .isThrownBy(() -> amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT,
                        VIRTUAL_HOST));
    }

    @Test
    @Description("Tests the update of an action of a target without a exist action id")
    public void updateActionStatusWithoutExistActionId() {
        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        messageProperties.setHeader(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ACTION_STATUS.name());
        when(controllerManagementMock.findActionWithDetails(anyLong())).thenReturn(Optional.empty());

        final DmfActionUpdateStatus actionUpdateStatus = createActionUpdateStatus(DmfActionStatus.DOWNLOAD);
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(actionUpdateStatus,
                messageProperties);

        assertThatExceptionOfType(AmqpRejectAndDontRequeueException.class)
                .as(FAIL_MESSAGE_AMQP_REJECT_REASON + "since no action id was set")
                .isThrownBy(() -> amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT,
                        VIRTUAL_HOST));
    }

    @Test
    @Description("Tests that an download request is denied for an artifact which does not exists")
    public void authenticationRequestDeniedForArtifactWhichDoesNotExists() {
        final MessageProperties messageProperties = createMessageProperties(null);
        final DmfTenantSecurityToken securityToken = new DmfTenantSecurityToken(TENANT, TENANT_ID, CONTROLLER_ID,
                TARGET_ID, FileResource.createFileResourceBySha1("12345"));
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        // test
        final Message onMessage = amqpAuthenticationMessageHandlerService.onAuthenticationRequest(message);

        // verify
        final DmfDownloadResponse downloadResponse = (DmfDownloadResponse) messageConverter.fromMessage(onMessage);
        assertThat(downloadResponse).as("Message body should not null").isNotNull();
        assertThat(downloadResponse.getResponseCode()).as("Message body response code is wrong")
                .isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @Description("Tests that an download request is denied for an artifact which is not assigned to the requested target")
    public void authenticationRequestDeniedForArtifactWhichIsNotAssignedToTarget() {
        final MessageProperties messageProperties = createMessageProperties(null);
        final DmfTenantSecurityToken securityToken = new DmfTenantSecurityToken(TENANT, TENANT_ID, CONTROLLER_ID,
                TARGET_ID, FileResource.createFileResourceBySha1("12345"));
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        final Artifact localArtifactMock = mock(Artifact.class);
        when(artifactManagementMock.findFirstBySHA1(anyString())).thenReturn(Optional.of(localArtifactMock));

        // test
        final Message onMessage = amqpAuthenticationMessageHandlerService.onAuthenticationRequest(message);

        // verify
        final DmfDownloadResponse downloadResponse = (DmfDownloadResponse) messageConverter.fromMessage(onMessage);
        assertThat(downloadResponse).as("Message body should not null").isNotNull();
        assertThat(downloadResponse.getResponseCode()).as("Message body response code is wrong")
                .isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @Description("Tests that an download request is allowed for an artifact which exists and assigned to the requested target")
    public void authenticationRequestAllowedForArtifactWhichExistsAndAssignedToTarget() throws MalformedURLException {
        final MessageProperties messageProperties = createMessageProperties(null);
        final DmfTenantSecurityToken securityToken = new DmfTenantSecurityToken(TENANT, TENANT_ID, CONTROLLER_ID,
                TARGET_ID, FileResource.createFileResourceBySha1("12345"));
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        // mock
        final Artifact localArtifactMock = mock(Artifact.class);
        when(localArtifactMock.getSha1Hash()).thenReturn(SHA1);
        when(localArtifactMock.getMd5Hash()).thenReturn("md5");
        when(localArtifactMock.getSize()).thenReturn(1L);

        when(artifactManagementMock.findFirstBySHA1(SHA1)).thenReturn(Optional.of(localArtifactMock));
        when(controllerManagementMock.hasTargetArtifactAssigned(securityToken.getControllerId(), SHA1))
                .thenReturn(true);
        when(hostnameResolverMock.resolveHostname()).thenReturn(new URL("http://localhost"));

        // test
        final Message onMessage = amqpAuthenticationMessageHandlerService.onAuthenticationRequest(message);

        // verify
        final DmfDownloadResponse downloadResponse = (DmfDownloadResponse) messageConverter.fromMessage(onMessage);
        assertThat(downloadResponse).as("Message body should not null").isNotNull();
        assertThat(downloadResponse.getResponseCode()).as("Message body response code is wrong")
                .isEqualTo(HttpStatus.OK.value());
        assertThat(downloadResponse.getArtifact().getSize()).as("Wrong artifact size in message body").isEqualTo(1L);
        assertThat(downloadResponse.getArtifact().getHashes().getSha1()).as("Wrong sha1 hash").isEqualTo(SHA1);
        assertThat(downloadResponse.getArtifact().getHashes().getMd5()).as("Wrong md5 hash").isEqualTo("md5");
        assertThat(downloadResponse.getDownloadUrl()).as("download url is wrong")
                .startsWith("http://localhost/api/v1/downloadserver/downloadId/");
    }

    @Test
    @Description("Tests TODO")
    public void lookupNextUpdateActionAfterFinished() throws IllegalAccessException {

        // Mock
        final Action action = createActionWithTarget(22L, Status.FINISHED);
        when(controllerManagementMock.findActionWithDetails(anyLong())).thenReturn(Optional.of(action));
        when(controllerManagementMock.addUpdateActionStatus(any())).thenReturn(action);
        final ActionStatusBuilder builder = mock(ActionStatusBuilder.class);
        final ActionStatusCreate create = mock(ActionStatusCreate.class);
        when(builder.create(22L)).thenReturn(create);
        when(create.status(any())).thenReturn(create);
        when(entityFactoryMock.actionStatus()).thenReturn(builder);
        // for the test the same action can be used
        when(controllerManagementMock.findActiveActionWithHighestWeight(any()))
                .thenReturn(Optional.of(action));

        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        messageProperties.setHeader(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ACTION_STATUS.name());
        final DmfActionUpdateStatus actionUpdateStatus = createActionUpdateStatus(DmfActionStatus.FINISHED, 23L);
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(actionUpdateStatus,
                messageProperties);

        // test
        amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT, VIRTUAL_HOST);

        final ArgumentCaptor<ActionProperties> actionPropertiesCaptor = ArgumentCaptor.forClass(ActionProperties.class);
        final ArgumentCaptor<Target> targetCaptor = ArgumentCaptor.forClass(Target.class);

        verify(amqpMessageDispatcherServiceMock, times(1)).sendUpdateMessageToTarget(actionPropertiesCaptor.capture(),
                targetCaptor.capture(), any(Map.class));
        final ActionProperties actionProperties = actionPropertiesCaptor.getValue();
        assertThat(actionProperties).isNotNull();
        assertThat(actionProperties.getTenant()).as("event has tenant").isEqualTo("DEFAULT");
        assertThat(targetCaptor.getValue().getControllerId()).as("event has wrong controller id").isEqualTo("target1");
        assertThat(actionProperties.getId()).as("event has wrong action id").isEqualTo(22L);
    }

    private DmfActionUpdateStatus createActionUpdateStatus(final DmfActionStatus status) {
        return createActionUpdateStatus(status, 2L);
    }

    private DmfActionUpdateStatus createActionUpdateStatus(final DmfActionStatus status, final Long id) {
        final DmfActionUpdateStatus actionUpdateStatus = new DmfActionUpdateStatus(id, status);
        actionUpdateStatus.setSoftwareModuleId(Long.valueOf(2));
        return actionUpdateStatus;
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

    private Action createActionWithTarget(final Long targetId, final Status status) throws IllegalAccessException {
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

    @Test
    @Description("Tests the deletion of a target/thing, requested by the target itself.")
    public void deleteThing() {
        // prepare valid message
        final String knownThingId = "1";
        final MessageProperties messageProperties = createMessageProperties(MessageType.THING_REMOVED);
        messageProperties.setHeader(MessageHeaderKey.THING_ID, knownThingId);
        final Message message = messageConverter.toMessage(new byte[0], messageProperties);

        // test
        amqpMessageHandlerService.onMessage(message, MessageType.THING_REMOVED.name(), TENANT, VIRTUAL_HOST);

        // verify
        verify(controllerManagementMock).deleteExistingTarget(knownThingId);
    }

    @Test
    @Description("Tests the deletion of a target/thing with missing thingId")
    public void deleteThingWithoutThingId() {
        // prepare invalid message
        final MessageProperties messageProperties = createMessageProperties(MessageType.THING_REMOVED);
        final Message message = messageConverter.toMessage(new byte[0], messageProperties);

        assertThatExceptionOfType(AmqpRejectAndDontRequeueException.class)
                .as(FAIL_MESSAGE_AMQP_REJECT_REASON + "since no thingId was set")
                .isThrownBy(() -> amqpMessageHandlerService.onMessage(message, MessageType.THING_REMOVED.name(), TENANT,
                        VIRTUAL_HOST));
    }

    private MessageProperties getThingCreatedMessageProperties(String thingId) {
        final MessageProperties messageProperties = createMessageProperties(MessageType.THING_CREATED);
        messageProperties.setHeader(MessageHeaderKey.THING_ID, thingId);
        return messageProperties;
    }

    private Message createMessage(Object object, MessageProperties messageProperties) {
        return messageConverter.toMessage(object, messageProperties);
    }
}
