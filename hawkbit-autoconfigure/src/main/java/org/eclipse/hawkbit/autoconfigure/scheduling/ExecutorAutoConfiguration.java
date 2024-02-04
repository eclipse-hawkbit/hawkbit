/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.scheduling;

import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import lombok.extern.slf4j.Slf4j;
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

/**
 * Central event processors inside update server.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(AsyncConfigurerThreadpoolProperties.class)
public class ExecutorAutoConfiguration {

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
                threadFactory("central-executor-pool-%d"),
                new PoolSizeExceededPolicy());
    }

    private static class PoolSizeExceededPolicy extends CallerRunsPolicy {
        @Override
        public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
            log.warn(
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
                blockingQueue, threadFactory("ui-executor-pool-%d"));
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
                        threadFactory("central-scheduled-executor-pool-%d")));
    }

    /**
     * @return {@link TaskScheduler} for task execution
     */
    @Bean
    @ConditionalOnMissingBean
    public TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler(scheduledExecutorService());
    }

    private static ThreadFactory threadFactory(final String format) {
        final AtomicLong count = new AtomicLong(0);
        return (runnable) -> {
            final Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setName(String.format(Locale.ROOT, format, count.getAndIncrement()));
            return thread;
        };
    }
}
