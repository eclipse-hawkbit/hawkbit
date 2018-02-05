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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
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
import org.eclipse.hawkbit.dmf.json.model.DmfDownloadResponse;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.ActionStatusBuilder;
import org.eclipse.hawkbit.repository.builder.ActionStatusCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.helper.SecurityTokenGeneratorHolder;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.security.DmfTenantSecurityToken;
import org.eclipse.hawkbit.security.DmfTenantSecurityToken.FileResource;
import org.eclipse.hawkbit.security.SecurityTokenGenerator;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.http.HttpStatus;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@RunWith(MockitoJUnitRunner.class)
@Features("Component Tests - Device Management Federation API")
@Stories("AmqpMessage Handler Service Test")
public class AmqpMessageHandlerServiceTest {

    private static final String SHA1 = "12345";
    private static final String TENANT = "DEFAULT";
    private static final Long TENANT_ID = 123L;
    private static final String CONTROLLLER_ID = "123";
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

    @Captor
    private ArgumentCaptor<Map<String, String>> attributesCaptor;

    @Captor
    private ArgumentCaptor<String> targetIdCaptor;

    @Before
    public void before() throws Exception {
        messageConverter = new Jackson2JsonMessageConverter();
        when(rabbitTemplate.getMessageConverter()).thenReturn(messageConverter);
        when(artifactManagementMock.findFirstBySHA1(SHA1)).thenReturn(Optional.empty());

        amqpMessageHandlerService = new AmqpMessageHandlerService(rabbitTemplate, amqpMessageDispatcherServiceMock,
                controllerManagementMock, entityFactoryMock);

        amqpMessageHandlerService = new AmqpMessageHandlerService(rabbitTemplate, amqpMessageDispatcherServiceMock,
                controllerManagementMock, entityFactoryMock);
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
        try {
            amqpMessageHandlerService.onMessage(message, MessageType.THING_CREATED.name(), TENANT, "vHost");
            fail("AmqpRejectAndDontRequeueException was excepeted due to worng content type");
        } catch (final AmqpRejectAndDontRequeueException e) {
        }
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
        when(controllerManagementMock.findOrRegisterTargetIfItDoesNotexist(targetIdCaptor.capture(),
                uriCaptor.capture())).thenReturn(targetMock);
        when(controllerManagementMock.findOldestActiveActionByTarget(any())).thenReturn(Optional.empty());

        amqpMessageHandlerService.onMessage(message, MessageType.THING_CREATED.name(), TENANT, "vHost");

        // verify
        assertThat(targetIdCaptor.getValue()).as("Thing id is wrong").isEqualTo(knownThingId);
        assertThat(uriCaptor.getValue().toString()).as("Uri is not right").isEqualTo("amqp://vHost/MyTest");

    }

    @Test
    @Description("Tests the target attribute update by calling the same method that incoming RabbitMQ messages would access.")
    public void updateAttributes() {
        final String knownThingId = "1";
        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        messageProperties.setHeader(MessageHeaderKey.THING_ID, "1");
        messageProperties.setHeader(MessageHeaderKey.TOPIC, "UPDATE_ATTRIBUTES");
        final DmfAttributeUpdate attributeUpdate = new DmfAttributeUpdate();
        attributeUpdate.getAttributes().put("testKey1", "testValue1");
        attributeUpdate.getAttributes().put("testKey2", "testValue2");

        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(attributeUpdate,
                messageProperties);

        when(controllerManagementMock.updateControllerAttributes(targetIdCaptor.capture(), attributesCaptor.capture()))
                .thenReturn(null);

        amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT, "vHost");

        // verify
        assertThat(targetIdCaptor.getValue()).as("Thing id is wrong").isEqualTo(knownThingId);
        assertThat(attributesCaptor.getValue()).as("Attributes is not right")
                .isEqualTo(attributeUpdate.getAttributes());

    }

    @Test
    @Description("Tests the creation of a thing without a 'reply to' header in message.")
    public void createThingWitoutReplyTo() {
        final MessageProperties messageProperties = createMessageProperties(MessageType.THING_CREATED, null);
        messageProperties.setHeader(MessageHeaderKey.THING_ID, "1");
        final Message message = messageConverter.toMessage("", messageProperties);

        try {
            amqpMessageHandlerService.onMessage(message, MessageType.THING_CREATED.name(), TENANT, "vHost");
            fail("AmqpRejectAndDontRequeueException was excepeted since no replyTo header was set");
        } catch (final AmqpRejectAndDontRequeueException exception) {
            // test ok - exception was excepted
        }

    }

    @Test
    @Description("Tests the creation of a target/thing without a thingID by calling the same method that incoming RabbitMQ messages would access.")
    public void createThingWithoutID() {
        final MessageProperties messageProperties = createMessageProperties(MessageType.THING_CREATED);
        final Message message = messageConverter.toMessage(new byte[0], messageProperties);
        try {
            amqpMessageHandlerService.onMessage(message, MessageType.THING_CREATED.name(), TENANT, "vHost");
            fail("AmqpRejectAndDontRequeueException was excepeted since no thingID was set");
        } catch (final AmqpRejectAndDontRequeueException exception) {
            // test ok - exception was excepted
        }
    }

    @Test
    @Description("Tests the call of the same method that incoming RabbitMQ messages would access with an unknown message type.")
    public void unknownMessageType() {
        final String type = "bumlux";
        final MessageProperties messageProperties = createMessageProperties(MessageType.THING_CREATED);
        messageProperties.setHeader(MessageHeaderKey.THING_ID, "");
        final Message message = messageConverter.toMessage(new byte[0], messageProperties);

        try {
            amqpMessageHandlerService.onMessage(message, type, TENANT, "vHost");
            fail("AmqpRejectAndDontRequeueException was excepeted due to unknown message type");
        } catch (final AmqpRejectAndDontRequeueException exception) {
            // test ok - exception was excepted
        }
    }

    @Test
    @Description("Tests a invalid message without event topic")
    public void invalidEventTopic() {
        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        final Message message = new Message(new byte[0], messageProperties);
        try {
            amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT, "vHost");
            fail("AmqpRejectAndDontRequeueException was excepeted due to unknown message type");
        } catch (final AmqpRejectAndDontRequeueException e) {
        }

        try {
            messageProperties.setHeader(MessageHeaderKey.TOPIC, "wrongTopic");
            amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT, "vHost");
            fail("AmqpRejectAndDontRequeueException was excepeted due to unknown topic");
        } catch (final AmqpRejectAndDontRequeueException e) {
        }

        messageProperties.setHeader(MessageHeaderKey.TOPIC, EventTopic.CANCEL_DOWNLOAD.name());
        try {
            amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT, "vHost");
            fail("AmqpRejectAndDontRequeueException was excepeted because there was no event topic");
        } catch (final AmqpRejectAndDontRequeueException exception) {
            // test ok - exception was excepted
        }

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

        try {
            amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT, "vHost");
            fail("AmqpRejectAndDontRequeueException was excepeted since no action id was set");
        } catch (final AmqpRejectAndDontRequeueException exception) {
            // test ok - exception was excepted
        }
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

        try {
            amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT, "vHost");
            fail("AmqpRejectAndDontRequeueException was excepeted since no action id was set");
        } catch (final AmqpRejectAndDontRequeueException exception) {
            // test ok - exception was excepted
        }

    }

    @Test
    @Description("Tests that an download request is denied for an artifact which does not exists")
    public void authenticationRequestDeniedForArtifactWhichDoesNotExists() {
        final MessageProperties messageProperties = createMessageProperties(null);
        final DmfTenantSecurityToken securityToken = new DmfTenantSecurityToken(TENANT, TENANT_ID, CONTROLLLER_ID,
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
        final DmfTenantSecurityToken securityToken = new DmfTenantSecurityToken(TENANT, TENANT_ID, CONTROLLLER_ID,
                TARGET_ID, FileResource.createFileResourceBySha1("12345"));
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        final Artifact localArtifactMock = mock(Artifact.class);
        when(artifactManagementMock.findFirstBySHA1(anyString())).thenReturn(Optional.of(localArtifactMock));
        when(controllerManagementMock.getActionForDownloadByTargetAndSoftwareModule(anyString(), anyLong()))
                .thenThrow(EntityNotFoundException.class);

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
        final DmfTenantSecurityToken securityToken = new DmfTenantSecurityToken(TENANT, TENANT_ID, CONTROLLLER_ID,
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
        when(controllerManagementMock.findOldestActiveActionByTarget(any())).thenReturn(Optional.of(action));

        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        messageProperties.setHeader(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ACTION_STATUS.name());
        final DmfActionUpdateStatus actionUpdateStatus = createActionUpdateStatus(DmfActionStatus.FINISHED, 23L);
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(actionUpdateStatus,
                messageProperties);

        // test
        amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT, "vHost");

        final ArgumentCaptor<String> tenantCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Target> targetCaptor = ArgumentCaptor.forClass(Target.class);
        final ArgumentCaptor<Long> actionIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(amqpMessageDispatcherServiceMock, times(1)).sendUpdateMessageToTarget(tenantCaptor.capture(),
                targetCaptor.capture(), actionIdCaptor.capture(), any(Map.class));
        final String tenant = tenantCaptor.getValue();
        final String controllerId = targetCaptor.getValue().getControllerId();
        final Long actionId = actionIdCaptor.getValue();

        assertThat(tenant).as("event has tenant").isEqualTo("DEFAULT");
        assertThat(controllerId).as("event has wrong controller id").isEqualTo("target1");
        assertThat(actionId).as("event has wrong action id").isEqualTo(22L);

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
        initalizeSecurityTokenGenerator();

        // Mock
        final Action actionMock = mock(Action.class);
        final Target targetMock = mock(Target.class);
        final DistributionSet distributionSetMock = mock(DistributionSet.class);

        when(distributionSetMock.getId()).thenReturn(1L);
        when(actionMock.getDistributionSet()).thenReturn(distributionSetMock);
        when(actionMock.getId()).thenReturn(targetId);
        when(actionMock.getStatus()).thenReturn(status);
        when(actionMock.getTenant()).thenReturn("DEFAULT");
        when(actionMock.getTarget()).thenReturn(targetMock);
        when(targetMock.getControllerId()).thenReturn("target1");
        when(targetMock.getSecurityToken()).thenReturn("securityToken");
        when(targetMock.getAddress()).thenReturn(null);
        return actionMock;
    }

    private void initalizeSecurityTokenGenerator() throws IllegalAccessException {
        final SecurityTokenGeneratorHolder instance = SecurityTokenGeneratorHolder.getInstance();
        final Field[] fields = instance.getClass().getDeclaredFields();
        for (final Field field : fields) {
            if (field.getType().isAssignableFrom(SecurityTokenGenerator.class)) {
                field.setAccessible(true);
                field.set(instance, new SecurityTokenGenerator());
            }
        }
    }
}
