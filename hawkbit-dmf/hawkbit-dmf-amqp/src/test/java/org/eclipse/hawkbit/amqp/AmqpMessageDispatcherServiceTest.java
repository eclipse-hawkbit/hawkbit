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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.hawkbit.api.ArtifactUrl;
import org.eclipse.hawkbit.api.ArtifactUrlHandler;
import org.eclipse.hawkbit.artifact.repository.ArtifactFilesystem;
import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.DmfDownloadAndUpdateRequest;
import org.eclipse.hawkbit.dmf.json.model.DmfMetadata;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.jpa.RepositoryApplicationConfiguration;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.repository.test.util.AbstractIntegrationTest;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.eclipse.hawkbit.util.IpUtil;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@ActiveProfiles({ "test" })
@Features("Component Tests - Device Management Federation API")
@Stories("AmqpMessage Dispatcher Service Test")
@SpringApplicationConfiguration(classes = { RepositoryApplicationConfiguration.class })
public class AmqpMessageDispatcherServiceTest extends AbstractIntegrationTest {

    private static final String TENANT = "default";
    private static final Long TENANT_ID = 4711L;

    private static final URI AMQP_URI = IpUtil.createAmqpUri("vHost", "mytest");

    private static final String TEST_TOKEN = "testToken";

    private static final String CONTROLLER_ID = "1";

    private AmqpMessageDispatcherService amqpMessageDispatcherService;

    private RabbitTemplate rabbitTemplate;

    private DefaultAmqpMessageSenderService senderService;

    private Target testTarget;

    @Override
    public void before() throws Exception {
        super.before();
        testTarget = targetManagement.create(entityFactory.target().create().controllerId(CONTROLLER_ID)
                .securityToken(TEST_TOKEN).address(AMQP_URI.toString()));

        this.rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        when(rabbitTemplate.getMessageConverter()).thenReturn(new Jackson2JsonMessageConverter());

        senderService = Mockito.mock(DefaultAmqpMessageSenderService.class);

        final ArtifactUrlHandler artifactUrlHandlerMock = Mockito.mock(ArtifactUrlHandler.class);
        when(artifactUrlHandlerMock.getUrls(anyObject(), anyObject()))
                .thenReturn(Arrays.asList(new ArtifactUrl("http", "download", "http://mockurl")));

        systemManagement = Mockito.mock(SystemManagement.class);
        final TenantMetaData tenantMetaData = Mockito.mock(TenantMetaData.class);
        when(tenantMetaData.getId()).thenReturn(TENANT_ID);
        when(tenantMetaData.getTenant()).thenReturn(TENANT);

        when(systemManagement.getTenantMetadata()).thenReturn(tenantMetaData);

        amqpMessageDispatcherService = new AmqpMessageDispatcherService(rabbitTemplate, senderService,
                artifactUrlHandlerMock, systemSecurityContext, systemManagement, targetManagement, serviceMatcher,
                distributionSetManagement, softwareModuleManagement);

    }

    private Message getCaptureAdressEvent(final TargetAssignDistributionSetEvent targetAssignDistributionSetEvent) {
        final Target target = targetManagement
                .getByControllerID(targetAssignDistributionSetEvent.getActions().keySet().iterator().next()).get();
        final Message sendMessage = createArgumentCapture(target.getAddress());
        return sendMessage;
    }

    private Action createAction(final DistributionSet testDs) {
        return deploymentManagement.findAction(assignDistributionSet(testDs, testTarget).getActions().get(0)).get();
    }

    @Test
    @Description("Verifies that download and install event with 3 software moduls and no artifacts works")
    public void testSendDownloadRequesWithSoftwareModulesAndNoArtifacts() {
        final DistributionSet createDistributionSet = testdataFactory
                .createDistributionSet(UUID.randomUUID().toString());
        testdataFactory.addSoftwareModuleMetadata(createDistributionSet);

        final Action action = createAction(createDistributionSet);

        final TargetAssignDistributionSetEvent targetAssignDistributionSetEvent = new TargetAssignDistributionSetEvent(
                action, serviceMatcher.getServiceId());
        amqpMessageDispatcherService.targetAssignDistributionSet(targetAssignDistributionSetEvent);
        final Message sendMessage = getCaptureAdressEvent(targetAssignDistributionSetEvent);
        final DmfDownloadAndUpdateRequest downloadAndUpdateRequest = assertDownloadAndInstallMessage(sendMessage,
                action.getId());
        assertThat(createDistributionSet.getModules()).hasSameSizeAs(downloadAndUpdateRequest.getSoftwareModules());
        assertThat(downloadAndUpdateRequest.getTargetSecurityToken()).isEqualTo(TEST_TOKEN);
        for (final org.eclipse.hawkbit.dmf.json.model.DmfSoftwareModule softwareModule : downloadAndUpdateRequest
                .getSoftwareModules()) {
            assertTrue("Artifact list for softwaremodule should be empty", softwareModule.getArtifacts().isEmpty());

            assertThat(softwareModule.getMetadata()).containsExactly(
                    new DmfMetadata(TestdataFactory.VISIBLE_SM_MD_KEY, TestdataFactory.VISIBLE_SM_MD_VALUE));

            for (final SoftwareModule softwareModule2 : action.getDistributionSet().getModules()) {
                assertNotNull("Sofware module ID should be set", softwareModule.getModuleId());
                if (!softwareModule.getModuleId().equals(softwareModule2.getId())) {
                    continue;
                }
                assertEquals(
                        "Software module type in event should be the same as the softwaremodule in the distribution set",
                        softwareModule.getModuleType(), softwareModule2.getType().getKey());
                assertEquals(
                        "Software module version in event should be the same as the softwaremodule in the distribution set",
                        softwareModule.getModuleVersion(), softwareModule2.getVersion());
            }
        }
    }

    @Test
    @Description("Verifies that download and install event with software moduls and artifacts works")
    public void testSendDownloadRequest() {
        DistributionSet dsA = testdataFactory.createDistributionSet(UUID.randomUUID().toString());
        SoftwareModule module = dsA.getModules().iterator().next();
        final List<AbstractDbArtifact> receivedList = new ArrayList<>();
        for (final Artifact artifact : testdataFactory.createArtifacts(module.getId())) {
            receivedList.add(new ArtifactFilesystem(new File("./test"), artifact.getSha1Hash(),
                    new DbArtifactHash(artifact.getSha1Hash(), null), artifact.getSize(), null));
        }
        module = softwareModuleManagement.get(module.getId()).get();
        dsA = distributionSetManagement.get(dsA.getId()).get();

        final Action action = createAction(dsA);

        Mockito.when(rabbitTemplate.convertSendAndReceive(any())).thenReturn(receivedList);

        final TargetAssignDistributionSetEvent targetAssignDistributionSetEvent = new TargetAssignDistributionSetEvent(
                action, serviceMatcher.getServiceId());
        amqpMessageDispatcherService.targetAssignDistributionSet(targetAssignDistributionSetEvent);
        final Message sendMessage = getCaptureAdressEvent(targetAssignDistributionSetEvent);
        final DmfDownloadAndUpdateRequest downloadAndUpdateRequest = assertDownloadAndInstallMessage(sendMessage,
                action.getId());

        assertEquals("DownloadAndUpdateRequest event should contains 3 software modules", 3,
                downloadAndUpdateRequest.getSoftwareModules().size());
        assertThat(downloadAndUpdateRequest.getTargetSecurityToken()).isEqualTo(TEST_TOKEN);

        for (final org.eclipse.hawkbit.dmf.json.model.DmfSoftwareModule softwareModule : downloadAndUpdateRequest
                .getSoftwareModules()) {
            if (!softwareModule.getModuleId().equals(module.getId())) {
                continue;
            }
            assertThat(softwareModule.getArtifacts().size()).isEqualTo(module.getArtifacts().size()).isGreaterThan(0);

            module.getArtifacts().forEach(dbArtifact -> {
                final Optional<org.eclipse.hawkbit.dmf.json.model.DmfArtifact> found = softwareModule.getArtifacts()
                        .stream().filter(dmfartifact -> dmfartifact.getFilename().equals(dbArtifact.getFilename()))
                        .findAny();

                assertThat(found).as("The artifact should exist in message").isPresent();
                assertThat(found.get().getSize()).isEqualTo(dbArtifact.getSize());
                assertThat(found.get().getHashes().getMd5()).isEqualTo(dbArtifact.getMd5Hash());
                assertThat(found.get().getHashes().getSha1()).isEqualTo(dbArtifact.getSha1Hash());
            });
        }
    }

    @Test
    @Description("Verifies that send cancel event works")
    public void testSendCancelRequest() {
        final CancelTargetAssignmentEvent cancelTargetAssignmentDistributionSetEvent = new CancelTargetAssignmentEvent(
                testTarget, 1L, serviceMatcher.getServiceId());
        amqpMessageDispatcherService
                .targetCancelAssignmentToDistributionSet(cancelTargetAssignmentDistributionSetEvent);
        final Message sendMessage = createArgumentCapture(
                cancelTargetAssignmentDistributionSetEvent.getEntity().getAddress());
        assertCancelMessage(sendMessage);

    }

    @Test
    @Description("Verifies that sending a delete message when receiving a delete event works.")
    public void sendDeleteRequest() {

        // setup
        final String amqpUri = "amqp://anyhost";
        final TargetDeletedEvent targetDeletedEvent = new TargetDeletedEvent(TENANT, 1L, CONTROLLER_ID, amqpUri,
                Target.class.getName(), serviceMatcher.getServiceId());

        // test
        amqpMessageDispatcherService.targetDelete(targetDeletedEvent);

        // verify
        final Message sendMessage = createArgumentCapture(URI.create(amqpUri));
        assertDeleteMessage(sendMessage);
    }

    @Test
    @Description("Verifies that a delete message is not send if the address is not an amqp address.")
    public void sendDeleteRequestWithNoAmqpAdress() {

        // setup
        final String noAmqpUri = "http://anyhost";
        final TargetDeletedEvent targetDeletedEvent = new TargetDeletedEvent(TENANT, 1L, CONTROLLER_ID, noAmqpUri,
                Target.class.getName(), serviceMatcher.getServiceId());

        // test
        amqpMessageDispatcherService.targetDelete(targetDeletedEvent);

        // verify
        Mockito.verifyZeroInteractions(senderService);
    }

    @Test
    @Description("Verfies that a delete message is not send if the address is null.")
    public void sendDeleteRequestWithNullAdress() {

        // setup
        final String noAmqpUri = null;
        final TargetDeletedEvent targetDeletedEvent = new TargetDeletedEvent(TENANT, 1L, CONTROLLER_ID, noAmqpUri,
                Target.class.getName(), serviceMatcher.getServiceId());

        // test
        amqpMessageDispatcherService.targetDelete(targetDeletedEvent);

        // verify
        Mockito.verifyZeroInteractions(senderService);
    }

    private void assertCancelMessage(final Message sendMessage) {
        assertEventMessage(sendMessage);
        final Long actionId = convertMessage(sendMessage, Long.class);
        assertEquals("Action ID should be 1", actionId, Long.valueOf(1));
        assertEquals("The topc in the message should be a CANCEL_DOWNLOAD value", EventTopic.CANCEL_DOWNLOAD,
                sendMessage.getMessageProperties().getHeaders().get(MessageHeaderKey.TOPIC));
    }

    private void assertDeleteMessage(final Message sendMessage) {

        assertNotNull(sendMessage);
        assertThat(sendMessage.getMessageProperties().getHeaders().get(MessageHeaderKey.THING_ID))
                .isEqualTo(CONTROLLER_ID);
        assertThat(sendMessage.getMessageProperties().getHeaders().get(MessageHeaderKey.TENANT)).isEqualTo(TENANT);
        assertThat(sendMessage.getMessageProperties().getHeaders().get(MessageHeaderKey.TYPE))
                .isEqualTo(MessageType.THING_DELETED);
    }

    private DmfDownloadAndUpdateRequest assertDownloadAndInstallMessage(final Message sendMessage, final Long action) {
        assertEventMessage(sendMessage);
        final DmfDownloadAndUpdateRequest downloadAndUpdateRequest = convertMessage(sendMessage,
                DmfDownloadAndUpdateRequest.class);
        assertEquals(downloadAndUpdateRequest.getActionId(), action);
        assertEquals("The topic of the event shuold contain DOWNLOAD_AND_INSTALL", EventTopic.DOWNLOAD_AND_INSTALL,
                sendMessage.getMessageProperties().getHeaders().get(MessageHeaderKey.TOPIC));
        assertEquals("Security token of target", TEST_TOKEN, downloadAndUpdateRequest.getTargetSecurityToken());

        return downloadAndUpdateRequest;

    }

    private void assertEventMessage(final Message sendMessage) {
        assertNotNull("The message should not be null", sendMessage);

        assertEquals("The value of the message header THING_ID should be " + CONTROLLER_ID, CONTROLLER_ID,
                sendMessage.getMessageProperties().getHeaders().get(MessageHeaderKey.THING_ID));
        assertEquals("The value of the message header TYPE should be EVENT", MessageType.EVENT,
                sendMessage.getMessageProperties().getHeaders().get(MessageHeaderKey.TYPE));
        assertEquals("The content type message should be " + MessageProperties.CONTENT_TYPE_JSON,
                MessageProperties.CONTENT_TYPE_JSON, sendMessage.getMessageProperties().getContentType());
    }

    protected Message createArgumentCapture(final URI uri) {
        final ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(senderService).sendMessage(argumentCaptor.capture(), eq(uri));
        return argumentCaptor.getValue();
    }

    @SuppressWarnings("unchecked")
    private <T> T convertMessage(final Message message, final Class<T> clazz) {
        message.getMessageProperties().getHeaders().put(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
                clazz.getTypeName());
        return (T) rabbitTemplate.getMessageConverter().fromMessage(message);
    }

}
