/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator.amqp;

import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.dmf.amqp.api.AmqpSettings;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.ActionStatus;
import org.eclipse.hawkbit.dmf.json.model.ActionUpdateStatus;
import org.eclipse.hawkbit.dmf.json.model.AttributeUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Sender service to send messages to update server.
 */
@Service
public class SpSenderService extends SenderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpSenderService.class);

    private final String spExchange;

    /**
     *
     * @param rabbitTemplate
     *            the rabbit template
     * @param amqpProperties
     *            the amqp properties
     */
    @Autowired
    public SpSenderService(final RabbitTemplate rabbitTemplate, final AmqpProperties amqpProperties) {
        super(rabbitTemplate, amqpProperties);
        this.spExchange = AmqpSettings.DMF_EXCHANGE;
    }

    /**
     * Finish the update process. This will send a action status to SP.
     *
     * @param update
     *            the simulated update object
     * @param updateResultMessages
     *            a description according the update process
     */
    public void finishUpdateProcess(final SimulatedUpdate update, final List<String> updateResultMessages) {
        final Message updateResultMessage = createUpdateResultMessage(update, ActionStatus.FINISHED,
                updateResultMessages);
        sendMessage(spExchange, updateResultMessage);
    }

    /**
     * Finish update process with error and send error to SP.
     *
     * @param update
     *            the simulated update object
     * @param messageDescription
     *            a description according the update process
     */
    public void finishUpdateProcessWithError(final SimulatedUpdate update, final List<String> updateResultMessages) {
        sendErrorgMessage(update, updateResultMessages);
        LOGGER.debug("Update process finished with error \"{}\" reported by thing {}", updateResultMessages,
                update.getThingId());
    }

    /**
     * Send an error message to SP.
     *
     * @param tenant
     *            the tenant
     * @param updateResultMessages
     *            the error message description to send
     * @param actionId
     *            the ID of the action for the error message
     */
    public void sendErrorMessage(final String tenant, final List<String> updateResultMessages, final Long actionId) {
        final Message message = createActionStatusMessage(tenant, ActionStatus.ERROR, updateResultMessages, actionId);
        sendMessage(spExchange, message);
    }

    /**
     * Send a warning message to SP.
     *
     * @param update
     *            the simulated update object
     * @param updateResultMessages
     *            a warning description
     */
    public void sendWarningMessage(final SimulatedUpdate update, final List<String> updateResultMessages) {
        final Message message = createActionStatusMessage(update, updateResultMessages, ActionStatus.WARNING);
        sendMessage(spExchange, message);
    }

    /**
     * Method to send a action status to SP.
     *
     * @param tenant
     *            the tenant
     * @param actionStatus
     *            the action status
     * @param updateResultMessages
     *            the message to get send
     * @param actionId
     *            the cached value
     */
    public void sendActionStatusMessage(final String tenant, final ActionStatus actionStatus,
            final List<String> updateResultMessages, final Long actionId) {
        final Message message = createActionStatusMessage(tenant, actionStatus, updateResultMessages, actionId);
        sendMessage(message);

    }

    /**
     * Create new thing created message and send to udpate server.
     *
     * @param tenant
     *            the tenant to create the target
     * @param targetId
     *            the ID of the target to create or update
     */
    public void createOrUpdateThing(final String tenant, final String targetId) {
        sendMessage(spExchange, thingCreatedMessage(tenant, targetId));

        LOGGER.debug("Created thing created message and send to update server for Thing \"{}\"", targetId);
    }

    /**
     * Create new attribute update message and send to udpate server.
     *
     * @param tenant
     *            the tenant to create the target
     * @param targetId
     *            the ID of the target to create or update
     */
    public void updateAttributesOfThing(final String tenant, final String targetId) {
        sendMessage(spExchange, attributeUpdateMessage(tenant, targetId));

        LOGGER.debug("Send update attributes message and send to update server for Thing \"{}\"", targetId);
    }

    private Message thingCreatedMessage(final String tenant, final String targetId) {
        final MessageProperties messagePropertiesForSP = new MessageProperties();
        messagePropertiesForSP.setHeader(MessageHeaderKey.TYPE, MessageType.THING_CREATED.name());
        messagePropertiesForSP.setHeader(MessageHeaderKey.TENANT, tenant);
        messagePropertiesForSP.setHeader(MessageHeaderKey.THING_ID, targetId);
        messagePropertiesForSP.setHeader(MessageHeaderKey.SENDER, "simulator");
        messagePropertiesForSP.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messagePropertiesForSP.setReplyTo(amqpProperties.getSenderForSpExchange());
        return new Message(null, messagePropertiesForSP);
    }

    private Message attributeUpdateMessage(final String tenant, final String targetId) {
        final MessageProperties messagePropertiesForSP = new MessageProperties();
        messagePropertiesForSP.setHeader(MessageHeaderKey.TYPE, MessageType.EVENT.name());
        messagePropertiesForSP.setHeader(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ATTRIBUTES);
        messagePropertiesForSP.setHeader(MessageHeaderKey.TENANT, tenant);
        messagePropertiesForSP.setHeader(MessageHeaderKey.THING_ID, targetId);
        messagePropertiesForSP.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messagePropertiesForSP.setReplyTo(amqpProperties.getSenderForSpExchange());
        final AttributeUpdate attributeUpdate = new AttributeUpdate();

        // FIXME take from configuration
        attributeUpdate.getAttributes().put("isoCode", "DE");

        return convertMessage(attributeUpdate, messagePropertiesForSP);
    }

    /**
     * Send a created message to SP.
     *
     * @param message
     *            the message to get send
     */
    private void sendMessage(final Message message) {
        sendMessage(spExchange, message);
    }

    /**
     * Send error message to SP.
     *
     * @param context
     *            the current context
     * @param updateResultMessages
     *            a list of descriptions according the update process
     */
    private void sendErrorgMessage(final SimulatedUpdate update, final List<String> updateResultMessages) {
        final Message message = createActionStatusMessage(update, updateResultMessages, ActionStatus.ERROR);
        sendMessage(spExchange, message);
    }

    /**
     * Create a action status message.
     *
     * @param actionStatus
     *            the ActionStatus
     * @param actionMessage
     *            the message description
     * @param actionId
     *            the action id
     * @param cacheValue
     *            the cacheValue value
     */
    private Message createActionStatusMessage(final String tenant, final ActionStatus actionStatus,
            final List<String> updateResultMessages, final Long actionId) {
        final MessageProperties messageProperties = new MessageProperties();
        final Map<String, Object> headers = messageProperties.getHeaders();
        final ActionUpdateStatus actionUpdateStatus = new ActionUpdateStatus();
        actionUpdateStatus.setActionStatus(actionStatus);
        headers.put(MessageHeaderKey.TYPE, MessageType.EVENT.name());
        headers.put(MessageHeaderKey.TENANT, tenant);
        headers.put(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ACTION_STATUS.name());
        headers.put(MessageHeaderKey.CONTENT_TYPE, MessageProperties.CONTENT_TYPE_JSON);
        actionUpdateStatus.getMessage().addAll(updateResultMessages);

        actionUpdateStatus.setActionId(actionId);
        return convertMessage(actionUpdateStatus, messageProperties);
    }

    private Message createUpdateResultMessage(final SimulatedUpdate cacheValue, final ActionStatus actionStatus,
            final List<String> updateResultMessages) {
        final MessageProperties messageProperties = new MessageProperties();
        final Map<String, Object> headers = messageProperties.getHeaders();
        final ActionUpdateStatus actionUpdateStatus = new ActionUpdateStatus();
        actionUpdateStatus.setActionStatus(actionStatus);
        headers.put(MessageHeaderKey.TYPE, MessageType.EVENT.name());
        headers.put(MessageHeaderKey.TENANT, cacheValue.getTenant());
        headers.put(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ACTION_STATUS.name());
        headers.put(MessageHeaderKey.CONTENT_TYPE, MessageProperties.CONTENT_TYPE_JSON);
        actionUpdateStatus.addMessage(updateResultMessages);
        actionUpdateStatus.setActionId(cacheValue.getActionId());
        return convertMessage(actionUpdateStatus, messageProperties);
    }

    private Message createActionStatusMessage(final SimulatedUpdate update, final List<String> updateResultMessages,
            final ActionStatus status) {
        return createActionStatusMessage(update.getTenant(), status, updateResultMessages, update.getActionId());
    }

}
