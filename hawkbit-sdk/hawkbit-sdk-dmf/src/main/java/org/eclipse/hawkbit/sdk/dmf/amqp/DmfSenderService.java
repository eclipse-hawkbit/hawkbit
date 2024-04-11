/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk.dmf.amqp;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.dmf.amqp.api.AmqpSettings;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.DmfActionStatus;
import org.eclipse.hawkbit.dmf.json.model.DmfActionUpdateStatus;
import org.eclipse.hawkbit.dmf.json.model.DmfAttributeUpdate;
import org.eclipse.hawkbit.dmf.json.model.DmfUpdateMode;
import org.eclipse.hawkbit.sdk.dmf.DmfProperties;
import org.eclipse.hawkbit.sdk.dmf.UpdateInfo;
import org.eclipse.hawkbit.sdk.dmf.UpdateStatus;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;
import org.springframework.util.ObjectUtils;

/**
 * Sender service to send messages to update server.
 */
@Slf4j
public class DmfSenderService extends MessageService {

    private static final byte[] EMPTY_BODY = new byte[0];

    private final String spExchange;
    private final ConcurrentHashMap<String, BiConsumer<String, Message>> pingListeners = new ConcurrentHashMap<>();

    DmfSenderService(final RabbitTemplate rabbitTemplate, final DmfProperties dmfProperties) {
        super(rabbitTemplate, dmfProperties);
        spExchange = AmqpSettings.DMF_EXCHANGE;
    }

    public void createOrUpdateThing(final String tenant, final String controllerId) {
        final MessageProperties messagePropertiesForSP = new MessageProperties();
        messagePropertiesForSP.setHeader(MessageHeaderKey.TYPE, MessageType.THING_CREATED.name());
        messagePropertiesForSP.setHeader(MessageHeaderKey.TENANT, tenant);
        messagePropertiesForSP.setHeader(MessageHeaderKey.THING_ID, controllerId);
        messagePropertiesForSP.setHeader(MessageHeaderKey.SENDER, "hawkBit-sdk");
        messagePropertiesForSP.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messagePropertiesForSP.setReplyTo(dmfProperties.getSenderForSpExchange());

        sendMessage(spExchange, new Message(EMPTY_BODY, messagePropertiesForSP));
    }

    /**
     * Finish the update process. This will send a action status to SP.
     *
     * @param update the simulated update object
     * @param updateResultMessages a description according the update process
     */
    public void finishUpdateProcess(final UpdateInfo update, final List<String> updateResultMessages) {
        sendMessage(spExchange, createActionStatusMessage(update, updateResultMessages, DmfActionStatus.FINISHED));
    }

    /**
     * Send a message if the message is not null.
     *
     * @param address the exchange name
     * @param message the amqp message which will be send if its not null
     */
    // Exception squid:S4449 - Cannot modify RabbitTemplate method definitions.
    @SuppressWarnings({ "squid:S4449" })
    public void sendMessage(final String address, final Message message) {
        if (message == null) {
            return;
        }
        message.getMessageProperties().getHeaders().remove(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME);

        final String correlationId = UUID.randomUUID().toString();

        if (ObjectUtils.isEmpty(message.getMessageProperties().getCorrelationId())) {
            message.getMessageProperties().setCorrelationId(correlationId);
        }

        if (log.isTraceEnabled()) {
            log.trace("Sending message {} to exchange {} with correlationId {}", message, address, correlationId);
        } else {
            log.debug("Sending message to exchange {} with correlationId {}", address, correlationId);
        }

        rabbitTemplate.send(address, null, message, new CorrelationData(correlationId));
    }

    public Message convertMessage(final Object object, final MessageProperties messageProperties) {
        return rabbitTemplate.getMessageConverter().toMessage(object, messageProperties);
    }

    public void sendFeedback(
            final String tenant, final Long actionId,
            final UpdateStatus updateStatus) {
        final Message message = createActionStatusMessage(tenant, actionId, updateStatus.status(), updateStatus.messages());
        sendMessage(spExchange, message);
    }

    public void updateAttributes(
            final String tenant, final String controllerId,
            final DmfUpdateMode mode,
            final String key, final String value) {
        updateAttributes(tenant, controllerId, mode, Collections.singletonMap(key, value));
    }
    public void updateAttributes(
            final String tenant, final String controllerId,
            final DmfUpdateMode mode,
            final Map<String, String> attributes) {
        final MessageProperties messagePropertiesForSP = new MessageProperties();
        messagePropertiesForSP.setHeader(MessageHeaderKey.TYPE, MessageType.EVENT.name());
        messagePropertiesForSP.setHeader(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ATTRIBUTES);
        messagePropertiesForSP.setHeader(MessageHeaderKey.TENANT, tenant);
        messagePropertiesForSP.setHeader(MessageHeaderKey.THING_ID, controllerId);
        messagePropertiesForSP.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messagePropertiesForSP.setReplyTo(dmfProperties.getSenderForSpExchange());

        final DmfAttributeUpdate attributeUpdate = new DmfAttributeUpdate();
        attributeUpdate.setMode(mode);
        attributeUpdate.getAttributes().putAll(attributes);

        sendMessage(spExchange, convertMessage(attributeUpdate, messagePropertiesForSP));
    }

    public void ping(final String tenant, final String correlationId, final BiConsumer<String, Message> listener) {
        final MessageProperties messageProperties = new MessageProperties();
        messageProperties.getHeaders().put(MessageHeaderKey.TENANT, tenant);
        messageProperties.getHeaders().put(MessageHeaderKey.TYPE, MessageType.PING.toString());
        messageProperties.setCorrelationId(correlationId);
        messageProperties.setReplyTo(dmfProperties.getSenderForSpExchange());
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);

        if (listener != null) {
            pingListeners.put(correlationId, listener);
        }

        sendMessage(spExchange, new Message(EMPTY_BODY, messageProperties));
    }

    void pingResponse(final String controllerId, final Message message) {
        final BiConsumer<String, Message> pingListener = pingListeners.remove(controllerId);
        if (pingListener != null) {
            pingListener.accept(controllerId, message);
        }
    }

    private Message createActionStatusMessage(final UpdateInfo update,
            final List<String> updateResultMessages, final DmfActionStatus status) {
        return createActionStatusMessage(update.getTenant(), update.getActionId(), status, updateResultMessages);
    }

    private Message createActionStatusMessage(final String tenant, final Long actionId,
            final DmfActionStatus actionStatus, final List<String> updateResultMessages) {
        final MessageProperties messageProperties = new MessageProperties();
        final Map<String, Object> headers = messageProperties.getHeaders();
        final DmfActionUpdateStatus actionUpdateStatus = new DmfActionUpdateStatus(actionId, actionStatus);
        headers.put(MessageHeaderKey.TYPE, MessageType.EVENT.name());
        headers.put(MessageHeaderKey.TENANT, tenant);
        headers.put(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ACTION_STATUS.name());
        headers.put(MessageHeaderKey.CONTENT_TYPE, MessageProperties.CONTENT_TYPE_JSON);
        actionUpdateStatus.addMessage(updateResultMessages);

        return convertMessage(actionUpdateStatus, messageProperties);
    }
}