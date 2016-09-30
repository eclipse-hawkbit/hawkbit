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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bean which holds the necessary properties for configuring the AMQP
 * connection.
 * 
 */
@ConfigurationProperties("hawkbit.dmf.rabbitmq")
public class AmqpProperties {

    private static final int ONE_MINUTE = 60;

    private static final int DEFAULT_QUEUE_DECLARATION_RETRIES = 50;

    private static final int DEFAULT_INITIAL_CONSUMERS = 3;

    private static final int DEFAULT_PREFETCH_COUNT = 10;

    private static final int DEFAULT_MAX_CONSUMERS = 10;

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
     * Requested heartbeat interval from broker in {@link TimeUnit#SECONDS}.
     */
    private int requestedHeartBeat = (int) TimeUnit.SECONDS.toSeconds(ONE_MINUTE);

    /**
     * Sets an upper limit to the number of consumers.
     */
    private int maxConcurrentConsumers = DEFAULT_MAX_CONSUMERS;

    /**
     * Tells the broker how many messages to send to each consumer in a single
     * request. Often this can be set quite high to improve throughput.
     */
    private int prefetchCount = DEFAULT_PREFETCH_COUNT;

    /**
     * Initial number of consumers. Is scaled up if necessary up to
     * {@link #maxConcurrentConsumers}.
     */
    private int initialConcurrentConsumers = DEFAULT_INITIAL_CONSUMERS;

    /**
     * The number of retry attempts when passive queue declaration fails.
     * Passive queue declaration occurs when the consumer starts or, when
     * consuming from multiple queues, when not all queues were available during
     * initialization.
     */
    private int declarationRetries = DEFAULT_QUEUE_DECLARATION_RETRIES;

    public int getDeclarationRetries() {
        return declarationRetries;
    }

    public void setDeclarationRetries(final int declarationRetries) {
        this.declarationRetries = declarationRetries;
    }

    public String getAuthenticationReceiverQueue() {
        return authenticationReceiverQueue;
    }

    public void setAuthenticationReceiverQueue(final String authenticationReceiverQueue) {
        this.authenticationReceiverQueue = authenticationReceiverQueue;
    }

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

    public int getRequestedHeartBeat() {
        return requestedHeartBeat;
    }

    public void setRequestedHeartBeat(final int requestedHeartBeat) {
        this.requestedHeartBeat = requestedHeartBeat;
    }

    public void setReceiverQueue(final String receiverQueue) {
        this.receiverQueue = receiverQueue;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }
}
