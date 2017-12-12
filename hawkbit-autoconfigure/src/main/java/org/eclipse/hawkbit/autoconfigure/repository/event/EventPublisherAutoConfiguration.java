/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.repository.event;

import java.util.concurrent.Executor;

import org.eclipse.hawkbit.event.BusProtoStuffMessageConverter;
import org.eclipse.hawkbit.repository.event.ApplicationEventFilter;
import org.eclipse.hawkbit.repository.event.remote.RemoteTenantAwareEvent;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.bus.ConditionalOnBusEnabled;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.messaging.converter.MessageConverter;

import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;

/**
 * Auto configuration for the event bus.
 *
 */
@Configuration
@RemoteApplicationEventScan(basePackages = "org.eclipse.hawkbit.repository.event.remote")
@PropertySource("classpath:/hawkbit-eventbus-defaults.properties")
public class EventPublisherAutoConfiguration {

    @Autowired
    @Qualifier("asyncExecutor")
    private Executor executor;

    @Autowired
    private TenantAware tenantAware;

    /**
     * Server internal event publisher that allows parallel event processing if
     * the event listener is marked as so.
     *
     * @return publisher bean
     */
    @Bean(name = AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME)
    public ApplicationEventMulticaster applicationEventMulticaster() {
        final SimpleApplicationEventMulticaster simpleApplicationEventMulticaster = new TenantAwareApplicationEventPublisher(
                tenantAware, applicationEventFilter());
        simpleApplicationEventMulticaster.setTaskExecutor(executor);
        return simpleApplicationEventMulticaster;
    }

    /**
     * Bean for creating a singleton instance of the
     * {@link EventPublisherHolder}
     * 
     * @return the singleton instance of the {@link EventPublisherHolder}
     */
    @Bean
    public EventPublisherHolder eventBusHolder() {
        return EventPublisherHolder.getInstance();
    }

    /**
     * @return default {@link ApplicationEventFilter} that does not filter any
     *         events
     */
    @Bean
    @ConditionalOnMissingBean
    public ApplicationEventFilter applicationEventFilter() {
        return e -> false;
    }

    private static class TenantAwareApplicationEventPublisher extends SimpleApplicationEventMulticaster {

        private final TenantAware tenantAware;

        private final ApplicationEventFilter applicationEventFilter;

        @Autowired(required = false)
        private ServiceMatcher serviceMatcher;

        /**
         * Constructor.
         * 
         * @param tenantAware
         *            the tenant ware
         */
        protected TenantAwareApplicationEventPublisher(final TenantAware tenantAware,
                final ApplicationEventFilter applicationEventFilter) {
            this.tenantAware = tenantAware;
            this.applicationEventFilter = applicationEventFilter;
        }

        /**
         * Was overridden that not every event has to run within a own
         * tenantAware.
         */
        @Override
        public void multicastEvent(final ApplicationEvent event, final ResolvableType eventType) {
            if (applicationEventFilter.filter(event)) {
                return;
            }

            if (serviceMatcher == null || !(event instanceof RemoteTenantAwareEvent)) {
                super.multicastEvent(event, eventType);
                return;
            }
            final RemoteTenantAwareEvent remoteEvent = (RemoteTenantAwareEvent) event;

            if (serviceMatcher.isFromSelf(remoteEvent)) {
                super.multicastEvent(event, eventType);
                return;
            }

            tenantAware.runAsTenant(remoteEvent.getTenant(), () -> {
                super.multicastEvent(event, eventType);
                return null;
            });
        }

    }

    @ConditionalOnBusEnabled
    @ConditionalOnClass({ Schema.class, ProtostuffIOUtil.class })
    protected static class BusProtoStuffAutoConfiguration {
        /**
         * 
         * @return the protostuff io message converter
         */
        @Bean
        public MessageConverter busProtoBufConverter() {
            return new BusProtoStuffMessageConverter();
        }

    }

}
