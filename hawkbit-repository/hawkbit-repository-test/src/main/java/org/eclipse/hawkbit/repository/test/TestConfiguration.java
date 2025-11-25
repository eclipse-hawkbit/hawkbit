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

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.hawkbit.artifact.ArtifactStorage;
import org.eclipse.hawkbit.artifact.fs.FileArtifactProperties;
import org.eclipse.hawkbit.artifact.fs.FileArtifactStorage;
import org.eclipse.hawkbit.artifact.urlresolver.PropertyBasedArtifactUrlResolver;
import org.eclipse.hawkbit.artifact.urlresolver.PropertyBasedArtifactUrlResolverProperties;
import org.eclipse.hawkbit.auth.Hierarchy;
import org.eclipse.hawkbit.context.Auditor;
import org.eclipse.hawkbit.repository.RepositoryConfiguration;
import org.eclipse.hawkbit.repository.RolloutApprovalStrategy;
import org.eclipse.hawkbit.repository.event.ApplicationEventFilter;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyResolver;
import org.eclipse.hawkbit.repository.test.util.RolloutTestApprovalStrategy;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.eclipse.hawkbit.security.DdiSecurityProperties;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.security.SecurityTokenGenerator;
import org.eclipse.hawkbit.tenancy.configuration.ControllerPollProperties;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
        DdiSecurityProperties.class, PropertyBasedArtifactUrlResolverProperties.class, FileArtifactProperties.class,
        HawkbitSecurityProperties.class, ControllerPollProperties.class, TenantConfigurationProperties.class })
@Profile("test")
@EnableAutoConfiguration
@PropertySource("classpath:/hawkbit-test-defaults.properties")
public class TestConfiguration implements AsyncConfigurer {

    @Configuration
    @Import(RepositoryConfiguration.class)
    static class OverridePropertiesSourceFromRepositoryConfiguration {}

    static {
        Hierarchy.setRoleHierarchy(RoleHierarchyImpl.fromHierarchy(Hierarchy.DEFAULT));
    }

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

    @Bean
    LockRegistry lockRegistry() {
        return new DefaultLockRegistry();
    }

    @Bean
    SecurityTokenGenerator securityTokenGenerator() {
        return new SecurityTokenGenerator();
    }

    @Bean
    ArtifactStorage artifactStorage(final FileArtifactProperties artifactFilesystemProperties) {
        return new FileArtifactStorage(artifactFilesystemProperties);
    }

    /** @return the {@link org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch} to be injected. */
    @Bean
    SecurityContextSwitch securityContextSwitch() {
        return SecurityContextSwitch.getInstance();
    }

    @Bean
    PropertyBasedArtifactUrlResolver testPropertyBasedArtifactUrlHandler(
            final PropertyBasedArtifactUrlResolverProperties urlHandlerProperties) {
        return new PropertyBasedArtifactUrlResolver(urlHandlerProperties, "");
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
        return () -> Optional.ofNullable(Auditor.currentAuditor());
    }

    @Bean
    VirtualPropertyResolver virtualPropertyResolver() {
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