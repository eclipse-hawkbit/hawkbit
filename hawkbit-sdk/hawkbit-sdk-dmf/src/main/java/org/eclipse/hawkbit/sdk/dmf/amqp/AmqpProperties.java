/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk.dmf.amqp;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bean which holds the necessary properties for configuring the AMQP connection.
 */
@Data
@ToString
@ConfigurationProperties(AmqpProperties.CONFIGURATION_PREFIX)
public class AmqpProperties {

    /**
     * The prefix for this configuration.
     */
    public static final String CONFIGURATION_PREFIX = "hawkbit.sdk.dmf.amqp";

    /**
     * Queue for receiving DMF messages from update server.
     */
    private String receiverConnectorQueueFromSp = "sdk_receiver";

    /**
     * Exchange for sending DMF messages to update server.
     */
    private String senderForSpExchange = "sdk.replyTo";

    /**
     * Message time to live (ttl) for the deadletter queue. Default ttl is 1 hour.
     */
    private int deadLetterTtl = 60_000;
}