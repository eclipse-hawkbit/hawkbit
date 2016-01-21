/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator.amqp;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The spring AMQP configuration to use a AMQP for communication with SP update
 * server.
 *
 *
 *
 */
@Configuration
@EnableConfigurationProperties(AmqpProperties.class)
public class AmqpConfiguration {

    @Autowired
    protected AmqpProperties amqpProperties;

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * Create jackson message converter bean.
     *
     * @return the jackson message converter
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        final Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter);
        return jackson2JsonMessageConverter;
    }

    /**
     * Create the receiver queue from sp. Receive messages from sp.
     *
     * @return the queue
     */
    @Bean
    public Queue receiverConnectorQueueFromSp() {
        return new Queue(amqpProperties.getReceiverConnectorQueueFromSp(), true, false, false,
                getDeadLetterExchangeArgs());
    }

    /**
     * Create the recevier exchange. Sp send messages to this exchange.
     *
     * @return the exchange
     */
    @Bean
    public FanoutExchange exchangeQueueToConnector() {
        return new FanoutExchange(amqpProperties.getSenderForSpExchange());
    }

    /**
     * Create the Binding
     * {@link AmqpConfiguration#receiverConnectorQueueFromSp()} to
     * {@link AmqpConfiguration#exchangeQueueToConnector()}.
     *
     * @return the binding and create the queue and exchange
     */
    @Bean
    public Binding bindReceiverQueueToSpExchange() {
        return BindingBuilder.bind(receiverConnectorQueueFromSp()).to(exchangeQueueToConnector());
    }

    /**
     * Create dead letter queue.
     *
     * @return the queue
     */
    @Bean
    public Queue deadLetterQueue() {
        return new Queue(amqpProperties.getDeadLetterQueue(), true, false, true);
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
     * Returns the Listener factory.
     *
     * @return the {@link SimpleMessageListenerContainer} that gets used receive
     *         AMQP messages
     */
    @Bean(name = { "listenerContainerFactory" })
    public SimpleRabbitListenerContainerFactory listenerContainerFactory() {
        final SimpleRabbitListenerContainerFactory containerFactory = new SimpleRabbitListenerContainerFactory();
        containerFactory.setDefaultRequeueRejected(false);
        containerFactory.setConnectionFactory(connectionFactory);
        containerFactory.setConcurrentConsumers(20);
        containerFactory.setMaxConcurrentConsumers(20);
        containerFactory.setPrefetchCount(20);
        return containerFactory;
    }

    private Map<String, Object> getDeadLetterExchangeArgs() {
        final Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", amqpProperties.getDeadLetterExchange());
        return args;
    }

}
