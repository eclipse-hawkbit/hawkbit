/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.event;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import jakarta.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.event.ApplicationEventFilter;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.event.remote.AbstractRemoteEvent;
import org.eclipse.hawkbit.repository.event.remote.RemoteTenantAwareEvent;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.messaging.converter.MessageConverter;

/**
 * Autoconfiguration for the events.
 */
@Slf4j
@Configuration
public class EventPublisherConfiguration {

    private final ConfigurableEnvironment environment;

    public EventPublisherConfiguration(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void registerProperties() {
        try {
            ResourcePropertySource props = new ResourcePropertySource("classpath:/hawkbit-events-defaults.properties");
            // load manually to ensure that they are with the lowest precedence allowing to override them
            environment.getPropertySources().addLast(props);
        } catch (IOException ex) {
            log.error("Failed to load default properties for event publisher", ex);
        }
    }

    /**
     * Server internal event publisher that allows parallel event processing if the event listener is marked as so.
     *
     * @return publisher bean
     */
    @Bean(name = AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME)
    ApplicationEventMulticaster applicationEventMulticaster(
            @Qualifier("asyncExecutor") final Executor executor,
            final SystemSecurityContext systemSecurityContext, final ApplicationEventFilter applicationEventFilter) {
        final SimpleApplicationEventMulticaster simpleApplicationEventMulticaster =
                new TenantAwareApplicationEventPublisher(systemSecurityContext, applicationEventFilter);
        simpleApplicationEventMulticaster.setTaskExecutor(executor);
        return simpleApplicationEventMulticaster;
    }

    /**
     * Bean for creating a singleton instance of the {@link EventPublisherHolder}
     *
     * @return the singleton instance of the {@link EventPublisherHolder}
     */
    @Bean
    public EventPublisherHolder eventPublisherHolder() {
        return EventPublisherHolder.getInstance();
    }

    /**
     * @return default {@link ApplicationEventFilter} that does not filter any events
     */
    @Bean
    @ConditionalOnMissingBean
    ApplicationEventFilter applicationEventFilter() {
        return e -> false;
    }

    private static class TenantAwareApplicationEventPublisher extends SimpleApplicationEventMulticaster {

        private final SystemSecurityContext systemSecurityContext;
        private final ApplicationEventFilter applicationEventFilter;

        protected TenantAwareApplicationEventPublisher(
                final SystemSecurityContext systemSecurityContext, final ApplicationEventFilter applicationEventFilter) {
            this.systemSecurityContext = systemSecurityContext;
            this.applicationEventFilter = applicationEventFilter;
        }

        /**
         * Was overridden that not every event has to run within an own tenantAware.
         */
        @Override
        public void multicastEvent(final ApplicationEvent event, final ResolvableType eventType) {
            if (applicationEventFilter.filter(event)) {
                return;
            }

            if (event instanceof final RemoteTenantAwareEvent remoteEvent) {
                systemSecurityContext.runAsSystemAsTenant(() -> {
                    super.multicastEvent(event, eventType);
                    return null;
                }, remoteEvent.getTenant());
                return;
            }

            super.multicastEvent(event, eventType);
        }
    }

    @Bean
    @ConditionalOnProperty(name = "org.eclipse.hawkbit.events.remote-enabled", havingValue = "true")
    public Consumer<AbstractRemoteEvent> serviceEventConsumer(ApplicationEventPublisher publisher) {
        return publisher::publishEvent;
    }

    @Bean
    @ConditionalOnProperty(name = "org.eclipse.hawkbit.events.remote-enabled", havingValue = "true")
    public Consumer<AbstractRemoteEvent> fanoutEventConsumer(ApplicationEventPublisher publisher) {
        return publisher::publishEvent;
    }

    @Bean
    @ConditionalOnProperty(name = "org.eclipse.hawkbit.events.remote-enabled", havingValue = "true")
    public MessageConverter eventMessageConverter(BindingProperties bindingProperties) {

        final String contentType = bindingProperties.getContentType();

        if (contentType == null) {
            throw new IllegalStateException("RemoteEvents are enabled and Content type must be specified in spring.cloud.stream.default.content-type.");
        }

        if (contentType.equals("application/binary+protostuff")) {
            return new EventProtoStuffMessageConverter();
        } else if (contentType.equals("application/remote-event-json")) {
            return new EventJacksonMessageConverter();
        } else {
            throw new IllegalStateException("Unsupported content type: " + contentType + ". Supported types: application/x-protostuff, application/remote-event-json");
        }
    }
}