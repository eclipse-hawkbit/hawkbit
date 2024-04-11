/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk.dmf;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Bean which holds the necessary properties for configuring the AMQP connection.
 */
@Data
@ToString
@Component
@ConfigurationProperties(DmfProperties.CONFIGURATION_PREFIX)
public class DmfProperties {

    /**
     * The prefix for this configuration.
     */
    public static final String CONFIGURATION_PREFIX = "hawkbit.sdk.dmf.amqp";

    /**
     * The property string of ~.amqp.enabled
     */
    public static final String CONFIGURATION_ENABLED_PROPERTY = CONFIGURATION_PREFIX + ".enabled";

    /**
     * Indicates if the AMQP interface is enabled for the device simulator.
     */
    private boolean enabled;

    /**
     * Set to true for the simulator run DMF health check.
     */
    private boolean healthCheckEnabled;

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

    private String customVhost;
}