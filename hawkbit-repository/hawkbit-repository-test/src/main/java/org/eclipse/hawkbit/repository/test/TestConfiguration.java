/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.test;

import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.hawkbit.ContextAware;
import org.eclipse.hawkbit.im.authentication.Hierarchy;
import org.eclipse.hawkbit.repository.artifact.filesystem.ArtifactFilesystemProperties;
import org.eclipse.hawkbit.repository.artifact.filesystem.ArtifactFilesystemRepository;
import org.eclipse.hawkbit.repository.artifact.ArtifactRepository;
import org.eclipse.hawkbit.repository.artifact.urlhandler.ArtifactUrlHandlerProperties;
import org.eclipse.hawkbit.repository.artifact.urlhandler.PropertyBasedArtifactUrlHandler;
import org.eclipse.hawkbit.cache.TenantAwareCacheManager;
import org.eclipse.hawkbit.repository.RolloutApprovalStrategy;
import org.eclipse.hawkbit.repository.RolloutStatusCache;
import org.eclipse.hawkbit.repository.event.ApplicationEventFilter;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyResolver;
import org.eclipse.hawkbit.repository.test.util.RolloutTestApprovalStrategy;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.eclipse.hawkbit.security.DdiSecurityProperties;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.security.SecurityContextSerializer;
import org.eclipse.hawkbit.security.SecurityContextTenantAware;
import org.eclipse.hawkbit.security.SecurityTokenGenerator;
import org.eclipse.hawkbit.security.SpringSecurityAuditorAware;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.tenancy.TenantAware.DefaultTenantResolver;
import org.eclipse.hawkbit.tenancy.TenantAware.TenantResolver;
import org.eclipse.hawkbit.tenancy.UserAuthoritiesResolver;
import org.eclipse.hawkbit.tenancy.configuration.ControllerPollProperties;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.data.domain.AuditorAware;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.concurrent.DelegatingSecurityContextScheduledExecutorService;

/**
 * Spring context configuration required for Dev.Environment.
 */
@Configuration
@EnableConfigurationProperties({
        DdiSecurityProperties.class, ArtifactUrlHandlerProperties.class, ArtifactFilesystemProperties.class,
        HawkbitSecurityProperties.class, ControllerPollProperties.class, TenantConfigurationProperties.class })
@Profile("test")
@EnableAutoConfiguration
@PropertySource("classpath:/hawkbit-test-defaults.properties")
public class TestConfiguration implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        return asyncExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }

    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        final AtomicLong count = new AtomicLong(0);
        return new DelegatingSecurityContextScheduledExecutorService(
                Executors.newScheduledThreadPool(1, runnable -> {
                    final Thread thread = Executors.defaultThreadFactory().newThread(runnable);
                    thread.setName(String.format(Locale.ROOT, "central-scheduled-executor-pool-%d", count.getAndIncrement()));
                    return thread;
                }));
    }

    /**
     * Disables caching during test to avoid concurrency failures during test.
     */
    @Bean
    RolloutStatusCache rolloutStatusCache(final TenantAware tenantAware) {
        return new RolloutStatusCache(tenantAware, 0);
    }

    @Bean
    LockRegistry lockRegistry() {
        return new DefaultLockRegistry();
    }

    @Bean
    SecurityTokenGenerator securityTokenGenerator() {
        return new SecurityTokenGenerator();
    }

    @Bean
    SystemSecurityContext systemSecurityContext(final TenantAware tenantAware) {
        return new SystemSecurityContext(tenantAware, RoleHierarchyImpl.fromHierarchy(Hierarchy.DEFAULT));
    }

    @Bean
    ArtifactRepository artifactRepository(final ArtifactFilesystemProperties artifactFilesystemProperties) {
        return new ArtifactFilesystemRepository(artifactFilesystemProperties);
    }

    /** @return the {@link org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch} to be injected. */
    @Bean
    SecurityContextSwitch securityContextSwitch() {
        return SecurityContextSwitch.getInstance();
    }

    @Bean
    PropertyBasedArtifactUrlHandler testPropertyBasedArtifactUrlHandler(final ArtifactUrlHandlerProperties urlHandlerProperties) {
        return new PropertyBasedArtifactUrlHandler(urlHandlerProperties, "");
    }

    @Bean
    UserAuthoritiesResolver authoritiesResolver() {
        return (tenant, username) -> Collections.emptyList();
    }

    @Bean
    SecurityContextSerializer securityContextSerializer() {
        return SecurityContextSerializer.JAVA_SERIALIZATION;
    }

    @Bean
    TenantResolver tenantResolver() {
        return new DefaultTenantResolver();
    }

    @Bean
    ContextAware contextAware(
            final UserAuthoritiesResolver authoritiesResolver, final SecurityContextSerializer securityContextSerializer,
            final TenantResolver tenantResolver) {
        // allow spying the security context
        return org.mockito.Mockito.spy(new SecurityContextTenantAware(authoritiesResolver, securityContextSerializer, tenantResolver));
    }

    @Bean
    TenantAwareCacheManager cacheManager(final TenantAware tenantAware) {
        return new TenantAwareCacheManager(new CaffeineCacheManager(), tenantAware);
    }

    @Bean(name = AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME)
    SimpleApplicationEventMulticaster applicationEventMulticaster(final ApplicationEventFilter applicationEventFilter) {
        final SimpleApplicationEventMulticaster simpleApplicationEventMulticaster =
                new FilterEnabledApplicationEventPublisher(applicationEventFilter);
        simpleApplicationEventMulticaster.setTaskExecutor(asyncExecutor());
        return simpleApplicationEventMulticaster;
    }

    @Bean
    EventPublisherHolder eventPublisherHolder() {
        return EventPublisherHolder.getInstance();
    }

    @Bean
    Executor asyncExecutor() {
        return new DelegatingSecurityContextExecutorService(Executors.newSingleThreadExecutor());
    }

    @Bean
    AuditorAware<String> auditorAware() {
        return new SpringSecurityAuditorAware();
    }

    /**
     * @return returns a VirtualPropertyReplacer
     */
    @Bean
    VirtualPropertyReplacer virtualPropertyReplacer() {
        return new VirtualPropertyResolver();
    }

    @Bean
    RolloutApprovalStrategy rolloutApprovalStrategy() {
        return new RolloutTestApprovalStrategy();
    }

    private static class FilterEnabledApplicationEventPublisher extends SimpleApplicationEventMulticaster {

        private final ApplicationEventFilter applicationEventFilter;

        FilterEnabledApplicationEventPublisher(final ApplicationEventFilter applicationEventFilter) {
            this.applicationEventFilter = applicationEventFilter;
        }

        @Override
        public void multicastEvent(final ApplicationEvent event, final ResolvableType eventType) {
            if (applicationEventFilter.filter(event)) {
                return;
            }

            super.multicastEvent(event, eventType);
        }
    }
}