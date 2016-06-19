/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import java.util.concurrent.TimeUnit;

import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bean which holds the necessary properties for configuring the AMQP
 * connection.
 * 
 */
@ConfigurationProperties("hawkbit.dmf.rabbitmq")
public class AmqpProperties {
    /**
     * DMF API dead letter queue.
     */
    private String deadLetterQueue = "dmf_connector_deadletter_ttl";

    /**
     * DMF API dead letter exchange.
     */
    private String deadLetterExchange = "dmf.connector.deadletter";

    /**
     * DMF API receiving queue.
     */
    private String receiverQueue = "dmf_receiver";

    /**
     * Missing queue fatal.
     */
    private boolean missingQueuesFatal;

    /**
     * Requested heartbeat interval from broker in {@link TimeUnit#SECONDS}.
     */
    private int requestedHeartBeat = (int) TimeUnit.SECONDS.toSeconds(60);

    /**
     * Sets an upper limit to the number of consumers.
     */
    private int maxConcurrentConsumers = 10;

    /**
     * Tells the broker how many messages to send to each consumer in a single
     * request. Often this can be set quite high to improve throughput.
     */
    private int prefetchCount = 10;

    /**
     * Initial number of consumers. Is scaled up if necessary up to
     * {@link #maxConcurrentConsumers}.
     */
    private int initialConcurrentConsumers = 3;

    public int getPrefetchCount() {
        return prefetchCount;
    }

    public void setPrefetchCount(final int prefetchCount) {
        this.prefetchCount = prefetchCount;
    }

    public int getInitialConcurrentConsumers() {
        return initialConcurrentConsumers;
    }

    public void setInitialConcurrentConsumers(final int initialConcurrentConsumers) {
        this.initialConcurrentConsumers = initialConcurrentConsumers;
    }

    public int getMaxConcurrentConsumers() {
        return maxConcurrentConsumers;
    }

    public void setMaxConcurrentConsumers(final int maxConcurrentConsumers) {
        this.maxConcurrentConsumers = maxConcurrentConsumers;
    }

    /**
     * Is missingQueuesFatal enabled
     * 
     * @see SimpleMessageListenerContainer#setMissingQueuesFatal
     * @return the missingQueuesFatal <true> enabled <false> disabled
     */
    public boolean isMissingQueuesFatal() {
        return missingQueuesFatal;
    }

    /**
     * @param missingQueuesFatal
     *            the missingQueuesFatal to set.
     * @see SimpleMessageListenerContainer#setMissingQueuesFatal
     */
    public void setMissingQueuesFatal(final boolean missingQueuesFatal) {
        this.missingQueuesFatal = missingQueuesFatal;
    }

    /**
     * Returns the dead letter exchange.
     * 
     * @return dead letter exchange
     */
    public String getDeadLetterExchange() {
        return deadLetterExchange;
    }

    /**
     * Sets the dead letter exchange.
     * 
     * @param deadLetterExchange
     *            the deadLetterExchange to be set
     */
    public void setDeadLetterExchange(final String deadLetterExchange) {
        this.deadLetterExchange = deadLetterExchange;
    }

    /**
     * Returns the dead letter queue.
     * 
     * @return the dead letter queue
     */
    public String getDeadLetterQueue() {
        return deadLetterQueue;
    }

    /**
     * Sets the dead letter queue.
     * 
     * @param deadLetterQueue
     *            the deadLetterQueue ro be set
     */
    public void setDeadLetterQueue(final String deadLetterQueue) {
        this.deadLetterQueue = deadLetterQueue;
    }

    public String getReceiverQueue() {
        return receiverQueue;
    }

    public void setReceiverQueue(final String receiverQueue) {
        this.receiverQueue = receiverQueue;
    }

    public int getRequestedHeartBeat() {
        return requestedHeartBeat;
    }

    public void setRequestedHeartBeat(final int requestedHeartBeat) {
        this.requestedHeartBeat = requestedHeartBeat;
    }

}
