/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.amqp;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bean which holds the necessary properties for configuring the AMQP deadletter
 * queue.
 */
@Data
@ConfigurationProperties("hawkbit.dmf.rabbitmq.dead-letter")
public class AmqpDeadletterProperties {

    private static final int THREE_WEEKS = 21;

    /**
     * Message time to live (ttl) for the dead letter queue. Default ttl is 3 weeks.
     */
    private long ttl = Duration.ofDays(THREE_WEEKS).toMillis();

    /**
     * Return the dead letter arguments.
     *
     * @param exchange the dead letter exchange
     * @return map which holds the properties
     */
    public Map<String, Object> getDeadLetterExchangeArgs(final String exchange) {
        final Map<String, Object> args = new HashMap<>(1);
        args.put("x-dead-letter-exchange", exchange);
        return args;
    }

    /**
     * Create a dead letter queue with ttl for messages
     *
     * @param queueName the dead letter queue name
     * @return the dead letter queue
     */
    public Queue createDeadletterQueue(final String queueName) {
        return new Queue(queueName, true, false, false, getTTLArgs());
    }

    private Map<String, Object> getTTLArgs() {
        final Map<String, Object> args = new HashMap<>(1);
        args.put("x-message-ttl", getTtl());
        return args;
    }
}