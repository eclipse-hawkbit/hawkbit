/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.integration.listener;

import org.eclipse.hawkbit.AmqpTestConfiguration;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

public class ReplyToListener extends AbstractTestRabbitListener {

    public static final String LISTENER_ID = "replyto";

    @Override
    @RabbitListener(id = LISTENER_ID, queues = AmqpTestConfiguration.REPLY_TO_QUEUE)
    public void handleMessage(Message message) {
        setMessage(message);
    }

}