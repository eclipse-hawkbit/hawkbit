/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.hawkbit.RabbitMqSetupService.AlivenessException;
import org.eclipse.hawkbit.amqp.AmqpProperties;
import org.eclipse.hawkbit.api.HostnameResolver;
import org.eclipse.hawkbit.integration.listener.DeadletterListener;
import org.eclipse.hawkbit.integration.listener.ReplyToListener;
import org.eclipse.hawkbit.repository.jpa.model.helper.SystemSecurityContextHolder;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.junit.BrokerRunning;
import org.springframework.amqp.rabbit.test.RabbitListenerTest;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 *
 */
@Configuration
@EnableConfigurationProperties({ AmqpProperties.class })
@RabbitListenerTest
public class AmqpTestConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(AmqpTestConfiguration.class);

    public static final String REPLY_TO_EXCHANGE = "reply.queue";
    public static final String REPLY_TO_QUEUE = "reply_queue";

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
     * @return ExecutorService with security context availability in thread
     *         execution..
     */
    @Bean(destroyMethod = "shutdown")
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
    public TaskExecutor taskExecutor() {
        return new ConcurrentTaskExecutor(asyncExecutor());
    }

    /**
     * @return {@link ScheduledExecutorService} based on
     *         {@link #threadPoolTaskScheduler()}.
     */
    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        return threadPoolTaskScheduler().getScheduledExecutor();
    }

    /**
     * @return {@link ThreadPoolTaskScheduler} for scheduled operations.
     */
    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    @Bean
    public HostnameResolver hostnameResolver(final HawkbitServerProperties serverProperties) {
        return () -> {
            try {
                return new URL(serverProperties.getUrl());
            } catch (final MalformedURLException e) {
                throw Throwables.propagate(e);
            }
        };
    }

    @Bean(name = "dmfClient")
    public RabbitTemplate dmfClient(ConnectionFactory connectionFactory) {
        final RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setReceiveTimeout(TimeUnit.SECONDS.toMillis(5));
        return template;
    }

    @Bean
    public Queue replyToQueue() {
        return new Queue(REPLY_TO_QUEUE, false, false, true);
    }

    @Bean
    public FanoutExchange replyToExchange() {
        return new FanoutExchange(REPLY_TO_EXCHANGE, false, true);
    }

    @Bean
    public Binding bindQueueToReplyToExchange() {
        return BindingBuilder.bind(replyToQueue()).to(replyToExchange());
    }

    @Bean
    public DeadletterListener deadletterListener() {
        return new DeadletterListener();
    }

    @Bean
    public ReplyToListener replyToListener() {
        return new ReplyToListener();
    }

    @Bean
    public ConnectionFactory rabbitConnectionFactory(final RabbitMqSetupService rabbitmqSetupService)
            throws AlivenessException {
        final CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost(rabbitmqSetupService.getHostname());
        factory.setPort(5672);
        factory.setUsername(rabbitmqSetupService.getUsername());
        factory.setPassword(rabbitmqSetupService.getPassword());
        try {
            rabbitmqSetupService.createVirtualHost();
            factory.setVirtualHost(rabbitmqSetupService.getVirtualHost());

        } catch (final Exception e) {
            Throwables.propagateIfInstanceOf(e, AlivenessException.class);
            LOG.error("Cannot create virtual host {}", e.getMessage());
        }
        return factory;
    }

    @Bean
    public RabbitMqSetupService rabbitmqSetupService(RabbitProperties properties) {
        return new RabbitMqSetupService(properties);
    }

    @Bean
    public BrokerRunning brokerRunning(RabbitMqSetupService rabbitmqSetupService) {
        final BrokerRunning brokerRunning = BrokerRunning.isRunning();
        brokerRunning.setHostName(rabbitmqSetupService.getHostname());
        brokerRunning.getConnectionFactory().setUsername(rabbitmqSetupService.getUsername());
        brokerRunning.getConnectionFactory().setPassword(rabbitmqSetupService.getPassword());
        return brokerRunning;
    }

}
