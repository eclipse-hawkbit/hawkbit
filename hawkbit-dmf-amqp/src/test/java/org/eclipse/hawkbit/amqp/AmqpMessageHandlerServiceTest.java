/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.api.HostnameResolver;
import org.eclipse.hawkbit.artifact.repository.ArtifactRepository;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.ActionStatus;
import org.eclipse.hawkbit.dmf.json.model.ActionUpdateStatus;
import org.eclipse.hawkbit.dmf.json.model.DownloadResponse;
import org.eclipse.hawkbit.dmf.json.model.TenantSecurityToken;
import org.eclipse.hawkbit.dmf.json.model.TenantSecurityToken.FileResource;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.eventbus.event.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.helper.SecurityTokenGeneratorHolder;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.security.SecurityTokenGenerator;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.cache.Cache;
import org.springframework.http.HttpStatus;

import com.google.common.eventbus.EventBus;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@RunWith(MockitoJUnitRunner.class)
@Features("Component Tests - Device Management Federation API")
@Stories("AmqpMessage Handler Service Test")
public class AmqpMessageHandlerServiceTest {

    private static final String TENANT = "DEFAULT";

    private AmqpMessageHandlerService amqpMessageHandlerService;

    private MessageConverter messageConverter;

    @Mock
    private ControllerManagement controllerManagementMock;

    @Mock
    private EntityFactory entityFactoryMock;

    @Mock
    private ArtifactManagement artifactManagementMock;

    @Mock
    private AmqpControllerAuthentfication authenticationManagerMock;

    @Mock
    private ArtifactRepository artifactRepositoryMock;

    @Mock
    private Cache cacheMock;

    @Mock
    private HostnameResolver hostnameResolverMock;

    @Mock
    private EventBus eventBus;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private SystemSecurityContext systemSecurityContextMock;

    @Before
    public void before() throws Exception {
        messageConverter = new Jackson2JsonMessageConverter();
        when(rabbitTemplate.getMessageConverter()).thenReturn(messageConverter);
        amqpMessageHandlerService = new AmqpMessageHandlerService(rabbitTemplate);
        amqpMessageHandlerService.setControllerManagement(controllerManagementMock);
        amqpMessageHandlerService.setAuthenticationManager(authenticationManagerMock);
        amqpMessageHandlerService.setArtifactManagement(artifactManagementMock);
        amqpMessageHandlerService.setCache(cacheMock);
        amqpMessageHandlerService.setHostnameResolver(hostnameResolverMock);
        amqpMessageHandlerService.setEventBus(eventBus);
        amqpMessageHandlerService.setEntityFactory(entityFactoryMock);
        amqpMessageHandlerService.setSystemSecurityContext(systemSecurityContextMock);

    }

    @Test
    @Description("Tests not allowed content-type in message")
    public void testWrongContentType() {
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
    public void testCreateThing() {
        final String knownThingId = "1";
        final MessageProperties messageProperties = createMessageProperties(MessageType.THING_CREATED);
        messageProperties.setHeader(MessageHeaderKey.THING_ID, "1");
        final Message message = messageConverter.toMessage(new byte[0], messageProperties);

        final ArgumentCaptor<String> targetIdCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        when(controllerManagementMock.findOrRegisterTargetIfItDoesNotexist(targetIdCaptor.capture(),
                uriCaptor.capture())).thenReturn(null);

        amqpMessageHandlerService.onMessage(message, MessageType.THING_CREATED.name(), TENANT, "vHost");

        // verify
        assertThat(targetIdCaptor.getValue()).as("Thing id is wrong").isEqualTo(knownThingId);
        assertThat(uriCaptor.getValue().toString()).as("Uri is not right").isEqualTo("amqp://vHost/MyTest");

    }

    @Test
    @Description("Tests the creation of a thing without a 'reply to' header in message.")
    public void testCreateThingWitoutReplyTo() {
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
    public void testCreateThingWithoutID() {
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
    public void testUnknownMessageType() {
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
    public void testInvalidEventTopic() {
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
    public void testUpdateActionStatusWithoutActionId() {
        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        messageProperties.setHeader(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ACTION_STATUS.name());
        final ActionUpdateStatus actionUpdateStatus = new ActionUpdateStatus();
        actionUpdateStatus.setActionStatus(ActionStatus.DOWNLOAD);
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
    public void testUpdateActionStatusWithoutExistActionId() {
        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        messageProperties.setHeader(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ACTION_STATUS.name());
        final ActionUpdateStatus actionUpdateStatus = createActionUpdateStatus(ActionStatus.DOWNLOAD);
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
        final TenantSecurityToken securityToken = new TenantSecurityToken(TENANT, "123",
                FileResource.createFileResourceBySha1("12345"));
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        // test
        final Message onMessage = amqpMessageHandlerService.onAuthenticationRequest(message);

        // verify
        final DownloadResponse downloadResponse = (DownloadResponse) messageConverter.fromMessage(onMessage);
        assertThat(downloadResponse).as("Message body should not null").isNotNull();
        assertThat(downloadResponse.getResponseCode()).as("Message body response code is wrong")
                .isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @Description("Tests that an download request is denied for an artifact which is not assigned to the requested target")
    public void authenticationRequestDeniedForArtifactWhichIsNotAssignedToTarget() {
        final MessageProperties messageProperties = createMessageProperties(null);
        final TenantSecurityToken securityToken = new TenantSecurityToken(TENANT, "123",
                FileResource.createFileResourceBySha1("12345"));
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        final LocalArtifact localArtifactMock = mock(LocalArtifact.class);
        when(artifactManagementMock.findFirstLocalArtifactsBySHA1(anyString())).thenReturn(localArtifactMock);
        when(controllerManagementMock.getActionForDownloadByTargetAndSoftwareModule(anyObject(), anyObject()))
                .thenThrow(EntityNotFoundException.class);

        // test
        final Message onMessage = amqpMessageHandlerService.onAuthenticationRequest(message);

        // verify
        final DownloadResponse downloadResponse = (DownloadResponse) messageConverter.fromMessage(onMessage);
        assertThat(downloadResponse).as("Message body should not null").isNotNull();
        assertThat(downloadResponse.getResponseCode()).as("Message body response code is wrong")
                .isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @Description("Tests that an download request is allowed for an artifact which exists and assigned to the requested target")
    public void authenticationRequestAllowedForArtifactWhichExistsAndAssignedToTarget() throws MalformedURLException {
        final MessageProperties messageProperties = createMessageProperties(null);
        final TenantSecurityToken securityToken = new TenantSecurityToken(TENANT, "123",
                FileResource.createFileResourceBySha1("12345"));
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        // mock
        final LocalArtifact localArtifactMock = mock(LocalArtifact.class);
        final DbArtifact dbArtifactMock = mock(DbArtifact.class);
        when(artifactManagementMock.findFirstLocalArtifactsBySHA1(anyString())).thenReturn(localArtifactMock);
        when(controllerManagementMock.hasTargetArtifactAssigned(securityToken.getControllerId(), localArtifactMock))
                .thenReturn(true);
        when(artifactManagementMock.loadLocalArtifactBinary(localArtifactMock)).thenReturn(dbArtifactMock);
        when(dbArtifactMock.getArtifactId()).thenReturn("artifactId");
        when(dbArtifactMock.getSize()).thenReturn(1L);
        when(dbArtifactMock.getHashes()).thenReturn(new DbArtifactHash("sha1", "md5"));
        when(hostnameResolverMock.resolveHostname()).thenReturn(new URL("http://localhost"));

        // test
        final Message onMessage = amqpMessageHandlerService.onAuthenticationRequest(message);

        // verify
        final DownloadResponse downloadResponse = (DownloadResponse) messageConverter.fromMessage(onMessage);
        assertThat(downloadResponse).as("Message body should not null").isNotNull();
        assertThat(downloadResponse.getResponseCode()).as("Message body response code is wrong")
                .isEqualTo(HttpStatus.OK.value());
        assertThat(downloadResponse.getArtifact().getSize()).as("Wrong artifact size in message body").isEqualTo(1L);
        assertThat(downloadResponse.getArtifact().getHashes().getSha1()).as("Wrong sha1 hash").isEqualTo("sha1");
        assertThat(downloadResponse.getArtifact().getHashes().getMd5()).as("Wrong md5 hash").isEqualTo("md5");
        assertThat(downloadResponse.getDownloadUrl()).as("download url is wrong")
                .startsWith("http://localhost/api/v1/downloadserver/downloadId/");
    }

    @Test
    @Description("Tests TODO")
    public void lookupNextUpdateActionAfterFinished() throws IllegalAccessException {

        // Mock
        final Action action = createActionWithTarget(22L, Status.FINISHED);
        when(controllerManagementMock.findActionWithDetails(Matchers.any())).thenReturn(action);
        when(controllerManagementMock.addUpdateActionStatus(Matchers.any())).thenReturn(action);
        when(entityFactoryMock.generateActionStatus()).thenReturn(new JpaActionStatus());
        // for the test the same action can be used
        final List<Action> actionList = new ArrayList<>();
        actionList.add(action);
        when(controllerManagementMock.findActionByTargetAndActive(Matchers.any())).thenReturn(actionList);

        final List<SoftwareModule> softwareModuleList = createSoftwareModuleList();
        when(controllerManagementMock.findSoftwareModulesByDistributionSet(Matchers.any()))
                .thenReturn(softwareModuleList);

        when(systemSecurityContextMock.runAsSystem(anyObject())).thenReturn("securityToken");

        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        messageProperties.setHeader(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ACTION_STATUS.name());
        final ActionUpdateStatus actionUpdateStatus = createActionUpdateStatus(ActionStatus.FINISHED, 23L);
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(actionUpdateStatus,
                messageProperties);

        // test
        amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT, "vHost");

        // verify
        final ArgumentCaptor<TargetAssignDistributionSetEvent> captorTargetAssignDistributionSetEvent = ArgumentCaptor
                .forClass(TargetAssignDistributionSetEvent.class);
        verify(eventBus, times(1)).post(captorTargetAssignDistributionSetEvent.capture());
        final TargetAssignDistributionSetEvent targetAssignDistributionSetEvent = captorTargetAssignDistributionSetEvent
                .getValue();

        assertThat(targetAssignDistributionSetEvent.getControllerId()).as("event has wrong controller id")
                .isEqualTo("target1");
        assertThat(targetAssignDistributionSetEvent.getTargetToken()).as("targetoken not filled correctly")
                .isEqualTo(action.getTarget().getSecurityToken());
        assertThat(targetAssignDistributionSetEvent.getActionId()).as("event has wrong action id").isEqualTo(22L);
        assertThat(targetAssignDistributionSetEvent.getSoftwareModules()).as("event has wrong sofware modules")
                .isEqualTo(softwareModuleList);

    }

    private ActionUpdateStatus createActionUpdateStatus(final ActionStatus status) {
        return createActionUpdateStatus(status, 2L);
    }

    private ActionUpdateStatus createActionUpdateStatus(final ActionStatus status, final Long id) {
        final ActionUpdateStatus actionUpdateStatus = new ActionUpdateStatus();
        actionUpdateStatus.setActionId(id);
        actionUpdateStatus.setSoftwareModuleId(Long.valueOf(2));
        actionUpdateStatus.setActionStatus(status);
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

    private List<SoftwareModule> createSoftwareModuleList() {
        final List<SoftwareModule> softwareModuleList = new ArrayList<>();
        final JpaSoftwareModule softwareModule = new JpaSoftwareModule();
        softwareModule.setId(777L);
        softwareModuleList.add(softwareModule);
        return softwareModuleList;
    }

    private Action createActionWithTarget(final Long targetId, final Status status) throws IllegalAccessException {
        // is needed for the creation of targets
        initalizeSecurityTokenGenerator();

        // Mock
        final JpaAction actionMock = mock(JpaAction.class);
        final JpaTarget targetMock = mock(JpaTarget.class);
        final TargetInfo targetInfoMock = mock(TargetInfo.class);
        when(actionMock.getId()).thenReturn(targetId);
        when(actionMock.getStatus()).thenReturn(status);
        when(actionMock.getTenant()).thenReturn("DEFAULT");
        when(actionMock.getTarget()).thenReturn(targetMock);
        when(targetMock.getControllerId()).thenReturn("target1");
        when(targetMock.getSecurityToken()).thenReturn("securityToken");
        when(targetMock.getTargetInfo()).thenReturn(targetInfoMock);
        when(targetInfoMock.getAddress()).thenReturn(null);
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
