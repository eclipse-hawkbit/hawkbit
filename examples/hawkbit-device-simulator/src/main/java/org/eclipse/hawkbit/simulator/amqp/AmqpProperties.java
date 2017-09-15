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
import org.springframework.stereotype.Component;

/**
 * Bean which holds the necessary properties for configuring the AMQP
 * connection.
 *
 */
@Component
@ConfigurationProperties("hawkbit.device.simulator.amqp")
public class AmqpProperties {

    /**
     * The prefix for this configuration.
     */
    public static final String CONFIGURATION_PREFIX = "hawkbit.device.simulator.amqp";

    /**
     * Indicates if the AMQP interface is enabled for the device simulator.
     */
    private boolean enabled;

    /**
     * Set to true for the simulator run DMF health check.
     */
    private boolean checkDmfHealth = false;

    /**
     * Queue for receiving DMF messages from update server.
     */
    private String receiverConnectorQueueFromSp = "simulator_receiver";

    /**
     * Exchange for sending DMF messages to update server.
     */
    private String senderForSpExchange = "simulator.replyTo";

    /**
     * Message time to live (ttl) for the deadletter queue. Default ttl is 1
     * hour.
     */
    private int deadLetterTtl = 60_000;

    public boolean isCheckDmfHealth() {
        return checkDmfHealth;
    }

    public void setCheckDmfHealth(final boolean checkDmfHealth) {
        this.checkDmfHealth = checkDmfHealth;
    }

    public String getReceiverConnectorQueueFromSp() {
        return receiverConnectorQueueFromSp;
    }

    public void setReceiverConnectorQueueFromSp(final String receiverConnectorQueueFromSp) {
        this.receiverConnectorQueueFromSp = receiverConnectorQueueFromSp;
    }

    public String getSenderForSpExchange() {
        return senderForSpExchange;
    }

    public void setSenderForSpExchange(final String senderForSpExchange) {
        this.senderForSpExchange = senderForSpExchange;
    }

    public int getDeadLetterTtl() {
        return deadLetterTtl;
    }

    public void setDeadLetterTtl(final int deadLetterTtl) {
        this.deadLetterTtl = deadLetterTtl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }
}
