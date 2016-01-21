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
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 *
 *
 */

@Configuration
@EnableConfigurationProperties(AsyncConfigurerThreadpoolProperties.class)
public class ExecutorAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorAutoConfiguration.class);

    @Autowired
    private AsyncConfigurerThreadpoolProperties asyncConfigurerProperties;

    /**
     * @return ExecutorService for general pupose multi threaded operations
     */
    @Bean
    @ConditionalOnMissingBean
    public Executor asyncExecutor() {
        final BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<Runnable>(
                asyncConfigurerProperties.getQueuesize());
        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                asyncConfigurerProperties.getCorethreads(), asyncConfigurerProperties.getMaxthreads(),
                asyncConfigurerProperties.getIdletimeout(), TimeUnit.MILLISECONDS, blockingQueue,
                new ThreadFactoryBuilder().setNameFormat("central-executor-pool-%d").build());
        threadPoolExecutor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(final Runnable r, final java.util.concurrent.ThreadPoolExecutor executor) {
                LOGGER.warn("Reject runnable for centralExecutorService, reached limit of queue size {}", executor
                        .getQueue().size());
            }
        });
        return new DelegatingSecurityContextExecutor(threadPoolExecutor);
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

}
