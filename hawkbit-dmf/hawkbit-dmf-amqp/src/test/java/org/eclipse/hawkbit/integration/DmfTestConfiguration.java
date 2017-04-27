/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.integration;

import org.eclipse.hawkbit.integration.listener.DeadletterListener;
import org.eclipse.hawkbit.integration.listener.ReplyToListener;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.test.RabbitListenerTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DMF Test configuration.
 */
@Configuration
@RabbitListenerTest
public class DmfTestConfiguration {

    public static final String REPLY_TO_EXCHANGE = "reply.queue";

    @Bean
    DeadletterListener deadletterListener() {
        return new DeadletterListener();
    }

    @Bean
    ReplyToListener replyToListener() {
        return new ReplyToListener();
    }

    @Bean
    Queue replyToQueue() {
        return new Queue(ReplyToListener.REPLY_TO_QUEUE, false, false, true);
    }

    @Bean
    FanoutExchange replyToExchange() {
        return new FanoutExchange(REPLY_TO_EXCHANGE, false, true);
    }

    @Bean
    Binding bindQueueToReplyToExchange() {
        return BindingBuilder.bind(replyToQueue()).to(replyToExchange());
    }

}
