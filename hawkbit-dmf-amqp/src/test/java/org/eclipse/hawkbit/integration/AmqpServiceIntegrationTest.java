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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.eclipse.hawkbit.AmqpTestConfiguration;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.DownloadAndUpdateRequest;
import org.eclipse.hawkbit.integration.listener.ReplyToListener;
import org.eclipse.hawkbit.matcher.SoftwareMouleJsonMatcher;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.util.IpUtil;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

/**
 * 
 * Common class for {@link AmqpMessageHandlerServiceIntegrationTest} and
 * {@link AmqpMessageDispatcherServiceIntegrationTest}.
 */
public class AmqpServiceIntegrationTest extends AbstractAmqpIntegrationTest {

    protected static final String TENANT_EXIST = "DEFAULT";
    protected static final String REGISTER_TARGET = "NewDmfTarget";

    private ReplyToListener replyToListener;

    @Before
    public void initListener() {
        replyToListener = getHarness().getSpy(ReplyToListener.LISTENER_ID);
        assertThat(replyToListener).isNotNull();
        Mockito.reset(replyToListener);
    }

    protected <T> T waitUntilIsPresent(final Callable<Optional<T>> callable) {
        createConditionFactory().until(() -> {
            return securityRule.runAsPrivileged(() -> callable.call().isPresent());
        });

        try {
            return callable.call().get();
        } catch (final Exception e) {
            return null;
        }
    }

    protected DistributionSet registerTargetAndAssignDistributionSet() {
        registerAndAssertTargetWithExistingTenant(REGISTER_TARGET, 1);

        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        assignDistributionSet(distributionSet.getId(), REGISTER_TARGET);
        assertDownloadAndInstallMessage(distributionSet);
        Mockito.reset(replyToListener);
        return distributionSet;
    }

    protected void assertCancelActionMessage(Long actionId) {
        final Message replyMessage = assertReplyMessageHeader(EventTopic.CANCEL_DOWNLOAD);

        final Long actionUpdateStatus = (Long) getDmfClient().getMessageConverter().fromMessage(replyMessage);
        assertThat(actionUpdateStatus).isEqualTo(actionId);

        Mockito.reset(replyToListener);
    }

    protected void assertDownloadAndInstallMessage(final DistributionSet distributionSet) {
        final Message replyMessage = assertReplyMessageHeader(EventTopic.DOWNLOAD_AND_INSTALL);
        assertAllTargetsCount(1);

        final DownloadAndUpdateRequest downloadAndUpdateRequest = (DownloadAndUpdateRequest) getDmfClient()
                .getMessageConverter().fromMessage(replyMessage);
        assertThat(downloadAndUpdateRequest).isNotNull();

        Assert.assertThat(distributionSet.getModules(),
                SoftwareMouleJsonMatcher.containsExactly(downloadAndUpdateRequest.getSoftwareModules()));

        final Target updatedTarget = waitUntilIsPresent(
                () -> targetManagement.findTargetByControllerID(REGISTER_TARGET));

        assertThat(updatedTarget.getSecurityToken()).isEqualTo(downloadAndUpdateRequest.getTargetSecurityToken());
    }

    protected void createAndSendTarget(String target, String tenant) {
        final Message message = createTargetMessage(target, tenant);
        getDmfClient().send(message);
    }

    protected Message verifyReplyToListener(int expectedMessages) {
        return verifyTestRabbitListener(replyToListener, expectedMessages);
    }

    protected Long cancelReplyAction() {
        final DownloadAndUpdateRequest downloadAndUpdateRequest = (DownloadAndUpdateRequest) getDmfClient()
                .getMessageConverter().fromMessage(replyToListener.getMessage());
        final Long actionId = downloadAndUpdateRequest.getActionId();
        deploymentManagement.cancelAction(actionId);
        return actionId;
    }

    protected ReplyToListener getReplyToListener() {
        return replyToListener;
    }

    protected void assertAllTargetsCount(long expectedTargetsCount) {
        assertThat(targetManagement.countTargetsAll()).isEqualTo(expectedTargetsCount);
    }

    private Message assertReplyMessageHeader(EventTopic eventTopic) {
        final Message replyMessage = verifyReplyToListener(1);
        assertAllTargetsCount(1);
        final Map<String, Object> headers = replyMessage.getMessageProperties().getHeaders();
        assertThat(headers.get(MessageHeaderKey.TOPIC)).isEqualTo(eventTopic.toString());
        assertThat(headers.get(MessageHeaderKey.THING_ID)).isEqualTo(REGISTER_TARGET);
        assertThat(headers.get(MessageHeaderKey.TENANT)).isEqualTo(TENANT_EXIST);
        assertThat(headers.get(MessageHeaderKey.TYPE)).isEqualTo(MessageType.EVENT.toString());
        return replyMessage;
    }

    protected void registerAndAssertTargetWithExistingTenant(String target, int existingTargetsAfterCreation) {
        createAndSendTarget(target, TENANT_EXIST);
        final Target registerdTarget = waitUntilIsPresent(() -> targetManagement.findTargetByControllerID(target));
        assertAllTargetsCount(existingTargetsAfterCreation);
        assertTarget(registerdTarget, TargetUpdateStatus.REGISTERED);
    }

    private void assertTarget(Target target, TargetUpdateStatus updateStatus) {
        assertThat(target.getTenant()).isEqualTo(TENANT_EXIST);
        assertThat(target.getDescription()).contains("Plug and Play");
        assertThat(target.getDescription()).contains(target.getControllerId());
        assertThat(target.getCreatedBy()).isEqualTo("AMQP-Controller");
        assertThat(target.getTargetInfo().getUpdateStatus()).isEqualTo(updateStatus);
        assertThat(target.getTargetInfo().getAddress())
                .isEqualTo(IpUtil.createAmqpUri("/", AmqpTestConfiguration.REPLY_TO_EXCHANGE));
    }

    protected Message createTargetMessage(String target, String tenant) {
        final MessageProperties messageProperties = createMessagePropertiesWithTenant(tenant);
        messageProperties.getHeaders().put(MessageHeaderKey.THING_ID, target);
        messageProperties.getHeaders().put(MessageHeaderKey.TYPE, MessageType.THING_CREATED.toString());
        messageProperties.setReplyTo(AmqpTestConfiguration.REPLY_TO_EXCHANGE);

        return createMessage("", messageProperties);
    }

    protected Message createMessage(Object payload, final MessageProperties messageProperties) {
        if (payload == null) {
            messageProperties.setContentType("json");
            return new Message(null, messageProperties);
        }
        return getDmfClient().getMessageConverter().toMessage(payload, messageProperties);
    }

    protected MessageProperties createMessagePropertiesWithTenant(String tenant) {
        final MessageProperties messageProperties = new MessageProperties();
        messageProperties.getHeaders().put(MessageHeaderKey.TENANT, tenant);
        return messageProperties;
    }

}
