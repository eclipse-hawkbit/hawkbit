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
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.hawkbit.amqp.AmqpProperties;
import org.eclipse.hawkbit.amqp.AmqpSenderService;
import org.eclipse.hawkbit.amqp.DefaultAmqpSenderService;
import org.eclipse.hawkbit.api.HostnameResolver;
import org.eclipse.hawkbit.dmf.amqp.api.AmqpSettings;
import org.eclipse.hawkbit.integration.listener.DeadletterListener;
import org.eclipse.hawkbit.integration.listener.ReplyToListener;
import org.eclipse.hawkbit.repository.jpa.model.helper.SystemSecurityContextHolder;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.test.RabbitListenerTest;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    @Bean
    public Queue replyToQueue() {
        return new Queue(REPLY_TO_QUEUE, false, false, true);
    }

    @Bean
    public FanoutExchange replyToExchange() {
        return new FanoutExchange(REPLY_TO_EXCHANGE, false, true);
    }

    @Bean
    public Binding bindReplyToQueueToReplyTorExchange() {
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

    @Configuration
    @ConditionalOnProperty(prefix = "hawkbit.dmf.rabbitmq", name = "enabled", matchIfMissing = true)
    public static class RabbitConnectionFactoryCreator {

        @Autowired
        private RabbitProperties rabbitProperties;

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
         * @throws GeneralSecurityException
         *             in case of problems with enabled SSL connections
         * @throws URISyntaxException
         * @throws MalformedURLException
         */
        @Bean
        // @RefreshScope
        public ConnectionFactory rabbitConnectionFactory(final RabbitProperties config,
                final AmqpVHostService amqpVHostHelper)
                throws GeneralSecurityException, MalformedURLException, URISyntaxException {

            final CachingConnectionFactory factory = new CachingConnectionFactory();
            factory.setRequestedHeartBeat(amqpProperties.getRequestedHeartBeat());
            factory.setExecutor(threadPoolExecutor);
            factory.getRabbitConnectionFactory().setHeartbeatExecutor(scheduledExecutorService);
            factory.setPublisherConfirms(true);

            if (config.getSsl().isEnabled()) {
                factory.getRabbitConnectionFactory().useSslProtocol();
            }

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

            config.setVirtualHost(amqpVHostHelper.generateNewVHost(rabbitProperties.getHost(),
                    rabbitProperties.getUsername(), rabbitProperties.getPassword()));

            if (config.getVirtualHost() != null) {
                factory.setVirtualHost(config.getVirtualHost());
            }
            return factory;
        }

    }

    @Bean
    public AmqpVHostService amqpVHostHelper() {
        return new AmqpVHostService();
    }

    // TODO look if we can start the same bean with different exchange
    @Bean
    public RabbitTemplate authenticationClient(final RabbitProperties rabbitProperties,
            final AmqpVHostService amqpVHostHelper) {
        final RabbitTemplate template = new RabbitTemplate(rabbitConnectionFactory(rabbitProperties, amqpVHostHelper));
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setReceiveTimeout(TimeUnit.SECONDS.toMillis(35));
        // TODO has to be a variable at runtime getting string within test
        template.setExchange(AmqpSettings.AUTHENTICATION_EXCHANGE);
        return template;
    }

    @Bean
    public RabbitTemplate dmfClient(final RabbitProperties rabbitProperties, final AmqpVHostService amqpVHostHelper) {
        final RabbitTemplate template = new RabbitTemplate(rabbitConnectionFactory(rabbitProperties, amqpVHostHelper));
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setReceiveTimeout(TimeUnit.SECONDS.toMillis(35));
        // TODO has to be a variable at runtime getting string within test
        template.setExchange(AmqpSettings.DMF_EXCHANGE);
        return template;
    }

    private ConnectionFactory rabbitConnectionFactory(final RabbitProperties rabbitProperties,
            final AmqpVHostService amqpVHostHelper) {
        final CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(rabbitProperties.getHost());
        connectionFactory.setVirtualHost(amqpVHostHelper.getCurrentVhost());
        return connectionFactory;
    }

}
