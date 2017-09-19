/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.integration.listener;

import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.rabbitmq.test.listener.TestRabbitListener;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

public class ReplyToListener implements TestRabbitListener {

    public static final String LISTENER_ID = "replyto";
    public static final String REPLY_TO_QUEUE = "reply_queue";

    private final Map<EventTopic, Message> eventTopicMessages = new EnumMap<>(EventTopic.class);
    private final Map<String, Message> deleteMessages = new HashMap<>();
    private final Map<String, Message> pingResponseMessages = new HashMap<>();

    @Override
    @RabbitListener(id = LISTENER_ID, queues = REPLY_TO_QUEUE)
    public void handleMessage(final Message message) {

        final MessageType messageType = MessageType
                .valueOf(message.getMessageProperties().getHeaders().get(MessageHeaderKey.TYPE).toString());

        if (messageType == MessageType.EVENT) {
            final EventTopic eventTopic = EventTopic
                    .valueOf(message.getMessageProperties().getHeaders().get(MessageHeaderKey.TOPIC).toString());
            eventTopicMessages.put(eventTopic, message);
            return;
        }

        if (messageType == MessageType.THING_DELETED) {
            final String targetName = message.getMessageProperties().getHeaders().get(MessageHeaderKey.THING_ID)
                    .toString();
            deleteMessages.put(targetName, message);
            return;
        }

        if (messageType == MessageType.PING_RESPONSE) {
            final String correlationId = new String(message.getMessageProperties().getCorrelationId(),
                    StandardCharsets.UTF_8);
            pingResponseMessages.put(correlationId, message);
            return;
        }

        // if message type is not EVENT or THING_DELETED something unexpected
        // happened
        fail("Unexpected message type");

    }

    public Map<EventTopic, Message> getEventTopicMessages() {
        return eventTopicMessages;
    }

    public Map<String, Message> getDeleteMessages() {
        return deleteMessages;
    }

    public Map<String, Message> getPingResponseMessages() {
        return pingResponseMessages;
    }

}
