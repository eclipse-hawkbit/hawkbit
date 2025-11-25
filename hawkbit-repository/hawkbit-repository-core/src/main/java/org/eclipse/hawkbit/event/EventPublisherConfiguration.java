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

import java.util.concurrent.Executor;
import java.util.function.Consumer;

import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.context.System;
import org.eclipse.hawkbit.repository.event.ApplicationEventFilter;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.event.remote.AbstractRemoteEvent;
import org.eclipse.hawkbit.repository.event.remote.RemoteTenantAwareEvent;
import org.eclipse.hawkbit.repository.event.remote.service.AbstractServiceRemoteEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.messaging.converter.MessageConverter;

/**
 * Autoconfiguration for the events.
 */
@Slf4j
@Configuration
public class EventPublisherConfiguration {

    /**
     * Server internal event publisher that allows parallel event processing if the event listener is marked as so.
     *
     * @return publisher bean
     */
    @Bean(name = AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME)
    ApplicationEventMulticaster applicationEventMulticaster(
            @Qualifier("asyncExecutor") final Executor executor, final ApplicationEventFilter applicationEventFilter) {
        final SimpleApplicationEventMulticaster simpleApplicationEventMulticaster =
                new TenantAwareApplicationEventPublisher(applicationEventFilter);
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

        private final ApplicationEventFilter applicationEventFilter;

        protected TenantAwareApplicationEventPublisher(final ApplicationEventFilter applicationEventFilter) {
            this.applicationEventFilter = applicationEventFilter;
        }

        /**
         * Was overridden that not every event has to run within an own tenant.
         */
        @Override
        public void multicastEvent(final ApplicationEvent event, final ResolvableType eventType) {
            if (applicationEventFilter.filter(event)) {
                return;
            }

            if (event instanceof final RemoteTenantAwareEvent remoteEvent) {
                System.asSystemAsTenant(remoteEvent.getTenant(), () -> {
                    super.multicastEvent(event, eventType);
                    return null;
                });
                return;
            }

            if (event instanceof final AbstractServiceRemoteEvent<?> serviceRemoteEvent
                    && serviceRemoteEvent.getRemoteEvent() instanceof RemoteTenantAwareEvent tenantAwareEvent) {
                System.asSystemAsTenant(tenantAwareEvent.getTenant(), () -> {
                    super.multicastEvent(event, eventType);
                    return null;
                });
                return;
            }

            super.multicastEvent(event, eventType);
        }
    }

    @Bean
    public Consumer<AbstractRemoteEvent> serviceEventConsumer(ApplicationEventPublisher publisher) {
        return publisher::publishEvent;
    }

    @Bean
    public Consumer<AbstractRemoteEvent> fanoutEventConsumer(ApplicationEventPublisher publisher) {
        return publisher::publishEvent;
    }

    @ConditionalOnClass({ Schema.class, ProtostuffIOUtil.class })
    protected static class EventProtostuffConfiguration {

        @Bean
        public MessageConverter eventProtostuffMessageConverter() {
            return new EventProtoStuffMessageConverter();
        }
    }

    @Bean
    public MessageConverter eventJacksonMessageConverter() {
        return new EventJacksonMessageConverter();
    }
}