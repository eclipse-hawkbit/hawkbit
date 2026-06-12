/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rabbitmq.test;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

@Configuration
public class AmqpTestConfiguration {

    @Bean
    @Primary
    public RabbitTemplate rabbitTemplateForTest(final ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        rabbitTemplate.setReplyTimeout(TimeUnit.SECONDS.toMillis(3));
        rabbitTemplate.setReceiveTimeout(TimeUnit.SECONDS.toMillis(3));
        return rabbitTemplate;
    }

    @Bean
    Executor asyncExecutor() {
        return new DelegatingSecurityContextExecutorService(Executors.newSingleThreadExecutor());
    }

    @Bean
    TaskExecutor taskExecutor() {
        return new ConcurrentTaskExecutor(asyncExecutor());
    }

    @Bean
    ScheduledExecutorService scheduledExecutorService() {
        return threadPoolTaskScheduler().getScheduledExecutor();
    }

    @Bean
    ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    @Bean
    ConnectionFactory rabbitConnectionFactory(RabbitMqSetupService rabbitMqSetupService) {
        return rabbitMqSetupService.newVirtualHostWithConnectionFactory();
    }

    @Bean
    RabbitMqSetupService rabbitMqSetupService() {
        return new RabbitMqSetupService();
    }

    // note - it MUST be the same as DmfApiConfiguration#messageConverter for the test to work properly (to test the real AMQP)
    public static @NonNull Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter("org.eclipse.hawkbit.dmf.json.model") {

            @Override
            public @NonNull Object fromMessage(@NonNull final Message message, final @Nullable Object conversionHint) {
                if (message.getBody().length == 0) {
                    return message.getBody();
                } else {
                    return super.fromMessage(message, conversionHint);
                }
            }
        };
    }
}