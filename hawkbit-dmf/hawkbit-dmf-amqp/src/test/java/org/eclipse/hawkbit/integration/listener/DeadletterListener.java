/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.integration.listener;

import org.eclipse.hawkbit.rabbitmq.test.listener.TestRabbitListener;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

public class DeadletterListener implements TestRabbitListener {

    public static final String LISTENER_ID = "deadletter";

    @Override
    @RabbitListener(id = "deadletter", queues = "dmf_connector_deadletter_ttl")
    public void handleMessage(Message message) {
        // currently the message is not needed
    }
}