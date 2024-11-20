/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rabbitmq.test.listener;

import org.springframework.amqp.core.Message;

@FunctionalInterface
public interface TestRabbitListener {

    /**
     * handle incoming message
     *
     * @param message the message
     */
    void handleMessage(Message message);
}