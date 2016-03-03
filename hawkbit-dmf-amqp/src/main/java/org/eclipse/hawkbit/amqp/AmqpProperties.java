/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bean which holds the necessary properties for configuring the AMQP
 * connection.
 *
 */
@ConfigurationProperties("hawkbit.dmf.rabbitmq")
public class AmqpProperties {

    private String deadLetterQueue = "dmf_receiver_deadletter";
    private String deadLetterExchange = "dmf.receiver.deadletter";
    private String receiverQueue = "dmf_receiver";
    private boolean missingQueuesFatal = false;

    public boolean isMissingQueuesFatal() {
        return missingQueuesFatal;
    }

    public void setMissingQueuesFatal(final boolean missingQueuesFatal) {
        this.missingQueuesFatal = missingQueuesFatal;
    }

    public String getDeadLetterExchange() {
        return deadLetterExchange;
    }

    public void setDeadLetterExchange(final String deadLetterExchange) {
        this.deadLetterExchange = deadLetterExchange;
    }

    public String getDeadLetterQueue() {
        return deadLetterQueue;
    }

    public void setDeadLetterQueue(final String deadLetterQueue) {
        this.deadLetterQueue = deadLetterQueue;
    }

    public String getReceiverQueue() {
        return receiverQueue;
    }

    public void setReceiverQueue(final String receiverQueue) {
        this.receiverQueue = receiverQueue;
    }
}
