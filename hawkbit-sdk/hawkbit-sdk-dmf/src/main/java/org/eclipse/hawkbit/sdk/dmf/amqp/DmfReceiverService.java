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

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.DmfActionRequest;
import org.eclipse.hawkbit.dmf.json.model.DmfDownloadAndUpdateRequest;
import org.eclipse.hawkbit.dmf.json.model.DmfMultiActionRequest;
import org.eclipse.hawkbit.dmf.json.model.DmfUpdateMode;
import org.eclipse.hawkbit.sdk.dmf.DeviceManagement;
import org.eclipse.hawkbit.sdk.dmf.DmfProperties;
import org.eclipse.hawkbit.sdk.dmf.UpdateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.Header;

/**
 * Handle all incoming Messages from hawkBit update server.
 *
 */
public class DmfReceiverService extends MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DmfReceiverService.class);

    private static final String REGEX_EXTRACT_ACTION_ID = "[^0-9]";

    private final DmfSenderService dmfSenderService;
    private final DeviceManagement deviceManagement;

    private final Set<Long> openActions = Collections.synchronizedSet(new HashSet<>());

    public DmfReceiverService(final RabbitTemplate rabbitTemplate, final DmfSenderService dmfSenderService,
            final DeviceManagement deviceManagement, final AmqpProperties amqpProperties) {
        super(rabbitTemplate, amqpProperties);
        this.dmfSenderService = dmfSenderService;
        this.deviceManagement = deviceManagement;
    }

    @RabbitListener(queues = "${" + DmfProperties.CONFIGURATION_PREFIX + ".receiverConnectorQueueFromSp}")
    public void receiveMessageSp(
            @Header(MessageHeaderKey.TENANT) final String tenant,
            @Header(name = MessageHeaderKey.THING_ID, required = false) final String controllerId,
            @Header(MessageHeaderKey.TYPE) final String type,
            final Message message) {
        switch (MessageType.valueOf(type)) {
            case EVENT: {
                checkContentTypeJson(message);
                handleEventMessage(message, controllerId);
                break;
            }
            case THING_DELETED: {
                checkContentTypeJson(message);
                deviceManagement.remove(tenant, controllerId);
                break;
            }
            case PING_RESPONSE: {
                final String correlationId = message.getMessageProperties().getCorrelationId();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Got ping response from tenant {} with correlationId {} with timestamp {}", tenant,
                            correlationId, new String(message.getBody(), StandardCharsets.UTF_8));
                }
                dmfSenderService.pingResponse(controllerId, message);
                break;
            }
            default: {
                LOGGER.info("No valid message type property.");
            }
        }
    }

    private void handleEventMessage(final Message message, final String thingId) {
        final Object eventHeader = message.getMessageProperties().getHeaders().get(MessageHeaderKey.TOPIC);
        if (eventHeader == null) {
            LOGGER.error("Error \"Event Topic is not set\" reported by message {}", message.getMessageProperties().getMessageId());
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
                LOGGER.info("No valid event property: {}", eventTopic);
                break;
        }
    }

    private void handleConfirmation(final Message message, final String controllerId) {
        LOGGER.warn("Handle confirmed received for {}! Skip it!", controllerId);
    }

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
            LOGGER.info("No valid event property in MULTI_ACTION.");
            break;
        }
    }

    private void handleAttributeUpdateRequest(final Message message, final String controllerId) {
        final String tenantId = getTenant(message);
        deviceManagement.getController(tenantId, controllerId).ifPresent(controller ->
            dmfSenderService.updateAttributes(tenantId, controllerId, DmfUpdateMode.MERGE, controller.getAttributes()));
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
        final UpdateInfo update = new UpdateInfo(tenant, thingId, actionId);
        dmfSenderService.finishUpdateProcess(update, Collections.singletonList("Simulation canceled"));
        openActions.remove(actionId);
    }

    private void handleUpdateProcess(final Message message, final String controllerId, final EventTopic actionType) {
        final String tenant = getTenant(message);
        final DmfDownloadAndUpdateRequest downloadAndUpdateRequest = convertMessage(message,
                DmfDownloadAndUpdateRequest.class);
        processUpdate(tenant, controllerId, actionType, downloadAndUpdateRequest);
    }

    private void processUpdate(final String tenant, final String thingId, final EventTopic actionType, final DmfDownloadAndUpdateRequest updateRequest) {
        deviceManagement.getController(tenant, thingId).ifPresent(controller ->
            controller.processUpdate(actionType, updateRequest));
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