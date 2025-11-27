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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import com.cronutils.utils.StringUtils;
import org.assertj.core.api.HamcrestCondition;
import org.eclipse.hawkbit.amqp.DmfApiConfiguration;
import org.eclipse.hawkbit.dmf.amqp.api.AmqpSettings;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.DmfActionRequest;
import org.eclipse.hawkbit.dmf.json.model.DmfActionStatus;
import org.eclipse.hawkbit.dmf.json.model.DmfActionUpdateStatus;
import org.eclipse.hawkbit.dmf.json.model.DmfAttributeUpdate;
import org.eclipse.hawkbit.dmf.json.model.DmfConfirmRequest;
import org.eclipse.hawkbit.dmf.json.model.DmfCreateThing;
import org.eclipse.hawkbit.dmf.json.model.DmfDownloadAndUpdateRequest;
import org.eclipse.hawkbit.dmf.json.model.DmfMetadata;
import org.eclipse.hawkbit.dmf.json.model.DmfSoftwareModule;
import org.eclipse.hawkbit.integration.listener.DeadletterListener;
import org.eclipse.hawkbit.integration.listener.ReplyToListener;
import org.eclipse.hawkbit.matcher.SoftwareModuleJsonMatcher;
import org.eclipse.hawkbit.rabbitmq.test.AbstractAmqpIntegrationTest;
import org.eclipse.hawkbit.rabbitmq.test.AmqpTestConfiguration;
import org.eclipse.hawkbit.repository.jpa.JpaRepositoryConfiguration;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.eclipse.hawkbit.utils.IpUtil;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.test.RabbitListenerTestHarness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.CollectionUtils;

/**
 * Common class for {@link AmqpMessageHandlerServiceIntegrationTest} and {@link AmqpMessageDispatcherServiceIntegrationTest}.
 */
@ContextConfiguration(classes = {
        DmfApiConfiguration.class, DmfTestConfiguration.class,
        JpaRepositoryConfiguration.class, AmqpTestConfiguration.class })
abstract class AbstractAmqpServiceIntegrationTest extends AbstractAmqpIntegrationTest {

    protected static final String TENANT_EXIST = "DEFAULT";
    protected static final String CREATED_BY = "CONTROLLER_PLUG_AND_PLAY";
    protected static final String CORRELATION_ID = UUID.randomUUID().toString();

    protected ReplyToListener replyToListener;
    private DeadletterListener deadletterListener;
    private DistributionSet distributionSet;

    @Autowired
    private RabbitListenerTestHarness harness;

    @BeforeEach
    void initListener() {
        deadletterListener = harness.getSpy(DeadletterListener.LISTENER_ID);
        assertThat(deadletterListener).isNotNull();
        Mockito.reset(deadletterListener);
        replyToListener = harness.getSpy(ReplyToListener.LISTENER_ID);
        assertThat(replyToListener).isNotNull();
        replyToListener.purge();
        Mockito.reset(replyToListener);
        getDmfClient().setExchange(AmqpSettings.DMF_EXCHANGE);
    }

    protected <T> T waitUntilIsPresent(final Callable<Optional<T>> callable) {
        await().until(() -> SecurityContextSwitch.asPrivileged(() -> callable.call().isPresent()));
        try {
            return SecurityContextSwitch.asPrivileged(() -> callable.call().get());
        } catch (final Exception e) {
            return null;
        }
    }

    protected void waitUntilEventMessagesAreDispatchedToTarget(final EventTopic... eventTopics) {
        await().untilAsserted(() ->
                assertThat(replyToListener.getLatestEventMessageTopics()).containsExactlyInAnyOrderElementsOf(Arrays.asList(eventTopics)));
        replyToListener.resetLatestEventMessageTopics();
    }

    protected DeadletterListener getDeadletterListener() {
        return deadletterListener;
    }

    protected DistributionSet getDistributionSet() {
        return distributionSet;
    }

    protected DistributionSetAssignmentResult registerTargetAndAssignDistributionSet(final String controllerId) {
        distributionSet = testdataFactory.createDistributionSet(UUID.randomUUID().toString());
        testdataFactory.addSoftwareModuleMetadata(distributionSet);

        return registerTargetAndAssignDistributionSet(distributionSet.getId(), TargetUpdateStatus.REGISTERED,
                distributionSet.getModules(), controllerId);
    }

    protected DistributionSetAssignmentResult prepareDistributionSetAndAssign(final String controllerId) {
        distributionSet = testdataFactory.createDistributionSet(UUID.randomUUID().toString());
        testdataFactory.addSoftwareModuleMetadata(distributionSet);
        return assignDistributionSet(distributionSet.getId(), controllerId);
    }

    protected DistributionSetAssignmentResult registerTargetAndAssignDistributionSet(final Long assignDs,
            final TargetUpdateStatus expectedStatus, final Set<SoftwareModule> expectedSoftwareModulesInMessage,
            final String controllerId) {
        registerAndAssertTargetWithExistingTenant(controllerId, 1, expectedStatus, CREATED_BY);

        final DistributionSetAssignmentResult assignmentResult = assignDistributionSet(assignDs, controllerId);
        if (isConfirmationFlowEnabled()) {
            assertConfirmMessage(expectedSoftwareModulesInMessage, controllerId);
        } else {
            assertDownloadAndInstallMessage(expectedSoftwareModulesInMessage, controllerId);
        }
        return assignmentResult;
    }

    protected void assertCancelActionMessage(final Long actionId, final String controllerId) {
        final Message replyMessage = assertReplyMessageHeader(EventTopic.CANCEL_DOWNLOAD, controllerId);

        final DmfActionRequest actionUpdateStatus = (DmfActionRequest) getDmfClient().getMessageConverter()
                .fromMessage(replyMessage);
        assertThat(actionUpdateStatus.getActionId()).isEqualTo(actionId);
    }

    protected void assertDeleteMessage(final String target) {

        verifyReplyToListener();
        final Message replyMessage = replyToListener.getDeleteMessages().get(target);
        assertAllTargetsCount(0);
        final Map<String, Object> headers = replyMessage.getMessageProperties().getHeaders();
        assertThat(headers).containsEntry(MessageHeaderKey.THING_ID, target)
                .containsEntry(MessageHeaderKey.TENANT, TENANT_EXIST)
                .containsEntry(MessageHeaderKey.TYPE, MessageType.THING_DELETED.toString());
    }

    protected void assertRequestAttributesUpdateMessage(final String target) {
        assertReplyMessageHeader(EventTopic.REQUEST_ATTRIBUTES_UPDATE, target);
    }

    protected void assertRequestAttributesUpdateMessageAbsent() {
        assertThat(replyToListener.getEventMessages()).doesNotContainKey(EventTopic.REQUEST_ATTRIBUTES_UPDATE);
    }

    protected void assertPingReplyMessage(final String correlationId) {

        verifyReplyToListener();
        final Message replyMessage = replyToListener.getPingResponseMessages().get(correlationId);

        final Map<String, Object> headers = replyMessage.getMessageProperties().getHeaders();

        assertThat(headers).containsEntry(MessageHeaderKey.TENANT, TENANT_EXIST);
        assertThat(correlationId).isEqualTo(replyMessage.getMessageProperties().getCorrelationId());
        assertThat(headers).containsEntry(MessageHeaderKey.TYPE, MessageType.PING_RESPONSE.toString());
        assertThat(Long.valueOf(new String(replyMessage.getBody(), StandardCharsets.UTF_8)))
                .isLessThanOrEqualTo(System.currentTimeMillis());

    }

    protected void assertDmfDownloadAndUpdateRequest(
            final DmfDownloadAndUpdateRequest request, final Set<SoftwareModule> softwareModules, final String controllerId) {
        assertSoftwareModules(softwareModules, request.getSoftwareModules());
        final Target updatedTarget = waitUntilIsPresent(() -> targetManagement.findByControllerId(controllerId));
        assertThat(updatedTarget).isNotNull();
        assertThat(updatedTarget.getSecurityToken()).isEqualTo(request.getTargetSecurityToken());
    }

    protected void assertDownloadAndInstallMessage(final Set<SoftwareModule> softwareModules,
            final String controllerId) {
        assertAssignmentMessage(softwareModules, controllerId, EventTopic.DOWNLOAD_AND_INSTALL);
    }

    protected void assertDownloadMessage(final Set<SoftwareModule> dsModules, final String controllerId) {
        assertAssignmentMessage(dsModules, controllerId, EventTopic.DOWNLOAD);
    }

    protected void createAndSendThingCreated(final String controllerId) {
        createAndSendThingCreated(controllerId, null, null);
    }

    protected void createAndSendThingCreated(final String controllerId, final String name,
            final Map<String, String> attributes) {
        final Message message = createTargetMessage(controllerId, name, attributes,
                AbstractAmqpServiceIntegrationTest.TENANT_EXIST);
        getDmfClient().send(message);
    }

    protected void verifyReplyToListener() {
        await().untilAsserted(() -> Mockito.verify(replyToListener, Mockito.atLeast(1)).handleMessage(Mockito.any()));
    }

    protected Long cancelAction(final Long actionId, final String controllerId) {
        deploymentManagement.cancelAction(actionId);
        assertCancelActionMessage(actionId, controllerId);
        return actionId;
    }

    protected Long registerTargetAndCancelActionId(final String controllerId) {
        final DistributionSetAssignmentResult assignmentResult = registerTargetAndAssignDistributionSet(controllerId);
        return cancelAction(getFirstAssignedActionId(assignmentResult), controllerId);
    }

    protected void assertAllTargetsCount(final long expectedTargetsCount) {
        assertThat(targetManagement.count()).isEqualTo(expectedTargetsCount);
    }

    protected Message assertReplyMessageHeader(final EventTopic eventTopic, final String controllerId) {
        verifyReplyToListener();

        final Message replyMessage = replyToListener.getLatestEventMessage(eventTopic);
        assertAllTargetsCount(1);
        final Map<String, Object> headers = replyMessage.getMessageProperties().getHeaders();

        assertThat(headers).containsEntry(MessageHeaderKey.TOPIC, eventTopic.toString())
                .containsEntry(MessageHeaderKey.THING_ID, controllerId)
                .containsEntry(MessageHeaderKey.TENANT, TENANT_EXIST)
                .containsEntry(MessageHeaderKey.TYPE, MessageType.EVENT.toString());
        return replyMessage;
    }

    protected void registerAndAssertTargetWithExistingTenant(final String controllerId) {
        registerAndAssertTargetWithExistingTenant(controllerId, 1);
    }

    protected void registerAndAssertTargetWithExistingTenant(final String controllerId,
            final int existingTargetsAfterCreation) {
        registerAndAssertTargetWithExistingTenant(controllerId, existingTargetsAfterCreation,
                TargetUpdateStatus.REGISTERED, CREATED_BY);
    }

    protected void registerAndAssertTargetWithExistingTenant(final String controllerId,
            final int existingTargetsAfterCreation, final TargetUpdateStatus expectedTargetStatus,
            final String createdBy) {
        registerAndAssertTargetWithExistingTenant(controllerId, null, existingTargetsAfterCreation,
                expectedTargetStatus, createdBy, null);
    }

    protected void registerAndAssertTargetWithExistingTenant(final String controllerId, final String name,
            final int existingTargetsAfterCreation, final TargetUpdateStatus expectedTargetStatus,
            final String createdBy, final Map<String, String> attributes) {
        registerAndAssertTargetWithExistingTenant(
                controllerId, name, existingTargetsAfterCreation, expectedTargetStatus, createdBy, attributes,
                () -> targetManagement.findByControllerId(controllerId));
    }

    protected void registerSameTargetAndAssertBasedOnVersion(final String controllerId,
            final int existingTargetsAfterCreation, final TargetUpdateStatus expectedTargetStatus) {
        registerSameTargetAndAssertBasedOnVersion(controllerId, null, existingTargetsAfterCreation,
                expectedTargetStatus, null);
    }

    protected void registerSameTargetAndAssertBasedOnVersion(final String controllerId, final String name,
            final int existingTargetsAfterCreation, final TargetUpdateStatus expectedTargetStatus,
            final Map<String, String> attributes) {
        final int version = controllerManagement.findByControllerId(controllerId).get().getOptLockRevision();
        registerAndAssertTargetWithExistingTenant(controllerId, name, existingTargetsAfterCreation,
                expectedTargetStatus, CREATED_BY, attributes, () -> findTargetBasedOnNewVersion(controllerId, version));
    }

    protected Message createTargetMessage(final String controllerId, final String tenant) {
        return createTargetMessage(controllerId, null, null, tenant);
    }

    protected Message createTargetMessage(final String controllerId, final String name,
            final Map<String, String> attributes, final String tenant) {
        final MessageProperties messageProperties = createMessagePropertiesWithTenant(tenant);
        messageProperties.getHeaders().put(MessageHeaderKey.THING_ID, controllerId);
        messageProperties.getHeaders().put(MessageHeaderKey.TYPE, MessageType.THING_CREATED.toString());
        messageProperties.setReplyTo(DmfTestConfiguration.REPLY_TO_EXCHANGE);

        DmfCreateThing payload = null;
        if (!StringUtils.isEmpty(name) || !CollectionUtils.isEmpty(attributes)) {
            payload = new DmfCreateThing(
                    name, null,
                    CollectionUtils.isEmpty(attributes) ? null : new DmfAttributeUpdate(attributes, null));
        }

        return createMessage(payload, messageProperties);
    }

    protected Message createPingMessage(final String correlationId, final String tenant) {
        final MessageProperties messageProperties = createMessagePropertiesWithTenant(tenant);
        messageProperties.getHeaders().put(MessageHeaderKey.TYPE, MessageType.PING.toString());
        messageProperties.setCorrelationId(correlationId);
        messageProperties.setReplyTo(DmfTestConfiguration.REPLY_TO_EXCHANGE);

        return createMessage(null, messageProperties);
    }

    protected void createAndSendActionStatusUpdateMessage(final String target, final long actionId,
            final DmfActionStatus status) {
        final DmfActionUpdateStatus dmfActionUpdateStatus = new DmfActionUpdateStatus(
                actionId, status, System.currentTimeMillis(), null, null, null);

        final Message eventMessage = createUpdateActionEventMessage(dmfActionUpdateStatus);
        eventMessage.getMessageProperties().getHeaders().put(MessageHeaderKey.THING_ID, target);

        getDmfClient().send(eventMessage);
    }

    protected Message createUpdateActionEventMessage(final Object payload) {
        final MessageProperties messageProperties = createMessagePropertiesWithTenant(
                AbstractAmqpServiceIntegrationTest.TENANT_EXIST);
        messageProperties.getHeaders().put(MessageHeaderKey.TYPE, MessageType.EVENT.toString());
        messageProperties.getHeaders().put(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ACTION_STATUS.toString());
        messageProperties.setCorrelationId(CORRELATION_ID);

        return createMessage(payload, messageProperties);
    }

    protected MessageProperties createMessagePropertiesWithTenant(final String tenant) {
        final MessageProperties messageProperties = new MessageProperties();
        messageProperties.getHeaders().put(MessageHeaderKey.TENANT, tenant);
        return messageProperties;
    }

    protected Message createUpdateAttributesMessage(final String target, final String tenant,
            final DmfAttributeUpdate attributeUpdate) {
        final MessageProperties messageProperties = createMessagePropertiesWithTenant(tenant);
        messageProperties.getHeaders().put(MessageHeaderKey.THING_ID, target);
        messageProperties.getHeaders().put(MessageHeaderKey.TYPE, MessageType.EVENT.toString());
        messageProperties.getHeaders().put(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ATTRIBUTES.toString());

        return createMessage(attributeUpdate, messageProperties);

    }

    protected Message createUpdateAttributesMessageWrongBody(final String target) {
        final MessageProperties messageProperties = createMessagePropertiesWithTenant(
                AbstractAmqpServiceIntegrationTest.TENANT_EXIST);
        messageProperties.getHeaders().put(MessageHeaderKey.THING_ID, target);
        messageProperties.getHeaders().put(MessageHeaderKey.TYPE, MessageType.EVENT.toString());
        messageProperties.getHeaders().put(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ATTRIBUTES.toString());

        return createMessage("wrongbody", messageProperties);

    }

    protected void assertUpdateAttributes(final String controllerId, final Map<String, String> attributes) {
        waitUntilIsPresent(() -> controllerManagement.findByControllerId(controllerId));
        await().untilAsserted(() -> {
            try {
                final Map<String, String> controllerAttributes = SecurityContextSwitch.asPrivileged(
                        () -> targetManagement.getControllerAttributes(controllerId));
                assertThat(controllerAttributes).hasSameSizeAs(attributes);
                assertThat(controllerAttributes).containsAllEntriesOf(attributes);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    protected String getExchange() {
        return AmqpSettings.DMF_EXCHANGE;
    }

    protected DistributionSet createTargetAndDistributionSetAndAssign(final String controllerId, final Action.ActionType actionType) {
        registerAndAssertTargetWithExistingTenant(controllerId);

        final DistributionSet distributionSetLocal = testdataFactory.createDistributionSet(UUID.randomUUID().toString());
        testdataFactory.addSoftwareModuleMetadata(distributionSetLocal);

        assignDistributionSet(distributionSetLocal.getId(), controllerId, actionType);
        return distributionSetLocal;
    }

    protected void assertSoftwareModules(final Set<SoftwareModule> expectedSoftwareModules,
            final List<DmfSoftwareModule> softwareModules) {
        assertThat(expectedSoftwareModules)
                .is(new HamcrestCondition<>(SoftwareModuleJsonMatcher.containsExactly(softwareModules)));
        softwareModules.forEach(dmfModule -> assertThat(dmfModule.getMetadata()).containsExactly(
                new DmfMetadata(TestdataFactory.VISIBLE_SM_MD_KEY, TestdataFactory.VISIBLE_SM_MD_VALUE)));
    }

    protected void assertConfirmMessage(final Set<SoftwareModule> dsModules, final String controllerId) {

        final Message replyMessage = assertReplyMessageHeader(EventTopic.CONFIRM, controllerId);
        assertAllTargetsCount(1);

        final DmfConfirmRequest confirmRequest = (DmfConfirmRequest) getDmfClient()
                .getMessageConverter().fromMessage(replyMessage);

        assertConfirmRequest(confirmRequest, dsModules, controllerId);
    }

    protected void assertConfirmRequest(final DmfConfirmRequest request, final Set<SoftwareModule> softwareModules, final String controllerId) {
        assertSoftwareModules(softwareModules, request.getSoftwareModules());
        final Target updatedTarget = waitUntilIsPresent(() -> targetManagement.findByControllerId(controllerId));
        assertThat(updatedTarget).isNotNull();
        assertThat(updatedTarget.getSecurityToken()).isEqualTo(request.getTargetSecurityToken());
    }

    private void assertAssignmentMessage(final Set<SoftwareModule> dsModules, final String controllerId, final EventTopic topic) {
        final Message replyMessage = assertReplyMessageHeader(topic, controllerId);
        assertAllTargetsCount(1);

        final DmfDownloadAndUpdateRequest downloadAndUpdateRequest = (DmfDownloadAndUpdateRequest) getDmfClient()
                .getMessageConverter().fromMessage(replyMessage);

        assertDmfDownloadAndUpdateRequest(downloadAndUpdateRequest, dsModules, controllerId);
    }

    private void registerAndAssertTargetWithExistingTenant(
            final String controllerId, final String name,
            final int existingTargetsAfterCreation, final TargetUpdateStatus expectedTargetStatus,
            final String createdBy, final Map<String, String> attributes,
            final Callable<Optional<Target>> fetchTarget) {
        createAndSendThingCreated(controllerId, name, attributes);
        final Target registeredTarget = waitUntilIsPresent(fetchTarget::call);
        assertAllTargetsCount(existingTargetsAfterCreation);
        assertThat(registeredTarget).isNotNull();
        assertTarget(
                registeredTarget, name != null ? name : controllerId, expectedTargetStatus,
                createdBy, attributes != null ? attributes : Collections.emptyMap());
    }

    private Optional<Target> findTargetBasedOnNewVersion(final String controllerId, final int version) {
        final Optional<Target> target2 = controllerManagement.findByControllerId(controllerId);
        if (version < target2.get().getOptLockRevision()) {
            return target2;
        }
        return Optional.empty();
    }

    private void assertTarget(
            final Target target, final String name, final TargetUpdateStatus updateStatus,
            final String createdBy, final Map<String, String> attributes) {
        assertThat(target.getTenant()).isEqualTo(TENANT_EXIST);
        assertThat(target.getName()).isEqualTo(name);
        assertThat(target.getDescription()).contains("Plug and Play");
        assertThat(target.getDescription()).contains(target.getControllerId());
        assertThat(target.getCreatedBy()).isEqualTo(createdBy);
        assertThat(target.getUpdateStatus()).isEqualTo(updateStatus);
        assertThat(target.getAddress()).isEqualTo(IpUtil.createAmqpUri(getVirtualHost(), DmfTestConfiguration.REPLY_TO_EXCHANGE).toString());
        assertThat(targetManagement.getControllerAttributes(target.getControllerId())).isEqualTo(attributes);
    }
}