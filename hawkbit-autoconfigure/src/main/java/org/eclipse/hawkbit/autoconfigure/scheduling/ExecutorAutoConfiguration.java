/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.scheduling;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.concurrent.DelegatingSecurityContextScheduledExecutorService;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Central event processors inside update server.
 *
 */
@Configuration
@EnableConfigurationProperties(AsyncConfigurerThreadpoolProperties.class)
public class ExecutorAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorAutoConfiguration.class);

    @Autowired
    private AsyncConfigurerThreadpoolProperties asyncConfigurerProperties;

    /**
     * @return ExecutorService with security context availability in thread
     *         execution.
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public ExecutorService asyncExecutor() {
        return new DelegatingSecurityContextExecutorService(threadPoolExecutor());
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
     * @return central ThreadPoolExecutor for general purpose multi threaded
     *         operations. Tries an orderly shutdown when destroyed.
     */
    private ThreadPoolExecutor threadPoolExecutor() {
        final BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(
                asyncConfigurerProperties.getQueuesize());
        return new ThreadPoolExecutor(asyncConfigurerProperties.getCorethreads(),
                asyncConfigurerProperties.getMaxthreads(), asyncConfigurerProperties.getIdletimeout(),
                TimeUnit.MILLISECONDS, blockingQueue,
                new ThreadFactoryBuilder().setNameFormat("central-executor-pool-%d").build(),
                new PoolSizeExceededPolicy());
    }

    private static class PoolSizeExceededPolicy extends CallerRunsPolicy {
        @Override
        public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
            LOGGER.warn(
                    "Caller has to run on its own instead of centralExecutorService, reached limit of queue size {}",
                    executor.getQueue().size());
            super.rejectedExecution(r, executor);
        }
    }

    /**
     * @return the executor for UI background processes.
     */
    @Bean(name = "uiExecutor")
    @ConditionalOnMissingBean(name = "uiExecutor")
    public Executor uiExecutor() {
        final BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(20);
        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 20, 10000, TimeUnit.MILLISECONDS,
                blockingQueue, new ThreadFactoryBuilder().setNameFormat("ui-executor-pool-%d").build());
        threadPoolExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return new DelegatingSecurityContextExecutor(threadPoolExecutor);
    }

    /**
     * @return {@link ScheduledExecutorService} with security context
     *         availability in thread execution.
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public ScheduledExecutorService scheduledExecutorService() {
        return new DelegatingSecurityContextScheduledExecutorService(
                Executors.newScheduledThreadPool(asyncConfigurerProperties.getSchedulerThreads(),
                        new ThreadFactoryBuilder().setNameFormat("central-scheduled-executor-pool-%d").build()));
    }

    /**
     * @return {@link TaskScheduler} for task execution
     */
    @Bean
    @ConditionalOnMissingBean
    public TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler(scheduledExecutorService());
    }

}
