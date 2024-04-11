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

import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.DmfActionRequest;
import org.eclipse.hawkbit.dmf.json.model.DmfDownloadAndUpdateRequest;
import org.eclipse.hawkbit.dmf.json.model.DmfMultiActionRequest;
import org.eclipse.hawkbit.dmf.json.model.DmfUpdateMode;
import org.eclipse.hawkbit.sdk.dmf.DmfTenant;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.handler.annotation.Header;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract class for sender and receiver service.
 */
@Slf4j
public class VHost extends DmfSender implements MessageListener {

    private final SimpleMessageListenerContainer container;
    private final ConcurrentHashMap<String, DmfTenant> dmfTenants = new ConcurrentHashMap<>();

    private final Set<Long> openActions = Collections.synchronizedSet(new HashSet<>());

    VHost(final ConnectionFactory connectionFactory, final AmqpProperties amqpProperties) {
        super(new RabbitTemplate(connectionFactory), amqpProperties);

        // It is necessary to define rabbitTemplate as a Bean and set
        // Jackson2JsonMessageConverter explicitly here in order to convert only
        // OUTCOMING messages to json. In case of INCOMING messages,
        // Jackson2JsonMessageConverter can not handle messages with NULL
        // payload (e.g. REQUEST_ATTRIBUTES_UPDATE), so the
        // SimpleMessageConverter is used instead per default.
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());

        final RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        final Queue queue = QueueBuilder.nonDurable(amqpProperties.getReceiverConnectorQueueFromSp()).autoDelete()
                .withArguments(Map.of(
                        "x-message-ttl", Duration.ofDays(1).toMillis(),
                        "x-max-length", 100_000))
                .build();
        rabbitAdmin.declareQueue(queue);
        final FanoutExchange exchange = new FanoutExchange(amqpProperties.getSenderForSpExchange(), false, true);
        rabbitAdmin.declareExchange(exchange);
        rabbitAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange));
        
        container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(amqpProperties.getReceiverConnectorQueueFromSp());
        container.setMessageListener(this);
        container.start();
    }

    void stop() {
        container.stop();
        rabbitTemplate.destroy();
    }

    public void register(final DmfTenant dmfTenant) {
        dmfTenants.put(dmfTenant.getTenant().getTenantId(), dmfTenant);
    }

    @Override
    public void onMessage(final Message message) {
        final String tenantId = (String)message.getMessageProperties().getHeaders().get(MessageHeaderKey.TENANT);
        final String controllerId = (String)message.getMessageProperties().getHeaders().get(MessageHeaderKey.THING_ID);
        final String type = (String)message.getMessageProperties().getHeaders().get(MessageHeaderKey.TYPE);

        switch (MessageType.valueOf(type)) {
            case EVENT: {
                checkContentTypeJson(message);
                handleEventMessage(message, controllerId);
                break;
            }
            case THING_DELETED: {
                checkContentTypeJson(message);
                Optional.ofNullable(dmfTenants.get(tenantId)).ifPresent(dmfTenant -> dmfTenant.remove(controllerId));
                break;
            }
            case PING_RESPONSE: {
                final String correlationId = message.getMessageProperties().getCorrelationId();
                if (log.isDebugEnabled()) {
                    log.debug("Got ping response from tenantId {} with correlationId {} with timestamp {}", tenantId,
                            correlationId, new String(message.getBody(), StandardCharsets.UTF_8));
                }
                pingResponse(controllerId, message);
                break;
            }
            default: {
                log.info("No valid message type property.");
            }
        }
    }

    private void handleEventMessage(final Message message, final String thingId) {
        final Object eventHeader = message.getMessageProperties().getHeaders().get(MessageHeaderKey.TOPIC);
        if (eventHeader == null) {
            log.error("Error \"Event Topic is not set\" reported by message {}", message.getMessageProperties().getMessageId());
            throw new IllegalArgumentException("Event Topic is not set");
        }

        // Exception squid:S2259 - Checked before
        @SuppressWarnings({ "squid:S2259" })
        final EventTopic eventTopic = EventTopic.valueOf(eventHeader.toString());
        switch (eventTopic) {
        case CONFIRM:
            handleConfirmation(message, thingId);
            break;
        case DOWNLOAD_AND_INSTALL:
        case DOWNLOAD:
            handleUpdateProcess(message, thingId, eventTopic);
            break;
        case CANCEL_DOWNLOAD:
            handleCancelDownloadAction(message, thingId);
            break;
        case REQUEST_ATTRIBUTES_UPDATE:
            handleAttributeUpdateRequest(message, thingId);
            break;
        case MULTI_ACTION:
            handleMultiActionRequest(message, thingId);
            break;
        default:
            log.info("No valid event property: {}", eventTopic);
            break;
        }
    }

    private void handleConfirmation(final Message message, final String controllerId) {
        log.warn("Handle confirmed received for {}! Skip it!", controllerId);
    }

    private static final String REGEX_EXTRACT_ACTION_ID = "[^0-9]";
    private long extractActionIdFrom(final Message message) {
        final String messageAsString = message.toString();
        final String requiredMessageContent = messageAsString
                .substring(messageAsString.indexOf('{') + 1, messageAsString.indexOf('}'));
        final String[] splitMessageContent = requiredMessageContent.split(",");
        return Long.parseLong(splitMessageContent[0].replaceAll(REGEX_EXTRACT_ACTION_ID, ""));
    }

    private void handleMultiActionRequest(final Message message, final String controllerId) {
        final DmfMultiActionRequest multiActionRequest = convertMessage(message, DmfMultiActionRequest.class);
        final String tenant = getTenant(message);
        final DmfMultiActionRequest.DmfMultiActionElement actionElement = multiActionRequest.getElements().get(0);

        final EventTopic eventTopic = actionElement.getTopic();
        final DmfActionRequest action = actionElement.getAction();
        final long actionId = action.getActionId();

        if (openActions.contains(actionId)) {
            return;
        }

        openActions.add(actionId);

        switch (eventTopic) {
        case DOWNLOAD:
        case DOWNLOAD_AND_INSTALL:
            if (action instanceof DmfDownloadAndUpdateRequest) {
                processUpdate(tenant, controllerId, eventTopic, (DmfDownloadAndUpdateRequest) action);
            }
            break;
        case CANCEL_DOWNLOAD:
            processCancelDownloadAction(controllerId, tenant, action.getActionId());
            break;
        default:
            openActions.remove(actionId);
            log.info("No valid event property in MULTI_ACTION.");
            break;
        }
    }

    private void handleAttributeUpdateRequest(final Message message, final String controllerId) {
        final String tenantId = getTenant(message);
        Optional.ofNullable(dmfTenants.get(tenantId))
                .flatMap(dmfTenant -> dmfTenant.getController(controllerId))
                .ifPresent(controller ->
                        updateAttributes(tenantId, controllerId, DmfUpdateMode.MERGE, controller.getAttributes()));
    }

    private static String getTenant(final Message message) {
        final MessageProperties messageProperties = message.getMessageProperties();
        final Map<String, Object> headers = messageProperties.getHeaders();
        return (String) headers.get(MessageHeaderKey.TENANT);
    }

    private void handleCancelDownloadAction(final Message message, final String thingId) {
        final String tenant = getTenant(message);
        final Long actionId = extractActionIdFrom(message);

        processCancelDownloadAction(thingId, tenant, actionId);
    }

    private void processCancelDownloadAction(final String thingId, final String tenant, final Long actionId) {
        finishUpdateProcess(thingId, actionId, Collections.singletonList("Simulation canceled"));
        openActions.remove(actionId);
    }

    private void handleUpdateProcess(final Message message, final String controllerId, final EventTopic actionType) {
        final String tenant = getTenant(message);
        final DmfDownloadAndUpdateRequest downloadAndUpdateRequest = convertMessage(message,
                DmfDownloadAndUpdateRequest.class);
        processUpdate(tenant, controllerId, actionType, downloadAndUpdateRequest);
    }

    private void processUpdate(final String tenantId, final String controllerId, final EventTopic actionType, final DmfDownloadAndUpdateRequest updateRequest) {
        Optional.ofNullable(dmfTenants.get(tenantId))
                .flatMap(dmfTenant -> dmfTenant.getController(controllerId))
                .ifPresent(controller -> controller.processUpdate(actionType, updateRequest));
    }

    /**
     * Convert a message body to a given class and set the message header
     * AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME for Jackson converter.
     */
    @SuppressWarnings("unchecked")
    private <T> T convertMessage(final Message message, final Class<T> clazz) {
        message.getMessageProperties().getHeaders().put(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
                clazz.getTypeName());
        return (T) rabbitTemplate.getMessageConverter().fromMessage(message);
    }

    /**
     * Method to validate if content type is set in the message properties.
     *
     * @param message
     *            the message to get validated
     */
    private static void checkContentTypeJson(final Message message) {
        if (message.getBody().length == 0) {
            return;
        }
        final MessageProperties messageProperties = message.getMessageProperties();
        final String headerContentType = (String) messageProperties.getHeaders().get("content-type");
        if (null != headerContentType) {
            messageProperties.setContentType(headerContentType);
        }
        final String contentType = messageProperties.getContentType();
        if (contentType != null && contentType.contains("json")) {
            return;
        }
        throw new AmqpRejectAndDontRequeueException("Content-Type is not JSON compatible");
    }
}