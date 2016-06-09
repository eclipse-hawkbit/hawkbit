/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import org.eclipse.hawkbit.dmf.amqp.api.AmqpSettings;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The spring AMQP configuration which is enabled by using the profile
 * {@code amqp} to use a AMQP for communication with SP enabled devices.
 *
 */
@EnableConfigurationProperties({ AmqpProperties.class, AmqpDeadletterProperties.class })
public class AmqpConfiguration {

    @Autowired
    protected AmqpProperties amqpProperties;

    @Autowired
    protected AmqpDeadletterProperties amqpDeadletterProperties;

    @Autowired
    private ConnectionFactory rabbitConnectionFactory;

    @Configuration
    protected static class HawkBitRabbitConnectionFactoryCreator {
        @Autowired
        @Qualifier("threadPoolExecutor")
        private ThreadPoolExecutor threadPoolExecutor;

        @Autowired
        private ScheduledExecutorService scheduledExecutorService;

        @Autowired
        protected AmqpProperties amqpProperties;

        @Bean
        public ConnectionFactory rabbitConnectionFactory(final RabbitProperties config) {
            final CachingConnectionFactory factory = new CachingConnectionFactory();
            factory.setRequestedHeartBeat(amqpProperties.getRequestedHeartBeat());
            factory.setExecutor(threadPoolExecutor);
            factory.getRabbitConnectionFactory().setHeartbeatExecutor(scheduledExecutorService);

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
     * Method to set the Jackson2JsonMessageConverter.
     *
     * @return the Jackson2JsonMessageConverter
     */
    @Bean
    public RabbitTemplate rabbitTemplate() {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(rabbitConnectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        return rabbitTemplate;
    }

    /**
     * Create the sp receiver queue.
     *
     * @return the receiver queue
     */
    @Bean
    public Queue receiverQueue() {
        return new Queue(amqpProperties.getReceiverQueue(), true, false, false,
                amqpDeadletterProperties.getDeadLetterExchangeArgs(amqpProperties.getDeadLetterExchange()));
    }

    /**
     * Create the dead letter fanout exchange.
     *
     * @return the fanout exchange
     */
    @Bean
    public FanoutExchange senderExchange() {
        return new FanoutExchange(AmqpSettings.DMF_EXCHANGE);
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
    public FanoutExchange exchangeDeadLetter() {
        return new FanoutExchange(amqpProperties.getDeadLetterExchange());
    }

    /**
     * Create the Binding deadLetterQueue to exchangeDeadLetter.
     *
     * @return the binding
     */
    @Bean
    public Binding bindDeadLetterQueueToLwm2mExchange() {
        return BindingBuilder.bind(deadLetterQueue()).to(exchangeDeadLetter());
    }

    /**
     * Create the Binding {@link AmqpConfiguration#receiverQueueFromSp()} to
     * {@link AmqpConfiguration#senderConnectorToSpExchange()}.
     *
     * @return the binding and create the queue and exchange
     */
    @Bean
    public Binding bindSenderExchangeToSpQueue() {
        return BindingBuilder.bind(receiverQueue()).to(senderExchange());
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
    public SimpleRabbitListenerContainerFactory listenerContainerFactory() {
        final SimpleRabbitListenerContainerFactory containerFactory = new SimpleRabbitListenerContainerFactory();
        containerFactory.setDefaultRequeueRejected(false);
        containerFactory.setConnectionFactory(rabbitConnectionFactory);
        containerFactory.setMissingQueuesFatal(amqpProperties.isMissingQueuesFatal());
        return containerFactory;
    }

}
