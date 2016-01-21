/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator.amqp;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bean which holds the necessary properties for configuring the AMQP
 * connection.
 *
 *
 *
 */
@ConfigurationProperties("hawkbit.device.simulator.amqp")
public class AmqpProperties {

    private String receiverConnectorQueueFromSp;
    private String senderForSpExchange;

    private String deadLetterQueue;
    private String deadLetterExchange;

    public String getReceiverConnectorQueueFromSp() {
        return receiverConnectorQueueFromSp;
    }

    public void setReceiverConnectorQueueFromSp(final String receiverConnectorQueueFromSp) {
        this.receiverConnectorQueueFromSp = receiverConnectorQueueFromSp;
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

    public String getSenderForSpExchange() {
        return senderForSpExchange;
    }

    public void setSenderForSpExchange(final String senderForSpExchange) {
        this.senderForSpExchange = senderForSpExchange;
    }
}
