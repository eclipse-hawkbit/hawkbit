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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bean which holds the necessary properties for configuring the AMQP
 * connection.
 * 
 */
@Data
@ConfigurationProperties("hawkbit.dmf.rabbitmq")
public class AmqpProperties {

    private static final int DEFAULT_QUEUE_DECLARATION_RETRIES = 50;

    private static final long DEFAULT_REQUEUE_DELAY = 0;

    /**
     * Enable DMF API based on AMQP 0.9
     */
    private boolean enabled = true;

    /**
     * DMF API dead letter queue.
     */
    private String deadLetterQueue = "dmf_connector_deadletter_ttl";

    /**
     * DMF API dead letter exchange.
     */
    private String deadLetterExchange = "dmf.connector.deadletter";

    /**
     * DMF API receiving queue for EVENT or THING_CREATED message.
     */
    private String receiverQueue = "dmf_receiver";

    /**
     * Authentication request called by 3rd party artifact storages for download
     * authorizations.
     */
    private String authenticationReceiverQueue = "authentication_receiver";

    /**
     * Missing queue fatal.
     */
    private boolean missingQueuesFatal;

    /**
     * The number of retry attempts when passive queue declaration fails.
     * Passive queue declaration occurs when the consumer starts or, when
     * consuming from multiple queues, when not all queues were available during
     * initialization.
     */
    private int declarationRetries = DEFAULT_QUEUE_DECLARATION_RETRIES;

    /**
     * Delay for messages that are requeued in milliseconds.
     */
    private long requeueDelay = DEFAULT_REQUEUE_DELAY;
}
