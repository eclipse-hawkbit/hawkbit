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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bean which holds the necessary properties for configuring the AMQP connection.
 */
@Data
@ConfigurationProperties("hawkbit.dmf.rabbitmq")
public class AmqpProperties {

    private static final int DEFAULT_QUEUE_DECLARATION_RETRIES = 50;

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
     * Authentication request called by 3rd party artifact storages for download authorizations.
     */
    private String authenticationReceiverQueue = "authentication_receiver";

    /**
     * Missing queue fatal.
     */
    private boolean missingQueuesFatal;

    /**
     * The number of retry attempts when passive queue declaration fails.
     * Passive queue declaration occurs when the consumer starts or, when consuming from multiple queues, when not all queues were
     * available during initialization.
     */
    private int declarationRetries = DEFAULT_QUEUE_DECLARATION_RETRIES;

    /**
     * Represents which {@link }SQLExceptions} should be considered fatal. By default, (without any configuration) it's simply disabled.
     */
    private final FatalSqlExceptionPolicy fatalSqlExceptionPolicy = new FatalSqlExceptionPolicy();

    @Data
    public static class FatalSqlExceptionPolicy {

        /**
         * The mode of the policy. If set to {@code true}, the every {@link java.sql.SQLException} would be assessed as fatal unless
         * matching the filters. Otherwise, every {@link java.sql.SQLException} will be assessed as non-fatal unless matching the filter.
         * The {@link java.sql.SQLException} that matches the filters are considered non-fatal if byDefault is {@code true} and fatal otherwise.
         */
        private boolean byDefault = false;
        /**
         * Error codes of the {@link java.sql.SQLException} that will be excluded from the default fatal policy. DB depended.
         */
        private final List<Integer> unlessErrorCodeIn = new ArrayList<>();
        /**
         * SQL states of the {@link java.sql.SQLException} that will be excluded from the default fatal policy. DB depended.
         */
        private final List<String> unlessSqlStateIn = new ArrayList<>();
        /**
         * Java regex message matching patterns. The {@link java.sql.SQLException} with messages matching any of the patterns
         * will be excluded from the default fatal policy. DB depended.
         */
        private final List<Pattern> unlessMessageMatches = new ArrayList<>();
    }
}