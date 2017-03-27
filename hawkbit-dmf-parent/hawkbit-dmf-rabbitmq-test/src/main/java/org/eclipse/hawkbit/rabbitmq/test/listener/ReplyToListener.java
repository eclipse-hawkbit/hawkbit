/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rabbitmq.test.listener;

import java.util.EnumMap;
import java.util.Map;

import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.rabbitmq.test.AmqpTestConfiguration;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

public class ReplyToListener implements TestRabbitListener {

    public static final String LISTENER_ID = "replyto";

    private final EnumMap<EventTopic, Message> messages = new EnumMap<>(EventTopic.class);

    @Override
    @RabbitListener(id = LISTENER_ID, queues = AmqpTestConfiguration.REPLY_TO_QUEUE)
    public void handleMessage(Message message) {
        final EventTopic eventTopic = EventTopic
                .valueOf(message.getMessageProperties().getHeaders().get(MessageHeaderKey.TOPIC).toString());
        messages.put(eventTopic, message);
    }

    public Map<EventTopic, Message> getMessages() {
        return messages;
    }
}
