/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator.amqp;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;

import com.google.common.collect.Maps;

/**
 * The spring AMQP configuration to use a AMQP for communication with SP update
 * server.
 */
@Configuration
@EnableConfigurationProperties(AmqpProperties.class)
@ConditionalOnProperty(prefix = AmqpProperties.CONFIGURATION_PREFIX, name = "enabled")
public class AmqpConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmqpConfiguration.class);

    @Autowired
    protected AmqpProperties amqpProperties;

    @Autowired
    private ConnectionFactory connectionFactory;

    /**
     * @return {@link RabbitTemplate} with automatic retry, published confirms
     *         and {@link Jackson2JsonMessageConverter}.
     */
    @Bean
    public RabbitTemplate rabbitTemplate() {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());

        final RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setBackOffPolicy(new ExponentialBackOffPolicy());
        rabbitTemplate.setRetryTemplate(retryTemplate);

        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                LOGGER.debug("Message with correlation ID {} confirmed by broker.", correlationData.getId());
            } else {
                LOGGER.error("Broker is unable to handle message with correlation ID {} : {}", correlationData.getId(),
                        cause);
            }

        });

        return rabbitTemplate;
    }

    @Configuration
    protected static class RabbitConnectionFactoryCreator {

        /**
         * {@link ConnectionFactory} with enabled publisher confirms and
         * heartbeat.
         * 
         * @param config
         *            with standard {@link RabbitProperties}
         * @return {@link ConnectionFactory}
         */
        @Bean
        public ConnectionFactory rabbitConnectionFactory(final RabbitProperties config) {
            final CachingConnectionFactory factory = new CachingConnectionFactory();
            factory.setRequestedHeartBeat(60);
            factory.setPublisherConfirms(true);

            final String addresses = config.getAddresses();
            factory.setAddresses(addresses);
            if (config.getHost() != null) {
                factory.setHost(config.getHost());
                factory.setPort(config.getPort());
            }
            if (config.getUsername() != null) {
                factory.setUsername(config.getUsername());
            }
            if (config.getPassword() != null) {
                factory.setPassword(config.getPassword());
            }
            if (config.getVirtualHost() != null) {
                factory.setVirtualHost(config.getVirtualHost());
            }
            return factory;
        }
    }

    /**
     * Creates the receiver queue from update server for receiving message from
     * update server.
     *
     * @return the queue
     */
    @Bean
    public Queue receiverConnectorQueueFromHawkBit() {
        final Map<String, Object> arguments = getTTLMaxArgs();

        return QueueBuilder.nonDurable(amqpProperties.getReceiverConnectorQueueFromSp()).autoDelete()
                .withArguments(arguments).build();
    }

    /**
     * Creates the receiver exchange for sending messages to update server.
     *
     * @return the exchange
     */
    @Bean
    public FanoutExchange exchangeQueueToConnector() {
        return new FanoutExchange(amqpProperties.getSenderForSpExchange(), false, true);
    }

    /**
     * Create the Binding
     * {@link AmqpConfiguration#receiverConnectorQueueFromHawkBit()} to
     * {@link AmqpConfiguration#exchangeQueueToConnector()}.
     *
     * @return the binding and create the queue and exchange
     */
    @Bean
    public Binding bindReceiverQueueToSpExchange() {
        return BindingBuilder.bind(receiverConnectorQueueFromHawkBit()).to(exchangeQueueToConnector());
    }

    /**
     * Returns the Listener factory.
     *
     * @return the {@link SimpleMessageListenerContainer} that gets used receive
     *         AMQP messages
     */
    @Bean(name = { "listenerContainerFactory" })
    public SimpleRabbitListenerContainerFactory listenerContainerFactory() {
        final SimpleRabbitListenerContainerFactory containerFactory = new SimpleRabbitListenerContainerFactory();
        containerFactory.setDefaultRequeueRejected(true);
        containerFactory.setConnectionFactory(connectionFactory);
        containerFactory.setConcurrentConsumers(3);
        containerFactory.setMaxConcurrentConsumers(10);
        containerFactory.setPrefetchCount(20);
        return containerFactory;
    }

    private static Map<String, Object> getTTLMaxArgs() {
        final Map<String, Object> args = Maps.newHashMapWithExpectedSize(2);
        args.put("x-message-ttl", Duration.ofDays(1).toMillis());
        args.put("x-max-length", 100_000);
        return args;
    }

}
