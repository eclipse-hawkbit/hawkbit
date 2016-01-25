/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.AbstractIntegrationTestWithMongoDB;
import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.DownloadAndUpdateRequest;
import org.eclipse.hawkbit.eventbus.event.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.eventbus.event.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.util.ArtifactUrlHandler;
import org.eclipse.hawkbit.util.IpUtil;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@ActiveProfiles({ "test" })
@Features("AMQP Dispatcher Test")
@Stories("Tests send messages")
public class AmqpMessageDispatcherServiceTest extends AbstractIntegrationTestWithMongoDB {

    private AmqpMessageDispatcherService amqpMessageDispatcherService;

    private MessageConverter messageConverter;

    private RabbitTemplate rabbitTemplate;

    private static final String CONTROLLER_ID = "1";

    @Override
    public void before() throws Exception {
        super.before();
        amqpMessageDispatcherService = new AmqpMessageDispatcherService();
        amqpMessageDispatcherService = spy(amqpMessageDispatcherService);
        messageConverter = new Jackson2JsonMessageConverter();

        final ArtifactUrlHandler artifactUrlHandlerMock = Mockito.mock(ArtifactUrlHandler.class);
        when(artifactUrlHandlerMock.getUrl(anyString(), any(), anyObject())).thenReturn("http://mockurl");

        this.rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        when(rabbitTemplate.getMessageConverter()).thenReturn(messageConverter);

        amqpMessageDispatcherService.setRabbitTemplate(rabbitTemplate);
        amqpMessageDispatcherService.setTenantAware(tenantAware);
        amqpMessageDispatcherService.setArtifactUrlHandler(artifactUrlHandlerMock);
    }

    @Test
    @Description("Verfies that download and install event with no software modul works")
    public void testSendDownloadRequesWithEmptySoftwareModules() {
        final TargetAssignDistributionSetEvent targetAssignDistributionSetEvent = new TargetAssignDistributionSetEvent(
                1L, "default", CONTROLLER_ID, 1l, new ArrayList<SoftwareModule>(), IpUtil.createAmqpUri("mytest"));
        amqpMessageDispatcherService.targetAssignDistributionSet(targetAssignDistributionSetEvent);
        final Message sendMessage = createArgumentCapture(targetAssignDistributionSetEvent.getTargetAdress().getHost());
        final DownloadAndUpdateRequest downloadAndUpdateRequest = assertDownloadAndInstallMessage(sendMessage);
        assertTrue(downloadAndUpdateRequest.getSoftwareModules().isEmpty());
    }

    @Test
    @Description("Verfies that download and install event with 3 software moduls and no artifacts works")
    public void testSendDownloadRequesWithSoftwareModulesAndNoArtifacts() {
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        final TargetAssignDistributionSetEvent targetAssignDistributionSetEvent = new TargetAssignDistributionSetEvent(
                1L, "default", CONTROLLER_ID, 1l, dsA.getModules(), IpUtil.createAmqpUri("mytest"));
        amqpMessageDispatcherService.targetAssignDistributionSet(targetAssignDistributionSetEvent);
        final Message sendMessage = createArgumentCapture(targetAssignDistributionSetEvent.getTargetAdress().getHost());
        final DownloadAndUpdateRequest downloadAndUpdateRequest = assertDownloadAndInstallMessage(sendMessage);
        assertEquals(3, downloadAndUpdateRequest.getSoftwareModules().size());
        for (final org.eclipse.hawkbit.dmf.json.model.SoftwareModule softwareModule : downloadAndUpdateRequest
                .getSoftwareModules()) {
            assertTrue(softwareModule.getArtifacts().isEmpty());
            for (final SoftwareModule softwareModule2 : dsA.getModules()) {
                assertNotNull(softwareModule.getModuleId());
                if (!softwareModule.getModuleId().equals(softwareModule2.getId())) {
                    continue;
                }
                assertEquals(softwareModule.getModuleType(), softwareModule2.getType().getKey());
                assertEquals(softwareModule.getModuleVersion(), softwareModule2.getVersion());
            }
        }
    }

    @Test
    @Description("Verfies that download and install event with software moduls and artifacts works")
    public void testSendDownloadRequest() {
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        final SoftwareModule module = dsA.getModules().iterator().next();
        final List<DbArtifact> receivedList = new ArrayList<>();
        for (final Artifact artifact : TestDataUtil.generateArtifacts(artifactManagement, module.getId())) {
            module.addArtifact((LocalArtifact) artifact);
            receivedList.add(new DbArtifact());
        }

        Mockito.when(rabbitTemplate.convertSendAndReceive(any())).thenReturn(receivedList);

        final TargetAssignDistributionSetEvent targetAssignDistributionSetEvent = new TargetAssignDistributionSetEvent(
                1L, "default", CONTROLLER_ID, 1l, dsA.getModules(), IpUtil.createAmqpUri("mytest"));
        amqpMessageDispatcherService.targetAssignDistributionSet(targetAssignDistributionSetEvent);
        final Message sendMessage = createArgumentCapture(targetAssignDistributionSetEvent.getTargetAdress().getHost());
        final DownloadAndUpdateRequest downloadAndUpdateRequest = assertDownloadAndInstallMessage(sendMessage);
        assertEquals(3, downloadAndUpdateRequest.getSoftwareModules().size());
        for (final org.eclipse.hawkbit.dmf.json.model.SoftwareModule softwareModule : downloadAndUpdateRequest
                .getSoftwareModules()) {
            if (!softwareModule.getModuleId().equals(module.getId())) {
                continue;
            }
            assertFalse(softwareModule.getArtifacts().isEmpty());
        }
    }

    @Test
    @Description("Verfies that send cancel event works")
    public void testSendCancelRequest() {
        final CancelTargetAssignmentEvent cancelTargetAssignmentDistributionSetEvent = new CancelTargetAssignmentEvent(
                1L, "default", CONTROLLER_ID, 1l, IpUtil.createAmqpUri("mytest"));
        amqpMessageDispatcherService
                .targetCancelAssignmentToDistributionSet(cancelTargetAssignmentDistributionSetEvent);
        final Message sendMessage = createArgumentCapture(cancelTargetAssignmentDistributionSetEvent.getTargetAdress()
                .getHost());
        assertCancelMessage(sendMessage);

    }

    private void assertCancelMessage(final Message sendMessage) {
        assertEventMessage(sendMessage);
        final Long actionId = convertMessage(sendMessage, Long.class);
        assertEquals(actionId, Long.valueOf(1));
        assertEquals(EventTopic.CANCEL_DOWNLOAD,
                sendMessage.getMessageProperties().getHeaders().get(MessageHeaderKey.TOPIC));

    }

    private DownloadAndUpdateRequest assertDownloadAndInstallMessage(final Message sendMessage) {
        assertEventMessage(sendMessage);
        final DownloadAndUpdateRequest downloadAndUpdateRequest = convertMessage(sendMessage,
                DownloadAndUpdateRequest.class);
        assertEquals(downloadAndUpdateRequest.getActionId(), Long.valueOf(1));
        assertEquals(EventTopic.DOWNLOAD_AND_INSTALL,
                sendMessage.getMessageProperties().getHeaders().get(MessageHeaderKey.TOPIC));
        return downloadAndUpdateRequest;

    }

    /**
     * @param sendMessage
     */
    private void assertEventMessage(final Message sendMessage) {
        assertNotNull(sendMessage);

        assertEquals(CONTROLLER_ID, sendMessage.getMessageProperties().getHeaders().get(MessageHeaderKey.THING_ID));
        assertEquals(MessageType.EVENT, sendMessage.getMessageProperties().getHeaders().get(MessageHeaderKey.TYPE));
        assertEquals(MessageProperties.CONTENT_TYPE_JSON, sendMessage.getMessageProperties().getContentType());
    }

    protected Message createArgumentCapture(final String exchange) {
        final ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(amqpMessageDispatcherService).sendMessage(eq(exchange), argumentCaptor.capture());
        return argumentCaptor.getValue();
    }

    @SuppressWarnings("unchecked")
    private <T> T convertMessage(final Message message, final Class<T> clazz) {
        message.getMessageProperties().getHeaders()
                .put(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME, clazz.getTypeName());
        return (T) rabbitTemplate.getMessageConverter().fromMessage(message);
    }

}
