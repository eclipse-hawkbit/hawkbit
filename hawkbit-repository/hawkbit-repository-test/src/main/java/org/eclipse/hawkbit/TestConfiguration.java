/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties;
import org.eclipse.hawkbit.api.PropertyBasedArtifactUrlHandler;
import org.eclipse.hawkbit.cache.CacheConstants;
import org.eclipse.hawkbit.cache.TenancyCacheManager;
import org.eclipse.hawkbit.cache.TenantAwareCacheManager;
import org.eclipse.hawkbit.repository.jpa.model.helper.EventBusHolder;
import org.eclipse.hawkbit.repository.test.util.JpaTestRepositoryManagement;
import org.eclipse.hawkbit.repository.test.util.TestRepositoryManagement;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.eclipse.hawkbit.security.DdiSecurityProperties;
import org.eclipse.hawkbit.security.SecurityContextTenantAware;
import org.eclipse.hawkbit.security.SpringSecurityAuditorAware;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.guava.GuavaCacheManager;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.AuditorAware;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.mongodb.MongoClientOptions;

/**
 * Spring context configuration required for Dev.Environment.
 *
 *
 *
 */
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, mode = AdviceMode.PROXY, proxyTargetClass = false, securedEnabled = true)
@EnableConfigurationProperties({ HawkbitServerProperties.class, DdiSecurityProperties.class,
        ArtifactUrlHandlerProperties.class })
@Profile("test")
@EnableAutoConfiguration
public class TestConfiguration implements AsyncConfigurer {
    @Bean
    public TestRepositoryManagement testRepositoryManagement() {
        return new JpaTestRepositoryManagement();
    }

    @Bean
    public TestdataFactory testdataFactory() {
        return new TestdataFactory();
    }

    @Bean
    public PropertyBasedArtifactUrlHandler testPropertyBasedArtifactUrlHandler(
            final ArtifactUrlHandlerProperties urlHandlerProperties, final TenantAware tenantAware) {
        return new PropertyBasedArtifactUrlHandler(urlHandlerProperties, tenantAware);
    }

    @Bean
    public MongoClientOptions options() {
        return MongoClientOptions.builder().connectTimeout(500).maxWaitTime(500).connectionsPerHost(2)
                .serverSelectionTimeout(500).build();

    }

    @Bean
    public TenantAware tenantAware() {
        return new SecurityContextTenantAware();
    }

    @Bean
    public TenancyCacheManager cacheManager() {
        return new TenantAwareCacheManager(new GuavaCacheManager(), tenantAware());
    }

    /**
     * Bean for the download id cache.
     * 
     * @return the cache
     */
    @Bean(name = CacheConstants.DOWNLOAD_ID_CACHE)
    public Cache downloadIdCache() {
        return cacheManager().getDirectCache(CacheConstants.DOWNLOAD_ID_CACHE);
    }

    @Bean
    public EventBus eventBus() {
        return new AsyncEventBus(asyncExecutor());
    }

    @Bean
    public EventBusHolder eventBusHolder() {
        return EventBusHolder.getInstance();
    }

    @Bean
    public Executor asyncExecutor() {
        return new DelegatingSecurityContextExecutorService(Executors.newSingleThreadExecutor());
    }

    @Bean
    public AuditorAware<String> auditorAware() {
        return new SpringSecurityAuditorAware();
    }

    @Override
    public Executor getAsyncExecutor() {
        return asyncExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }

}
