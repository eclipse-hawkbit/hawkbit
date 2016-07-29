/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.hawkbit.dmf.amqp.api.AmqpSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.ErrorHandler;;

/**
 * The spring AMQP configuration which is enabled by using the profile
 * {@code amqp} to use a AMQP for communication with SP enabled devices.
 *
 */
@EnableConfigurationProperties({ AmqpProperties.class, AmqpDeadletterProperties.class })
public class AmqpConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmqpConfiguration.class);

    @Autowired
    private AmqpProperties amqpProperties;

    @Autowired
    private AmqpDeadletterProperties amqpDeadletterProperties;

    @Autowired
    private ConnectionFactory rabbitConnectionFactory;

    @Autowired
    private ErrorHandler errorHandler;

    @Configuration
    @ConditionalOnMissingBean(ConnectionFactory.class)
    protected static class RabbitConnectionFactoryCreator {

        @Autowired
        private AmqpProperties amqpProperties;

        @Autowired
        @Qualifier("asyncExecutor")
        private Executor threadPoolExecutor;

        @Autowired
        private ScheduledExecutorService scheduledExecutorService;

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
            factory.setRequestedHeartBeat(amqpProperties.getRequestedHeartBeat());
            factory.setExecutor(threadPoolExecutor);
            factory.getRabbitConnectionFactory().setHeartbeatExecutor(scheduledExecutorService);
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
     * @return {@link RabbitTemplate} with automatic retry, published confirms
     *         and {@link Jackson2JsonMessageConverter}.
     */
    @Bean
    public RabbitTemplate rabbitTemplate() {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(rabbitConnectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());

        final RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setBackOffPolicy(new ExponentialBackOffPolicy());
        rabbitTemplate.setRetryTemplate(retryTemplate);

        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                LOGGER.debug("Message with {} confirmed by broker.", correlationData);
            } else {
                LOGGER.error("Broker is unable to handle message with {} : {}", correlationData, cause);
            }
        });

        return rabbitTemplate;
    }

    /**
     * Create the DMF API receiver queue for retrieving DMF messages.
     *
     * @return the receiver queue
     */
    @Bean
    public Queue dmfReceiverQueue() {
        return new Queue(amqpProperties.getReceiverQueue(), true, false, false,
                amqpDeadletterProperties.getDeadLetterExchangeArgs(amqpProperties.getDeadLetterExchange()));
    }

    /**
     * Create the DMF API receiver queue for authentication requests called by
     * 3rd party artifact storages for download authorization by devices.
     *
     * @return the receiver queue
     */
    @Bean
    public Queue authenticationReceiverQueue() {
        return QueueBuilder.nonDurable(amqpProperties.getAuthenticationReceiverQueue()).autoDelete()
                .withArguments(getTTLMaxArgsAuthenticationQueue()).build();
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
     * Create the Binding {@link AmqpConfiguration#dmfReceiverQueue()} to
     * {@link AmqpConfiguration#dmfSenderExchange()}.
     *
     * @return the binding and create the queue and exchange
     */
    @Bean
    public Binding bindDmfSenderExchangeToDmfQueue() {
        return BindingBuilder.bind(dmfReceiverQueue()).to(dmfSenderExchange());
    }

    /**
     * Create authentication exchange.
     *
     * @return the fanout exchange
     */
    @Bean
    public FanoutExchange authenticationExchange() {
        return new FanoutExchange(AmqpSettings.AUTHENTICATION_EXCHANGE, false, true);
    }

    /**
     * Create the Binding
     * {@link AmqpConfiguration#authenticationReceiverQueue()} to
     * {@link AmqpConfiguration#authenticationExchange()}.
     *
     * @return the binding and create the queue and exchange
     */
    @Bean
    public Binding bindAuthenticationSenderExchangeToAuthenticationQueue() {
        return BindingBuilder.bind(authenticationReceiverQueue()).to(authenticationExchange());
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
    public Binding bindDeadLetterQueueToDeadLetterExchange() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange());
    }

    /**
     * Create amqp handler service bean.
     *
     * @return handler service bean
     */
    @Bean
    public AmqpMessageHandlerService amqpMessageHandlerService() {
        return new AmqpMessageHandlerService(rabbitTemplate());
    }

    /**
     * Create default amqp sender service bean.
     *
     * @return the default amqp sender service bean
     */
    @Bean
    @ConditionalOnMissingBean
    public AmqpSenderService amqpSenderServiceBean() {
        return new DefaultAmqpSenderService(rabbitTemplate());
    }

    /**
     * Returns the Listener factory.
     *
     * @return the {@link SimpleMessageListenerContainer} that gets used receive
     *         AMQP messages
     */
    @Bean(name = { "listenerContainerFactory" })
    public RabbitListenerContainerFactory<SimpleMessageListenerContainer> listenerContainerFactory() {
        return new ConfigurableRabbitListenerContainerFactory(amqpProperties, rabbitConnectionFactory, errorHandler);
    }

    private static Map<String, Object> getTTLMaxArgsAuthenticationQueue() {
        final Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", Duration.ofSeconds(30).toMillis());
        args.put("x-max-length", 1_000);
        return args;
    }

    /**
     * Create default error handler bean.
     * 
     * @return the default error handler bean
     */
    @Bean
    @ConditionalOnMissingBean
    public ErrorHandler errorHandler() {
        return new ConditionalRejectingErrorHandler();
    }

}
