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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.artifact.repository.ArtifactFilesystem;
import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.eclipse.hawkbit.artifact.repository.urlhandler.ArtifactUrl;
import org.eclipse.hawkbit.artifact.repository.urlhandler.ArtifactUrlHandler;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.DmfActionRequest;
import org.eclipse.hawkbit.dmf.json.model.DmfDownloadAndUpdateRequest;
import org.eclipse.hawkbit.dmf.json.model.DmfSoftwareModule;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.event.remote.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({ "test" })
@Feature("Component Tests - Device Management Federation API")
@Story("AmqpMessage Dispatcher Service Test")
@SpringBootTest(classes = { RepositoryApplicationConfiguration.class }, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AmqpMessageDispatcherServiceTest extends AbstractIntegrationTest {

    private static final String TENANT = "DEFAULT";
    private static final Long TENANT_ID = 4711L;

    private static final URI AMQP_URI = IpUtil.createAmqpUri("vHost", "mytest");
    private static final String TEST_TOKEN = "testToken";
    private static final String CONTROLLER_ID = "1";

    private AmqpMessageDispatcherService amqpMessageDispatcherService;
    private RabbitTemplate rabbitTemplate;
    private DefaultAmqpMessageSenderService senderService;
    private Target testTarget;

    @BeforeEach
    public void beforeEach() throws Exception {
        testTarget = targetManagement.create(entityFactory.target().create().controllerId(CONTROLLER_ID)
                .securityToken(TEST_TOKEN).address(AMQP_URI.toString()));

        this.rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        when(rabbitTemplate.getMessageConverter()).thenReturn(new Jackson2JsonMessageConverter());

        senderService = Mockito.mock(DefaultAmqpMessageSenderService.class);

        final ArtifactUrlHandler artifactUrlHandlerMock = Mockito.mock(ArtifactUrlHandler.class);
        when(artifactUrlHandlerMock.getUrls(any(), any()))
                .thenReturn(Collections.singletonList(new ArtifactUrl("http", "download", "http://mockurl")));

        systemManagement = Mockito.mock(SystemManagement.class);
        final TenantMetaData tenantMetaData = Mockito.mock(TenantMetaData.class);
        when(tenantMetaData.getId()).thenReturn(TENANT_ID);
        when(tenantMetaData.getTenant()).thenReturn(TENANT);

        when(systemManagement.getTenantMetadata()).thenReturn(tenantMetaData);

        amqpMessageDispatcherService = new AmqpMessageDispatcherService(rabbitTemplate, senderService,
                artifactUrlHandlerMock, systemSecurityContext, systemManagement, targetManagement, serviceMatcher,
                distributionSetManagement, softwareModuleManagement, deploymentManagement, tenantConfigurationManagement);

    }

    protected Message createArgumentCapture(final URI uri) {
        final ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(senderService).sendMessage(argumentCaptor.capture(), eq(uri));
        return argumentCaptor.getValue();
    }

    @Test
    @Description("Verifies that download and install event with 3 software modules and no artifacts works")
    void testSendDownloadRequestWithSoftwareModulesAndNoArtifacts() {
        final DistributionSet createDistributionSet = testdataFactory
                .createDistributionSet(UUID.randomUUID().toString());
        testdataFactory.addSoftwareModuleMetadata(createDistributionSet);

        final Action action = createAction(createDistributionSet);

        final TargetAssignDistributionSetEvent targetAssignDistributionSetEvent = new TargetAssignDistributionSetEvent(
                action, serviceMatcher.getBusId());
        amqpMessageDispatcherService.targetAssignDistributionSet(targetAssignDistributionSetEvent);
        final Message sendMessage = getCaptureAddressEvent(targetAssignDistributionSetEvent);
        final DmfDownloadAndUpdateRequest downloadAndUpdateRequest = assertDownloadAndInstallMessage(sendMessage,
                action.getId());
        assertThat(createDistributionSet.getModules()).hasSameSizeAs(downloadAndUpdateRequest.getSoftwareModules());
        assertThat(downloadAndUpdateRequest.getTargetSecurityToken()).isEqualTo(TEST_TOKEN);
        for (final org.eclipse.hawkbit.dmf.json.model.DmfSoftwareModule softwareModule : downloadAndUpdateRequest
                .getSoftwareModules()) {
            assertThat(softwareModule.getArtifacts().isEmpty()).as("Artifact list for softwaremodule should be empty")
                    .isTrue();

            assertThat(softwareModule.getMetadata()).allSatisfy(metadata -> {
                assertThat(metadata.getKey()).isEqualTo(TestdataFactory.VISIBLE_SM_MD_KEY);
                assertThat(metadata.getValue()).isEqualTo(TestdataFactory.VISIBLE_SM_MD_VALUE);
            });
            for (final SoftwareModule softwareModule2 : action.getDistributionSet().getModules()) {
                if (!softwareModule.getModuleId().equals(softwareModule2.getId())) {
                    continue;
                }
                assertThat(softwareModule.getModuleType())
                        .as("Software module type in event should be the same as the softwaremodule in the distribution set")
                        .isEqualTo(softwareModule2.getType().getKey());
                assertThat(softwareModule.getModuleVersion())
                        .as("Software module version in event should be the same as the softwaremodule in the distribution set")
                        .isEqualTo(softwareModule2.getVersion());
            }
        }
    }

    @Test
    @Description("Verifies that download and install event with software modules and artifacts works")
    void testSendDownloadRequest() {
        DistributionSet dsA = testdataFactory.createDistributionSet(UUID.randomUUID().toString());
        SoftwareModule module = dsA.getModules().iterator().next();
        final List<AbstractDbArtifact> receivedList = new ArrayList<>();
        for (final Artifact artifact : testdataFactory.createArtifacts(module.getId())) {
            receivedList.add(new ArtifactFilesystem(new File("./test"), artifact.getSha1Hash(),
                    new DbArtifactHash(artifact.getSha1Hash(), null, null), artifact.getSize(), null));
        }
        module = softwareModuleManagement.get(module.getId()).get();
        dsA = distributionSetManagement.get(dsA.getId()).get();

        final Action action = createAction(dsA);

        Mockito.when(rabbitTemplate.convertSendAndReceive(any())).thenReturn(receivedList);

        final TargetAssignDistributionSetEvent targetAssignDistributionSetEvent = new TargetAssignDistributionSetEvent(
                action, serviceMatcher.getBusId());
        amqpMessageDispatcherService.targetAssignDistributionSet(targetAssignDistributionSetEvent);
        final Message sendMessage = getCaptureAddressEvent(targetAssignDistributionSetEvent);
        final DmfDownloadAndUpdateRequest downloadAndUpdateRequest = assertDownloadAndInstallMessage(sendMessage,
                action.getId());

        assertThat(downloadAndUpdateRequest.getSoftwareModules())
                .as("DownloadAndUpdateRequest event should contains 3 software modules")
                .hasSize(3);
        assertThat(downloadAndUpdateRequest.getTargetSecurityToken()).isEqualTo(TEST_TOKEN);

        for (final DmfSoftwareModule softwareModule : downloadAndUpdateRequest.getSoftwareModules()) {
            if (!softwareModule.getModuleId().equals(module.getId())) {
                continue;
            }
            assertThat(softwareModule.getArtifacts().size())
                    .isEqualTo(module.getArtifacts().size())
                    .isNotZero();

            module.getArtifacts().forEach(dbArtifact -> {
                final Optional<org.eclipse.hawkbit.dmf.json.model.DmfArtifact> found = softwareModule.getArtifacts()
                        .stream().filter(dmfartifact -> dmfartifact.getFilename().equals(dbArtifact.getFilename()))
                        .findAny();

                assertThat(found).as("The artifact should exist in message").isPresent();
                assertThat(found.get().getSize()).isEqualTo(dbArtifact.getSize());
                assertThat(found.get().getHashes().getMd5()).isEqualTo(dbArtifact.getMd5Hash());
                assertThat(found.get().getHashes().getSha1()).isEqualTo(dbArtifact.getSha1Hash());
                assertThat(found.get().getLastModified()).isGreaterThan(0L);
            });
        }
    }

    @Test
    @Description("Verifies that sending update controller attributes event works.")
    void sendUpdateAttributesRequest() {
        final String amqpUri = "amqp://anyhost";
        final TargetAttributesRequestedEvent targetAttributesRequestedEvent = new TargetAttributesRequestedEvent(TENANT,
                1L, CONTROLLER_ID, amqpUri, Target.class, serviceMatcher.getBusId());

        amqpMessageDispatcherService.targetTriggerUpdateAttributes(targetAttributesRequestedEvent);

        final Message sendMessage = createArgumentCapture(URI.create(amqpUri));
        assertUpdateAttributesMessage(sendMessage);
    }

    @Test
    @Description("Verifies that send cancel event works")
    void testSendCancelRequest() {
        final Action action = mock(Action.class);
        when(action.getId()).thenReturn(1L);
        when(action.getTenant()).thenReturn(TENANT);
        when(action.getTarget()).thenReturn(testTarget);
        final CancelTargetAssignmentEvent cancelTargetAssignmentDistributionSetEvent = new CancelTargetAssignmentEvent(
                action, serviceMatcher.getBusId());
        amqpMessageDispatcherService
                .targetCancelAssignmentToDistributionSet(cancelTargetAssignmentDistributionSetEvent);
        final Message sendMessage = createArgumentCapture(AMQP_URI);
        assertCancelMessage(sendMessage);

    }

    @Test
    @Description("Verifies that sending a delete message when receiving a delete event works.")
    void sendDeleteRequest() {

        // setup
        final String amqpUri = "amqp://anyhost";
        final TargetDeletedEvent targetDeletedEvent = new TargetDeletedEvent(TENANT, 1L, CONTROLLER_ID, amqpUri,
                Target.class, serviceMatcher.getBusId());

        // test
        amqpMessageDispatcherService.targetDelete(targetDeletedEvent);

        // verify
        final Message sendMessage = createArgumentCapture(URI.create(amqpUri));
        assertDeleteMessage(sendMessage);
    }

    @Test
    @Description("Verifies that a delete message is not send if the address is not an amqp address.")
    void sendDeleteRequestWithNoAmqpAddress() {

        // setup
        final String noAmqpUri = "http://anyhost";
        final TargetDeletedEvent targetDeletedEvent = new TargetDeletedEvent(TENANT, 1L, CONTROLLER_ID, noAmqpUri,
                Target.class, serviceMatcher.getBusId());

        // test
        amqpMessageDispatcherService.targetDelete(targetDeletedEvent);

        // verify
        Mockito.verifyNoInteractions(senderService);
    }

    @Test
    @Description("Verifies that a delete message is not send if the address is null.")
    void sendDeleteRequestWithNullAddress() {

        // setup
        final String noAmqpUri = null;
        final TargetDeletedEvent targetDeletedEvent = new TargetDeletedEvent(TENANT, 1L, CONTROLLER_ID, noAmqpUri,
                Target.class, serviceMatcher.getBusId());

        // test
        amqpMessageDispatcherService.targetDelete(targetDeletedEvent);

        // verify
        Mockito.verifyNoInteractions(senderService);
    }

    private Message getCaptureAddressEvent(final TargetAssignDistributionSetEvent targetAssignDistributionSetEvent) {
        final Target target = targetManagement
                .getByControllerID(targetAssignDistributionSetEvent.getActions().keySet().iterator().next()).get();
        return createArgumentCapture(target.getAddress());
    }

    private Action createAction(final DistributionSet testDs) {
        return getFirstAssignedAction(assignDistributionSet(testDs, testTarget));
    }

    private void assertCancelMessage(final Message sendMessage) {
        assertEventMessage(sendMessage);
        final DmfActionRequest actionId = convertMessage(sendMessage, DmfActionRequest.class);
        assertThat(actionId.getActionId())
                .as("Action ID should be 1")
                .isOne();
        assertThat(sendMessage.getMessageProperties().getHeaders())
                .as("The topc in the message should be a CANCEL_DOWNLOAD value")
                .containsEntry(MessageHeaderKey.TOPIC, EventTopic.CANCEL_DOWNLOAD);
    }

    private void assertDeleteMessage(final Message sendMessage) {
        assertThat(sendMessage).isNotNull();
        assertThat(sendMessage.getMessageProperties().getHeaders())
                .containsEntry(MessageHeaderKey.THING_ID, CONTROLLER_ID);
        assertThat(sendMessage.getMessageProperties().getHeaders())
                .containsEntry(MessageHeaderKey.TENANT, TENANT);
        assertThat(sendMessage.getMessageProperties().getHeaders())
                .containsEntry(MessageHeaderKey.TYPE, MessageType.THING_DELETED);
    }

    private DmfDownloadAndUpdateRequest assertDownloadAndInstallMessage(final Message sendMessage, final Long action) {
        assertEventMessage(sendMessage);
        final DmfDownloadAndUpdateRequest downloadAndUpdateRequest = convertMessage(sendMessage,
                DmfDownloadAndUpdateRequest.class);
        assertThat(downloadAndUpdateRequest.getActionId()).isEqualTo(action);
        assertThat(sendMessage.getMessageProperties().getHeaders())
                .as("The topic of the event should contain DOWNLOAD_AND_INSTALL")
                .containsEntry(MessageHeaderKey.TOPIC, EventTopic.DOWNLOAD_AND_INSTALL);
        assertThat(downloadAndUpdateRequest.getTargetSecurityToken())
                .as("Security token of target")
                .isEqualTo(TEST_TOKEN);

        return downloadAndUpdateRequest;
    }

    private void assertUpdateAttributesMessage(final Message sendMessage) {
        assertEventMessage(sendMessage);
        assertThat(sendMessage.getMessageProperties().getHeaders())
                .as("The topic of the event should contain REQUEST_ATTRIBUTES_UPDATE")
                .containsEntry(MessageHeaderKey.TOPIC, EventTopic.REQUEST_ATTRIBUTES_UPDATE);
    }

    private void assertEventMessage(final Message sendMessage) {
        assertThat(sendMessage).as("The message should not be null").isNotNull();

        assertThat(sendMessage.getMessageProperties().getHeaders())
                .as("The value of the message header THING_ID should be " + CONTROLLER_ID)
                .containsEntry(MessageHeaderKey.THING_ID, CONTROLLER_ID);
        assertThat(sendMessage.getMessageProperties().getHeaders())
                .as("The value of the message header TYPE should be EVENT")
                .containsEntry(MessageHeaderKey.TYPE, MessageType.EVENT);
        assertThat(sendMessage.getMessageProperties().getContentType())
                .as("The content type message should be " + MessageProperties.CONTENT_TYPE_JSON)
                .isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
    }

    @SuppressWarnings("unchecked")
    private <T> T convertMessage(final Message message, final Class<T> clazz) {
        message.getMessageProperties().getHeaders().put(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
                clazz.getTypeName());
        return (T) rabbitTemplate.getMessageConverter().fromMessage(message);
    }
}