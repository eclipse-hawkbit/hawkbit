/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.hawkbit.amqp.AmqpProperties;
import org.eclipse.hawkbit.amqp.AmqpSenderService;
import org.eclipse.hawkbit.amqp.DefaultAmqpSenderService;
import org.eclipse.hawkbit.repository.jpa.model.helper.SystemSecurityContextHolder;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 *
 */
@Configuration
@EnableConfigurationProperties({ AmqpProperties.class })
public class AmqpTestConfiguration {
    /**
     * @return the {@link SystemSecurityContext} singleton bean which make it
     *         accessible in beans which cannot access the service directly,
     *         e.g. JPA entities.
     */
    @Bean
    public SystemSecurityContextHolder systemSecurityContextHolder() {
        return SystemSecurityContextHolder.getInstance();
    }

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

    /**
     * @return ExecutorService with security context availability in thread
     *         execution..
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public Executor asyncExecutor() {
        return new DelegatingSecurityContextExecutorService(threadPoolExecutor());
    }

    /**
     * @return central ThreadPoolExecutor for general purpose multi threaded
     *         operations. Tries an orderly shutdown when destroyed.
     */
    private ThreadPoolExecutor threadPoolExecutor() {
        final BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(10);
        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 10, 1000, TimeUnit.MILLISECONDS,
                blockingQueue, new ThreadFactoryBuilder().setNameFormat("central-executor-pool-%d").build());

        return threadPoolExecutor;
    }

    /**
     * @return {@link TaskExecutor} for task execution
     */
    @Bean
    @ConditionalOnMissingBean
    public TaskExecutor taskExecutor() {
        return new ConcurrentTaskExecutor(asyncExecutor());
    }

    /**
     * @return {@link ScheduledExecutorService} based on
     *         {@link #threadPoolTaskScheduler()}.
     */
    @Bean
    @ConditionalOnMissingBean
    public ScheduledExecutorService scheduledExecutorService() {
        return threadPoolTaskScheduler().getScheduledExecutor();
    }

    /**
     * @return {@link ThreadPoolTaskScheduler} for scheduled operations.
     */
    @Bean
    @ConditionalOnMissingBean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        return new ThreadPoolTaskScheduler();
    }
}
