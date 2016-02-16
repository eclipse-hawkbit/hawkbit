/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.dmf.amqp.api.AmqpSettings;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * The spring AMQP configuration which is enabled by using the profile
 * {@code amqp} to use a AMQP for communication with SP enabled devices.
 *
 *
 *
 */
@EnableConfigurationProperties(AmqpProperties.class)
public class AmqpConfiguration {

    @Autowired
    protected AmqpProperties amqpProperties;

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * Method to set the Jackson2JsonMessageConverter.
     *
     * @return the Jackson2JsonMessageConverter
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        final Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter);
        return jackson2JsonMessageConverter;
    }

    /**
     * Create the sp receiver queue.
     *
     * @return the receiver queue
     */
    @Bean
    public Queue receiverQueue() {
        return new Queue(amqpProperties.getReceiverQueue(), true, false, false, getDeadLetterExchangeArgs());
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
        return new Queue(amqpProperties.getDeadLetterQueue());
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
     * @return
     */
    @Bean
    public AmqpMessageHandlerService amqpMessageHandlerService() {
        return new AmqpMessageHandlerService(jsonMessageConverter(), rabbitTemplate);
    }

    /**
     * Create default amqp sender service bean.
     *
     * @return the default amqp sender service bean
     */
    @Bean
    @ConditionalOnMissingBean
    public AmqpSenderService amqpSenderServiceBean() {
        return new DefaultAmqpSenderService(rabbitTemplate);
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
        containerFactory.setMissingQueuesFatal(amqpProperties.isMissingQueuesFatal());
        return containerFactory;
    }

    private Map<String, Object> getDeadLetterExchangeArgs() {
        final Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", amqpProperties.getDeadLetterExchange());
        return args;
    }

}
