/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator.amqp;

import java.util.Map;

import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.DmfDownloadAndUpdateRequest;
import org.eclipse.hawkbit.simulator.DeviceSimulatorRepository;
import org.eclipse.hawkbit.simulator.DeviceSimulatorUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

/**
 * Handle all incoming Messages from hawkBit update server.
 *
 */
@Component
@ConditionalOnProperty(prefix = AmqpProperties.CONFIGURATION_PREFIX, name = "enabled")
public class SpReceiverService extends ReceiverService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiverService.class);

    private final SpSenderService spSenderService;

    private final DeviceSimulatorUpdater deviceUpdater;

    private final DeviceSimulatorRepository repository;

    /**
     * Constructor.
     * 
     * @param rabbitTemplate
     *            for sending messages
     * @param amqpProperties
     *            for amqp configuration
     * @param spSenderService
     *            to send messages
     * @param deviceUpdater
     *            simulator service for updates
     * @param repository
     *            to manage simulated devices
     */
    @Autowired
    public SpReceiverService(final RabbitTemplate rabbitTemplate, final AmqpProperties amqpProperties,
            final SpSenderService spSenderService, final DeviceSimulatorUpdater deviceUpdater,
            final DeviceSimulatorRepository repository) {
        super(rabbitTemplate, amqpProperties);
        this.spSenderService = spSenderService;
        this.deviceUpdater = deviceUpdater;
        this.repository = repository;
    }

    /**
     * Handle the incoming Message from Queue with the property
     * (hawkbit.device.simulator.amqp.receiverConnectorQueueFromSp).
     *
     * @param message
     *            the incoming message
     * @param type
     *            the action type
     * @param thingId
     *            the thing id in message header
     * @param tenant
     *            the device belongs to
     */
    @RabbitListener(queues = "${hawkbit.device.simulator.amqp.receiverConnectorQueueFromSp}", containerFactory = "listenerContainerFactory")
    public void recieveMessageSp(final Message message, @Header(MessageHeaderKey.TYPE) final String type,
            @Header(MessageHeaderKey.THING_ID) final String thingId,
            @Header(MessageHeaderKey.TENANT) final String tenant) {
        checkContentTypeJson(message);
        delegateMessage(message, type, thingId, tenant);
    }

    private void delegateMessage(final Message message, final String type, final String thingId, final String tenant) {
        final MessageType messageType = MessageType.valueOf(type);

        if (MessageType.EVENT.equals(messageType)) {
            handleEventMessage(message, thingId);
            return;
        }

        if (MessageType.THING_DELETED.equals(messageType)) {
            repository.remove(tenant, thingId);
            return;
        }

        LOGGER.info("No valid message type property.");
    }

    private void handleEventMessage(final Message message, final String thingId) {
        final Object eventHeader = message.getMessageProperties().getHeaders().get(MessageHeaderKey.TOPIC);
        if (eventHeader == null) {
            logAndThrowMessageError(message, "Event Topic is not set");
        }
        // Exception squid:S2259 - Checked before
        @SuppressWarnings({ "squid:S2259" })
        final EventTopic eventTopic = EventTopic.valueOf(eventHeader.toString());
        switch (eventTopic) {
        case DOWNLOAD_AND_INSTALL:
            handleUpdateProcess(message, thingId);
            break;
        case CANCEL_DOWNLOAD:
            handleCancelDownloadAction(message, thingId);
            break;
        default:
            LOGGER.info("No valid event property.");
            break;
        }
    }

    private void handleCancelDownloadAction(final Message message, final String thingId) {
        final MessageProperties messageProperties = message.getMessageProperties();
        final Map<String, Object> headers = messageProperties.getHeaders();
        final String tenant = (String) headers.get(MessageHeaderKey.TENANT);
        final Long actionId = convertMessage(message, Long.class);

        final SimulatedUpdate update = new SimulatedUpdate(tenant, thingId, actionId);
        spSenderService.finishUpdateProcess(update, Lists.newArrayList("Simulation canceled"));
    }

    private void handleUpdateProcess(final Message message, final String thingId) {
        final MessageProperties messageProperties = message.getMessageProperties();
        final Map<String, Object> headers = messageProperties.getHeaders();
        final String tenant = (String) headers.get(MessageHeaderKey.TENANT);

        final DmfDownloadAndUpdateRequest downloadAndUpdateRequest = convertMessage(message,
                DmfDownloadAndUpdateRequest.class);
        final Long actionId = downloadAndUpdateRequest.getActionId();
        final String targetSecurityToken = downloadAndUpdateRequest.getTargetSecurityToken();

        deviceUpdater.startUpdate(tenant, thingId, actionId, null, downloadAndUpdateRequest.getSoftwareModules(),
                targetSecurityToken, (device, actionId1) -> {
                    switch (device.getUpdateStatus().getResponseStatus()) {
                    case SUCCESSFUL:
                        spSenderService.finishUpdateProcess(
                                new SimulatedUpdate(device.getTenant(), device.getId(), actionId1),
                                device.getUpdateStatus().getStatusMessages());
                        break;
                    case ERROR:
                        spSenderService.finishUpdateProcessWithError(
                                new SimulatedUpdate(device.getTenant(), device.getId(), actionId1),
                                device.getUpdateStatus().getStatusMessages());
                        break;
                    default:
                        break;
                    }
                });
    }
}
