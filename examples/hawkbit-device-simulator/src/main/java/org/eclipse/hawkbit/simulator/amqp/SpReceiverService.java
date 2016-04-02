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
import org.eclipse.hawkbit.dmf.json.model.DownloadAndUpdateRequest;
import org.eclipse.hawkbit.simulator.DeviceSimulatorUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Handle all incoming Messages from hawkBit update server.
 *
 */
@Component
public class SpReceiverService extends ReceiverService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiverService.class);

    public static final String SOFTWARE_MODULE_FIRMWARE = "firmware";

    private final SpSenderService spSenderService;

    private final DeviceSimulatorUpdater deviceUpdater;

    /**
     * Constructor.
     */
    @Autowired
    public SpReceiverService(final RabbitTemplate rabbitTemplate, final AmqpProperties amqpProperties,
            final SpSenderService spSenderService, final DeviceSimulatorUpdater deviceUpdater) {
        super(rabbitTemplate, amqpProperties);
        this.spSenderService = spSenderService;
        this.deviceUpdater = deviceUpdater;
    }

    /**
     * Handle the incoming Message from Queue with the property
     * (hawkbit.device.simulator.amqp.receiverConnectorQueueFromSp).
     *
     * @param message
     *            the incoming message
     * @param type
     *            the action type
     * @param contentType
     *            the content type in message header
     * @param thingId
     *            the thing id in message header
     */
    @RabbitListener(queues = "${hawkbit.device.simulator.amqp.receiverConnectorQueueFromSp}", containerFactory = "listenerContainerFactory")
    public void recieveMessageSp(final Message message, @Header(MessageHeaderKey.TYPE) final String type,
            @Header(MessageHeaderKey.THING_ID) final String thingId) {
        checkContentTypeJson(message);
        delegateMessage(message, type, thingId);
    }

    private void delegateMessage(final Message message, final String type, final String thingId) {
        final MessageType messageType = MessageType.valueOf(type);

        switch (messageType) {
        case EVENT:
            handleEventMessage(message, thingId);
            break;
        default:
            LOGGER.info("No valid message type property.");
            break;
        }
    }

    private void handleEventMessage(final Message message, final String thingId) {
        final Object eventHeader = message.getMessageProperties().getHeaders().get(MessageHeaderKey.TOPIC);
        if (eventHeader == null) {
            logAndThrowMessageError(message, "Event Topic is not set");
        }
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
        spSenderService.finishUpdateProcess(update, "Simulation canceled");
    }

    private void handleUpdateProcess(final Message message, final String thingId) {
        final MessageProperties messageProperties = message.getMessageProperties();
        final Map<String, Object> headers = messageProperties.getHeaders();
        final String tenant = (String) headers.get(MessageHeaderKey.TENANT);

        final DownloadAndUpdateRequest downloadAndUpdateRequest = convertMessage(message,
                DownloadAndUpdateRequest.class);
        final Long actionId = downloadAndUpdateRequest.getActionId();

        deviceUpdater.startUpdate(tenant, thingId, actionId,
                downloadAndUpdateRequest.getSoftwareModules().get(0).getModuleVersion(), (device, actionId1) -> {
                    switch (device.getResponseStatus()) {
                    case SUCCESSFUL:
                        spSenderService.finishUpdateProcess(
                                new SimulatedUpdate(device.getTenant(), device.getId(), actionId1),
                                "Simulation complete!");
                        break;
                    case ERROR:
                        spSenderService.finishUpdateProcessWithError(
                                new SimulatedUpdate(device.getTenant(), device.getId(), actionId1),
                                "Simulation complete with error!");
                        break;
                    default:
                        break;
                    }
                });
    }
}
