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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.AmqpTestConfiguration;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

public class ReplyToListener implements TestRabbitListener {

    public static final String LISTENER_ID = "replyto";

    private final Map<EventTopic, Message> eventTopicMessages = new HashMap<>();
    private final Map<String, Message> deleteMessages = new HashMap<>();

    @Override
    @RabbitListener(id = LISTENER_ID, queues = AmqpTestConfiguration.REPLY_TO_QUEUE)
    public void handleMessage(final Message message) {

        final MessageType messageType = MessageType
                .valueOf(message.getMessageProperties().getHeaders().get(MessageHeaderKey.TYPE).toString());

        switch (messageType) {
        case EVENT:
            final EventTopic eventTopic = EventTopic
                    .valueOf(message.getMessageProperties().getHeaders().get(MessageHeaderKey.TOPIC).toString());
            eventTopicMessages.put(eventTopic, message);
            break;
        case THING_DELETED:
            final String targetName = message.getMessageProperties().getHeaders().get(MessageHeaderKey.THING_ID)
                    .toString();
            deleteMessages.put(targetName, message);
            break;
        default:
            fail("Unexpected message type");
        }
    }

    public Map<EventTopic, Message> getEventTopicMessages() {
        return eventTopicMessages;
    }

    public Map<String, Message> getDeleteMessages() {
        return deleteMessages;
    }

}
