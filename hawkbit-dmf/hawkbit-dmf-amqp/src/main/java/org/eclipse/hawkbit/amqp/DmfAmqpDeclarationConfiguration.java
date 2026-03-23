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

import org.eclipse.hawkbit.dmf.amqp.api.AmqpSettings;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-declaration of DMF AMQP infrastructure (queues, exchanges, bindings and {@link RabbitAdmin}).
 * <p>
 * Imported by {@link DmfApiConfiguration} — not a {@code @Configuration} class, so it is not
 * discoverable by component scanning independently. The {@code hawkbit.dmf.enabled} guard
 * is inherited from the importing configuration.
 * <p>
 * Set {@code hawkbit.dmf.rabbitmq.auto-declare=false} to disable auto-declaration.
 */
@ConditionalOnProperty(prefix = "hawkbit.dmf.rabbitmq", name = "auto-declare", matchIfMissing = true)
@EnableConfigurationProperties({ AmqpProperties.class, AmqpDeadletterProperties.class })
public class DmfAmqpDeclarationConfiguration {

    private final AmqpProperties amqpProperties;
    private final AmqpDeadletterProperties amqpDeadletterProperties;
    private final ConnectionFactory rabbitConnectionFactory;

    DmfAmqpDeclarationConfiguration(
            final AmqpProperties amqpProperties,
            final AmqpDeadletterProperties amqpDeadletterProperties,
            final ConnectionFactory rabbitConnectionFactory) {
        this.amqpProperties = amqpProperties;
        this.amqpDeadletterProperties = amqpDeadletterProperties;
        this.rabbitConnectionFactory = rabbitConnectionFactory;
    }

    /**
     * Create a {@link RabbitAdmin} and ignore declaration exceptions.
     * {@link RabbitAdmin#setIgnoreDeclarationExceptions(boolean)}
     *
     * @return the bean
     */
    @Bean
    public RabbitAdmin rabbitAdmin() {
        final RabbitAdmin rabbitAdmin = new RabbitAdmin(rabbitConnectionFactory);
        rabbitAdmin.setIgnoreDeclarationExceptions(true);
        return rabbitAdmin;
    }

    /**
     * Create the DMF API receiver queue for retrieving DMF messages.
     *
     * @return the receiver queue
     */
    @Bean
    public Queue dmfReceiverQueue() {
        return new Queue(
                amqpProperties.getReceiverQueue(),
                true, false, false,
                amqpDeadletterProperties.getDeadLetterExchangeArgs(amqpProperties.getDeadLetterExchange()));
    }

    /**
     * Create the DMF API receiver queue for authentication requests called by 3rd
     * party artifact storages for download authorization by devices.
     *
     * @return the receiver queue
     */
    @Bean
    public Queue authenticationReceiverQueue() {
        return QueueBuilder.nonDurable(amqpProperties.getAuthenticationReceiverQueue())
                .autoDelete()
                .withArguments(getTTLMaxArgsAuthenticationQueue())
                .build();
    }

    /**
     * Create DMF exchange.
     *
     * @return the fanout exchange
     */
    @Bean
    public FanoutExchange dmfSenderExchange() {
        return new FanoutExchange(AmqpSettings.DMF_EXCHANGE);
    }

    /**
     * Create the Binding dmfReceiverQueue to dmfSenderExchange.
     *
     * @return the binding and create the queue and exchange
     */
    @Bean
    public Binding bindDmfSenderExchangeToDmfQueue(final Queue dmfReceiverQueue, final FanoutExchange dmfSenderExchange) {
        return BindingBuilder.bind(dmfReceiverQueue).to(dmfSenderExchange);
    }

    /**
     * Create dead letter queue.
     *
     * @return the queue
     */
    @Bean
    public Queue deadLetterQueue() {
        return amqpDeadletterProperties.createDeadletterQueue(amqpProperties.getDeadLetterQueue());
    }

    /**
     * Create the dead letter fanout exchange.
     *
     * @return the fanout exchange
     */
    @Bean
    public FanoutExchange deadLetterExchange() {
        return new FanoutExchange(amqpProperties.getDeadLetterExchange());
    }

    /**
     * Create the Binding deadLetterQueue to deadLetterExchange.
     *
     * @return the binding
     */
    @Bean
    public Binding bindDeadLetterQueueToDeadLetterExchange(final Queue deadLetterQueue, final FanoutExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange);
    }

    private static Map<String, Object> getTTLMaxArgsAuthenticationQueue() {
        final Map<String, Object> args = new HashMap<>(2);
        args.put("x-message-ttl", Duration.ofSeconds(30).toMillis());
        args.put("x-max-length", 1_000);
        return args;
    }
}
