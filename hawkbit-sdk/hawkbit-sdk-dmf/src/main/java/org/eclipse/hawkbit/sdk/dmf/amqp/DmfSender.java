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

import static org.eclipse.hawkbit.dmf.amqp.api.AmqpSettings.DMF_EXCHANGE;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.DmfActionStatus;
import org.eclipse.hawkbit.dmf.json.model.DmfActionUpdateStatus;
import org.eclipse.hawkbit.dmf.json.model.DmfAttributeUpdate;
import org.eclipse.hawkbit.dmf.json.model.DmfUpdateMode;
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
public class DmfSender {

    protected final RabbitTemplate rabbitTemplate;
    private static final byte[] EMPTY_BODY = new byte[0];
    private final AmqpProperties amqpProperties;
    private final ConcurrentHashMap<String, BiConsumer<String, Message>> pingListeners = new ConcurrentHashMap<>();

    public DmfSender(final RabbitTemplate rabbitTemplate, final AmqpProperties amqpProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.amqpProperties = amqpProperties;
    }

    public void createOrUpdateThing(final String tenant, final String controllerId) {
        sendThingMessage(tenant, controllerId, MessageType.THING_CREATED.name());
    }

    public void removeThing(final String tenant, final String controllerId) {
        sendThingMessage(tenant, controllerId, MessageType.THING_REMOVED.name());
    }

    public void sendThingMessage(final String tenant, final String controllerId, String thingStatusChange) {
        final MessageProperties messagePropertiesForSP = new MessageProperties();
        messagePropertiesForSP.setHeader(MessageHeaderKey.TYPE, thingStatusChange);
        messagePropertiesForSP.setHeader(MessageHeaderKey.TENANT, tenant);
        messagePropertiesForSP.setHeader(MessageHeaderKey.THING_ID, controllerId);
        messagePropertiesForSP.setHeader(MessageHeaderKey.SENDER, "hawkBit-sdk");
        messagePropertiesForSP.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messagePropertiesForSP.setReplyTo(amqpProperties.getSenderForSpExchange());

        sendMessage(DMF_EXCHANGE, new Message(EMPTY_BODY, messagePropertiesForSP));
    }

    public void finishUpdateProcess(final String tenantId, final long actionId, final List<String> updateResultMessages) {
        sendMessage(
                DMF_EXCHANGE,
                createActionStatusMessage(tenantId, actionId, DmfActionStatus.FINISHED, updateResultMessages));
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

        String correlationId = message.getMessageProperties().getCorrelationId();
        if (ObjectUtils.isEmpty(correlationId)) {
            correlationId = UUID.randomUUID().toString();
            message.getMessageProperties().setCorrelationId(correlationId);
        }

        if (log.isTraceEnabled()) {
            log.trace("Sending message {} to exchange {} with correlationId {}", message, address, correlationId);
        } else {
            log.debug("Sending message to exchange {} with correlationId {}", address, correlationId);
        }

        rabbitTemplate.send(address, null, message, new CorrelationData(correlationId));
    }

    public void sendFeedback(
            final String tenant, final Long actionId,
            final UpdateStatus updateStatus) {
        final Message message = createActionStatusMessage(tenant, actionId, updateStatus.status(), updateStatus.messages());
        sendMessage(DMF_EXCHANGE, message);
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
        messagePropertiesForSP.setReplyTo(amqpProperties.getSenderForSpExchange());

        final DmfAttributeUpdate attributeUpdate = new DmfAttributeUpdate();
        attributeUpdate.setMode(mode);
        attributeUpdate.getAttributes().putAll(attributes);

        sendMessage(DMF_EXCHANGE, convertMessage(attributeUpdate, messagePropertiesForSP));
    }

    public void ping(final String tenant, final String correlationId, final BiConsumer<String, Message> listener) {
        final MessageProperties messageProperties = new MessageProperties();
        messageProperties.getHeaders().put(MessageHeaderKey.TENANT, tenant);
        messageProperties.getHeaders().put(MessageHeaderKey.TYPE, MessageType.PING.toString());
        messageProperties.setCorrelationId(correlationId);
        messageProperties.setReplyTo(amqpProperties.getSenderForSpExchange());
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);

        if (listener != null) {
            pingListeners.put(correlationId, listener);
        }

        sendMessage(DMF_EXCHANGE, new Message(EMPTY_BODY, messageProperties));
    }

    void pingResponse(final String controllerId, final Message message) {
        final BiConsumer<String, Message> pingListener = pingListeners.remove(controllerId);
        if (pingListener != null) {
            pingListener.accept(controllerId, message);
        }
    }

    private Message convertMessage(final Object object, final MessageProperties messageProperties) {
        return rabbitTemplate.getMessageConverter().toMessage(object, messageProperties);
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