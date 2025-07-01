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

import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import org.eclipse.hawkbit.repository.event.ApplicationEventFilter;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.event.remote.RemoteTenantAwareEvent;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.bus.BusProperties;
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

/**
 * Autoconfiguration for the event bus.
 */
@Configuration
@RemoteApplicationEventScan(basePackages = "org.eclipse.hawkbit.repository.event.remote")
@PropertySource("classpath:/hawkbit-eventbus-defaults.properties")
@EnableConfigurationProperties(BusProperties.class)
public class EventPublisherConfiguration {

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
    EventPublisherHolder eventBusHolder() {
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

        private ServiceMatcher serviceMatcher;

        protected TenantAwareApplicationEventPublisher(
                final SystemSecurityContext systemSecurityContext, final ApplicationEventFilter applicationEventFilter) {
            this.systemSecurityContext = systemSecurityContext;
            this.applicationEventFilter = applicationEventFilter;
        }

        @Autowired(required = false)
        public void setServiceMatcher(final ServiceMatcher serviceMatcher) {
            this.serviceMatcher = serviceMatcher;
        }

        /**
         * Was overridden that not every event has to run within an own tenantAware.
         */
        @Override
        public void multicastEvent(final ApplicationEvent event, final ResolvableType eventType) {
            if (applicationEventFilter.filter(event)) {
                return;
            }

            if (serviceMatcher == null || !(event instanceof final RemoteTenantAwareEvent remoteEvent)) {
                super.multicastEvent(event, eventType);
                return;
            }

            if (serviceMatcher.isFromSelf(remoteEvent)) {
                super.multicastEvent(event, eventType);
                return;
            }

            systemSecurityContext.runAsSystemAsTenant(() -> {
                super.multicastEvent(event, eventType);
                return null;
            }, remoteEvent.getTenant());
        }
    }

    @ConditionalOnBusEnabled
    @ConditionalOnClass({ Schema.class, ProtostuffIOUtil.class })
    protected static class BusProtoStuffAutoConfiguration {

        /**
         * @return the protostuff io message converter
         */
        @Bean
        public MessageConverter busProtoBufConverter() {
            return new BusProtoStuffMessageConverter();
        }
    }
}