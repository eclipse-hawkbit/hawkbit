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

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.eclipse.hawkbit.amqp.DmfApiConfiguration;
import org.eclipse.hawkbit.dmf.amqp.api.AmqpSettings;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.DmfAttributeUpdate;
import org.eclipse.hawkbit.dmf.json.model.DmfDownloadAndUpdateRequest;
import org.eclipse.hawkbit.dmf.json.model.DmfMetadata;
import org.eclipse.hawkbit.integration.listener.DeadletterListener;
import org.eclipse.hawkbit.integration.listener.ReplyToListener;
import org.eclipse.hawkbit.matcher.SoftwareModuleJsonMatcher;
import org.eclipse.hawkbit.rabbitmq.test.AbstractAmqpIntegrationTest;
import org.eclipse.hawkbit.rabbitmq.test.AmqpTestConfiguration;
import org.eclipse.hawkbit.repository.jpa.RepositoryApplicationConfiguration;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.eclipse.hawkbit.util.IpUtil;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.test.RabbitListenerTestHarness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;

/**
 *
 * Common class for {@link AmqpMessageHandlerServiceIntegrationTest} and
 * {@link AmqpMessageDispatcherServiceIntegrationTest}.
 */
@SpringApplicationConfiguration(classes = { RepositoryApplicationConfiguration.class, AmqpTestConfiguration.class,
        DmfApiConfiguration.class, DmfTestConfiguration.class })
public abstract class AmqpServiceIntegrationTest extends AbstractAmqpIntegrationTest {

    protected static final String TENANT_EXIST = "DEFAULT";
    protected static final String CREATED_BY = "CONTROLLER_PLUG_AND_PLAY";

    private DeadletterListener deadletterListener;
    private ReplyToListener replyToListener;
    private DistributionSet distributionSet;

    @Autowired
    private RabbitListenerTestHarness harness;

    @Autowired
    private ConnectionFactory connectionFactory;

    @Before
    public void initListener() {
        deadletterListener = harness.getSpy(DeadletterListener.LISTENER_ID);
        assertThat(deadletterListener).isNotNull();
        Mockito.reset(deadletterListener);
        replyToListener = harness.getSpy(ReplyToListener.LISTENER_ID);
        assertThat(replyToListener).isNotNull();
        Mockito.reset(replyToListener);
        getDmfClient().setExchange(AmqpSettings.DMF_EXCHANGE);
    }

    protected <T> T waitUntilIsPresent(final Callable<Optional<T>> callable) {
        createConditionFactory().until(() -> {
            return securityRule.runAsPrivileged(() -> callable.call().isPresent());
        });

        try {
            return securityRule.runAsPrivileged(() -> callable.call().get());
        } catch (final Exception e) {
            return null;
        }
    }

    protected void verifyDeadLetterMessages(final int expectedMessages) {
        createConditionFactory().until(() -> {
            Mockito.verify(getDeadletterListener(), Mockito.times(expectedMessages)).handleMessage(Mockito.any());
        });
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

    protected DistributionSetAssignmentResult registerTargetAndAssignDistributionSet(final Long assignDs,
            final TargetUpdateStatus expectedStatus, final Set<SoftwareModule> expectedSoftwareModulesInMessage,
            final String controllerId) {
        registerAndAssertTargetWithExistingTenant(controllerId, 1, expectedStatus, CREATED_BY);

        final DistributionSetAssignmentResult assignmentResult = assignDistributionSet(assignDs, controllerId);
        assertDownloadAndInstallMessage(expectedSoftwareModulesInMessage, controllerId);
        return assignmentResult;
    }

    protected void assertCancelActionMessage(final Long actionId, final String controllerId) {
        final Message replyMessage = assertReplyMessageHeader(EventTopic.CANCEL_DOWNLOAD, controllerId);

        final Long actionUpdateStatus = (Long) getDmfClient().getMessageConverter().fromMessage(replyMessage);
        assertThat(actionUpdateStatus).isEqualTo(actionId);
    }

    protected void assertDeleteMessage(final String target) {

        verifyReplyToListener();
        final Message replyMessage = replyToListener.getDeleteMessages().get(target);
        assertAllTargetsCount(0);
        final Map<String, Object> headers = replyMessage.getMessageProperties().getHeaders();
        assertThat(headers.get(MessageHeaderKey.THING_ID)).isEqualTo(target);
        assertThat(headers.get(MessageHeaderKey.TENANT)).isEqualTo(TENANT_EXIST);
        assertThat(headers.get(MessageHeaderKey.TYPE)).isEqualTo(MessageType.THING_DELETED.toString());
    }

    protected void assertPingReplyMessage(final String correlationId) {

        verifyReplyToListener();
        final Message replyMessage = replyToListener.getPingResponseMessages().get(correlationId);

        final Map<String, Object> headers = replyMessage.getMessageProperties().getHeaders();

        assertThat(headers.get(MessageHeaderKey.TENANT)).isEqualTo(TENANT_EXIST);
        assertThat(correlationId)
                .isEqualTo(new String(replyMessage.getMessageProperties().getCorrelationId(), StandardCharsets.UTF_8));
        assertThat(headers.get(MessageHeaderKey.TYPE)).isEqualTo(MessageType.PING_RESPONSE.toString());
        assertThat(Long.valueOf(new String(replyMessage.getBody(), StandardCharsets.UTF_8)))
                .isLessThanOrEqualTo(System.currentTimeMillis());

    }

    protected void assertDownloadAndInstallMessage(final Set<SoftwareModule> dsModules, final String controllerId) {
        final Message replyMessage = assertReplyMessageHeader(EventTopic.DOWNLOAD_AND_INSTALL, controllerId);
        assertAllTargetsCount(1);

        final DmfDownloadAndUpdateRequest downloadAndUpdateRequest = (DmfDownloadAndUpdateRequest) getDmfClient()
                .getMessageConverter().fromMessage(replyMessage);

        Assert.assertThat(dsModules,
                SoftwareModuleJsonMatcher.containsExactly(downloadAndUpdateRequest.getSoftwareModules()));

        downloadAndUpdateRequest.getSoftwareModules()
                .forEach(dmfModule -> assertThat(dmfModule.getMetadata()).containsExactly(
                        new DmfMetadata(TestdataFactory.VISIBLE_SM_MD_KEY, TestdataFactory.VISIBLE_SM_MD_VALUE)));

        final Target updatedTarget = waitUntilIsPresent(() -> targetManagement.getByControllerID(controllerId));

        assertThat(updatedTarget.getSecurityToken()).isEqualTo(downloadAndUpdateRequest.getTargetSecurityToken());
    }

    protected void createAndSendTarget(final String target, final String tenant) {
        final Message message = createTargetMessage(target, tenant);
        getDmfClient().send(message);
    }

    protected Message createAndSendPingMessage(final String correlationId, final String tenant) {
        final Message message = createPingMessage(correlationId, tenant);
        getDmfClient().send(message);
        return message;
    }

    protected void verifyReplyToListener() {
        createConditionFactory().until(() -> {
            Mockito.verify(replyToListener, Mockito.atLeast(1)).handleMessage(Mockito.any());
        });
    }

    protected Long cancelAction(final Long actionId, final String controllerId) {
        deploymentManagement.cancelAction(actionId);
        assertCancelActionMessage(actionId, controllerId);
        return actionId;
    }

    protected Long registerTargetAndCancelActionId(final String controllerId) {
        final DistributionSetAssignmentResult assignmentResult = registerTargetAndAssignDistributionSet(controllerId);
        return cancelAction(assignmentResult.getActions().get(0), controllerId);
    }

    protected void assertAllTargetsCount(final long expectedTargetsCount) {
        assertThat(targetManagement.count()).isEqualTo(expectedTargetsCount);
    }

    private Message assertReplyMessageHeader(final EventTopic eventTopic, final String controllerId) {
        verifyReplyToListener();
        final Message replyMessage = replyToListener.getEventTopicMessages().get(eventTopic);
        assertAllTargetsCount(1);
        final Map<String, Object> headers = replyMessage.getMessageProperties().getHeaders();
        assertThat(headers.get(MessageHeaderKey.TOPIC)).isEqualTo(eventTopic.toString());
        assertThat(headers.get(MessageHeaderKey.THING_ID)).isEqualTo(controllerId);
        assertThat(headers.get(MessageHeaderKey.TENANT)).isEqualTo(TENANT_EXIST);
        assertThat(headers.get(MessageHeaderKey.TYPE)).isEqualTo(MessageType.EVENT.toString());
        return replyMessage;
    }

    protected void registerAndAssertTargetWithExistingTenant(final String target,
            final int existingTargetsAfterCreation) {

        registerAndAssertTargetWithExistingTenant(target, existingTargetsAfterCreation, TargetUpdateStatus.REGISTERED,
                CREATED_BY);

    }

    protected void registerAndAssertTargetWithExistingTenant(final String target,
            final int existingTargetsAfterCreation, final TargetUpdateStatus expectedTargetStatus,
            final String createdBy) {
        createAndSendTarget(target, TENANT_EXIST);
        final Target registerdTarget = waitUntilIsPresent(() -> targetManagement.getByControllerID(target));
        assertAllTargetsCount(existingTargetsAfterCreation);
        assertTarget(registerdTarget, expectedTargetStatus, createdBy);
    }

    protected void registerSameTargetAndAssertBasedOnVersion(final String controllerId,
            final int existingTargetsAfterCreation, final TargetUpdateStatus expectedTargetStatus) {
        final int version = controllerManagement.getByControllerId(controllerId).get().getOptLockRevision();
        createAndSendTarget(controllerId, TENANT_EXIST);
        final Target registeredTarget = waitUntilIsPresent(() -> findTargetBasedOnNewVersion(controllerId, version));
        assertAllTargetsCount(existingTargetsAfterCreation);
        assertThat(registeredTarget.getUpdateStatus()).isEqualTo(expectedTargetStatus);
    }

    private Optional<Target> findTargetBasedOnNewVersion(final String controllerId, final int version) {
        final Optional<Target> target2 = controllerManagement.getByControllerId(controllerId);
        if (version < target2.get().getOptLockRevision()) {
            return target2;
        }
        return Optional.empty();
    }

    private void assertTarget(final Target target, final TargetUpdateStatus updateStatus, final String createdBy) {
        assertThat(target.getTenant()).isEqualTo(TENANT_EXIST);
        assertThat(target.getDescription()).contains("Plug and Play");
        assertThat(target.getDescription()).contains(target.getControllerId());
        assertThat(target.getCreatedBy()).isEqualTo(createdBy);
        assertThat(target.getUpdateStatus()).isEqualTo(updateStatus);
        assertThat(target.getAddress()).isEqualTo(
                IpUtil.createAmqpUri(connectionFactory.getVirtualHost(), DmfTestConfiguration.REPLY_TO_EXCHANGE));
    }

    protected Message createTargetMessage(final String target, final String tenant) {
        final MessageProperties messageProperties = createMessagePropertiesWithTenant(tenant);
        messageProperties.getHeaders().put(MessageHeaderKey.THING_ID, target);
        messageProperties.getHeaders().put(MessageHeaderKey.TYPE, MessageType.THING_CREATED.toString());
        messageProperties.setReplyTo(DmfTestConfiguration.REPLY_TO_EXCHANGE);

        return createMessage(null, messageProperties);
    }

    protected Message createPingMessage(final String correlationId, final String tenant) {
        final MessageProperties messageProperties = createMessagePropertiesWithTenant(tenant);
        messageProperties.getHeaders().put(MessageHeaderKey.TYPE, MessageType.PING.toString());
        messageProperties.setCorrelationId(correlationId.getBytes());
        messageProperties.setReplyTo(DmfTestConfiguration.REPLY_TO_EXCHANGE);

        return createMessage(null, messageProperties);
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

    protected Message createUpdateAttributesMessageWrongBody(final String target, final String tenant) {
        final MessageProperties messageProperties = createMessagePropertiesWithTenant(tenant);
        messageProperties.getHeaders().put(MessageHeaderKey.THING_ID, target);
        messageProperties.getHeaders().put(MessageHeaderKey.TYPE, MessageType.EVENT.toString());
        messageProperties.getHeaders().put(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ATTRIBUTES.toString());

        return createMessage("wrongbody", messageProperties);

    }

    protected void assertUpdateAttributes(final String controllerId, final Map<String, String> attributes) {
        final Target findByControllerId = waitUntilIsPresent(
                () -> controllerManagement.getByControllerId(controllerId));
        final Map<String, String> controllerAttributes = targetManagement
                .getControllerAttributes(findByControllerId.getControllerId());
        assertThat(controllerAttributes.size()).isEqualTo(attributes.size());
        attributes.forEach((k, v) -> assertKeyValueInMap(k, v, controllerAttributes));
    }

    private void assertKeyValueInMap(final String key, final String value,
            final Map<String, String> controllerAttributes) {
        assertThat(controllerAttributes.containsKey(key)).isTrue();
        assertThat(controllerAttributes.get(key)).isEqualTo(value);
    }

    @Override
    protected String getExchange() {
        return AmqpSettings.DMF_EXCHANGE;
    }

}
