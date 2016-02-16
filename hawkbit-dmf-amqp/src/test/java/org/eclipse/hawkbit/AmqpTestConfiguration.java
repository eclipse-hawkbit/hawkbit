/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit;

import org.eclipse.hawkbit.amqp.AmqpSenderService;
import org.eclipse.hawkbit.amqp.DefaultAmqpSenderService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 */
@Configuration
public class AmqpTestConfiguration {

    /**
     * Method to set the Jackson2JsonMessageConverter.
     *
     * @return the Jackson2JsonMessageConverter
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Create default amqp sender service bean.
     * 
     * @param rabbitTemplate
     *
     * @return the default amqp sender service bean
     */
    @Bean
    @Autowired
    public AmqpSenderService amqpSenderServiceBean(final RabbitTemplate rabbitTemplate) {
        return new DefaultAmqpSenderService(rabbitTemplate);
    }
}
